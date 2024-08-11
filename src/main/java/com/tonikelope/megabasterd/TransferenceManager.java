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

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static java.util.logging.Level.SEVERE;

/**
 * Yes, this class is a f*cking mess (inside "natural" MegaBasterd mess) and
 * should be completely refactored (in another life maybe...).
 *
 * @author tonikelope
 */
abstract public class TransferenceManager implements Runnable, SecureSingleThreadNotifiable {

    public static final int MAX_WAIT_QUEUE = 10000;
    public static final int MAX_PROVISION_WORKERS = 50;
    private static final Logger LOG = Logger.getLogger(TransferenceManager.class.getName());

    protected final ConcurrentLinkedQueue<Object> _transference_preprocess_global_queue;
    protected final ConcurrentLinkedQueue<Runnable> _transference_preprocess_queue;
    protected final ConcurrentLinkedQueue<Transference> _transference_provision_queue;
    protected final ConcurrentLinkedQueue<Transference> _transference_waitstart_queue;
    protected final ConcurrentLinkedQueue<Transference> _transference_waitstart_aux_queue;
    protected final ConcurrentLinkedQueue<Transference> _transference_remove_queue;
    protected final ConcurrentLinkedQueue<Transference> _transference_finished_queue;
    protected final ConcurrentLinkedQueue<Transference> _transference_running_list;

    //    private javax.swing.JPanel _scroll_panel;
//    private javax.swing.JLabel _status;
//    private javax.swing.JButton _close_all_button;
//    private javax.swing.JButton _pause_all_button;
//    private javax.swing.MenuElement _clean_all_menu;
    private int _max_running_trans;
    private final MainPanel _main_panel;
    private final Object _secure_notify_lock;
    private final Object _wait_queue_lock;
    private final Object _pause_all_lock;
    private volatile boolean _notified;
    private volatile boolean _removing_transferences;
    private volatile boolean _provisioning_transferences;
    private volatile boolean _starting_transferences;
    private volatile boolean _preprocessing_transferences;
    private volatile boolean _paused_all;
    private boolean _tray_icon_finish;
    protected volatile long _total_size;
    protected final Object _total_size_lock;
    protected volatile long _total_progress;
    protected final Object _total_progress_lock;
    protected final Object _transference_queue_sort_lock;
    private volatile Boolean _sort_wait_start_queue;
    protected volatile boolean _all_finished = false;

    public boolean isAll_finished() {
        return this._all_finished;
    }

    public void setAll_finished(final boolean all_finished) {
        this._all_finished = all_finished;
    }

    public TransferenceManager(final MainPanel main_panel) {
        this._notified = false;
        this._paused_all = false;
        this._removing_transferences = false;
        this._provisioning_transferences = false;
        this._starting_transferences = false;
        this._preprocessing_transferences = false;
        this._tray_icon_finish = false;
        this._main_panel = main_panel;
        this._pause_all_lock = new Object();
        this._total_size = 0L;
        this._total_progress = 0L;
        this._secure_notify_lock = new Object();
        this._total_size_lock = new Object();
        this._total_progress_lock = new Object();
        this._transference_queue_sort_lock = new Object();
        this._wait_queue_lock = new Object();
        this._sort_wait_start_queue = true;
        this._transference_preprocess_global_queue = new ConcurrentLinkedQueue<>();
        this._transference_waitstart_queue = new ConcurrentLinkedQueue<>();
        this._transference_waitstart_aux_queue = new ConcurrentLinkedQueue<>();
        this._transference_provision_queue = new ConcurrentLinkedQueue<>();
        this._transference_remove_queue = new ConcurrentLinkedQueue<>();
        this._transference_finished_queue = new ConcurrentLinkedQueue<>();
        this._transference_running_list = new ConcurrentLinkedQueue<>();
        this._transference_preprocess_queue = new ConcurrentLinkedQueue<>();
    }

    public TransferenceManager(final MainPanel main_panel, final int max_running_trans, final javax.swing.JLabel status, final javax.swing.JPanel scroll_panel, final javax.swing.JButton close_all_button, final javax.swing.JButton pause_all_button, final javax.swing.MenuElement clean_all_menu) {
        this._notified = false;
        this._paused_all = false;
        this._removing_transferences = false;
        this._provisioning_transferences = false;
        this._starting_transferences = false;
        this._preprocessing_transferences = false;
        this._tray_icon_finish = false;
        this._main_panel = main_panel;
        this._max_running_trans = max_running_trans;
//        this._scroll_panel = scroll_panel;
//        this._status = status;
        this._pause_all_lock = new Object();
//        this._close_all_button = close_all_button;
//        this._pause_all_button = pause_all_button;
//        this._clean_all_menu = clean_all_menu;
        this._total_size = 0L;
        this._total_progress = 0L;
        this._secure_notify_lock = new Object();
        this._total_size_lock = new Object();
        this._total_progress_lock = new Object();
        this._transference_queue_sort_lock = new Object();
        this._wait_queue_lock = new Object();
        this._sort_wait_start_queue = true;
        this._transference_preprocess_global_queue = new ConcurrentLinkedQueue<>();
        this._transference_waitstart_queue = new ConcurrentLinkedQueue<>();
        this._transference_waitstart_aux_queue = new ConcurrentLinkedQueue<>();
        this._transference_provision_queue = new ConcurrentLinkedQueue<>();
        this._transference_remove_queue = new ConcurrentLinkedQueue<>();
        this._transference_finished_queue = new ConcurrentLinkedQueue<>();
        this._transference_running_list = new ConcurrentLinkedQueue<>();
        this._transference_preprocess_queue = new ConcurrentLinkedQueue<>();
    }

    public Boolean getSort_wait_start_queue() {
        return this._sort_wait_start_queue;
    }

    public void setSort_wait_start_queue(final Boolean sort_wait_start_queue) {
        this._sort_wait_start_queue = sort_wait_start_queue;
    }

    public void setPaused_all(final boolean _paused_all) {
        this._paused_all = _paused_all;
    }

    public boolean no_transferences() {
        return this.getTransference_preprocess_queue().isEmpty() && this.getTransference_provision_queue().isEmpty() && this.getTransference_waitstart_queue().isEmpty() && this.getTransference_running_list().isEmpty();
    }

    public boolean isPaused_all() {
        return this._paused_all;
    }

    public Object getWait_queue_lock() {
        return this._wait_queue_lock;
    }

    public ConcurrentLinkedQueue<Object> getTransference_preprocess_global_queue() {
        return this._transference_preprocess_global_queue;
    }

    abstract public void provision(Transference transference);

    abstract public void remove(Transference[] transference);

    public boolean isRemoving_transferences() {

        return this._removing_transferences;
    }

    public void setRemoving_transferences(final boolean removing) {
        this._removing_transferences = removing;
    }

    public long get_total_size() {

        synchronized (this._total_size_lock) {
            return this._total_size;
        }
    }

    public void increment_total_size(final long val) {

        synchronized (this._total_size_lock) {

            this._total_size += val;
        }
    }

    public long get_total_progress() {

        synchronized (this._total_progress_lock) {
            return this._total_progress;
        }
    }

    public void increment_total_progress(final long val) {

        synchronized (this._total_progress_lock) {

            this._total_progress += val;
        }
    }

    public boolean isProvisioning_transferences() {
        return this._provisioning_transferences;
    }

    public void setProvisioning_transferences(final boolean provisioning) {
        this._provisioning_transferences = provisioning;
    }

    public boolean isStarting_transferences() {
        return this._starting_transferences;
    }

    public void setStarting_transferences(final boolean starting) {
        this._starting_transferences = starting;
    }

    public void setPreprocessing_transferences(final boolean preprocessing) {
        this._preprocessing_transferences = preprocessing;
    }

    public ConcurrentLinkedQueue<Runnable> getTransference_preprocess_queue() {
        return this._transference_preprocess_queue;
    }

    public void setMax_running_trans(final int max_running_trans) {
        this._max_running_trans = max_running_trans;
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
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            this._notified = false;
        }
    }

    public MainPanel getMain_panel() {
        return this._main_panel;
    }

    public boolean isPreprocessing_transferences() {

        return this._preprocessing_transferences;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_provision_queue() {

        return this._transference_provision_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_waitstart_queue() {

        return this._transference_waitstart_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_remove_queue() {

        return this._transference_remove_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_finished_queue() {

        return this._transference_finished_queue;

    }

    public ConcurrentLinkedQueue<Transference> getTransference_running_list() {

        return this._transference_running_list;

    }

//    public JPanel getScroll_panel() {
//        return this._scroll_panel;
//    }

    public void closeAllFinished() {

        this._transference_finished_queue.stream().filter((t) -> !t.isCanceled()).map((t) -> {
            this._transference_finished_queue.remove(t);
            return t;
        }).forEachOrdered((t) -> {
            this._transference_remove_queue.add(t);
        });

        this.secureNotify();
    }

    public int calcTotalSlotsCount() {

        int slots = 0;

        slots = this._transference_running_list.stream().map((trans) -> trans.getSlotsCount()).reduce(slots, Integer::sum);

        return slots;

    }

    public void closeAllPreProWaiting() {
        this._transference_preprocess_queue.clear();

        this._transference_preprocess_global_queue.clear();

        this._transference_provision_queue.clear();

        this._transference_remove_queue.addAll(new ArrayList(this.getTransference_waitstart_queue()));

        this.getTransference_waitstart_queue().clear();

        synchronized (this.getWait_queue_lock()) {
            this.getWait_queue_lock().notifyAll();
        }

        this.secureNotify();
    }

    public void cancelAllTransferences() {
        this._transference_preprocess_queue.clear();

        this._transference_preprocess_global_queue.clear();

        this._transference_provision_queue.clear();

        this._transference_remove_queue.addAll(new ArrayList(this.getTransference_waitstart_aux_queue()));

        this._transference_remove_queue.addAll(new ArrayList(this.getTransference_waitstart_queue()));

        this.getTransference_waitstart_queue().clear();

        this.getTransference_waitstart_aux_queue().clear();

        for (final Transference t : this.getTransference_running_list()) {

            if (t instanceof Download) {
                ((Download) t).setGlobal_cancel(true);
            }

            t.stop();
        }

        synchronized (this.getWait_queue_lock()) {
            this.getWait_queue_lock().notifyAll();
        }

        this.secureNotify();
    }

    public void topWaitQueue(final Transference t) {

        synchronized (this.getWait_queue_lock()) {

            final ArrayList<Transference> wait_array = new ArrayList();

            wait_array.add(t);

            for (final Transference t1 : this.getTransference_waitstart_queue()) {

                if (t1 != t) {
                    wait_array.add(t1);
                }
            }

            this.getTransference_waitstart_queue().clear();

            this.getTransference_waitstart_queue().addAll(wait_array);

//            this.getTransference_waitstart_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
//            this.getTransference_finished_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
        }

        this.secureNotify();
    }

    public void bottomWaitQueue(final Transference t) {

        synchronized (this.getWait_queue_lock()) {

            final ArrayList<Transference> wait_array = new ArrayList();

            for (final Transference t1 : this.getTransference_waitstart_queue()) {

                if (t1 != t) {
                    wait_array.add(t1);
                }
            }

            wait_array.add(t);

            this.getTransference_waitstart_queue().clear();

            this.getTransference_waitstart_queue().addAll(wait_array);

//            this.getTransference_waitstart_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
//            this.getTransference_finished_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
        }

        this.secureNotify();

    }

    public void upWaitQueue(final Transference t) {

        synchronized (this.getWait_queue_lock()) {

            final ArrayList<Transference> wait_array = new ArrayList(this.getTransference_waitstart_queue());

            int pos = 0;

            for (final Transference t1 : wait_array) {

                if (t1 == t) {
                    break;
                }

                pos++;
            }

            if (pos > 0) {
                Collections.swap(wait_array, pos, pos - 1);
            }

            this.getTransference_waitstart_queue().clear();

            this.getTransference_waitstart_queue().addAll(wait_array);

//            this.getTransference_waitstart_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
//            this.getTransference_finished_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
        }

        this.secureNotify();

    }

    public void downWaitQueue(final Transference t) {

        synchronized (this.getWait_queue_lock()) {

            final ArrayList<Transference> wait_array = new ArrayList(this.getTransference_waitstart_queue());

            int pos = 0;

            for (final Transference t1 : wait_array) {

                if (t1 == t) {
                    break;
                }

                pos++;
            }

            if (pos < wait_array.size() - 1) {
                Collections.swap(wait_array, pos, pos + 1);
            }

            this.getTransference_waitstart_queue().clear();

            this.getTransference_waitstart_queue().addAll(wait_array);

//            this.getTransference_waitstart_queue().forEach((t1) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t1.getView());
//                    this.getScroll_panel().add((Component) t1.getView());
//                });
//            });
//            this.getTransference_finished_queue().forEach((t2) -> {
//                MiscTools.GUIRun(() -> {
//                    this.getScroll_panel().remove((Component) t2.getView());
//                    this.getScroll_panel().add((Component) t2.getView());
//                });
//            });
        }

        this.secureNotify();
    }

    public void start(final Transference transference) {

        this._transference_running_list.add(transference);

        transference.start();
    }

    public void pauseAll() {

        this._transference_running_list.forEach((transference) -> {

            if (!transference.isPaused()) {
                transference.pause();
            }

        });

        this.secureNotify();

        THREAD_POOL.execute(() -> {

            boolean running;

            do {

                running = false;

                for (final Transference t : this._transference_running_list) {
                    if (t.getPausedWorkers() != t.getTotWorkers()) {
                        running = true;
                        break;
                    }
                }

                if (running) {
                    synchronized (this._pause_all_lock) {

                        try {
                            this._pause_all_lock.wait(1000);
                        } catch (final InterruptedException ex) {
                            Logger.getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }

            } while (running);

            this._paused_all = true;

//            MiscTools.GUIRun(() -> {
//
//                this._pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("RESUME ALL"));
//                this._pause_all_button.setEnabled(true);
//
//            });

            this.secureNotify();

        });

    }

    public void resumeAll() {

        this._transference_running_list.forEach((transference) -> {

            if (transference.isPaused()) {
                transference.pause();
            }

        });

        this._paused_all = false;

//        MiscTools.GUIRun(() -> {
//
//            this._pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("PAUSE ALL"));
//
//            this._pause_all_button.setEnabled(true);
//
//        });

        this.secureNotify();
    }

    protected boolean hasFrozenTransferences() {

        for (final Transference t : this.getTransference_waitstart_queue()) {

            if (t.isFrozen()) {
                return true;
            }
        }

        for (final Transference t : this.getTransference_waitstart_aux_queue()) {

            if (t.isFrozen()) {
                return true;
            }
        }

        return false;
    }

    public ConcurrentLinkedQueue<Transference> getTransference_waitstart_aux_queue() {
        return this._transference_waitstart_aux_queue;
    }

    protected void sortTransferenceQueue(final ConcurrentLinkedQueue<Transference> queue) {

        synchronized (this._transference_queue_sort_lock) {

            final ArrayList<Transference> trans_list = new ArrayList(queue);

            trans_list.sort((Transference o1, Transference o2) -> MiscTools.naturalCompare(o1.getFile_name(), o2.getFile_name(), true));

            queue.clear();

            queue.addAll(trans_list);
        }
    }

    protected void unfreezeTransferenceWaitStartQueue() {

        synchronized (this.getTransference_waitstart_aux_queue()) {

            this.getTransference_waitstart_queue().forEach((t) -> {
                t.unfreeze();
            });

            this.getTransference_waitstart_aux_queue().forEach((t) -> {
                t.unfreeze();
            });
        }

        this.secureNotify();
    }

    private void _updateView() {

//        MiscTools.GUIRun(() -> {
//
//            if (this instanceof DownloadManager) {
//
//                this._main_panel.getView().getForce_chunk_reset_button().setVisible(MainPanel.isUse_smart_proxy() && !this.getTransference_running_list().isEmpty());
//                this._main_panel.getView().getCancel_all_downloads_menu().setEnabled(!this._transference_preprocess_queue.isEmpty() || !this._transference_provision_queue.isEmpty() || !this.getTransference_waitstart_queue().isEmpty() || !this.getTransference_running_list().isEmpty());
//                this._main_panel.getView().getDownload_status_bar().setVisible(!this._transference_preprocess_global_queue.isEmpty() || !this._transference_preprocess_queue.isEmpty() || !this._transference_provision_queue.isEmpty());
//
//            } else {
//                this._main_panel.getView().getUpload_status_bar().setVisible(!this._transference_preprocess_global_queue.isEmpty() || !this._transference_preprocess_queue.isEmpty() || !this._transference_provision_queue.isEmpty());
//            }
//
//            if (this._paused_all) {
//                this._pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("RESUME ALL"));
//            } else {
//                this._pause_all_button.setText(LabelTranslatorSingleton.getInstance().translate("PAUSE ALL"));
//                this._pause_all_button.setVisible(!this.getTransference_running_list().isEmpty());
//            }
//
//            this._clean_all_menu.getComponent().setEnabled(!this._transference_preprocess_queue.isEmpty() || !this._transference_provision_queue.isEmpty() || !this.getTransference_waitstart_queue().isEmpty());
//
//            if (!this._transference_finished_queue.isEmpty()) {
//
//                this._close_all_button.setText(LabelTranslatorSingleton.getInstance().translate("Clear finished"));
//
//                this._close_all_button.setVisible(true);
//
//            } else {
//
//                this._close_all_button.setVisible(false);
//            }
//
//            this._status.setText(this._genStatus());
//
//            this._main_panel.getView().getUnfreeze_transferences_button().setVisible(this._main_panel.getDownload_manager().hasFrozenTransferences() || this._main_panel.getUpload_manager().hasFrozenTransferences());
//
//            this._main_panel.getView().revalidate();
//
//            this._main_panel.getView().repaint();
//        });
    }

    private String _genStatus() {

        final int pre = this._transference_preprocess_global_queue.size();

        final int prov = this._transference_provision_queue.size();

        final int rem = this._transference_remove_queue.size();

        final int wait = this._transference_waitstart_queue.size() + this._transference_waitstart_aux_queue.size();

        final int run = this._transference_running_list.size();

        final int finish = this._transference_finished_queue.size();

        if (!this._all_finished && !this._tray_icon_finish && finish > 0 && pre + prov + wait + run == 0 && !this._main_panel.getView().isVisible()) {

            this._tray_icon_finish = true;

            this._all_finished = true;

//            this._main_panel.getTrayicon().displayMessage("MegaBasterd says:", "All your transferences have finished", TrayIcon.MessageType.INFO);
        }

        return (pre + prov + rem + wait + run + finish > 0) ? LabelTranslatorSingleton.getInstance().translate("Pre:") + " " + pre + " / " + LabelTranslatorSingleton.getInstance().translate("Pro:") + " " + prov + " / " + LabelTranslatorSingleton.getInstance().translate("Wait:") + " " + wait + " / " + LabelTranslatorSingleton.getInstance().translate("Run:") + " " + run + " / " + LabelTranslatorSingleton.getInstance().translate("Finish:") + " " + finish + " / " + LabelTranslatorSingleton.getInstance().translate("Rem:") + " " + rem : "";
    }

    private boolean _isOKFinishedInQueue() {

        return this._transference_finished_queue.stream().anyMatch((t) -> (!t.isStatusError() && !t.isCanceled()));
    }

    @Override
    public void run() {

        while (true) {

            if (!this.isRemoving_transferences() && !this.getTransference_remove_queue().isEmpty()) {

                this.setRemoving_transferences(true);

                THREAD_POOL.execute(() -> {

                    if (!this.getTransference_remove_queue().isEmpty()) {

                        final ArrayList<Transference> transferences = new ArrayList(this.getTransference_remove_queue());

                        this.getTransference_remove_queue().clear();

                        this.remove(transferences.toArray(new Transference[transferences.size()]));
                    }

                    this.setRemoving_transferences(false);

                    this.secureNotify();
                });
            }

            if (!this.isPreprocessing_transferences() && !this.getTransference_preprocess_queue().isEmpty()) {

                this.setPreprocessing_transferences(true);

                if (this.isPaused_all()) {

                    this._paused_all = false;
                }

                THREAD_POOL.execute(() -> {

                    while (!this.getTransference_preprocess_queue().isEmpty()) {
                        final Runnable run = this.getTransference_preprocess_queue().poll();

                        if (run != null) {

                            boolean run_error;

                            do {
                                run_error = false;

                                try {
                                    run.run();
                                } catch (final Exception ex) {
                                    run_error = true;
                                    LOG.log(SEVERE, null, ex);
                                }
                            } while (run_error);
                        }
                    }

                    this.setPreprocessing_transferences(false);

                    synchronized (this.getTransference_preprocess_queue()) {
                        this.getTransference_preprocess_queue().notifyAll();
                    }

                    this.secureNotify();
                });
            }

            if (!this.isRemoving_transferences() && !this.isProvisioning_transferences() && !this.getTransference_provision_queue().isEmpty()) {

                this.setProvisioning_transferences(true);

                this._tray_icon_finish = false;

                THREAD_POOL.execute(() -> {

                    final ExecutorService executor = Executors.newFixedThreadPool(MAX_PROVISION_WORKERS);

                    final BoundedExecutor bounded_executor = new BoundedExecutor(executor, MAX_PROVISION_WORKERS);

                    while (!this.getTransference_provision_queue().isEmpty() || this.isPreprocessing_transferences()) {

                        if (this.getTransference_waitstart_aux_queue().size() < MAX_WAIT_QUEUE && this.getTransference_waitstart_queue().size() < MAX_WAIT_QUEUE) {

                            final Transference transference = this.getTransference_provision_queue().poll();

                            if (transference != null) {

                                boolean error;

                                do {
                                    error = false;

                                    try {
                                        bounded_executor.submitTask(() -> {
                                            this.provision(transference);

                                            if (this instanceof DownloadManager) {
                                                ((Download) transference).start();
                                            }
//                                                MiscTools.GUIRun(() -> {
//
//                                                    this._main_panel.getView().getDownload_status_bar().setIndeterminate(false);
//                                                    this._main_panel.getView().getDownload_status_bar().setValue(this._main_panel.getView().getDownload_status_bar().getValue() + 1);
//                                                    this._main_panel.getView().getDownload_status_bar().setVisible((this._main_panel.getView().getDownload_status_bar().getValue() < this._main_panel.getView().getDownload_status_bar().getMaximum()));
//
//                                                });
//                                            } else {
//                                                MiscTools.GUIRun(() -> {
//
//                                                    this._main_panel.getView().getUpload_status_bar().setIndeterminate(false);
//                                                    this._main_panel.getView().getUpload_status_bar().setValue(this._main_panel.getView().getUpload_status_bar().getValue() + 1);
//                                                    this._main_panel.getView().getUpload_status_bar().setVisible((this._main_panel.getView().getUpload_status_bar().getValue() < this._main_panel.getView().getUpload_status_bar().getMaximum()));
//
//                                                });
//                                            }
                                        });
                                    } catch (final InterruptedException ex) {
                                        Logger.getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                                        error = true;
                                        MiscTools.pausar(1000);
                                    }
                                } while (error);
                            }
                        }

                        if (this.isPreprocessing_transferences() || this.getTransference_waitstart_aux_queue().size() >= MAX_WAIT_QUEUE || this.getTransference_waitstart_queue().size() >= MAX_WAIT_QUEUE) {

                            synchronized (this.getTransference_preprocess_queue()) {
                                try {
                                    this.getTransference_preprocess_queue().wait(1000);
                                } catch (final InterruptedException ex) {
                                    Logger.getLogger(TransferenceManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }

                    executor.shutdown();

                    while (!executor.isTerminated()) {
                        MiscTools.pausar(1000);
                    }

                    synchronized (this._transference_queue_sort_lock) {

                        if (this.getSort_wait_start_queue()) {
                            this.sortTransferenceQueue(this.getTransference_waitstart_aux_queue());
                        }

                        if (this.getTransference_waitstart_aux_queue().peek() != null && this.getTransference_waitstart_aux_queue().peek().isPriority()) {

                            final ArrayList<Transference> trans_list = new ArrayList(this.getTransference_waitstart_queue());

                            trans_list.addAll(0, this.getTransference_waitstart_aux_queue());

                            this.getTransference_waitstart_queue().clear();

                            this.getTransference_waitstart_queue().addAll(trans_list);

                        } else {
                            this.getTransference_waitstart_queue().addAll(this.getTransference_waitstart_aux_queue());
                        }

                        this.getTransference_waitstart_aux_queue().clear();

//                        this.getTransference_waitstart_queue().forEach((t) -> {
//                            MiscTools.GUIRun(() -> {
//                                this.getScroll_panel().remove((Component) t.getView());
//                                this.getScroll_panel().add((Component) t.getView());
//                            });
//                        });

                        this.sortTransferenceQueue(this.getTransference_finished_queue());

//                        this.getTransference_finished_queue().forEach((t) -> {
//                            MiscTools.GUIRun(() -> {
//                                this.getScroll_panel().remove((Component) t.getView());
//                                this.getScroll_panel().add((Component) t.getView());
//                            });
//                        });

                    }

                    this.setSort_wait_start_queue(true);
                    this.setProvisioning_transferences(false);
                    this.secureNotify();
                });

            }

            if (!this._main_panel.isExit() && !this._paused_all && !this.isRemoving_transferences() && !this.isStarting_transferences() && (!this.getTransference_waitstart_queue().isEmpty() || !this.getTransference_waitstart_aux_queue().isEmpty()) && this.getTransference_running_list().size() < this._max_running_trans) {

                this.setStarting_transferences(true);

                THREAD_POOL.execute(() -> {

                    while (!this._main_panel.isExit() && !this._paused_all && (!this.getTransference_waitstart_queue().isEmpty() || !this.getTransference_waitstart_aux_queue().isEmpty()) && this.getTransference_running_list().size() < this._max_running_trans) {

                        synchronized (this._transference_queue_sort_lock) {
                            Transference transference = this.getTransference_waitstart_queue().peek();

                            if (transference == null) {
                                transference = this.getTransference_waitstart_aux_queue().peek();
                            }

                            if (transference != null && !transference.isFrozen()) {

                                this.getTransference_waitstart_queue().remove(transference);

                                this.getTransference_waitstart_aux_queue().remove(transference);

                                this.start(transference);

                            }
                        }
                    }

                    synchronized (this.getWait_queue_lock()) {
                        this.getWait_queue_lock().notifyAll();
                    }

                    this.setStarting_transferences(false);

                    this.secureNotify();
                });
            }

            this.secureWait();

            this._updateView();
        }

    }

}
