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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

/**
 * @author tonikelope
 */
public class UploadManager extends TransferenceManager {

    private static final Logger LOG = Logger.getLogger(UploadManager.class.getName());

    private final Object _log_file_lock;

    public UploadManager(final MainPanel main_panel) {

        super(main_panel, main_panel.getMax_ul(), main_panel.getView().getStatus_up_label(), main_panel.getView().getjPanel_scroll_up(), main_panel.getView().getClose_all_finished_up_button(), main_panel.getView().getPause_all_up_button(), main_panel.getView().getClean_all_up_menu());

        this._log_file_lock = new Object();
    }

    public UploadManager(final MainPanel main_panel, final String toto) {

        super(main_panel, main_panel.getMax_ul(), null, null, null, null, null);

        this._log_file_lock = new Object();
    }

    public Object getLog_file_lock() {
        return this._log_file_lock;
    }

    @Override
    public void provision(final Transference upload) {
//        MiscTools.GUIRun(() -> {
//            this.getScroll_panel().add(((Upload) upload).getView());
//        });

        ((Upload) upload).provisionIt();

        if (((Upload) upload).isProvision_ok()) {

            this.increment_total_size(upload.getFile_size());

            this.getTransference_waitstart_aux_queue().add(upload);

        } else {

            this.getTransference_finished_queue().add(upload);
        }

        this.secureNotify();
    }

    @Override
    public void remove(final Transference[] uploads) {

        final ArrayList<String[]> delete_up = new ArrayList<>();

        for (final Transference u : uploads) {

//            MiscTools.GUIRun(() -> {
//                this.getScroll_panel().remove(((Upload) u).getView());
//            });

            this.getTransference_waitstart_queue().remove(u);

            this.getTransference_running_list().remove(u);

            this.getTransference_finished_queue().remove(u);

            this.increment_total_size(-1 * u.getFile_size());

            this.increment_total_progress(-1 * u.getProgress());

            if (!u.isCanceled() || u.isClosed()) {
                delete_up.add(new String[]{u.getFile_name(), ((Upload) u).getMa().getFull_email()});
            }
        }

        try {
            DBTools.deleteUploads(delete_up.toArray(new String[delete_up.size()][]));
        } catch (final SQLException ex) {
            LOG.log(SEVERE, null, ex);
        }

        this.secureNotify();
    }

    public int copyAllLinksToClipboard() {

        int total = 0;

        final ArrayList<String> links = new ArrayList<>();

        String out = "";

        for (final Transference t : this._transference_waitstart_aux_queue) {
            final Upload up = (Upload) t;
            links.add(up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : ""));
        }

        for (final Transference t : this._transference_waitstart_queue) {

            final Upload up = (Upload) t;
            links.add(up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : ""));
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        for (final Transference t : this._transference_running_list) {

            final Upload up = (Upload) t;
            links.add(up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : "") + (up.getFile_link() != null ? " " + up.getFile_link() : ""));
        }

        out += String.join("\r\n", links);

        total += links.size();

        links.clear();

        for (final Transference t : this._transference_finished_queue) {

            final Upload up = (Upload) t;
            links.add("(UPLOAD FINISHED) " + up.getFile_name() + " [" + up.getMa().getEmail() + "] " + (up.getFolder_link() != null ? up.getFolder_link() : "") + (up.getFile_link() != null ? " " + up.getFile_link() : ""));
        }

        out += String.join("\r\n", links);

        total += links.size();

        MiscTools.copyTextToClipboard(out);

        return total;

    }

}
