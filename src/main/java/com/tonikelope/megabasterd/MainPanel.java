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

import com.tonikelope.megabasterd.SmartMegaProxyManager.SmartProxyAuthenticator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.DBTools.deleteUpload;
import static com.tonikelope.megabasterd.DBTools.selectDownloads;
import static com.tonikelope.megabasterd.DBTools.selectELCAccounts;
import static com.tonikelope.megabasterd.DBTools.selectMegaAccounts;
import static com.tonikelope.megabasterd.DBTools.selectSettingValue;
import static com.tonikelope.megabasterd.DBTools.selectUploads;
import static com.tonikelope.megabasterd.DBTools.setupSqliteTables;
import static com.tonikelope.megabasterd.MiscTools.BASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.bin2i32a;
import static com.tonikelope.megabasterd.MiscTools.checkMegaAccountLoginAndShowMasterPassDialog;
import static com.tonikelope.megabasterd.MiscTools.checkNewVersion;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static com.tonikelope.megabasterd.MiscTools.genRandomByteArray;
import static com.tonikelope.megabasterd.MiscTools.getCurrentJarParentPath;
import static com.tonikelope.megabasterd.MiscTools.restartApplication;
import static com.tonikelope.megabasterd.MiscTools.setNimbusLookAndFeel;
import static com.tonikelope.megabasterd.Transference.LIMIT_TRANSFERENCE_SPEED_DEFAULT;
import static com.tonikelope.megabasterd.Transference.MAX_TRANSFERENCE_SPEED_DEFAULT;
import static java.lang.Integer.parseInt;
import static java.lang.System.exit;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.logging.Level.SEVERE;

/**
 * @author tonikelope
 */
public final class MainPanel {

    public static final String VERSION = "8.21";
    public static final boolean FORCE_SMART_PROXY = false; //TRUE FOR DEBUGING SMART PROXY
    public static final int THROTTLE_SLICE_SIZE = 16 * 1024;
    public static final int DEFAULT_BYTE_BUFFER_SIZE = 16 * 1024;
    public static final int STREAMER_PORT = 1337;
    public static final int WATCHDOG_PORT = 1338;
    public static final int DEFAULT_MEGA_PROXY_PORT = 9999;
    public static final int RUN_COMMAND_TIME = 120;
    public static final String DEFAULT_LANGUAGE = "EN";
    public static final boolean DEFAULT_SMART_PROXY = false;
    public static final double FORCE_GARBAGE_COLLECTION_MAX_MEMORY_PERCENT = 0.7;
    //    public static Font GUI_FONT = new JLabel().getFont();
//    public static Font GUI_FONT = new Font("", 0, 0);
    public static final float ZOOM_FACTOR = 0.8f;
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:61.0) Gecko/20100101 Firefox/61.0";
    public static final String ICON_FILE = "/images/pica_roja_big.png";
    public static final ExecutorService THREAD_POOL = newCachedThreadPool();
    public static volatile String MEGABASTERD_HOME_DIR = System.getProperty("user.home");
    private static String _proxy_host;
    private static int _proxy_port;
    private static boolean _use_proxy;
    private static String _proxy_user;
    private static String _proxy_pass;
    private static boolean _use_smart_proxy;
    private static boolean _run_command;
    private static String _run_command_path;
    private static String _font;
    private static SmartMegaProxyManager _proxy_manager;
    private static String _language;
    private static String _new_version;
    private static Boolean _resume_uploads;
    private static Boolean _resume_downloads;
    public static volatile long LAST_EXTERNAL_COMMAND_TIMESTAMP;
    private static final Logger LOG = Logger.getLogger(MainPanel.class.getName());
    private static volatile boolean CHECK_RUNNING = true;

    public static void main(final String[] args) {
        final File f = new File(getCurrentJarParentPath() + "/.megabasterd_portable");
        if (f.exists()) {
            MEGABASTERD_HOME_DIR = f.getParentFile().getAbsolutePath();
        }

        try {
            setupSqliteTables();
        } catch (final SQLException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        setNimbusLookAndFeel("yes".equals(DBTools.selectSettingValue("dark_mode")));

        if ("yes".equals(DBTools.selectSettingValue("upload_log"))) {
            MiscTools.createUploadLogDir();
        }

        final MainPanel main_panel = new MainPanel();

        if (args.length > 0) {
            // Check for CLI mode flag
            if ("--cli".equals(args[0])) {
                final CLIHandler cliHandler = new CLIHandler();
                cliHandler.handle(main_panel, args);
                return; // Exit after handling CLI commands
            }

            if (args.length > 1) {
                try {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.INFO, "{0} Waiting {1} seconds before start...", new Object[]{Thread.currentThread().getName(), args[1]});
                    if (Long.parseLong(args[1]) >= 0) {
                        Thread.sleep(Long.parseLong(args[1]) * 1000);
                    } else {
                        CHECK_RUNNING = false;
                    }
                } catch (final InterruptedException ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            }
        }

//        invokeLater(() -> {
//            main_panel.getView().setVisible(true);
//        });
    }

    public static boolean isRun_command() {
        return _run_command;
    }

    public static String getRun_command_path() {
        return _run_command_path;
    }

    public static String getFont() {
        return _font;
    }

    public static String getNew_version() {
        return _new_version;
    }

    public static String getLanguage() {
        return _language;
    }

    public static String getProxy_user() {
        return _proxy_user;
    }

    public static String getProxy_pass() {
        return _proxy_pass;
    }

    public static void setProxy_manager(final SmartMegaProxyManager proxy_manager) {
        _proxy_manager = proxy_manager;
    }

    public static String getProxy_host() {
        return _proxy_host;
    }

    public static int getProxy_port() {
        return _proxy_port;
    }

    public static boolean isUse_proxy() {
        return _use_proxy;
    }

    public static boolean isUse_smart_proxy() {
        return _use_smart_proxy;
    }

    public static SmartMegaProxyManager getProxy_manager() {
        return _proxy_manager;
    }

    //    private final MainPanelView _view;
    private final SpeedMeter _global_dl_speed, _global_up_speed;
    private final DownloadManager _download_manager;
    private final UploadManager _upload_manager;
    private final StreamThrottlerSupervisor _stream_supervisor;
    private int _max_dl, _max_ul, _default_slots_down, _default_slots_up, _max_dl_speed, _max_up_speed;
    private boolean _use_slots_down, _limit_download_speed, _limit_upload_speed, _use_mega_account_down, _init_paused, _debug_file;
    private String _mega_account_down;
    private String _default_download_path;
    private boolean _use_custom_chunks_dir;
    private String _custom_chunks_dir;
    private HashMap<String, Object> _mega_accounts;
    private HashMap<String, Object> _elc_accounts;
    private final HashMap<String, MegaAPI> _mega_active_accounts;
    //    private TrayIcon _trayicon;
    private final ClipboardSpy _clipboardspy;
    private KissVideoStreamServer _streamserver;
    private byte[] _master_pass;
    private String _master_pass_hash;
    private String _master_pass_salt;
    private boolean _restart;
    private MegaProxyServer _mega_proxy_server;
    private int _megacrypter_reverse_port;
    private boolean _megacrypter_reverse;
    private float _zoom_factor;
    private volatile boolean _exit;

    public MainPanel() {

        _new_version = null;

        this._exit = false;

        LAST_EXTERNAL_COMMAND_TIMESTAMP = -1;

        this._restart = false;

        this._elc_accounts = new HashMap<>();

        this._master_pass = null;

        this._mega_active_accounts = new HashMap<>();

        _proxy_host = null;

        _proxy_port = 3128;

        _proxy_user = null;

        _proxy_pass = null;

        _use_proxy = false;

        _use_smart_proxy = false;

        _proxy_manager = null;

        _resume_uploads = false;

        _resume_downloads = false;

        this.loadUserSettings();

        if (this._debug_file) {

            final PrintStream fileOut;

            try {
                fileOut = new PrintStream(new FileOutputStream(MainPanel.MEGABASTERD_HOME_DIR + "/MEGABASTERD_DEBUG.log"));

                System.setOut(fileOut);
                System.setErr(fileOut);

            } catch (final FileNotFoundException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        System.out.println(System.getProperty("os.name") + "" + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") + " " + System.getProperty("java.home"));

//        UIManager.put("OptionPane.messageFont", GUI_FONT.deriveFont(15f * this.getZoom_factor()));
//
//        UIManager.put("OptionPane.buttonFont", GUI_FONT.deriveFont(15f * this.getZoom_factor()));
//
//        UIManager.put("OptionPane.cancelButtonText", LabelTranslatorSingleton.getInstance().translate("Cancel"));
//
//        UIManager.put("OptionPane.yesButtonText", LabelTranslatorSingleton.getInstance().translate("Yes"));
//
//        UIManager.put("OptionPane.okButtonText", LabelTranslatorSingleton.getInstance().translate("OK"));

//        this._view = new MainPanelView(this);

        if (CHECK_RUNNING && this.checkAppIsRunning()) {

            System.exit(0);
        }

//        try {
//            this.trayIcon();
//        } catch (final AWTException ex) {
//            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
//        }

        THREAD_POOL.execute((this._download_manager = new DownloadManager(this, null)));

        THREAD_POOL.execute((this._upload_manager = new UploadManager(this, null)));

        THREAD_POOL.execute((this._global_dl_speed = new SpeedMeter(this._download_manager)));

        THREAD_POOL.execute((this._global_up_speed = new SpeedMeter(this._upload_manager)));

        THREAD_POOL.execute((this._stream_supervisor = new StreamThrottlerSupervisor(this._limit_download_speed ? this._max_dl_speed * 1024 : 0, this._limit_upload_speed ? this._max_up_speed * 1024 : 0, THROTTLE_SLICE_SIZE)));

        THREAD_POOL.execute((this._clipboardspy = new ClipboardSpy()));

        try {
            this._streamserver = new KissVideoStreamServer(this);
            this._streamserver.start(STREAMER_PORT, "/video");
        } catch (final IOException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        this._check_old_version();

        THREAD_POOL.execute(() -> {
            _new_version = checkNewVersion(AboutDialog.MEGABASTERD_URL);

            if (_new_version != null) {

//                JOptionPane.showMessageDialog(this.getView(), LabelTranslatorSingleton.getInstance().translate("MegaBasterd NEW VERSION is available! -> ") + _new_version);
            }
        });

        if (this._megacrypter_reverse) {

            this._mega_proxy_server = new MegaProxyServer(this, UUID.randomUUID().toString(), this._megacrypter_reverse_port);

            THREAD_POOL.execute(this._mega_proxy_server);

        } else {
            this._mega_proxy_server = null;

        }

        if (_use_smart_proxy) {

            final MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                Authenticator.setDefault(new SmartProxyAuthenticator());

                final String lista_proxy = DBTools.selectSettingValue("custom_proxy_list");

                final String url_list = MiscTools.findFirstRegex("^#(http.+)$", lista_proxy.trim(), 1);

                _proxy_manager = new SmartMegaProxyManager(url_list, tthis);
            });

        } else {

//            this.getView().updateSmartProxyStatus("SmartProxy: OFF");

        }

//        MiscTools.GUIRun(() -> {
//            this.getView().getGlobal_speed_down_label().setForeground(this._limit_download_speed ? new Color(255, 0, 0) : new Color(0, 128, 255));
//
//            this.getView().getGlobal_speed_up_label().setForeground(this._limit_upload_speed ? new Color(255, 0, 0) : new Color(0, 128, 255));
//        });

        THREAD_POOL.execute(() -> {
            final Runtime instance = Runtime.getRuntime();
            while (true) {
                final long used_memory = instance.totalMemory() - instance.freeMemory();
                final long max_memory = instance.maxMemory();
//                MiscTools.GUIRun(() -> {
//                    this._view.getMemory_status().setText("JVM-RAM used: " + MiscTools.formatBytes(used_memory) + " / " + MiscTools.formatBytes(max_memory));
//                });
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException ex) {
                    Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            }
        });

        this.resumeDownloads();

        this.resumeUploads();

    }

    public static Boolean getResume_uploads() {
        return _resume_uploads;
    }

    public static void setResume_uploads(final Boolean resume_uploads) {
        MainPanel._resume_uploads = resume_uploads;
    }

    public static Boolean getResume_downloads() {
        return _resume_downloads;
    }

    public static void setResume_downloads(final Boolean resume_downloads) {
        MainPanel._resume_downloads = resume_downloads;
    }

    public boolean isUse_custom_chunks_dir() {
        return this._use_custom_chunks_dir;
    }

    public String getCustom_chunks_dir() {
        return this._custom_chunks_dir;
    }

    public boolean isExit() {
        return this._exit;
    }

    public void setExit(final boolean _exit) {
        this._exit = _exit;
    }

    public float getZoom_factor() {
        return this._zoom_factor;
    }

    public MegaProxyServer getMega_proxy_server() {
        return this._mega_proxy_server;
    }

    public boolean isMegacrypter_reverse() {
        return this._megacrypter_reverse;
    }

    public int getMegacrypter_reverse_port() {
        return this._megacrypter_reverse_port;
    }

    public void setMega_proxy_server(final MegaProxyServer mega_proxy_server) {
        this._mega_proxy_server = mega_proxy_server;
    }

    public boolean isUse_mega_account_down() {
        return this._use_mega_account_down;
    }

    public String getMega_account_down() {
        return this._mega_account_down;
    }

    public boolean isRestart() {
        return this._restart;
    }

    public void setRestart(final boolean restart) {
        this._restart = restart;
    }

    public HashMap<String, Object> getElc_accounts() {
        return this._elc_accounts;
    }

//    public TrayIcon getTrayicon() {
//        return this._trayicon;
//    }

    public String getMaster_pass_hash() {
        return this._master_pass_hash;
    }

    public void setMaster_pass_hash(final String master_pass_hash) {
        this._master_pass_hash = master_pass_hash;
    }

    public String getMaster_pass_salt() {
        return this._master_pass_salt;
    }

    public byte[] getMaster_pass() {
        return this._master_pass;
    }

    public void setMaster_pass(final byte[] pass) {

        if (this._master_pass != null) {

            Arrays.fill(this._master_pass, (byte) 0);

            this._master_pass = null;
        }

        if (pass != null) {

            this._master_pass = new byte[pass.length];

            System.arraycopy(pass, 0, this._master_pass, 0, pass.length);
        }
    }

    public MainPanelView getView() {

//        while (this._view == null) {
//            try {
//                Thread.sleep(250);
//            } catch (final InterruptedException ex) {
//                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
//            }
//        }

        return null;
    }

    public SpeedMeter getGlobal_dl_speed() {
        return this._global_dl_speed;
    }

    public SpeedMeter getGlobal_up_speed() {
        return this._global_up_speed;
    }

    public DownloadManager getDownload_manager() {
        return this._download_manager;
    }

    public UploadManager getUpload_manager() {
        return this._upload_manager;
    }

    public StreamThrottlerSupervisor getStream_supervisor() {
        return this._stream_supervisor;
    }

    public int getMax_dl() {
        return this._max_dl;
    }

    public int getMax_ul() {
        return this._max_ul;
    }

    public int getDefault_slots_down() {
        return this._default_slots_down;
    }

    public int getDefault_slots_up() {
        return this._default_slots_up;
    }

    public boolean isUse_slots_down() {
        return this._use_slots_down;
    }

    public String getDefault_download_path() {
        return this._default_download_path;
    }

    public HashMap<String, Object> getMega_accounts() {
        return this._mega_accounts;
    }

    public HashMap<String, MegaAPI> getMega_active_accounts() {
        return this._mega_active_accounts;
    }

//    public TrayIcon getIcon() {
//        return this._trayicon;
//    }

    public ClipboardSpy getClipboardspy() {
        return this._clipboardspy;
    }

    public KissVideoStreamServer getStreamserver() {
        return this._streamserver;
    }

    public int getMax_dl_speed() {
        return this._max_dl_speed;
    }

    public int getMax_up_speed() {
        return this._max_up_speed;
    }

    public boolean isLimit_download_speed() {
        return this._limit_download_speed;
    }

    public boolean isLimit_upload_speed() {
        return this._limit_upload_speed;
    }

    public boolean isInit_paused() {
        return this._init_paused;
    }

    public void loadUserSettings() {

        final String use_custom_chunks_dir = DBTools.selectSettingValue("use_custom_chunks_dir");

        if (use_custom_chunks_dir != null) {

            if (use_custom_chunks_dir.equals("yes")) {

                this._use_custom_chunks_dir = true;

                this._custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");

            } else {

                this._use_custom_chunks_dir = false;

                this._custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");
            }

        } else {

            this._custom_chunks_dir = null;
        }

        final String zoom_factor = selectSettingValue("font_zoom");

        if (zoom_factor != null) {
            this._zoom_factor = Float.parseFloat(zoom_factor) / 100;
        } else {
            this._zoom_factor = ZOOM_FACTOR;
        }

        final String _font = selectSettingValue("font");

        if (_font != null) {
            if (_font.equals("DEFAULT")) {

//                GUI_FONT = new JLabel().getFont();

            } else {

//                GUI_FONT = createAndRegisterFont("/fonts/NotoSansCJK-Regular.ttc");

            }
        } else {

//            GUI_FONT = createAndRegisterFont("/fonts/NotoSansCJK-Regular.ttc");
        }

        String def_slots = selectSettingValue("default_slots_down");

        if (def_slots != null) {
            this._default_slots_down = parseInt(def_slots);
        } else {
            this._default_slots_down = Download.WORKERS_DEFAULT;
        }

        def_slots = selectSettingValue("default_slots_up");

        if (def_slots != null) {
            this._default_slots_up = parseInt(def_slots);
        } else {
            this._default_slots_up = Upload.WORKERS_DEFAULT;
        }

        final String use_slots = selectSettingValue("use_slots_down");

        if (use_slots != null) {
            this._use_slots_down = use_slots.equals("yes");
        } else {
            this._use_slots_down = Download.USE_SLOTS_DEFAULT;
        }

        final String max_downloads = selectSettingValue("max_downloads");

        if (max_downloads != null) {
            this._max_dl = parseInt(max_downloads);
        } else {
            this._max_dl = Download.SIM_TRANSFERENCES_DEFAULT;
        }

        final String max_uploads = selectSettingValue("max_uploads");

        if (max_uploads != null) {
            this._max_ul = parseInt(max_uploads);
        } else {
            this._max_ul = Upload.SIM_TRANSFERENCES_DEFAULT;
        }

        this._default_download_path = selectSettingValue("default_down_dir");

        if (this._default_download_path == null) {
            this._default_download_path = ".";
        }

        final String limit_dl_speed = selectSettingValue("limit_download_speed");

        if (limit_dl_speed != null) {

            this._limit_download_speed = limit_dl_speed.equals("yes");

        } else {

            this._limit_download_speed = LIMIT_TRANSFERENCE_SPEED_DEFAULT;
        }

        final String limit_ul_speed = selectSettingValue("limit_upload_speed");

        if (limit_ul_speed != null) {

            this._limit_upload_speed = limit_ul_speed.equals("yes");

        } else {

            this._limit_upload_speed = LIMIT_TRANSFERENCE_SPEED_DEFAULT;
        }

        final String max_download_speed = selectSettingValue("max_download_speed");

        if (max_download_speed != null) {
            this._max_dl_speed = parseInt(max_download_speed);
        } else {
            this._max_dl_speed = MAX_TRANSFERENCE_SPEED_DEFAULT;
        }

        final String max_upload_speed = selectSettingValue("max_upload_speed");

        if (max_upload_speed != null) {
            this._max_up_speed = parseInt(max_upload_speed);
        } else {
            this._max_up_speed = MAX_TRANSFERENCE_SPEED_DEFAULT;
        }

        final String init_paused_string = DBTools.selectSettingValue("start_frozen");

        if (init_paused_string != null) {

            this._init_paused = init_paused_string.equals("yes");
        } else {
            this._init_paused = false;
        }

        try {
            this._mega_accounts = selectMegaAccounts();
            this._elc_accounts = selectELCAccounts();
        } catch (final SQLException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
        }

        this._mega_account_down = DBTools.selectSettingValue("mega_account_down");

        final String use_account;

        this._use_mega_account_down = ((use_account = DBTools.selectSettingValue("use_mega_account_down")) != null && use_account.equals("yes"));

        this._master_pass_hash = DBTools.selectSettingValue("master_pass_hash");

        this._master_pass_salt = DBTools.selectSettingValue("master_pass_salt");

        if (this._master_pass_salt == null) {

            try {

                this._master_pass_salt = Bin2BASE64(genRandomByteArray(CryptTools.MASTER_PASSWORD_PBKDF2_SALT_BYTE_LENGTH));

                DBTools.insertSettingValue("master_pass_salt", this._master_pass_salt);

            } catch (final SQLException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }

        final String use_proxy = selectSettingValue("use_proxy");

        if (use_proxy != null) {
            _use_proxy = use_proxy.equals("yes");
        } else {
            _use_proxy = false;
        }

        if (_use_proxy) {

            _proxy_host = DBTools.selectSettingValue("proxy_host");

            final String proxy_port = DBTools.selectSettingValue("proxy_port");

            _proxy_port = (proxy_port == null || proxy_port.isEmpty()) ? 8080 : Integer.parseInt(proxy_port);

            _proxy_user = DBTools.selectSettingValue("proxy_user");

            _proxy_pass = DBTools.selectSettingValue("proxy_pass");
        }

        final String run_command_string = DBTools.selectSettingValue("run_command");

        if (run_command_string != null) {

            _run_command = run_command_string.equals("yes");
        }

        final String old_run_command_path = _run_command_path;

        _run_command_path = DBTools.selectSettingValue("run_command_path");

        if (_run_command && old_run_command_path != null && !old_run_command_path.equals(_run_command_path)) {
            LAST_EXTERNAL_COMMAND_TIMESTAMP = -1;
        }

        final String use_megacrypter_reverse = selectSettingValue("megacrypter_reverse");

        if (use_megacrypter_reverse != null) {
            this._megacrypter_reverse = use_megacrypter_reverse.equals("yes");
        } else {
            this._megacrypter_reverse = false;
        }

        if (this._megacrypter_reverse) {

            final String reverse_port = DBTools.selectSettingValue("megacrypter_reverse_port");

            this._megacrypter_reverse_port = (reverse_port == null || reverse_port.isEmpty()) ? DEFAULT_MEGA_PROXY_PORT : Integer.parseInt(reverse_port);
        }

        final String use_smart_proxy = selectSettingValue("smart_proxy");

        if (use_smart_proxy != null) {
            _use_smart_proxy = use_smart_proxy.equals("yes");
        } else {
            _use_smart_proxy = DEFAULT_SMART_PROXY;
        }

        _language = DBTools.selectSettingValue("language");

        if (_language == null) {
            _language = DEFAULT_LANGUAGE;
        }

        final String debug_file = selectSettingValue("debug_file");

        if (debug_file != null) {
            this._debug_file = debug_file.equals("yes");
        } else {
            this._debug_file = false;
        }

        final String api_key = DBTools.selectSettingValue("mega_api_key");

        if (api_key != null && !"".equals(api_key)) {

            MegaAPI.API_KEY = api_key.trim();

        } else {
            MegaAPI.API_KEY = null;
        }

    }

    public static synchronized void run_external_command() {

        if (_run_command && (LAST_EXTERNAL_COMMAND_TIMESTAMP == -1 || LAST_EXTERNAL_COMMAND_TIMESTAMP + RUN_COMMAND_TIME * 1000 < System.currentTimeMillis())) {

            if (_run_command_path != null && !_run_command_path.equals("")) {
                try {
                    Runtime.getRuntime().exec(_run_command_path);
                } catch (final IOException ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }

                LAST_EXTERNAL_COMMAND_TIMESTAMP = System.currentTimeMillis();
            }
        }
    }

    public boolean checkByeBye() {

        boolean exit = true;

        if (!this._streamserver.getWorking_threads().isEmpty()) {

            final Object[] options = {"No",
                    LabelTranslatorSingleton.getInstance().translate("Yes")};

//            final int n = showOptionDialog(this.getView(),
//                    LabelTranslatorSingleton.getInstance().translate("It seems MegaBasterd is streaming video. Do you want to exit?"),
//                    LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
//                    null,
//                    options,
//                    options[0]);
//
//            if (n == 0) {

            exit = false;
//            }

        } else if (!this.getDownload_manager().getTransference_preprocess_global_queue().isEmpty() || !this.getDownload_manager().getTransference_provision_queue().isEmpty() || !this.getUpload_manager().getTransference_preprocess_global_queue().isEmpty() || !this.getUpload_manager().getTransference_provision_queue().isEmpty()) {

            final Object[] options = {"No",
                    LabelTranslatorSingleton.getInstance().translate("Yes")};

//            final int n = showOptionDialog(this.getView(),
//                    LabelTranslatorSingleton.getInstance().translate("It seems MegaBasterd is provisioning down/uploads.\n\nIf you exit now, unprovisioned down/uploads will be lost.\n\nDo you want to continue?"),
//                    LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, WARNING_MESSAGE,
//                    null,
//                    options,
//                    options[0]);
//
//            if (n == 0) {

            exit = false;
//            }

        }

        return exit;
    }

    public void byebyenow(final boolean restart) {

        MiscTools.purgeFolderCache();

        synchronized (DBTools.class) {

            try {
                DBTools.vaccum();
            } catch (final SQLException ex) {
                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
            }

            if (restart) {
                restartApplication();
            } else {
                exit(0);
            }

        }
    }

    public void byebyenow(final boolean restart, final boolean delete_db) {

        synchronized (DBTools.class) {

            if (delete_db) {

                final File db_file = new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SqliteSingleton.SQLITE_FILE);

                db_file.delete();

            } else {
                try {
                    DBTools.vaccum();
                } catch (final SQLException ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            }

            if (restart) {
                restartApplication();
            } else {
                exit(0);
            }

        }
    }

    private void _check_old_version() {

        try {

            if (!new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/.old_version_check").exists()) {

                new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/.old_version_check").createNewFile();

                final File directory = new File(MainPanel.MEGABASTERD_HOME_DIR);

                final String version_major = findFirstRegex("([0-9]+)\\.[0-9]+$", VERSION, 1);

                final String version_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", VERSION, 1);

                String old_version_major = null;

                String old_version_minor = null;

                String old_version = "0.0";

                final File old_backups_dir = new File(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd_old_backups");

                if (!old_backups_dir.exists()) {
                    old_backups_dir.mkdir();
                }

                for (final File file : directory.listFiles()) {

                    try {
                        if (file.isDirectory() && file.canRead() && file.getName().startsWith(".megabasterd") && !file.getName().endsWith("backups")) {

                            final String current_dir_version = MiscTools.findFirstRegex("[0-9.]+$", file.getName(), 0);

                            if (current_dir_version != null && !current_dir_version.equals(VERSION)) {

                                old_version_major = findFirstRegex("([0-9]+)\\.[0-9]+$", old_version, 1);
                                old_version_major = findFirstRegex("[0-9]+\\.([0-9]+)$", old_version, 1);

                                final String current_dir_major = findFirstRegex("([0-9]+)\\.[0-9]+$", current_dir_version, 1);
                                final String current_dir_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", current_dir_version, 1);

                                if (Integer.parseInt(current_dir_major) > Integer.parseInt(old_version_major) || (Integer.parseInt(current_dir_major) == Integer.parseInt(old_version_major) && Integer.parseInt(current_dir_minor) > Integer.parseInt(old_version_minor))) {
                                    old_version = current_dir_version;
                                    old_version_major = current_dir_major;
                                    old_version_minor = current_dir_minor;
                                }

                                Files.move(Paths.get(file.getAbsolutePath()), Paths.get(old_backups_dir.getAbsolutePath() + "/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);
                            }

                        }
                    } catch (final Exception e) {
                    }

                }

                if (!old_version.equals("0.0") && (Integer.parseInt(version_major) > Integer.parseInt(old_version_major) || (Integer.parseInt(version_major) == Integer.parseInt(old_version_major) && Integer.parseInt(version_minor) > Integer.parseInt(old_version_minor)))) {
                    final Object[] options = {"No",
                            LabelTranslatorSingleton.getInstance().translate("Yes")};

//                    final int n = showOptionDialog(this.getView(),
//                            LabelTranslatorSingleton.getInstance().translate("An older version (" + old_version + ") of MegaBasterd has been detected.\nDo you want to import all current settings and transfers from the previous version?\nWARNING: INCOMPATIBILITIES MAY EXIST BETWEEN VERSIONS."),
//                            LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE,
//                            null,
//                            options,
//                            options[0]);

//                    if (n == 1) {
//                        Files.copy(Paths.get(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd_old_backups/.megabasterd" + old_version + "/" + SqliteSingleton.SQLITE_FILE), Paths.get(MainPanel.MEGABASTERD_HOME_DIR + "/.megabasterd" + MainPanel.VERSION + "/" + SqliteSingleton.SQLITE_FILE), StandardCopyOption.REPLACE_EXISTING);
//
//                        JOptionPane.showMessageDialog(this.getView(), LabelTranslatorSingleton.getInstance().translate("MegaBasterd will restart"), LabelTranslatorSingleton.getInstance().translate("Restart required"), JOptionPane.WARNING_MESSAGE);
//
//                        restartApplication();
//                    }
                }
            }

        } catch (final IOException ex) {
            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

    }

    public void byebye(final boolean restart) {

        this._byebye(restart, true);
    }

    public void byebye(final boolean restart, final boolean restart_warning) {

        this._byebye(restart, restart_warning);
    }

    private void _byebye(final boolean restart, final boolean restart_warning) {

        if (!this._exit && this.checkByeBye()) {

            if (restart && restart_warning) {
//                JOptionPane.showMessageDialog(this.getView(), LabelTranslatorSingleton.getInstance().translate("MegaBasterd will restart"), LabelTranslatorSingleton.getInstance().translate("Restart required"), JOptionPane.WARNING_MESSAGE);
            }

            this._exit = true;

//            this.getView().getPause_all_down_button().setEnabled(false);
//
//            this.getView().getPause_all_up_button().setEnabled(false);
//
//            this.getView().setEnabled(false);

            if (!this._download_manager.getTransference_running_list().isEmpty() || !this._upload_manager.getTransference_running_list().isEmpty() || !this._download_manager.getTransference_waitstart_queue().isEmpty() || !this._upload_manager.getTransference_waitstart_queue().isEmpty()) {

                THREAD_POOL.execute(() -> {
                    boolean wait;
                    do {
                        wait = false;
                        if (!this._download_manager.getTransference_running_list().isEmpty()) {
                            for (final Transference trans : this._download_manager.getTransference_running_list()) {
                                final Download download = (Download) trans;
                                if (download.isPaused()) {
                                    download.pause();
                                }
                                if (!download.getChunkworkers().isEmpty()) {
                                    wait = true;
//                                    MiscTools.GUIRun(() -> {
//                                        download.getView().printStatusNormal("Stopping download safely before exit MegaBasterd, please wait...");
//                                        download.getView().getSlots_spinner().setEnabled(false);
//                                        download.getView().getPause_button().setEnabled(false);
//                                        download.getView().getCopy_link_button().setEnabled(false);
//                                        download.getView().getOpen_folder_button().setEnabled(false);
//                                        download.getView().getFile_size_label().setEnabled(false);
//                                        download.getView().getFile_name_label().setEnabled(false);
//                                        download.getView().getSpeed_label().setEnabled(false);
//                                        download.getView().getSlots_label().setEnabled(false);
//                                        download.getView().getProgress_pbar().setEnabled(false);
//                                    });
                                }
                            }
                        }
                        if (!this._upload_manager.getTransference_running_list().isEmpty()) {
                            for (final Transference trans : this._upload_manager.getTransference_running_list()) {
                                final Upload upload = (Upload) trans;
                                upload.getMac_generator().secureNotify();
                                if (upload.isPaused()) {
                                    upload.pause();
                                }
                                if (!upload.getChunkworkers().isEmpty()) {
                                    wait = true;
                                    MiscTools.GUIRun(() -> {
                                        upload.getView().printStatusNormal("Stopping upload safely before exit MegaBasterd, please wait...");
                                        upload.getView().getSlots_spinner().setEnabled(false);
                                        upload.getView().getPause_button().setEnabled(false);
                                        upload.getView().getFolder_link_button().setEnabled(false);
                                        upload.getView().getFile_link_button().setEnabled(false);
                                        upload.getView().getFile_size_label().setEnabled(false);
                                        upload.getView().getFile_name_label().setEnabled(false);
                                        upload.getView().getSpeed_label().setEnabled(false);
                                        upload.getView().getSlots_label().setEnabled(false);
                                        upload.getView().getProgress_pbar().setEnabled(false);
                                    });
                                } else {
                                    try {
                                        DBTools.updateUploadProgress(upload.getFile_name(), upload.getMa().getFull_email(), upload.getProgress(), upload.getTemp_mac_data() != null ? upload.getTemp_mac_data() : null);
                                    } catch (final SQLException ex) {
                                        Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                                    }
                                }
                            }
                        }

                        final ArrayList<String> downloads_queue = new ArrayList<>();
                        final ArrayList<String> uploads_queue = new ArrayList<>();

                        for (final Transference t : this._download_manager.getTransference_running_list()) {
                            downloads_queue.add(((Download) t).getUrl());
                        }

                        for (final Transference t : this._download_manager.getTransference_waitstart_queue()) {
                            downloads_queue.add(((Download) t).getUrl());
                        }

                        for (final Transference t : this._upload_manager.getTransference_running_list()) {
                            uploads_queue.add(t.getFile_name());
                        }

                        for (final Transference t : this._upload_manager.getTransference_waitstart_queue()) {
                            uploads_queue.add(t.getFile_name());
                        }

                        try {
                            DBTools.truncateDownloadsQueue();
                            DBTools.insertDownloadsQueue(downloads_queue);

                            DBTools.truncateUploadsQueue();
                            DBTools.insertUploadsQueue(uploads_queue);
                        } catch (final SQLException ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (wait) {

                            try {
                                Thread.sleep(1000);
                            } catch (final InterruptedException ex) {
                                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex.getMessage());
                            }
                        }
                    } while (wait);
                    this.byebyenow(restart);
                });

//                final WarningExitMessage exit_message = new WarningExitMessage(this.getView(), true, this, restart);
//
//                exit_message.setLocationRelativeTo(this.getView());
//
//                exit_message.setVisible(true);

            } else {
                this.byebyenow(restart);
            }
        }
    }

    private boolean checkAppIsRunning() {

        boolean app_is_running = true;

        try {
            final Socket clientSocket = new Socket(InetAddress.getLoopbackAddress(), WATCHDOG_PORT);

            clientSocket.close();

        } catch (final Exception ex) {

            app_is_running = false;

            try {

                final ServerSocket serverSocket = new ServerSocket(WATCHDOG_PORT, 0, InetAddress.getLoopbackAddress());

                THREAD_POOL.execute(() -> {
                    final ServerSocket socket = serverSocket;
                    while (true) {
                        try {
                            socket.accept();
//                            MiscTools.GUIRun(() -> {
//                                this.getView().setExtendedState(NORMAL);
//
//                                this.getView().setVisible(true);
//                            });
                        } catch (final Exception ex1) {
                            Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex1.getMessage());
                        }
                    }
                });
            } catch (final Exception ex2) {

                Logger.getLogger(MainPanel.class.getName()).log(Level.SEVERE, ex2.getMessage());

            }

        }

        return app_is_running;
    }

    public void resumeDownloads() {

        if (!getResume_downloads()) {

//            MiscTools.GUIRun(() -> {
//                this.getView().getStatus_down_label().setText(LabelTranslatorSingleton.getInstance().translate("Checking if there are previous downloads, please wait..."));
//            });

            final MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                int conta_downloads = 0, tot_downloads = -1;
                try {

                    final ArrayList<String> downloads_queue = DBTools.selectDownloadsQueue();

                    final HashMap<String, HashMap<String, Object>> res = selectDownloads();

                    tot_downloads = res.size();

                    final Iterator downloads_queue_iterator = downloads_queue.iterator();

                    while (downloads_queue_iterator.hasNext()) {

                        try {

                            final String url = (String) downloads_queue_iterator.next();

                            final HashMap<String, Object> o = res.remove(url);

                            if (o != null) {

                                String email = (String) o.get("email");

                                if (this._mega_accounts.get(email) == null) {
                                    email = null;
                                }

                                MegaAPI ma = new MegaAPI();

                                if (email == null || !tthis.isUse_mega_account_down() || (ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, email)) != null) {

                                    final Download download = new Download(tthis, ma, (String) url, (String) o.get("path"), (String) o.get("filename"), (String) o.get("filekey"), (Long) o.get("filesize"), (String) o.get("filepass"), (String) o.get("filenoexpire"), this._use_slots_down, false, (String) o.get("custom_chunks_dir"), false);

                                    this.getDownload_manager().getTransference_provision_queue().add(download);

                                    conta_downloads++;

                                    downloads_queue_iterator.remove();
                                } else {
                                    tot_downloads--;
                                }
                            }

                        } catch (final Exception ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                        }
                    }

                    DBTools.truncateDownloadsQueue();

                    if (!downloads_queue.isEmpty()) {
                        DBTools.insertDownloadsQueue(downloads_queue);
                    }

                    if (!res.isEmpty()) {

                        for (final Map.Entry<String, HashMap<String, Object>> entry : res.entrySet()) {

                            try {

                                String email = (String) entry.getValue().get("email");

                                if (this._mega_accounts.get(email) == null) {
                                    email = null;
                                }

                                MegaAPI ma = new MegaAPI();

                                if (email == null || !tthis.isUse_mega_account_down() || (ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, email)) != null) {

                                    final Download download = new Download(tthis, ma, (String) entry.getKey(), (String) entry.getValue().get("path"), (String) entry.getValue().get("filename"), (String) entry.getValue().get("filekey"), (Long) entry.getValue().get("filesize"), (String) entry.getValue().get("filepass"), (String) entry.getValue().get("filenoexpire"), this._use_slots_down, false, (String) entry.getValue().get("custom_chunks_dir"), false);

                                    this.getDownload_manager().getTransference_provision_queue().add(download);

                                    conta_downloads++;

                                } else {

                                    tot_downloads--;
                                }

                            } catch (final Exception ex) {
                                Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                            }

                        }

                    }

                } catch (final Exception ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                }

                if (conta_downloads > 0) {

                    if (conta_downloads == tot_downloads) {
                        setResume_downloads(true);
                    }

                    this._download_manager.setSort_wait_start_queue(false);
                    this.getDownload_manager().secureNotify();
//                    MiscTools.GUIRun(() -> {
//                        this.getView().getjTabbedPane1().setSelectedIndex(0);
//                    });

                } else {
                    setResume_downloads(true);
                }

//                MiscTools.GUIRun(() -> {
//                    this.getView().getStatus_down_label().setText("");
//                });
            });

        }

    }

    public void resumeDownloadsCli() {

        if (!getResume_downloads()) {

            final MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                int conta_downloads = 0, tot_downloads = -1;
                try {

                    final ArrayList<String> downloads_queue = DBTools.selectDownloadsQueue();

                    final HashMap<String, HashMap<String, Object>> res = selectDownloads();

                    tot_downloads = res.size();

                    final Iterator downloads_queue_iterator = downloads_queue.iterator();

                    while (downloads_queue_iterator.hasNext()) {

                        try {

                            final String url = (String) downloads_queue_iterator.next();

                            final HashMap<String, Object> o = res.remove(url);

                            if (o != null) {

                                String email = (String) o.get("email");

                                if (this._mega_accounts.get(email) == null) {
                                    email = null;
                                }

                                MegaAPI ma = new MegaAPI();

                                if (email == null || !tthis.isUse_mega_account_down() || (ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, email)) != null) {

                                    final Download download = new Download(tthis, ma, (String) url, (String) o.get("path"), (String) o.get("filename"), (String) o.get("filekey"), (Long) o.get("filesize"), (String) o.get("filepass"), (String) o.get("filenoexpire"), this._use_slots_down, false, (String) o.get("custom_chunks_dir"), false);

                                    this.getDownload_manager().getTransference_provision_queue().add(download);

                                    conta_downloads++;

                                    downloads_queue_iterator.remove();
                                } else {
                                    tot_downloads--;
                                }
                            }

                        } catch (final Exception ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                        }
                    }

                    DBTools.truncateDownloadsQueue();

                    if (!downloads_queue.isEmpty()) {
                        DBTools.insertDownloadsQueue(downloads_queue);
                    }

                    if (!res.isEmpty()) {

                        for (final Map.Entry<String, HashMap<String, Object>> entry : res.entrySet()) {

                            try {

                                String email = (String) entry.getValue().get("email");

                                if (this._mega_accounts.get(email) == null) {
                                    email = null;
                                }

                                MegaAPI ma = new MegaAPI();

                                if (email == null || !tthis.isUse_mega_account_down() || (ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, email)) != null) {

                                    final Download download = new Download(tthis, ma, (String) entry.getKey(), (String) entry.getValue().get("path"), (String) entry.getValue().get("filename"), (String) entry.getValue().get("filekey"), (Long) entry.getValue().get("filesize"), (String) entry.getValue().get("filepass"), (String) entry.getValue().get("filenoexpire"), this._use_slots_down, false, (String) entry.getValue().get("custom_chunks_dir"), false);

                                    this.getDownload_manager().getTransference_provision_queue().add(download);

                                    conta_downloads++;

                                } else {

                                    tot_downloads--;
                                }

                            } catch (final Exception ex) {
                                Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                            }

                        }

                    }

                } catch (final Exception ex) {
                    Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                }

                if (conta_downloads > 0) {

                    if (conta_downloads == tot_downloads) {
                        setResume_downloads(true);
                    }

                    this._download_manager.setSort_wait_start_queue(false);
                    this.getDownload_manager().secureNotify();

                } else {
                    setResume_downloads(true);
                }
            });

        }

    }

//    public void trayIcon() throws AWTException {
//
//        if (java.awt.SystemTray.isSupported()) {

//            final JPopupMenu menu = new JPopupMenu();

//            final Font new_font = GUI_FONT;

//            menu.setFont(new_font.deriveFont(Font.BOLD, Math.round(14 * ZOOM_FACTOR)));
//
//            final JMenuItem messageItem = new JMenuItem(LabelTranslatorSingleton.getInstance().translate("Restore window"));
//
//            messageItem.addActionListener((final ActionEvent e) -> {
//
//                this.getView().setExtendedState(NORMAL);
//
//                this.getView().setVisible(true);
//
//                this.getView().revalidate();
//
//                this.getView().repaint();
//
//            });
//
//            menu.add(messageItem);
//
//            final JMenuItem closeItem = new JMenuItem(LabelTranslatorSingleton.getInstance().translate("EXIT"));
//
//            closeItem.addActionListener((final ActionEvent e) -> {
//                if (!this.getView().isVisible()) {
//
//                    this.getView().setExtendedState(NORMAL);
//                    this.getView().setVisible(true);
//                    this.getView().revalidate();
//                    this.getView().repaint();
//
//                }
//
//                this.byebye(false);
//            });
//
//            menu.add(closeItem);

//            this._trayicon = new TrayIcon(getDefaultToolkit().getImage(this.getClass().getResource(ICON_FILE)), "MegaBasterd", null);
//
//            this._trayicon.setToolTip("MegaBasterd " + VERSION);
//
//            this._trayicon.setImageAutoSize(true);
//
//            this._trayicon.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseReleased(final MouseEvent e) {

//                    if (SwingUtilities.isRightMouseButton(e)) {
//                        menu.setLocation(e.getX(), e.getY());
//                        menu.setInvoker(menu);
//                        menu.setVisible(true);
//                    } else {
//                        if (!MainPanel.this.getView().isVisible()) {
//                            MainPanel.this.getView().setExtendedState(NORMAL);
//                            MainPanel.this.getView().setVisible(true);
//                            MainPanel.this.getView().revalidate();
//                            MainPanel.this.getView().repaint();
//
//                        } else {
//
//                            MainPanel.this.getView().dispatchEvent(new WindowEvent(MainPanel.this.getView(), WINDOW_CLOSING));
//                        }
//                    }

//                }
//            });
//
//            getSystemTray().add(this._trayicon);
//
//        }
//
//    }

    public void resumeUploads() {

        if (!getResume_uploads()) {

//            MiscTools.GUIRun(() -> {
//                this.getView().getStatus_up_label().setText(LabelTranslatorSingleton.getInstance().translate("Checking if there are previous uploads, please wait..."));
//            });

            final MainPanel tthis = this;

            THREAD_POOL.execute(() -> {
                        int conta_uploads = 0, tot_uploads = -1;
                        try {

                            final ArrayList<String> uploads_queue = DBTools.selectUploadsQueue();

                            final HashMap<String, HashMap<String, Object>> res = selectUploads();

                            tot_uploads = res.size();

                            final Iterator uploads_queue_iterator = uploads_queue.iterator();

                            while (uploads_queue_iterator.hasNext()) {

                                try {
                                    final String filename = (String) uploads_queue_iterator.next();

                                    final HashMap<String, Object> o = res.remove(filename);

                                    if (o != null) {

                                        final String email = (String) o.get("email");

                                        if (this._mega_accounts.get(email) != null) {

                                            final MegaAPI ma;

                                            if ((ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, email)) != null) {

                                                final Upload upload = new Upload(tthis, ma, (String) filename, (String) o.get("parent_node"), (String) o.get("ul_key") != null ? bin2i32a(BASE642Bin((String) o.get("ul_key"))) : null, (String) o.get("url"), (String) o.get("root_node"), BASE642Bin((String) o.get("share_key")), (String) o.get("folder_link"), false);

                                                this.getUpload_manager().getTransference_provision_queue().add(upload);

                                                conta_uploads++;

                                                uploads_queue_iterator.remove();

                                            }

                                        } else {

                                            deleteUpload((String) o.get("filename"), email);

                                            tot_uploads--;

                                            uploads_queue_iterator.remove();
                                        }

                                    }

                                } catch (final Exception ex) {
                                    Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                                }
                            }

                            DBTools.truncateUploadsQueue();

                            if (!uploads_queue.isEmpty()) {
                                DBTools.insertUploadsQueue(uploads_queue);
                            }

                            if (!res.isEmpty()) {

                                for (final Map.Entry<String, HashMap<String, Object>> entry : res.entrySet()) {

                                    try {
                                        final String email = (String) entry.getValue().get("email");

                                        if (this._mega_accounts.get(email) != null) {

                                            final MegaAPI ma;

                                            if ((ma = checkMegaAccountLoginAndShowMasterPassDialog(tthis, email)) != null) {

                                                final Upload upload = new Upload(tthis, ma, (String) entry.getKey(), (String) entry.getValue().get("parent_node"), (String) entry.getValue().get("ul_key") != null ? bin2i32a(BASE642Bin((String) entry.getValue().get("ul_key"))) : null, (String) entry.getValue().get("url"), (String) entry.getValue().get("root_node"), BASE642Bin((String) entry.getValue().get("share_key")), (String) entry.getValue().get("folder_link"), false);

                                                this.getUpload_manager().getTransference_provision_queue().add(upload);

                                                conta_uploads++;
                                            }

                                        } else {

                                            deleteUpload((String) entry.getValue().get("filename"), email);

                                            tot_uploads--;
                                        }

                                    } catch (final Exception ex) {
                                        Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                                    }
                                }

                            }

                        } catch (final Exception ex) {
                            Logger.getLogger(MainPanel.class.getName()).log(SEVERE, null, ex);
                        }

                        if (conta_uploads > 0) {

                            if (conta_uploads == tot_uploads) {
                                setResume_uploads(true);
                            }

                            this._upload_manager.setSort_wait_start_queue(false);
                            this.getUpload_manager().secureNotify();
//                            MiscTools.GUIRun(() -> {
//                                this.getView().getjTabbedPane1().setSelectedIndex(1);
//                            });
                        } else {
                            setResume_uploads(true);
                        }

//                        MiscTools.GUIRun(() -> {
//                            this.getView().getStatus_up_label().setText("");
//                        });
                    }
            );
        }
    }

}
