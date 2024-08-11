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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.CryptTools.genCrypter;
import static com.tonikelope.megabasterd.CryptTools.initMEGALinkKey;
import static com.tonikelope.megabasterd.DBTools.deleteDownload;
import static com.tonikelope.megabasterd.DBTools.insertDownload;
import static com.tonikelope.megabasterd.DBTools.selectSettingValue;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.bin2i32a;
import static com.tonikelope.megabasterd.MiscTools.checkMegaDownloadUrl;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static com.tonikelope.megabasterd.MiscTools.getWaitTimeExpBackOff;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Long.valueOf;
import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.logging.Level.SEVERE;

/**
 * @author tonikelope
 */
public class Download implements Transference, Runnable, SecureSingleThreadNotifiable {

    public static final boolean VERIFY_CBC_MAC_DEFAULT = false;
    public static final boolean USE_SLOTS_DEFAULT = true;
    public static final int WORKERS_DEFAULT = 6;
    public static final boolean USE_MEGA_ACCOUNT_DOWN = false;
    public static final boolean DEFAULT_CLIPBOARD_LINK_MONITOR = true;
    public static final int CHUNK_SIZE_MULTI = 20;
    private static final Logger LOG = Logger.getLogger(Download.class.getName());

    private MainPanel _main_panel;
    private DownloadView _view;
    private final ProgressMeter _progress_meter;
    private final Object _secure_notify_lock;
    private final Object _progress_lock;
    private final Object _workers_lock;
    private final Object _chunkid_lock;
    private final Object _dl_url_lock;
    private final Object _turbo_proxy_lock;
    private volatile boolean _notified;
    private final String _url;
    private final String _download_path;
    private final String _custom_chunks_dir;
    private String _file_name;
    private String _file_key;
    private Long _file_size;
    private String _file_pass;
    private String _file_noexpire;
    private volatile boolean _frozen;
    private final boolean _use_slots;
    private int _slots;
    private final boolean _restart;
    private final ArrayList<ChunkDownloader> _chunkworkers;
    private final ExecutorService _thread_pool;
    private volatile boolean _exit;
    private volatile boolean _pause;
    private final ConcurrentLinkedQueue<Long> _partialProgressQueue;
    private volatile long _progress;
    private ChunkWriterManager _chunkmanager;
    private String _last_download_url;
    private boolean _provision_ok;
    private boolean _auto_retry_on_error;
    private int _paused_workers;
    private File _file;
    private boolean _checking_cbc;
    private boolean _retrying_request;
    private Double _progress_bar_rate;
    private OutputStream _output_stream;
    private String _status_error;
    private final ConcurrentLinkedQueue<Long> _rejectedChunkIds;
    private long _last_chunk_id_dispatched;
    private final MegaAPI _ma;
    private volatile boolean _canceled;
    private volatile boolean _turbo;
    private volatile boolean _closed;
    private volatile boolean _finalizing;
    private final Object _progress_watchdog_lock;
    private final boolean _priority;
    private volatile boolean global_cancel = false;

    public void setGlobal_cancel(final boolean global_cancel) {
        this.global_cancel = global_cancel;
    }

    public String getStatus_error() {
        return this._status_error;
    }

    public Download(final MegaAPI ma, final String url, final String download_path, final String file_name, final String file_key, final Long file_size, final String file_pass, final String file_noexpire, final boolean use_slots, final boolean restart, final String custom_chunks_dir, final boolean priority) {
        this._priority = priority;
        this._paused_workers = 0;
        this._ma = ma;
        this._last_chunk_id_dispatched = 0L;
        this._canceled = false;
        this._auto_retry_on_error = true;
        this._status_error = null;
        this._retrying_request = false;
        this._checking_cbc = false;
        this._finalizing = false;
        this._closed = false;
        this._pause = false;
        this._exit = false;
        this._progress_watchdog_lock = new Object();
        this._last_download_url = null;
        this._provision_ok = false;
        this._progress = 0L;
        this._notified = false;
        this._url = url;
        this._download_path = download_path;
        this._file_name = file_name;
        this._file_key = file_key;
        this._file_size = file_size;
        this._file_pass = file_pass;
        this._file_noexpire = file_noexpire;
        this._use_slots = use_slots;
        this._restart = restart;
        this._secure_notify_lock = new Object();
        this._progress_lock = new Object();
        this._workers_lock = new Object();
        this._chunkid_lock = new Object();
        this._dl_url_lock = new Object();
        this._turbo_proxy_lock = new Object();
        this._chunkworkers = new ArrayList<>();
        this._partialProgressQueue = new ConcurrentLinkedQueue<>();
        this._rejectedChunkIds = new ConcurrentLinkedQueue<>();
        this._thread_pool = newCachedThreadPool();
//        this._view = new DownloadView(this);
        this._progress_meter = new ProgressMeter(this);
        this._custom_chunks_dir = custom_chunks_dir;
        this._turbo = false;
    }

    public Download(final MainPanel main_panel, final MegaAPI ma, final String url, final String download_path, final String file_name, final String file_key, final Long file_size, final String file_pass, final String file_noexpire, final boolean use_slots, final boolean restart, final String custom_chunks_dir, final boolean priority) {

        this._priority = priority;
        this._paused_workers = 0;
        this._ma = ma;
        this._frozen = main_panel.isInit_paused();
        this._last_chunk_id_dispatched = 0L;
        this._canceled = false;
        this._auto_retry_on_error = true;
        this._status_error = null;
        this._retrying_request = false;
        this._checking_cbc = false;
        this._finalizing = false;
        this._closed = false;
        this._pause = false;
        this._exit = false;
        this._progress_watchdog_lock = new Object();
        this._last_download_url = null;
        this._provision_ok = false;
        this._progress = 0L;
        this._notified = false;
        this._main_panel = main_panel;
        this._url = url;
        this._download_path = download_path;
        this._file_name = file_name;
        this._file_key = file_key;
        this._file_size = file_size;
        this._file_pass = file_pass;
        this._file_noexpire = file_noexpire;
        this._use_slots = use_slots;
        this._restart = restart;
        this._secure_notify_lock = new Object();
        this._progress_lock = new Object();
        this._workers_lock = new Object();
        this._chunkid_lock = new Object();
        this._dl_url_lock = new Object();
        this._turbo_proxy_lock = new Object();
        this._chunkworkers = new ArrayList<>();
        this._partialProgressQueue = new ConcurrentLinkedQueue<>();
        this._rejectedChunkIds = new ConcurrentLinkedQueue<>();
        this._thread_pool = newCachedThreadPool();
        this._view = new DownloadView(this);
        this._progress_meter = new ProgressMeter(this);
        this._custom_chunks_dir = custom_chunks_dir;
        this._turbo = false;
    }

    public Download(final Download download) {

        this._priority = download.isPriority();
        this._paused_workers = 0;
        this._ma = download.getMa();
        this._last_chunk_id_dispatched = 0L;
        this._canceled = false;
        this._status_error = null;
        this._finalizing = false;
        this._retrying_request = false;
        this._auto_retry_on_error = true;
        this._closed = false;
        this._checking_cbc = false;
        this._pause = false;
        this._exit = false;
        this._progress_watchdog_lock = new Object();
        this._last_download_url = null;
        this._provision_ok = false;
        this._progress = 0L;
        this._notified = false;
        this._main_panel = download.getMain_panel();
        this._url = download.getUrl();
        this._download_path = download.getDownload_path();
        this._file_name = download.getFile_name();
        this._file_key = download.getFile_key();
        this._file_size = download.getFile_size();
        this._file_pass = download.getFile_pass();
        this._file_noexpire = download.getFile_noexpire();
        this._use_slots = download.getMain_panel().isUse_slots_down();
        this._restart = true;
        this._secure_notify_lock = new Object();
        this._progress_lock = new Object();
        this._workers_lock = new Object();
        this._chunkid_lock = new Object();
        this._dl_url_lock = new Object();
        this._turbo_proxy_lock = new Object();
        this._chunkworkers = new ArrayList<>();
        this._partialProgressQueue = new ConcurrentLinkedQueue<>();
        this._rejectedChunkIds = new ConcurrentLinkedQueue<>();
        this._thread_pool = newCachedThreadPool();
        this._view = new DownloadView(this);
        this._progress_meter = new ProgressMeter(this);
        this._custom_chunks_dir = download.getCustom_chunks_dir();
        this._turbo = false;

    }

    @Override
    public boolean isPriority() {
        return this._priority;
    }

    @Override
    public boolean isCanceled() {
        return (this._canceled && !this.global_cancel);
    }

    public boolean isTurbo() {
        return this._turbo;
    }

    public String getCustom_chunks_dir() {
        return this._custom_chunks_dir;
    }

    public long getLast_chunk_id_dispatched() {
        return this._last_chunk_id_dispatched;
    }

    public long calculateLastWrittenChunk(final long temp_file_size) {
        if (temp_file_size > 3584 * 1024) {
            return 7 + (long) Math.floor((float) (temp_file_size - 3584 * 1024) / (1024 * 1024 * (this.isUse_slots() ? Download.CHUNK_SIZE_MULTI : 1)));
        } else {
            long i = 0, tot = 0;

            while (tot < temp_file_size) {
                i++;
                tot += i * 128 * 1024;
            }

            return i;
        }
    }

    public void disableTurboMode() {
        synchronized (this._turbo_proxy_lock) {
            if (this._turbo) {
                this._turbo = false;
//                MiscTools.GUIRun(() -> {
//
//                    this.getView().getSpeed_label().setForeground(new Color(0, 128, 255));
//
//                });
            }
        }
    }

    public void enableTurboMode() {

        synchronized (this._turbo_proxy_lock) {

            if (!this._turbo) {

                this._turbo = true;

                if (!this._finalizing) {
                    final Download tthis = this;

//                    MiscTools.GUIRun(() -> {
//
//                        this.getView().getSpeed_label().setForeground(new Color(255, 102, 0));
//
//                    });

                    synchronized (this._workers_lock) {

                        for (int t = this.getChunkworkers().size(); t <= Transference.MAX_WORKERS; t++) {

                            final ChunkDownloader c = new ChunkDownloader(t, tthis);

                            this._chunkworkers.add(c);

                            this._thread_pool.execute(c);
                        }

                    }

//                    MiscTools.GUIRun(() -> {
//                        this.getView().getSlots_spinner().setValue(Transference.MAX_WORKERS);
//
//                        this.getView().getSlots_spinner().setEnabled(true);
//                    });
                }
            }

        }

    }

    public ConcurrentLinkedQueue<Long> getRejectedChunkIds() {
        return this._rejectedChunkIds;
    }

    public Object getWorkers_lock() {
        return this._workers_lock;
    }

    public boolean isChecking_cbc() {
        return this._checking_cbc;
    }

    public boolean isRetrying_request() {
        return this._retrying_request;
    }

    public boolean isExit() {
        return this._exit;
    }

    public boolean isPause() {
        return this._pause;
    }

    public void setExit(final boolean exit) {
        this._exit = exit;
    }

    public void setPause(final boolean pause) {
        this._pause = pause;
    }

    public ChunkWriterManager getChunkmanager() {
        return this._chunkmanager;
    }

    public String getFile_key() {
        return this._file_key;
    }

    @Override
    public long getProgress() {
        return this._progress;
    }

    public OutputStream getOutput_stream() {
        return this._output_stream;
    }

    public ArrayList<ChunkDownloader> getChunkworkers() {

        synchronized (this._workers_lock) {
            return this._chunkworkers;
        }
    }

    public void setPaused_workers(final int paused_workers) {
        this._paused_workers = paused_workers;
    }

    public String getUrl() {
        return this._url;
    }

    public String getDownload_path() {
        return this._download_path;
    }

    @Override
    public String getFile_name() {
        return this._file_name;
    }

    public String getFile_pass() {
        return this._file_pass;
    }

    public String getFile_noexpire() {
        return this._file_noexpire;
    }

    public boolean isUse_slots() {
        return this._use_slots;
    }

    public int getSlots() {
        return this._slots;
    }

    public void setLast_chunk_id_dispatched(final long last_chunk_id_dispatched) {
        this._last_chunk_id_dispatched = last_chunk_id_dispatched;
    }

    public boolean isProvision_ok() {
        return this._provision_ok;
    }

    @Override
    public ProgressMeter getProgress_meter() {

        while (this._progress_meter == null) {
            try {
                Thread.sleep(250);
            } catch (final InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }

        return this._progress_meter;
    }

    @Override
    public DownloadView getView() {

        while (this._view == null) {
            try {
                Thread.sleep(250);
            } catch (final InterruptedException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }

        return this._view;
    }

    @Override
    public MainPanel getMain_panel() {
        return this._main_panel;
    }

    public void set_main_panel(final MainPanel _main_panel) {
        this._main_panel = _main_panel;
    }

    @Override
    public void start() {

        THREAD_POOL.execute(this);
    }

    @Override
    public void stop() {

        if (!this.isExit()) {
            this._canceled = true;
            this.stopDownloader();
        }
    }

    @Override
    public void pause() {

        if (this.isPause()) {

            this.setPause(false);

            this.setPaused_workers(0);

            synchronized (this._workers_lock) {

                this.getChunkworkers().forEach((downloader) -> {
                    downloader.secureNotify();
                });
            }

//            this.getView().resume();

            this._main_panel.getDownload_manager().setPaused_all(false);

        } else {

            this.setPause(true);

//            this.getView().pause();
        }

        this._main_panel.getDownload_manager().secureNotify();
    }

    public MegaAPI getMa() {
        return this._ma;
    }

    @Override
    public void restart() {

        final Download new_download = new Download(this);

        this.getMain_panel().getDownload_manager().getTransference_remove_queue().add(this);

        this.getMain_panel().getDownload_manager().getTransference_provision_queue().add(new_download);

        this.getMain_panel().getDownload_manager().secureNotify();
    }

    @Override
    public boolean isPaused() {
        return this.isPause();
    }

    @Override
    public boolean isStopped() {
        return this.isExit();
    }

    @Override
    public void checkSlotsAndWorkers() {

        if (!this.isExit() && !this._finalizing) {

            synchronized (this._workers_lock) {

                final int sl = this.getSlots();

                final int cworkers = this.getChunkworkers().size();

                if (sl != cworkers) {

                    if (sl > cworkers) {

                        this.startSlot();

                    } else {

                        this.stopLastStartedSlot();
                    }
                }
            }
        }
    }

    @Override
    public void close() {

        this._closed = true;

        if (this._provision_ok) {
            try {
                deleteDownload(this._url);
            } catch (final SQLException ex) {
                LOG.log(SEVERE, null, ex);
            }
        }

        this._main_panel.getDownload_manager().getTransference_remove_queue().add(this);

        this._main_panel.getDownload_manager().secureNotify();
    }

    @Override
    public ConcurrentLinkedQueue<Long> getPartialProgress() {
        return this._partialProgressQueue;
    }

    @Override
    public long getFile_size() {
        return this._file_size;
    }

    @Override
    public void run() {

//        MiscTools.GUIRun(() -> {
//            this.getView().getQueue_down_button().setVisible(false);
//            this.getView().getQueue_up_button().setVisible(false);
//            this.getView().getQueue_top_button().setVisible(false);
//            this.getView().getQueue_bottom_button().setVisible(false);
//            this.getView().getClose_button().setVisible(false);
//            this.getView().getCopy_link_button().setVisible(true);
//            this.getView().getOpen_folder_button().setVisible(true);
//        });
//
//        this.getView().printStatusNormal("Starting download, please wait...");

        try {

            final FileStore fs = Files.getFileStore(Paths.get(this._download_path));

            if (fs.getUsableSpace() < this._file_size) {
                this._status_error = "NO DISK SPACE AVAILABLE!";
                this._exit = true;
            }

            if (!this._exit) {

                String filename = this._download_path + "/" + this._file_name;

                this._file = new File(filename);

                if (this._file.getParent() != null) {
                    final File path = new File(this._file.getParent());

                    path.mkdirs();
                }

                if (!this._file.exists() || this._file.length() != this._file_size) {

                    if (this._file.exists()) {
                        this._file_name = this._file_name.replaceFirst("\\..*$", "_" + MiscTools.genID(8) + "_$0");

                        filename = this._download_path + "/" + this._file_name;

                        this._file = new File(filename);
                    }

                    LOG.info("Starting download (retrieving MEGA temp link), please wait...");

                    this._last_download_url = this.getMegaFileDownloadUrl(this._url);

                    if (!this._exit) {

                        final String temp_filename = (this.getCustom_chunks_dir() != null ? this.getCustom_chunks_dir() : this._download_path) + "/" + this._file_name + ".mctemp";

                        this._file = new File(temp_filename);

                        if (this._file.getParent() != null) {
                            final File path = new File(this._file.getParent());

                            path.mkdirs();
                        }

                        if (this._file.exists()) {
                            LOG.info("File exists, resuming download...");

                            final long max_size = this.calculateMaxTempFileSize(this._file.length());

                            if (max_size != this._file.length()) {

                                LOG.log(Level.INFO, "{0} Downloader truncating mctemp file {1} -> {2} ", new Object[]{Thread.currentThread().getName(), this._file.length(), max_size});

                                LOG.info("Truncating temp file...");

                                try (final FileChannel out_truncate = new FileOutputStream(temp_filename, true).getChannel()) {
                                    out_truncate.truncate(max_size);
                                }
                            }

                            this.setProgress(this._file.length());

                            this._last_chunk_id_dispatched = this.calculateLastWrittenChunk(this._progress);

                        } else {
                            this.setProgress(0);
                        }

                        this._output_stream = new BufferedOutputStream(new FileOutputStream(this._file, (this._progress > 0)));

                        this._thread_pool.execute(this.getProgress_meter());

                        this.getMain_panel().getGlobal_dl_speed().attachTransference(this);

                        synchronized (this._workers_lock) {

                            if (this._use_slots) {

                                this._chunkmanager = new ChunkWriterManager(this);

                                this._thread_pool.execute(this._chunkmanager);

                                this._slots = this.getMain_panel().getDefault_slots_down();

//                                this._view.getSlots_spinner().setValue(this._slots);

                                for (int t = 1; t <= this._slots; t++) {
                                    final ChunkDownloader c = new ChunkDownloader(t, this);

                                    this._chunkworkers.add(c);

                                    this._thread_pool.execute(c);
                                }

//                                MiscTools.GUIRun(() -> {
//                                    for (final JComponent c : new JComponent[]{this.getView().getSlots_label(), this.getView().getSlots_spinner(), this.getView().getSlot_status_label()}) {
//
//                                        c.setVisible(true);
//                                    }
//                                });

                            } else {

                                final ChunkDownloaderMono c = new ChunkDownloaderMono(this);

                                this._chunkworkers.add(c);

                                this._thread_pool.execute(c);

//                                MiscTools.GUIRun(() -> {
//                                    for (final JComponent c1 : new JComponent[]{this.getView().getSlots_label(), this.getView().getSlots_spinner(), this.getView().getSlot_status_label()}) {
//                                        c1.setVisible(false);
//                                    }
//                                });
                            }
                        }

                        LOG.info("Downloading file from mega " + (this._ma.getFull_email() != null ? "(" + this._ma.getFull_email() + ")" : "") + " ...");

//                        MiscTools.GUIRun(() -> {
//                            for (final JComponent c : new JComponent[]{this.getView().getPause_button(), this.getView().getProgress_pbar()}) {
//
//                                c.setVisible(true);
//                            }
//                        });

                        THREAD_POOL.execute(() -> {

                            //PROGRESS WATCHDOG If a download remains more than PROGRESS_WATCHDOG_TIMEOUT seconds without receiving data, we force fatal error in order to restart it.
                            LOG.log(Level.INFO, "{0} PROGRESS WATCHDOG HELLO!", Thread.currentThread().getName());

                            long last_progress, progress = this.getProgress();

                            do {
                                last_progress = progress;

                                synchronized (this._progress_watchdog_lock) {
                                    try {
                                        this._progress_watchdog_lock.wait(PROGRESS_WATCHDOG_TIMEOUT * 1000);
                                        progress = this.getProgress();
                                    } catch (final InterruptedException ex) {
                                        progress = -1;
                                        Logger.getLogger(Download.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                            } while (!this.isExit() && !this._thread_pool.isShutdown() && progress < this.getFile_size() && (this.isPaused() || progress > last_progress));

                            if (!this.isExit() && !this._thread_pool.isShutdown() && this._status_error == null && progress < this.getFile_size() && progress <= last_progress) {
                                this.stopDownloader("PROGRESS WATCHDOG TIMEOUT!");
                            }

                            LOG.log(Level.INFO, "{0} PROGRESS WATCHDOG BYE BYE!", Thread.currentThread().getName());

                        });

                        this.secureWait();

                        LOG.log(Level.INFO, "{0} Chunkdownloaders finished!", Thread.currentThread().getName());

                        this.getProgress_meter().setExit(true);

                        this.getProgress_meter().secureNotify();

                        try {

                            this._thread_pool.shutdown();

                            LOG.log(Level.INFO, "{0} Waiting all threads to finish...", Thread.currentThread().getName());

                            this._thread_pool.awaitTermination(MAX_WAIT_WORKERS_SHUTDOWN, TimeUnit.SECONDS);

                        } catch (final InterruptedException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }

                        if (!this._thread_pool.isTerminated()) {

                            LOG.log(Level.INFO, "{0} Closing thread pool ''mecag\u00fcen'' style...", Thread.currentThread().getName());

                            this._thread_pool.shutdownNow();
                        }

                        LOG.log(Level.INFO, "{0} Downloader thread pool finished!", Thread.currentThread().getName());

                        this.getMain_panel().getGlobal_dl_speed().detachTransference(this);

                        this._output_stream.close();

//                        MiscTools.GUIRun(() -> {
//                            for (final JComponent c : new JComponent[]{this.getView().getSpeed_label(), this.getView().getPause_button(), this.getView().getStop_button(), this.getView().getSlots_label(), this.getView().getSlots_spinner(), this.getView().getKeep_temp_checkbox()}) {
//
//                                c.setVisible(false);
//                            }
//                        });

                        if (this._progress == this._file_size) {

                            if (this._file.length() != this._file_size) {

                                throw new IOException("El tamaño del fichero es incorrecto!");
                            }

                            Files.move(Paths.get(this._file.getAbsolutePath()), Paths.get(filename), StandardCopyOption.REPLACE_EXISTING);

                            if (this._custom_chunks_dir != null) {

                                File temp_parent_download_dir = new File(temp_filename).getParentFile();

                                while (!temp_parent_download_dir.getAbsolutePath().equals(this._custom_chunks_dir) && temp_parent_download_dir.listFiles().length == 0) {
                                    temp_parent_download_dir.delete();
                                    temp_parent_download_dir = temp_parent_download_dir.getParentFile();
                                }

                            }

                            final String verify_file = selectSettingValue("verify_down_file");

                            if (verify_file != null && verify_file.equals("yes")) {
                                this._checking_cbc = true;

                                LOG.info("Waiting to check file integrity...");

                                this.setProgress(0);

                                LOG.info("Checking file integrity, please wait...");

//                                MiscTools.GUIRun(() -> {
//                                    this.getView().getStop_button().setVisible(true);
//
//                                    this.getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL CHECK"));
//                                });

                                this.getMain_panel().getDownload_manager().getTransference_running_list().remove(this);

                                this.getMain_panel().getDownload_manager().secureNotify();

                                if (this.verifyFileCBCMAC(filename)) {

                                    LOG.info("File successfully downloaded! (Integrity check PASSED)");

                                } else if (!this._exit) {

                                    this._status_error = "BAD NEWS :( File is DAMAGED!";

                                    LOG.info(this._status_error);

                                } else {

                                    LOG.info("File successfully downloaded! (but integrity check CANCELED)");

                                }

//                                MiscTools.GUIRun(() -> {
//                                    this.getView().getStop_button().setVisible(false);
//                                });

                            } else {

                                LOG.info("File successfully downloaded!");

                            }

                        } else if (this._status_error != null) {

//                            this.getView().hideAllExceptStatus();

                            LOG.info(this._status_error);

                        } else if (this._canceled) {

//                            this.getView().hideAllExceptStatus();

                            LOG.info("Download CANCELED!");

                        } else {

//                            this.getView().hideAllExceptStatus();

                            this._status_error = "UNEXPECTED ERROR!";

                            LOG.info(this._status_error);
                        }

                    } else if (this._status_error != null) {

//                        this.getView().hideAllExceptStatus();

                        LOG.info(this._status_error != null ? this._status_error : "ERROR");

                    } else if (this._canceled) {

//                        this.getView().hideAllExceptStatus();

                        LOG.info("Download CANCELED!");

                    } else {

//                        this.getView().hideAllExceptStatus();

                        this._status_error = "UNEXPECTED ERROR!";

                        LOG.info(this._status_error);
                    }

                } else {
//                    this.getView().hideAllExceptStatus();

                    this._status_error = "FILE WITH SAME NAME AND SIZE ALREADY EXISTS";

                    this._auto_retry_on_error = false;

                    LOG.info(this._status_error);
                }

            } else if (this._status_error != null) {

//                this.getView().hideAllExceptStatus();

                LOG.info(this._status_error);

            } else if (this._canceled) {

//                this.getView().hideAllExceptStatus();

                LOG.info("Download CANCELED!");

            } else {

//                this.getView().hideAllExceptStatus();

                this._status_error = "UNEXPECTED ERROR!";

                LOG.info(this._status_error);
            }

        } catch (final Exception ex) {
            this._status_error = "I/O ERROR " + ex.getMessage();

            LOG.info(this._status_error);

            LOG.log(Level.SEVERE, ex.getMessage());
        }

        if (this._file != null) {
            this._file.delete();

            if (this.getChunkmanager() != null) {

                this.getChunkmanager().delete_chunks_temp_dir();

                File parent_download_dir = new File(this.getDownload_path() + "/" + this.getFile_name()).getParentFile();

                while (!parent_download_dir.getAbsolutePath().equals(this.getDownload_path()) && parent_download_dir.listFiles().length == 0) {
                    parent_download_dir.delete();
                    parent_download_dir = parent_download_dir.getParentFile();
                }

                if (!(new File(this.getDownload_path() + "/" + this.getFile_name()).getParentFile().exists())) {

//                    this.getView().getOpen_folder_button().setEnabled(false);
                }
            }
        }

        if ((this._status_error == null && !this._canceled) || this.global_cancel || !this._auto_retry_on_error) {

            try {
                deleteDownload(this._url);
            } catch (final SQLException ex) {
                LOG.log(SEVERE, null, ex);
            }

        }

        this.getMain_panel().getDownload_manager().getTransference_running_list().remove(this);

        this.getMain_panel().getDownload_manager().getTransference_finished_queue().add(this);

//        MiscTools.GUIRun(() -> {
//            this.getMain_panel().getDownload_manager().getScroll_panel().remove(this.getView());
//
//            this.getMain_panel().getDownload_manager().getScroll_panel().add(this.getView());
//        });

        this.getMain_panel().getDownload_manager().secureNotify();

//        MiscTools.GUIRun(() -> {
//            this.getView().getClose_button().setVisible(true);
//
//            if ((this._status_error != null || this._canceled) && this.isProvision_ok() && !this.global_cancel) {
//
//                this.getView().getRestart_button().setVisible(true);
//
//            } else if (!this.global_cancel) {
//
//                this.getView().getClose_button().setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-ok-30.png")));
//            }
//        });

        if (this._status_error != null && !this._canceled && this._auto_retry_on_error) {
            THREAD_POOL.execute(() -> {
                for (int i = 3; !this._closed && i > 0; i--) {
                    final int j = i;
                    LOG.info("Restart (" + String.valueOf(j) + " secs...)");
//                    MiscTools.GUIRun(() -> {
//                        this.getView().getRestart_button().setText("Restart (" + String.valueOf(j) + " secs...)");
//                    });
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException ex) {
                        Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());
                    }
                }
                if (!this._closed) {
                    LOG.log(Level.INFO, "{0} Downloader {1} AUTO RESTARTING DOWNLOAD...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});
                    this.restart();
                }
            });
        } else {
            this.getMain_panel().getDownload_manager().setAll_finished(false);
        }

        this._exit = true;

        if (this._status_error != null && !this._canceled && this.getMain_panel().getDownload_manager().no_transferences() && this.getMain_panel().getUpload_manager().no_transferences() && (!this.getMain_panel().getDownload_manager().getTransference_finished_queue().isEmpty() || !this.getMain_panel().getUpload_manager().getTransference_finished_queue().isEmpty())) {
            System.exit(0);
        }

        synchronized (this._progress_watchdog_lock) {
            this._progress_watchdog_lock.notifyAll();
        }

        LOG.log(Level.INFO, "{0}{1} Downloader: bye bye", new Object[]{Thread.currentThread().getName(), this._file_name});

        LOG.info("EXITING");
        System.exit(0);
    }

    public void provisionIt(final boolean retry) throws APIException {

//        this.getView().printStatusNormal("Provisioning download, please wait...");

//        MiscTools.GUIRun(() -> {
//            this.getView().getCopy_link_button().setVisible(true);
//            this.getView().getOpen_folder_button().setVisible(true);
//        });

        final String[] file_info;

        this._provision_ok = false;

        try {
            if (this._file_name == null) {

                //New single file links
                file_info = this.getMegaFileMetadata(this._url, null, retry);

                if (file_info != null) {

                    this._file_name = file_info[0];

                    this._file_size = valueOf(file_info[1]);

                    this._file_key = file_info[2];

                    if (file_info.length == 5) {

                        this._file_pass = file_info[3];

                        this._file_noexpire = file_info[4];
                    }

                    final String filename = this._download_path + "/" + this._file_name;

                    final File file = new File(filename);

                    if (file.exists() && file.length() != this._file_size) {
                        this._file_name = this._file_name.replaceFirst("\\..*$", "_" + MiscTools.genID(8) + "_$0");
                    }

                    try {

                        insertDownload(this._url, this._ma.getFull_email(), this._download_path, this._file_name, this._file_key, this._file_size, this._file_pass, this._file_noexpire, this._custom_chunks_dir);

                        this._provision_ok = true;

                    } catch (final SQLException ex) {

                        this._status_error = "Error registering download: " + ex.getMessage() + " file is already downloading?";
                    }

                }
            } else {

                final String filename = this._download_path + "/" + this._file_name;

                final File file = new File(filename);

                final File temp_file = new File(filename + ".mctemp");

                if (file.exists() && !temp_file.exists() && file.length() != this._file_size) {
                    this._file_name = this._file_name.replaceFirst("\\..*$", "_" + MiscTools.genID(8) + "_$0");
                }

                //Resuming single file links and new/resuming folder links
                try {

                    deleteDownload(this._url); //If resuming

                    insertDownload(this._url, this._ma.getFull_email(), this._download_path, this._file_name, this._file_key, this._file_size, this._file_pass, this._file_noexpire, this._custom_chunks_dir);

                    this._provision_ok = true;

                } catch (final SQLException ex) {

                    this._status_error = "Error registering download: " + ex.getMessage();

                }
            }

        } catch (final APIException ex) {

            throw ex;

        } catch (final NumberFormatException ex) {

            this._status_error = ex.getMessage();
        }

        if (!this._provision_ok) {

            if (this._status_error == null) {
                this._status_error = "PROVISION FAILED";
            }

//            if (this._file_name != null) {
//                MiscTools.GUIRun(() -> {
//                    this.getView().getFile_name_label().setVisible(true);
//
//                    this.getView().getFile_name_label().setText(truncateText(new File(this._download_path + "/" + this._file_name).getName(), 150));
//
//                    this.getView().getFile_name_label().setToolTipText(this._download_path + "/" + this._file_name);
//
//                    this.getView().getFile_size_label().setVisible(true);
//
//                    this.getView().getFile_size_label().setText(formatBytes(this._file_size));
//                });
//            }
//
//            this.getView().hideAllExceptStatus();
//
//            this.getView().printStatusError(this._status_error);
//
//            MiscTools.GUIRun(() -> {
//                this.getView().getClose_button().setVisible(true);
//            });

        } else {

            this._progress_bar_rate = MAX_VALUE / (double) this._file_size;

            LOG.info(this._frozen ? "(FROZEN) Waiting to start..." : "Waiting to start...");
//
//            MiscTools.GUIRun(() -> {
//                this.getView().getFile_name_label().setVisible(true);
//
//                this.getView().getFile_name_label().setText(truncateText(new File(this._download_path + "/" + this._file_name).getName(), 150));
//
//                this.getView().getFile_name_label().setToolTipText(this._download_path + "/" + this._file_name);
//
//                this.getView().getFile_size_label().setVisible(true);
//
//                this.getView().getFile_size_label().setText(formatBytes(this._file_size));
//            });
//
//            MiscTools.GUIRun(() -> {
//                this.getView().getClose_button().setVisible(true);
//                this.getView().getQueue_up_button().setVisible(true);
//                this.getView().getQueue_down_button().setVisible(true);
//                this.getView().getQueue_top_button().setVisible(true);
//                this.getView().getQueue_bottom_button().setVisible(true);
//            });

        }

    }

    public void pause_worker() {

        synchronized (this._workers_lock) {

            if (++this._paused_workers == this._chunkworkers.size() && !this._exit) {

                LOG.info("Download paused!");

//                MiscTools.GUIRun(() -> {
//                    this.getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME DOWNLOAD"));
//                    this.getView().getPause_button().setEnabled(true);
//                });

            }
        }
    }

    public void pause_worker_mono() {

        LOG.info("Download paused!");

//        MiscTools.GUIRun(() -> {
//            this.getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME DOWNLOAD"));
//            this.getView().getPause_button().setEnabled(true);
//        });

    }

    public String getDownloadUrlForWorker() {

        synchronized (this._dl_url_lock) {

            if (this._last_download_url != null && checkMegaDownloadUrl(this._last_download_url)) {

                return this._last_download_url;
            }

            boolean error;

            int conta_error = 0;

            String download_url;

            do {

                error = false;

                try {
                    if (findFirstRegex("://mega(\\.co)?\\.nz/", this._url, 0) != null) {

                        download_url = this._ma.getMegaFileDownloadUrl(this._url);

                    } else {
                        download_url = MegaCrypterAPI.getMegaFileDownloadUrl(this._url, this._file_pass, this._file_noexpire, this._ma.getSid(), this.getMain_panel().getMega_proxy_server() != null ? (this.getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + this.getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8")) + ":" + MiscTools.getMyPublicIP()) : null);
                    }

                    if (checkMegaDownloadUrl(download_url)) {

                        this._last_download_url = download_url;

                    } else {

                        error = true;
                    }

                } catch (final Exception ex) {

                    error = true;

                    try {
                        Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                    } catch (final InterruptedException ex2) {
                        LOG.log(Level.SEVERE, ex2.getMessage());
                    }
                }

            } while (error);

            return this._last_download_url;

        }
    }

    public void startSlot() {

        if (!this._exit) {

            synchronized (this._workers_lock) {

                final int chunk_id = this._chunkworkers.size() + 1;

                final ChunkDownloader c = new ChunkDownloader(chunk_id, this);

                this._chunkworkers.add(c);

                try {

                    this._thread_pool.execute(c);

                } catch (final java.util.concurrent.RejectedExecutionException e) {
                    LOG.log(Level.INFO, e.getMessage());
                }
            }
        }
    }

    public void stopLastStartedSlot() {

        if (!this._exit) {

            synchronized (this._workers_lock) {

                if (!this._chunkworkers.isEmpty()) {

//                    MiscTools.GUIRun(() -> {
//                        this.getView().getSlots_spinner().setEnabled(false);
//                    });

                    int i = this._chunkworkers.size() - 1;

                    while (i >= 0) {

                        final ChunkDownloader chundownloader = this._chunkworkers.get(i);

                        if (!chundownloader.isExit()) {

                            chundownloader.setExit(true);

                            chundownloader.secureNotify();

                            this._view.updateSlotsStatus();

                            break;

                        } else {

                            i--;
                        }
                    }
                }
            }
        }
    }

    public void stopThisSlot(final ChunkDownloader chunkdownloader) {

        synchronized (this._workers_lock) {

            if (this._chunkworkers.remove(chunkdownloader) && !this._exit) {

                if (this._use_slots) {

                    if (chunkdownloader.isChunk_exception() || this.getMain_panel().isExit()) {

                        this._finalizing = true;

//                        MiscTools.GUIRun(() -> {
//                            this.getView().getSlots_spinner().setEnabled(false);
//
//                            this.getView().getSlots_spinner().setValue((int) this.getView().getSlots_spinner().getValue() - 1);
//                        });

                    } else if (!this._finalizing) {
//                        MiscTools.GUIRun(() -> {
//                            this.getView().getSlots_spinner().setEnabled(true);
//                        });
                    }

//                    this.getView().updateSlotsStatus();
                }

                if (!this._exit && this.isPause() && this._paused_workers == this._chunkworkers.size()) {

                    LOG.info("Download paused!");

//                    MiscTools.GUIRun(() -> {
//                        this.getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME DOWNLOAD"));
//
//                        this.getView().getPause_button().setEnabled(true);
//                    });

                }
            }
        }

    }

    private boolean verifyFileCBCMAC(final String filename) throws FileNotFoundException, Exception, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final int old_thread_priority = Thread.currentThread().getPriority();

        final int[] int_key = bin2i32a(UrlBASE642Bin(this._file_key));
        final int[] iv = new int[]{int_key[4], int_key[5]};
        final int[] meta_mac = new int[]{int_key[6], int_key[7]};
        int[] file_mac = {0, 0, 0, 0};
        final int[] cbc_iv = {0, 0, 0, 0};

        final byte[] byte_file_key = initMEGALinkKey(this.getFile_key());

        final Cipher cryptor = genCrypter("AES", "AES/CBC/NoPadding", byte_file_key, i32a2bin(cbc_iv));

        try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename))) {

            long chunk_id = 1L;
            long tot = 0L;
            final byte[] byte_block = new byte[16];
            int[] int_block;
            int reads;
            int[] chunk_mac = new int[4];

            try {
                while (!this._exit) {

                    final long chunk_offset = ChunkWriterManager.calculateChunkOffset(chunk_id, 1);

                    final long chunk_size = ChunkWriterManager.calculateChunkSize(chunk_id, this.getFile_size(), chunk_offset, 1);

                    ChunkWriterManager.checkChunkID(chunk_id, this.getFile_size(), chunk_offset);

                    tot += chunk_size;

                    chunk_mac[0] = iv[0];
                    chunk_mac[1] = iv[1];
                    chunk_mac[2] = iv[0];
                    chunk_mac[3] = iv[1];

                    long conta_chunk = 0L;

                    while (conta_chunk < chunk_size && (reads = is.read(byte_block)) != -1) {

                        if (reads < byte_block.length) {

                            for (int i = reads; i < byte_block.length; i++) {
                                byte_block[i] = 0;
                            }
                        }

                        int_block = bin2i32a(byte_block);

                        for (int i = 0; i < chunk_mac.length; i++) {
                            chunk_mac[i] ^= int_block[i];
                        }

                        chunk_mac = bin2i32a(cryptor.doFinal(i32a2bin(chunk_mac)));

                        conta_chunk += reads;
                    }

                    for (int i = 0; i < file_mac.length; i++) {
                        file_mac[i] ^= chunk_mac[i];
                    }

                    file_mac = bin2i32a(cryptor.doFinal(i32a2bin(file_mac)));

                    this.setProgress(tot);

                    chunk_id++;

                }

            } catch (final ChunkInvalidException e) {

            }

            final int[] cbc = {file_mac[0] ^ file_mac[1], file_mac[2] ^ file_mac[3]};

            return (cbc[0] == meta_mac[0] && cbc[1] == meta_mac[1]);
        }
    }

    public void stopDownloader() {

        if (!this._exit) {

            this._exit = true;

            if (this.isRetrying_request()) {

                LOG.info("Retrying cancelled! " + truncateText(this._url, 80));

            } else if (this.isChecking_cbc()) {

                LOG.info("Verification cancelled! " + truncateText(this._file_name, 80));

            } else {

                LOG.info("Stopping download, please wait...");

                synchronized (this._workers_lock) {

                    this._chunkworkers.forEach((downloader) -> {
                        downloader.secureNotify();
                    });
                }

                this.secureNotify();
            }
        }
    }

    public void stopDownloader(final String reason) {

        this._status_error = (reason != null ? LabelTranslatorSingleton.getInstance().translate("FATAL ERROR! ") + reason : LabelTranslatorSingleton.getInstance().translate("FATAL ERROR! "));

        this.stopDownloader();
    }

    public long calculateMaxTempFileSize(final long size) {
        if (size > 3584 * 1024) {
            final long reminder = (size - 3584 * 1024) % (1024 * 1024 * (this.isUse_slots() ? Download.CHUNK_SIZE_MULTI : 1));

            return reminder == 0 ? size : (size - reminder);
        } else {
            long i = 0, tot = 0;

            while (tot < size) {
                i++;
                tot += i * 128 * 1024;
            }

            return tot == size ? size : (tot - i * 128 * 1024);
        }
    }

    public String[] getMegaFileMetadata(final String link, final MainPanelView panel, final boolean retry_request) throws APIException {

        String[] file_info = null;
        int retry = 0, error_code;
        boolean error;

        do {
            error = false;

            try {

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {

                    file_info = this._ma.getMegaFileMetadata(link);

                } else {

                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel, this.getMain_panel().getMega_proxy_server() != null ? (this.getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + this.getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8"))) : null);
                }

            } catch (final APIException ex) {

                error = true;

                this._status_error = ex.getMessage();

                error_code = ex.getCode();

                if (error_code == -16) {
                    this._status_error = "ERROR: MEGA FILE BLOCKED/DELETED";
                }

                if (Arrays.asList(FATAL_API_ERROR_CODES).contains(error_code)) {

                    this._auto_retry_on_error = Arrays.asList(FATAL_API_ERROR_CODES_WITH_RETRY).contains(error_code);

                    this.stopDownloader(error_code == -16 ? this._status_error : ex.getMessage() + " " + truncateText(link, 80));

                } else {

                    if (!retry_request) {

                        throw ex;
                    }

                    this._retrying_request = true;

//                    MiscTools.GUIRun(() -> {
//
//                        this.getView().getStop_button().setVisible(true);
//
//                        this.getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL RETRY"));
//                    });

                    for (long i = getWaitTimeExpBackOff(retry++); i > 0 && !this._exit; i--) {
                        if (error_code == -18) {
                            LOG.info(LabelTranslatorSingleton.getInstance().translate("File temporarily unavailable! (Retrying in ") + i + LabelTranslatorSingleton.getInstance().translate(" secs...)"));
                        } else {
                            LOG.info("Mega/MC APIException error " + ex.getMessage() + LabelTranslatorSingleton.getInstance().translate(" (Retrying in ") + i + LabelTranslatorSingleton.getInstance().translate(" secs...)"));
                        }

                        try {
                            sleep(1000);
                        } catch (final InterruptedException ex2) {
                        }
                    }
                }

            } catch (final Exception ex) {

                if (!(ex instanceof APIException)) {
                    this.stopDownloader("Mega link is not valid! " + truncateText(link, 80));
                }
            }

        } while (!this._exit && error);

        if (!this._exit && !error) {

            this._auto_retry_on_error = true;

//            MiscTools.GUIRun(() -> {
//                this.getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL DOWNLOAD"));
//                this.getView().getStop_button().setVisible(false);
//            });

        }

        this._retrying_request = false;

        return file_info;

    }

    public String getMegaFileDownloadUrl(final String link) throws IOException, InterruptedException {

        String dl_url = null;
        int retry = 0, error_code;
        boolean error;

        do {
            error = false;

            try {
                if (findFirstRegex("://mega(\\.co)?\\.nz/", this._url, 0) != null) {

                    dl_url = this._ma.getMegaFileDownloadUrl(link);

                } else {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, this._file_pass, this._file_noexpire, this._ma.getSid(), this.getMain_panel().getMega_proxy_server() != null ? (this.getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + this.getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8")) + ":" + MiscTools.getMyPublicIP()) : null);
                }

            } catch (final APIException ex) {
                error = true;

                error_code = ex.getCode();

                if (error_code == -16) {
                    this._status_error = "ERROR: MEGA FILE BLOCKED/DELETED";
                }

                if (Arrays.asList(FATAL_API_ERROR_CODES).contains(error_code)) {

                    this._auto_retry_on_error = Arrays.asList(FATAL_API_ERROR_CODES_WITH_RETRY).contains(error_code);

                    this.stopDownloader(error_code == -16 ? this._status_error : ex.getMessage() + " " + truncateText(link, 80));

                } else {

                    this._retrying_request = true;

//                    MiscTools.GUIRun(() -> {
//                        this.getView().getStop_button().setVisible(true);
//
//                        this.getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL RETRY"));
//                    });

                    for (long i = getWaitTimeExpBackOff(retry++); i > 0 && !this._exit; i--) {
                        if (error_code == -18) {
                            LOG.info("File temporarily unavailable! (Retrying in " + i + " secs...)");
                        } else {
                            LOG.info("Mega/MC APIException error " + ex.getMessage() + " (Retrying in " + i + " secs...)");
                        }

                        try {
                            sleep(1000);
                        } catch (final InterruptedException ex2) {
                        }
                    }
                }
            }

        } while (!this._exit && error);

        if (!this._exit && !error) {

            this._auto_retry_on_error = true;

//            MiscTools.GUIRun(() -> {
//                this.getView().getStop_button().setText(LabelTranslatorSingleton.getInstance().translate("CANCEL DOWNLOAD"));
//                this.getView().getStop_button().setVisible(false);
//            });

        }

        this._retrying_request = false;

        return dl_url;
    }

    public long nextChunkId() throws ChunkInvalidException {

        synchronized (this._chunkid_lock) {

            if (this._main_panel.isExit()) {
                throw new ChunkInvalidException(null);
            }

            final Long next_id;

            if ((next_id = this._rejectedChunkIds.poll()) != null) {
                return next_id;
            } else {
                return ++this._last_chunk_id_dispatched;
            }
        }

    }

    public void rejectChunkId(final long chunk_id) {
        this._rejectedChunkIds.add(chunk_id);
    }

    @Override
    public void secureNotify() {
        synchronized (this._secure_notify_lock) {

            this._notified = true;

            this._secure_notify_lock.notify();
        }
    }

    @Override
    public void secureWait() {

        synchronized (this._secure_notify_lock) {
            while (!this._notified) {

                try {
                    this._secure_notify_lock.wait(1000);
                } catch (final InterruptedException ex) {
                    this._exit = true;
                    LOG.log(SEVERE, null, ex);
                }
            }

            this._notified = false;
        }
    }

    @Override
    public void setProgress(final long progress) {

        synchronized (this._progress_lock) {

            final long old_progress = this._progress;

            this._progress = progress;

            this.getMain_panel().getDownload_manager().increment_total_progress(this._progress - old_progress);

            final int old_percent_progress = (int) Math.floor(((double) old_progress / this._file_size) * 100);

            int new_percent_progress = (int) Math.floor(((double) progress / this._file_size) * 100);

            if (new_percent_progress == 100 && progress != this._file_size) {
                new_percent_progress = 99;
            }

            if (new_percent_progress > old_percent_progress) {
//                LOG.info(new_percent_progress + "% " + this._progress + "/" + this._file_size);
//                this.getView().updateProgressBar(this._progress, this._progress_bar_rate);
            }
        }
    }

    @Override
    public boolean isStatusError() {
        return this._status_error != null;
    }

    @Override
    public int getSlotsCount() {
        return this.getChunkworkers().size();
    }

    @Override
    public boolean isFrozen() {
        return this._frozen;
    }

    @Override
    public void unfreeze() {

//        LOG.info(this.getView().getStatus_label().getText().replaceFirst("^\\([^)]+\\) ", ""));

        this._frozen = false;
    }

    @Override
    public void upWaitQueue() {
        this._main_panel.getDownload_manager().upWaitQueue(this);
    }

    @Override
    public void downWaitQueue() {
        this._main_panel.getDownload_manager().downWaitQueue(this);
    }

    @Override
    public void bottomWaitQueue() {
        this._main_panel.getDownload_manager().bottomWaitQueue(this);
    }

    @Override
    public void topWaitQueue() {
        this._main_panel.getDownload_manager().topWaitQueue(this);
    }

    @Override
    public boolean isRestart() {
        return this._restart;
    }

    @Override
    public boolean isClosed() {
        return this._closed;
    }

    @Override
    public int getPausedWorkers() {
        return this._paused_workers;
    }

    @Override
    public int getTotWorkers() {
        return this.getChunkworkers().size();
    }

}
