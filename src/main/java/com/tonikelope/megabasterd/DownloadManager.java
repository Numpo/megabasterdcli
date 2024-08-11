/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.CryptTools.decryptMegaDownloaderLink;
import static com.tonikelope.megabasterd.DBTools.deleteDownloads;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.findAllRegex;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static java.util.logging.Level.SEVERE;

/**
 * @author tonikelope
 */
public class DownloadManager extends TransferenceManager {

    private static final Logger LOG = Logger.getLogger(DownloadManager.class.getName());

    public DownloadManager(final MainPanel main_panel) {

        super(main_panel, main_panel.getMax_dl(), main_panel.getView().getStatus_down_label(), main_panel.getView().getjPanel_scroll_down(), main_panel.getView().getClose_all_finished_down_button(), main_panel.getView().getPause_all_down_button(), main_panel.getView().getClean_all_down_menu());
    }

    public DownloadManager(final MainPanel main_panel, final String toto) {
        super(main_panel);
    }

    public void download(final String link_data, final String dl_path) {
        LOG.info("Starting download: " + link_data);

        final MegaAPI ma;

        ma = new MegaAPI();

        this.getMain_panel().resumeDownloadsCli();

        final Runnable run = () -> {
            // Generate URLs Set
            final Set<String> urls = new HashSet(findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", link_data, 0));

            final Set<String> megadownloader = new HashSet(findAllRegex("mega://enc[^\r\n]+", link_data, 0));

            megadownloader.forEach((link) -> {
                try {
                    urls.add(decryptMegaDownloaderLink(link));
                } catch (final Exception ex) {
                    LOG.log(SEVERE, null, ex);
                }
            });

            final Set<String> elc = new HashSet(findAllRegex("mega://elc[^\r\n]+", link_data, 0));

            elc.forEach((link) -> {
                try {
                    urls.addAll(CryptTools.decryptELC(link, this.getMain_panel()));
                } catch (final Exception ex) {
                    LOG.log(SEVERE, null, ex);
                }
            });

            final Set<String> dlc = new HashSet(findAllRegex("dlc://([^\r\n]+)", link_data, 1));

            dlc.stream().map((d) -> CryptTools.decryptDLC(d, null)).forEachOrdered((links) -> {
                links.stream().filter((link) -> (findFirstRegex("(?:https?|mega)://[^\r\n](#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", link, 0) != null)).forEachOrdered((link) -> {
                    urls.add(link);
                });
            });

            // Handle URLs
            if (!urls.isEmpty()) {
                // Add folders to URLs Set
                final Set<String> folder_file_links = new HashSet(findAllRegex("(?:https?|mega)://[^\r\n]+#F\\*[^\r\n!]*?![^\r\n!]+![^\\?\r\n/]+", link_data, 0));

                this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().addAll(folder_file_links);
                this.getMain_panel().getDownload_manager().secureNotify();

                if (!folder_file_links.isEmpty()) {
                    final ArrayList<String> nlinks = ma.GENERATE_N_LINKS(folder_file_links);
                    urls.removeAll(folder_file_links);
                    urls.addAll(nlinks);
                }

                this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().removeAll(folder_file_links);
                this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().addAll(urls);
                this.getMain_panel().getDownload_manager().secureNotify();

                boolean link_warning;

                for (String url : urls) {
                    try {
                        link_warning = false;
                        url = URLDecoder.decode(url, "UTF-8").replaceAll("^mega://", "https://mega.nz").trim();

                        final Download download;

                        if (findFirstRegex("#F!", url, 0) != null) {
                            // Download Folders
                            // TODO
                        } else {
                            // Download Files
                            while (this.getMain_panel().getDownload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || this.getMain_panel().getDownload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {
                                synchronized (this.getMain_panel().getDownload_manager().getWait_queue_lock()) {
                                    this.getMain_panel().getDownload_manager().getWait_queue_lock().wait(1000);
                                }
                            }

                            download = new Download(ma, url, dl_path, null, null, null, null, null, false, false, null, false);
                            download.set_main_panel(this.getMain_panel());
                            this.getMain_panel().getDownload_manager().getTransference_provision_queue().add(download);
                            this.getMain_panel().getDownload_manager().secureNotify();
                        }
                        this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().remove(url);
                        this.getMain_panel().getDownload_manager().secureNotify();
                    } catch (final UnsupportedEncodingException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    } catch (final InterruptedException ex) {
                        Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, ex.getMessage());
                    }
                }
            }
        };
        this.getMain_panel().getDownload_manager().getTransference_preprocess_queue().add(run);
        this.getMain_panel().getDownload_manager().secureNotify();
    }

    public synchronized void forceResetAllChunks() {
        THREAD_POOL.execute(() -> {

            final ConcurrentLinkedQueue<Transference> transference_running_list = this.getMain_panel().getDownload_manager().getTransference_running_list();

            if (!transference_running_list.isEmpty()) {
                transference_running_list.forEach((transference) -> {

                    final ArrayList<ChunkDownloader> chunkworkers = ((Download) transference).getChunkworkers();

                    chunkworkers.forEach((worker) -> {
                        worker.RESET_CURRENT_CHUNK();
                    });

                });

                MiscTools.GUIRun(() -> {
                    this.getMain_panel().getView().getForce_chunk_reset_button().setEnabled(true);
                });

//                JOptionPane.showMessageDialog(this.getMain_panel().getView(), LabelTranslatorSingleton.getInstance().translate("CURRENT DOWNLOAD CHUNKS RESET!"));
            }

        });
    }

    @Override
    public void closeAllFinished() {

        this._transference_finished_queue.stream().filter((t) -> (!t.isCanceled())).map((t) -> {
            this._transference_finished_queue.remove(t);
            return t;
        }).forEachOrdered((t) -> {
            this._transference_remove_queue.add(t);
        });

        this.secureNotify();
    }

    public int copyAllLinksToClipboard() {

        int total = 0;

        final ArrayList<String> links = new ArrayList<>();

        String out = "***PROVISIONING DOWNLOADS***\r\n\r\n";

        for (final Transference t : this._transference_provision_queue) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        out += "\r\n\r\n***WAITING DOWNLOADS***\r\n\r\n";

        for (final Transference t : this._transference_waitstart_aux_queue) {

            links.add(((Download) t).getUrl());
        }

        for (final Transference t : this._transference_waitstart_queue) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        out += "\r\n\r\n***RUNNING DOWNLOADS***\r\n\r\n";

        for (final Transference t : this._transference_running_list) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        out += "\r\n\r\n***FINISHED DOWNLOADS***\r\n\r\n";

        for (final Transference t : this._transference_finished_queue) {

            links.add(((Download) t).getUrl());
        }

        out += String.join("\r\n", links);

        total += links.size();

        MiscTools.copyTextToClipboard(out);

        return total;

    }

    @Override
    public void remove(final Transference[] downloads) {

        final ArrayList<String> delete_down = new ArrayList<>();

        for (final Transference d : downloads) {

//            MiscTools.GUIRun(() -> {
//                this.getScroll_panel().remove(((Download) d).getView());
//            });

            this.getTransference_waitstart_queue().remove(d);

            this.getTransference_running_list().remove(d);

            this.getTransference_finished_queue().remove(d);

            if (((Download) d).isProvision_ok()) {

                this.increment_total_size(-1 * d.getFile_size());

                this.increment_total_progress(-1 * d.getProgress());

                if (!d.isCanceled() || d.isClosed()) {
                    delete_down.add(((Download) d).getUrl());
                }
            }
        }

        try {
            deleteDownloads(delete_down.toArray(new String[delete_down.size()]));
        } catch (final SQLException ex) {
            LOG.log(SEVERE, null, ex);
        }

        this.secureNotify();
    }

    @Override
    public void provision(final Transference download) {
//        MiscTools.GUIRun(() -> {
//            this.getScroll_panel().add(((Download) download).getView());
//        });

        try {

            this._provision((Download) download, false);

            this.secureNotify();

        } catch (final APIException ex) {

            LOG.log(Level.INFO, "{0} Provision failed! Retrying in separated thread...", Thread.currentThread().getName());

            THREAD_POOL.execute(() -> {
                try {

                    this._provision((Download) download, true);

                } catch (final APIException ex1) {

                    LOG.log(SEVERE, null, ex1);
                }

                this.secureNotify();
            });
        }

    }

    private void _provision(final Download download, final boolean retry) throws APIException {

        download.provisionIt(retry);

        if (download.isProvision_ok()) {

            this.increment_total_size(download.getFile_size());

            this.getTransference_waitstart_aux_queue().add(download);

        } else {
            this.getTransference_finished_queue().add(download);
        }
    }

}
