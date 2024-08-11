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

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.formatBytes;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static java.lang.Integer.MAX_VALUE;

/**
 * @author tonikelope
 */
public class Upload implements Transference, Runnable, SecureSingleThreadNotifiable {

    public static final int WORKERS_DEFAULT = 6;
    public static final boolean DEFAULT_THUMBNAILS = true;
    public static final boolean UPLOAD_LOG = true;
    public static final boolean UPLOAD_PUBLIC_FOLDER = false;
    private static final Logger LOG = Logger.getLogger(Upload.class.getName());
    private final MainPanel _main_panel;
    private final UploadView _view;
    private final ProgressMeter _progress_meter;
    private final Object _progress_lock;
    private volatile String _status_error;
    private volatile String _thumbnail_file = "";
    private volatile boolean _exit;
    private volatile boolean _frozen;
    private int _slots;
    private final Object _secure_notify_lock;
    private final Object _workers_lock;
    private final Object _chunkid_lock;
    private byte[] _byte_file_key;
    private volatile long _progress;
    private byte[] _byte_file_iv;
    private final ConcurrentLinkedQueue<Long> _rejectedChunkIds;
    private long _last_chunk_id_dispatched;
    private final ConcurrentLinkedQueue<Long> _partialProgressQueue;
    private final ExecutorService _thread_pool;
    private volatile int[] _file_meta_mac;
    private String _fid;
    private volatile boolean _notified;
    private volatile String _completion_handler;
    private int _paused_workers;
    private Double _progress_bar_rate;
    private volatile boolean _pause;
    private final ArrayList<ChunkUploader> _chunkworkers;
    private long _file_size;
    private UploadMACGenerator _mac_generator;
    private boolean _create_dir;
    private boolean _provision_ok;
    private boolean _auto_retry_on_error;
    private String _file_link;
    private final MegaAPI _ma;
    private final String _file_name;
    private final String _parent_node;
    private int[] _ul_key;
    private String _ul_url;
    private final String _root_node;
    private final byte[] _share_key;
    private final String _folder_link;
    private final boolean _restart;
    private volatile boolean _closed;
    private volatile boolean _canceled;
    private volatile String _temp_mac_data;
    private final boolean _priority;
    private final Object _progress_watchdog_lock;
    private volatile boolean _finalizing;

    public Upload(final MainPanel main_panel, final MegaAPI ma, final String filename, final String parent_node, final int[] ul_key, final String ul_url, final String root_node, final byte[] share_key, final String folder_link, final boolean priority) {

        this._notified = false;
        this._priority = priority;
        this._progress_watchdog_lock = new Object();
        this._frozen = main_panel.isInit_paused();
        this._provision_ok = false;
        this._status_error = null;
        this._auto_retry_on_error = true;
        this._canceled = false;
        this._closed = false;
        this._finalizing = false;
        this._main_panel = main_panel;
        this._ma = ma;
        this._file_name = filename;
        this._parent_node = parent_node;
        this._ul_key = ul_key;
        this._ul_url = ul_url;
        this._root_node = root_node;
        this._share_key = share_key;
        this._folder_link = folder_link;
        this._restart = false;
        this._progress = 0L;
        this._last_chunk_id_dispatched = 0L;
        this._completion_handler = null;
        this._secure_notify_lock = new Object();
        this._workers_lock = new Object();
        this._chunkid_lock = new Object();
        this._chunkworkers = new ArrayList<>();
        this._progress_lock = new Object();
        this._partialProgressQueue = new ConcurrentLinkedQueue<>();
        this._rejectedChunkIds = new ConcurrentLinkedQueue<>();
        this._thread_pool = Executors.newCachedThreadPool();
        this._view = new UploadView(this);
        this._progress_meter = new ProgressMeter(this);
        this._file_meta_mac = null;
        this._temp_mac_data = null;
    }

    public Upload(final Upload upload) {

        this._notified = false;
        this._priority = upload.isPriority();
        this._progress_watchdog_lock = new Object();
        this._provision_ok = false;
        this._status_error = null;
        this._auto_retry_on_error = true;
        this._canceled = upload.isCanceled();
        this._finalizing = false;
        this._closed = false;
        this._restart = true;
        this._main_panel = upload.getMain_panel();
        this._ma = upload.getMa();
        this._file_name = upload.getFile_name();
        this._parent_node = upload.getParent_node();
        this._progress_lock = new Object();
        this._ul_key = upload.getUl_key();
        this._ul_url = upload.getUl_url();
        this._root_node = upload.getRoot_node();
        this._share_key = upload.getShare_key();
        this._folder_link = upload.getFolder_link();
        this._progress = 0L;
        this._last_chunk_id_dispatched = 0L;
        this._completion_handler = null;
        this._secure_notify_lock = new Object();
        this._workers_lock = new Object();
        this._chunkid_lock = new Object();
        this._chunkworkers = new ArrayList<>();
        this._partialProgressQueue = new ConcurrentLinkedQueue<>();
        this._rejectedChunkIds = new ConcurrentLinkedQueue<>();
        this._thread_pool = Executors.newCachedThreadPool();
        this._view = new UploadView(this);
        this._progress_meter = new ProgressMeter(this);
        this._file_meta_mac = null;
        this._temp_mac_data = upload.getTemp_mac_data();
    }

    @Override
    public boolean isPriority() {
        return this._priority;
    }

    @Override
    public boolean isCanceled() {
        return this._canceled;
    }

    public String getTemp_mac_data() {
        return this._temp_mac_data;
    }

    public void setTemp_mac_data(final String temp_mac_data) {
        this._temp_mac_data = temp_mac_data;
    }

    public Object getWorkers_lock() {
        return this._workers_lock;
    }

    public boolean isExit() {
        return this._exit;
    }

    public int getSlots() {
        return this._slots;
    }

    public Object getSecure_notify_lock() {
        return this._secure_notify_lock;
    }

    public byte[] getByte_file_key() {
        return this._byte_file_key;
    }

    @Override
    public long getProgress() {
        return this._progress;
    }

    public byte[] getByte_file_iv() {
        return this._byte_file_iv;
    }

    public ConcurrentLinkedQueue<Long> getRejectedChunkIds() {
        return this._rejectedChunkIds;
    }

    public long getLast_chunk_id_dispatched() {
        return this._last_chunk_id_dispatched;
    }

    public ExecutorService getThread_pool() {
        return this._thread_pool;
    }

    public String getFid() {
        return this._fid;
    }

    public boolean isNotified() {
        return this._notified;
    }

    public String getCompletion_handler() {
        return this._completion_handler;
    }

    public int getPaused_workers() {
        return this._paused_workers;
    }

    public Double getProgress_bar_rate() {
        return this._progress_bar_rate;
    }

    public boolean isPause() {
        return this._pause;
    }

    public ArrayList<ChunkUploader> getChunkworkers() {

        synchronized (this._workers_lock) {
            return this._chunkworkers;
        }

    }

    @Override
    public long getFile_size() {
        return this._file_size;
    }

    public UploadMACGenerator getMac_generator() {
        return this._mac_generator;
    }

    public boolean isCreate_dir() {
        return this._create_dir;
    }

    public boolean isProvision_ok() {
        return this._provision_ok;
    }

    public String getFile_link() {
        return this._file_link;
    }

    public MegaAPI getMa() {
        return this._ma;
    }

    @Override
    public String getFile_name() {
        return this._file_name;
    }

    public String getParent_node() {
        return this._parent_node;
    }

    public int[] getUl_key() {
        return this._ul_key;
    }

    public String getUl_url() {
        return this._ul_url;
    }

    public String getRoot_node() {
        return this._root_node;
    }

    public byte[] getShare_key() {
        return this._share_key;
    }

    public String getFolder_link() {
        return this._folder_link;
    }

    @Override
    public boolean isRestart() {
        return this._restart;
    }

    public void setCompletion_handler(final String completion_handler) {
        this._completion_handler = completion_handler;
    }

    public void setFile_meta_mac(final int[] file_meta_mac) {
        this._file_meta_mac = file_meta_mac;
    }

    public void setPaused_workers(final int paused_workers) {
        this._paused_workers = paused_workers;
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
    public UploadView getView() {

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
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }

            this._notified = false;
        }
    }

    public void provisionIt() {

        this.getView().printStatusNormal("Provisioning upload, please wait...");

        final File the_file = new File(this._file_name);

        this._provision_ok = false;

        if (!the_file.exists()) {

            this._status_error = "ERROR: FILE NOT FOUND";

        } else {

            try {
                this._file_size = the_file.length();

                this._progress_bar_rate = Integer.MAX_VALUE / (double) this._file_size;

                final HashMap upload_progress = DBTools.selectUploadProgress(this.getFile_name(), this.getMa().getFull_email());

                if (upload_progress == null) {

                    if (this._ul_key == null) {

                        this._ul_key = this._ma.genUploadKey();

                        DBTools.insertUpload(this._file_name, this._ma.getFull_email(), this._parent_node, Bin2BASE64(i32a2bin(this._ul_key)), this._root_node, Bin2BASE64(this._share_key), this._folder_link);
                    }

                    this._provision_ok = true;

                } else {

                    this._last_chunk_id_dispatched = this.calculateLastUploadedChunk((long) upload_progress.get("bytes_uploaded"));

                    this.setProgress((long) upload_progress.get("bytes_uploaded"));

                    this._provision_ok = true;

                    LOG.log(Level.INFO, "LAST CHUNK ID UPLOADED -> {0}", this._last_chunk_id_dispatched);
                }

            } catch (final SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }

        if (!this._provision_ok) {

            if (this._status_error == null) {
                this._status_error = "PROVISION FAILED";
            }

            if (this._file_name != null) {
                MiscTools.GUIRun(() -> {
                    this.getView().getFile_name_label().setVisible(true);

                    this.getView().getFile_name_label().setText(truncateText(new File(this._file_name).getName(), 150));

                    this.getView().getFile_name_label().setToolTipText(this._file_name);

                    this.getView().getFile_size_label().setVisible(true);

                    this.getView().getFile_size_label().setText(formatBytes(this._file_size));
                });
            }

            this.getView().hideAllExceptStatus();

            this.getView().printStatusError(this._status_error);

            MiscTools.GUIRun(() -> {
                this.getView().getRestart_button().setVisible(true);
            });

        } else {

            this.getView().printStatusNormal(LabelTranslatorSingleton.getInstance().translate(this._frozen ? "(FROZEN) Waiting to start (" : "Waiting to start (") + this._ma.getFull_email() + ") ...");

            MiscTools.GUIRun(() -> {
                this.getView().getFile_name_label().setVisible(true);

                this.getView().getFile_name_label().setText(truncateText(new File(this._file_name).getName(), 150));

                this.getView().getFile_name_label().setToolTipText(this._file_name);

                this.getView().getFile_size_label().setVisible(true);

                this.getView().getFile_size_label().setText(formatBytes(this._file_size));
            });

        }

        MiscTools.GUIRun(() -> {
            this.getView().getClose_button().setVisible(true);
            this.getView().getQueue_down_button().setVisible(true);
            this.getView().getQueue_up_button().setVisible(true);
            this.getView().getQueue_top_button().setVisible(true);
            this.getView().getQueue_bottom_button().setVisible(true);
        });

    }

    @Override
    public void start() {

        THREAD_POOL.execute(this);
    }

    @Override
    public void stop() {
        if (!this.isExit()) {
            this._canceled = true;
            this.stopUploader();
        }
    }

    @Override
    public void pause() {

        if (this.isPaused()) {

            this.setPause(false);

            this.setPaused_workers(0);

            synchronized (this._workers_lock) {

                this.getChunkworkers().forEach((uploader) -> {
                    uploader.secureNotify();
                });
            }

            this.getView().resume();

            this._main_panel.getUpload_manager().setPaused_all(false);

        } else {

            this.setPause(true);

            this.getView().pause();
        }

        this.getMain_panel().getUpload_manager().secureNotify();
    }

    @Override
    public void restart() {

        final Upload new_upload = new Upload(this);

        this.getMain_panel().getUpload_manager().getTransference_remove_queue().add(this);

        this.getMain_panel().getUpload_manager().getTransference_provision_queue().add(new_upload);

        this.getMain_panel().getUpload_manager().secureNotify();
    }

    @Override
    public void close() {

        this._closed = true;

        if (this._provision_ok) {
            try {
                DBTools.deleteUpload(this._file_name, this._ma.getFull_email());
            } catch (final SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
        this._main_panel.getUpload_manager().getTransference_remove_queue().add(this);

        this._main_panel.getUpload_manager().secureNotify();
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

                final int sl = this.getView().getSlots();

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
    public ConcurrentLinkedQueue<Long> getPartialProgress() {
        return this._partialProgressQueue;
    }

    @Override
    public MainPanel getMain_panel() {
        return this._main_panel;
    }

    public void startSlot() {

        if (!this._exit) {

            synchronized (this._workers_lock) {

                final int chunkthiser_id = this._chunkworkers.size() + 1;

                final ChunkUploader c = new ChunkUploader(chunkthiser_id, this);

                this._chunkworkers.add(c);

                try {

                    LOG.log(Level.INFO, "{0} Starting chunkuploader from startslot()...", Thread.currentThread().getName());

                    this._thread_pool.execute(c);

                } catch (final java.util.concurrent.RejectedExecutionException e) {
                    LOG.log(Level.INFO, e.getMessage());
                }

            }

        }
    }

    public void setPause(final boolean pause) {
        this._pause = pause;
    }

    public void stopLastStartedSlot() {

        if (!this._exit) {

            synchronized (this._workers_lock) {

                if (!this._chunkworkers.isEmpty()) {

                    MiscTools.GUIRun(() -> {
                        this.getView().getSlots_spinner().setEnabled(false);
                    });

                    int i = this._chunkworkers.size() - 1;

                    while (i >= 0) {

                        final ChunkUploader chunkuploader = this._chunkworkers.get(i);

                        if (!chunkuploader.isExit()) {

                            chunkuploader.setExit(true);

                            chunkuploader.secureNotify();

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

    public void rejectChunkId(final long chunk_id) {
        this._rejectedChunkIds.add(chunk_id);
    }

    @Override
    public void run() {

        LOG.log(Level.INFO, "{0} Uploader hello! {1}", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

        MiscTools.GUIRun(() -> {
            this.getView().getQueue_down_button().setVisible(false);
            this.getView().getQueue_up_button().setVisible(false);
            this.getView().getQueue_top_button().setVisible(false);
            this.getView().getQueue_bottom_button().setVisible(false);
        });

        this.getView().printStatusNormal("Starting upload, please wait...");

        if (!this._exit) {

            this._thread_pool.execute(() -> {

                final String thumbnails_string = DBTools.selectSettingValue("thumbnails");

                if ("yes".equals(thumbnails_string)) {

                    final Thumbnailer thumbnailer = new Thumbnailer();

                    this._thumbnail_file = thumbnailer.createThumbnail(this._file_name);
                } else {
                    this._thumbnail_file = null;
                }

            });

            if (this._ul_url == null) {

                int conta_error = 0;

                do {
                    try {
                        this._ul_url = this._ma.initUploadFile(this._file_name);
                    } catch (final MegaAPIException ex) {

                        Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());

                        if (Arrays.asList(FATAL_API_ERROR_CODES).contains(ex.getCode())) {
                            this.stopUploader(ex.getMessage());
                            this._auto_retry_on_error = Arrays.asList(FATAL_API_ERROR_CODES_WITH_RETRY).contains(ex.getCode());
                        }

                    }

                    if (this._ul_url == null && !this._exit) {

                        final long wait_time = MiscTools.getWaitTimeExpBackOff(++conta_error);

                        LOG.log(Level.INFO, "{0} Uploader {1} Upload URL is null, retrying in {2} secs...", new Object[]{Thread.currentThread().getName(), this.getFile_name(), wait_time});

                        try {

                            Thread.sleep(wait_time * 1000);

                        } catch (final InterruptedException ex) {

                            LOG.log(Level.SEVERE, ex.getMessage());
                        }
                    }

                } while (this._ul_url == null && !this._exit);

                if (this._ul_url != null) {

                    try {

                        DBTools.updateUploadUrl(this._file_name, this._ma.getFull_email(), this._ul_url);

                        this._auto_retry_on_error = true;

                    } catch (final SQLException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }
                }
            }

            this._canceled = false;

            if (!this._exit && this._ul_url != null && this._ul_key != null) {

                final int[] file_iv = {this._ul_key[4], this._ul_key[5], 0, 0};

                this._byte_file_key = i32a2bin(Arrays.copyOfRange(this._ul_key, 0, 4));

                this._byte_file_iv = i32a2bin(file_iv);

                MiscTools.GUIRun(() -> {
                    this.getView().getClose_button().setVisible(false);

                    this.getView().getCbc_label().setVisible(true);
                });

                if (this._file_size > 0) {

                    this.getView().updateProgressBar(0);

                } else {

                    this.getView().updateProgressBar(MAX_VALUE);
                }

                this._thread_pool.execute(this.getProgress_meter());

                this.getMain_panel().getGlobal_up_speed().attachTransference(this);

                this._mac_generator = new UploadMACGenerator(this);

                this._thread_pool.execute(this._mac_generator);

                synchronized (this._workers_lock) {

                    this._slots = this.getMain_panel().getDefault_slots_up();

                    this._view.getSlots_spinner().setValue(this._slots);

                    for (int t = 1; t <= this._slots; t++) {
                        final ChunkUploader c = new ChunkUploader(t, this);

                        this._chunkworkers.add(c);

                        LOG.log(Level.INFO, "{0} Starting chunkuploader {1} ...", new Object[]{Thread.currentThread().getName(), t});

                        this._thread_pool.execute(c);
                    }

                    MiscTools.GUIRun(() -> {
                        this.getView().getSlots_label().setVisible(true);

                        this.getView().getSlots_spinner().setVisible(true);

                        this.getView().getSlot_status_label().setVisible(true);
                    });

                }

                this.getView().printStatusNormal(LabelTranslatorSingleton.getInstance().translate("Uploading file to mega (") + this._ma.getFull_email() + ") ...");

                MiscTools.GUIRun(() -> {
                    this.getView().getPause_button().setVisible(true);

                    this.getView().getProgress_pbar().setVisible(true);
                });

                THREAD_POOL.execute(() -> {

                    //PROGRESS WATCHDOG If a upload remains more than PROGRESS_WATCHDOG_TIMEOUT seconds without receiving data, we force fatal error in order to restart it.
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
                        this.stopUploader("PROGRESS WATCHDOG TIMEOUT!");
                    }

                    LOG.log(Level.INFO, "{0} PROGRESS WATCHDOG BYE BYE!", Thread.currentThread().getName());

                });

                this.secureWait();

                LOG.log(Level.INFO, "{0} Chunkuploaders finished! {1}", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                this.getProgress_meter().setExit(true);

                this.getProgress_meter().secureNotify();

                try {

                    this._thread_pool.shutdown();

                    LOG.log(Level.INFO, "{0}Waiting for all threads to finish {1}...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                    this._thread_pool.awaitTermination(MAX_WAIT_WORKERS_SHUTDOWN, TimeUnit.SECONDS);

                } catch (final InterruptedException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }

                if (!this._thread_pool.isTerminated()) {

                    LOG.log(Level.INFO, "{0} Closing thread pool in ''mecag\u00fcen'' style {1}...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                    this._thread_pool.shutdownNow();
                }

                LOG.log(Level.INFO, "{0} Uploader thread pool finished! {1}", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                this.getMain_panel().getGlobal_up_speed().detachTransference(this);

                MiscTools.GUIRun(() -> {
                    for (final JComponent c : new JComponent[]{this.getView().getSpeed_label(), this.getView().getCbc_label(), this.getView().getPause_button(), this.getView().getStop_button(), this.getView().getSlots_label(), this.getView().getSlots_spinner()}) {
                        c.setVisible(false);
                    }
                });

                if (!this._exit) {

                    if (this._completion_handler != null) {

                        LOG.log(Level.INFO, "{0} Uploader creating NEW MEGA NODE {1}...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});

                        this.getView().printStatusWarning("Creating new MEGA node ... ***DO NOT EXIT MEGABASTERD NOW***");

                        final File f = new File(this._file_name);

                        HashMap<String, Object> upload_res = null;

                        final int[] ul_key = this._ul_key;

                        final int[] node_key = {ul_key[0] ^ ul_key[4], ul_key[1] ^ ul_key[5], ul_key[2] ^ this._file_meta_mac[0], ul_key[3] ^ this._file_meta_mac[1], ul_key[4], ul_key[5], this._file_meta_mac[0], this._file_meta_mac[1]};

                        int conta_error = 0;

                        do {
                            try {
                                upload_res = this._ma.finishUploadFile(f.getName(), ul_key, node_key, this._file_meta_mac, this._completion_handler, this._parent_node, i32a2bin(this._ma.getMaster_key()), this._root_node, this._share_key);
                            } catch (final MegaAPIException ex) {
                                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());

                                if (Arrays.asList(FATAL_API_ERROR_CODES).contains(ex.getCode())) {
                                    this.stopUploader(ex.getMessage());
                                    this._auto_retry_on_error = Arrays.asList(FATAL_API_ERROR_CODES_WITH_RETRY).contains(ex.getCode());
                                }
                            }

                            if (upload_res == null && !this._exit) {

                                final long wait_time = MiscTools.getWaitTimeExpBackOff(++conta_error);

                                LOG.log(Level.INFO, "{0} Uploader {1} Finisih upload res is null, retrying in {2} secs...", new Object[]{Thread.currentThread().getName(), this.getFile_name(), wait_time});

                                try {

                                    Thread.sleep(wait_time * 1000);

                                } catch (final InterruptedException ex) {

                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }
                            }

                        } while (upload_res == null && !this._exit);

                        if (upload_res != null && !this._exit) {
                            try {
                                final List files = (List) upload_res.get("f");

                                this._fid = (String) ((Map<String, Object>) files.get(0)).get("h");

                                while (this._thumbnail_file != null && "".equals(this._thumbnail_file)) {
                                    MiscTools.pausar(1000);
                                }

                                if (this._thumbnail_file != null) {

                                    this.getView().printStatusWarning("Creating thumbnail ... ***DO NOT EXIT MEGABASTERD NOW***");

                                    if (!Files.isReadable(Paths.get(this._thumbnail_file))) {
                                        final Thumbnailer thumbnailer = new Thumbnailer();

                                        this._thumbnail_file = thumbnailer.createThumbnail(this._file_name);
                                    }

                                    this.getView().printStatusWarning("Uploading thumbnail ... ***DO NOT EXIT MEGABASTERD NOW***");

                                    this._ma.uploadThumbnails(this, this._fid, this._thumbnail_file, this._thumbnail_file);

                                    Files.deleteIfExists(Paths.get(this._thumbnail_file));

                                }

                                try {

                                    this._file_link = this._ma.getPublicFileLink(this._fid, i32a2bin(node_key));

                                    MiscTools.GUIRun(() -> {
                                        this.getView().getFile_link_button().setEnabled(true);
                                    });

                                } catch (final Exception ex) {
                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }

                                this.getView().printStatusOK(LabelTranslatorSingleton.getInstance().translate("File successfully uploaded! (") + this._ma.getFull_email() + ")");

                                synchronized (this.getMain_panel().getUpload_manager().getLog_file_lock()) {

                                    final File upload_log = new File(MiscTools.UPLOAD_LOGS_DIR + "/megabasterd_upload_" + this._root_node + ".log");

                                    if (upload_log.exists()) {

                                        final FileWriter fr;
                                        try {
                                            fr = new FileWriter(upload_log, true);
                                            fr.write("[" + MiscTools.getFechaHoraActual() + "] " + this._file_name + "   [" + MiscTools.formatBytes(this._file_size) + "]   " + this._file_link + "\n");
                                            fr.close();
                                        } catch (final IOException ex) {
                                            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());
                                        }

                                    }
                                }

                            } catch (final MegaAPIException ex) {
                                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (final IOException ex) {
                                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        } else if (this._status_error != null) {
                            this.getView().hideAllExceptStatus();

                            this.getView().printStatusError(this._status_error);
                        }

                    } else {

                        this._status_error = "UPLOAD FAILED! (Empty completion handle!)";

                        this.getView().hideAllExceptStatus();

                        this.getView().printStatusError(this._status_error);

                    }

                } else if (this._canceled) {

                    this.getView().hideAllExceptStatus();

                    this.getView().printStatusNormal("Upload CANCELED!");

                } else {

                    this.getView().hideAllExceptStatus();

                    this._status_error = "UNEXPECTED ERROR!";

                    this.getView().printStatusError(this._status_error);
                }

            } else if (this._status_error != null) {

                this.getView().hideAllExceptStatus();

                this.getView().printStatusError(this._status_error);

            } else if (this._canceled) {

                this.getView().hideAllExceptStatus();

                this.getView().printStatusNormal("Upload CANCELED!");

            } else {

                this.getView().hideAllExceptStatus();

                this._status_error = "UNEXPECTED ERROR!";

                this.getView().printStatusError(this._status_error);
            }

        } else if (this._canceled) {

            this.getView().hideAllExceptStatus();

            this.getView().printStatusNormal("Upload CANCELED!");

        } else {

            this.getView().hideAllExceptStatus();

            this._status_error = "UNEXPECTED ERROR!";

            this.getView().printStatusError(this._status_error);
        }

        if (this._status_error == null && !this._canceled) {

            try {
                DBTools.deleteUpload(this._file_name, this._ma.getFull_email());
            } catch (final SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        } else {
            try {
                DBTools.updateUploadProgress(this.getFile_name(), this.getMa().getFull_email(), this.getProgress(), this.getTemp_mac_data() != null ? this.getTemp_mac_data() : null);
            } catch (final SQLException ex) {
                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        this.getMain_panel().getUpload_manager().getTransference_running_list().remove(this);

        this.getMain_panel().getUpload_manager().getTransference_finished_queue().add(this);

//        MiscTools.GUIRun(() -> {
//            getMain_panel().getUpload_manager().getScroll_panel().remove(getView());
//
//            getMain_panel().getUpload_manager().getScroll_panel().add(getView());
//
//            getMain_panel().getUpload_manager().secureNotify();
//        });

        MiscTools.GUIRun(() -> {
            this.getView().getClose_button().setVisible(true);

            if (this._status_error != null || this._canceled) {

                this.getView().getRestart_button().setVisible(true);

            } else {
                this.getView().getClose_button().setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-ok-30.png")));
            }
        });

        if (this._status_error != null && !this._canceled && this._auto_retry_on_error) {
            THREAD_POOL.execute(() -> {
                for (int i = 3; !this._closed && i > 0; i--) {
                    final int j = i;
                    MiscTools.GUIRun(() -> {
                        this.getView().getRestart_button().setText("Restart (" + String.valueOf(j) + " secs...)");
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException ex) {
                        Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());
                    }
                }
                if (!this._closed) {
                    LOG.log(Level.INFO, "{0} Uploader {1} AUTO RESTARTING UPLOAD...", new Object[]{Thread.currentThread().getName(), this.getFile_name()});
                    this.restart();
                }
            });
        } else {
            this.getMain_panel().getUpload_manager().setAll_finished(false);
        }

        this._exit = true;

        if (this._status_error != null && !this._canceled && this.getMain_panel().getDownload_manager().no_transferences() && this.getMain_panel().getUpload_manager().no_transferences() && (!this.getMain_panel().getDownload_manager().getTransference_finished_queue().isEmpty() || !this.getMain_panel().getUpload_manager().getTransference_finished_queue().isEmpty()) && this.getMain_panel().getView().getAuto_close_menu().isSelected()) {
            System.exit(0);
        }

        synchronized (this._progress_watchdog_lock) {
            this._progress_watchdog_lock.notifyAll();
        }

        LOG.log(Level.INFO, "{0} Uploader {1} BYE BYE", new Object[]{Thread.currentThread().getName(), this.getFile_name()});
    }

    public void pause_worker() {

        synchronized (this._workers_lock) {

            if (++this._paused_workers >= this._chunkworkers.size() && !this._exit) {

                this.getView().printStatusNormal("Upload paused!");

                MiscTools.GUIRun(() -> {
                    this.getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME UPLOAD"));
                    this.getView().getPause_button().setEnabled(true);
                });

            }
        }

    }

    public void pause_worker_mono() {

        this.getView().printStatusNormal("Upload paused!");

        MiscTools.GUIRun(() -> {
            this.getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME UPLOAD"));
            this.getView().getPause_button().setEnabled(true);
        });

    }

    public void stopThisSlot(final ChunkUploader chunkuploader) {

        synchronized (this._workers_lock) {

            if (this._chunkworkers.remove(chunkuploader) && !this._exit) {

                if (chunkuploader.isChunk_exception() || this.getMain_panel().isExit()) {

                    this._finalizing = true;

                    MiscTools.GUIRun(() -> {
                        this.getView().getSlots_spinner().setEnabled(false);

                        this.getView().getSlots_spinner().setValue((int) this.getView().getSlots_spinner().getValue() - 1);
                    });

                } else if (!this._finalizing) {
                    MiscTools.GUIRun(() -> {
                        this.getView().getSlots_spinner().setEnabled(true);
                    });
                }

                if (!this._exit && this.isPause() && this._paused_workers == this._chunkworkers.size()) {

                    this.getView().printStatusNormal("Upload paused!");

                    MiscTools.GUIRun(() -> {
                        this.getView().getPause_button().setText(LabelTranslatorSingleton.getInstance().translate("RESUME UPLOAD"));
                        this.getView().getPause_button().setEnabled(true);
                    });

                }

                this.getView().updateSlotsStatus();

            }
        }
    }

    public long nextChunkId() throws ChunkInvalidException {

        synchronized (this._chunkid_lock) {

            final Long next_id;

            if ((next_id = this._rejectedChunkIds.poll()) != null) {
                return next_id;
            } else {
                return ++this._last_chunk_id_dispatched;
            }
        }
    }

    public void setExit(final boolean exit) {
        this._exit = exit;
    }

    public void stopUploader() {

        if (!this._exit) {

            this._exit = true;

            this.getView().stop("Stopping upload, please wait...");

            synchronized (this._workers_lock) {

                this._chunkworkers.forEach((uploader) -> {
                    uploader.secureNotify();
                });
            }

            this.secureNotify();
        }
    }

    public void stopUploader(final String reason) {

        this._status_error = (reason != null ? LabelTranslatorSingleton.getInstance().translate("FATAL ERROR! ") + reason : LabelTranslatorSingleton.getInstance().translate("FATAL ERROR! "));

        this.stopUploader();
    }

    public int[] getFile_meta_mac() {
        return this._file_meta_mac;
    }

    @Override
    public void setProgress(final long progress) {

        synchronized (this._progress_lock) {

            final long old_progress = this._progress;

            this._progress = progress;

            this.getMain_panel().getUpload_manager().increment_total_progress(this._progress - old_progress);

            final int old_percent_progress = (int) Math.floor(((double) old_progress / this._file_size) * 100);

            int new_percent_progress = (int) Math.floor(((double) progress / this._file_size) * 100);

            if (new_percent_progress == 100 && progress != this._file_size) {
                new_percent_progress = 99;
            }

            if (new_percent_progress > old_percent_progress) {

                this.getView().updateProgressBar(this._progress, this._progress_bar_rate);
            }
        }
    }

    @Override
    public boolean isStatusError() {
        return this._status_error != null;
    }

    public long calculateLastUploadedChunk(final long bytes_read) {

        if (bytes_read > 3584 * 1024) {
            return 7 + (long) Math.floor((float) (bytes_read - 3584 * 1024) / (1024 * 1024 * 1));
        } else {
            long i = 0, tot = 0;

            while (tot < bytes_read) {
                i++;
                tot += i * 128 * 1024;
            }

            return i;
        }
    }

    public void secureNotifyWorkers() {

        synchronized (this._workers_lock) {

            this.getChunkworkers().forEach((uploader) -> {
                uploader.secureNotify();
            });
        }
    }

    @Override
    public void bottomWaitQueue() {
        this._main_panel.getUpload_manager().bottomWaitQueue(this);
    }

    @Override
    public void topWaitQueue() {
        this._main_panel.getUpload_manager().topWaitQueue(this);
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

        this.getView().printStatusNormal(this.getView().getStatus_label().getText().replaceFirst("^\\([^)]+\\) ", ""));

        this._frozen = false;
    }

    @Override
    public void upWaitQueue() {
        this._main_panel.getUpload_manager().upWaitQueue(this);
    }

    @Override
    public void downWaitQueue() {
        this._main_panel.getUpload_manager().downWaitQueue(this);
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
