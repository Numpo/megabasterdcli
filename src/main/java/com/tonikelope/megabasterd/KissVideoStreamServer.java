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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.CipherInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tonikelope.megabasterd.MainPanel.STREAMER_PORT;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.checkMegaAccountLoginAndShowMasterPassDialog;
import static com.tonikelope.megabasterd.MiscTools.checkMegaDownloadUrl;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static com.tonikelope.megabasterd.MiscTools.getWaitTimeExpBackOff;
import static java.util.logging.Level.SEVERE;

/**
 * @author tonikelope
 */
public class KissVideoStreamServer implements HttpHandler, SecureSingleThreadNotifiable {

    public static final int THREAD_START = 0x01;
    public static final int THREAD_STOP = 0x02;
    public static final int DEFAULT_WORKERS = 10;
    private static final Logger LOG = Logger.getLogger(KissVideoStreamServer.class.getName());

    private final MainPanel _main_panel;
    private final ConcurrentHashMap<String, HashMap<String, Object>> _link_cache;
    private final ConcurrentLinkedQueue<Thread> _working_threads;
    private final ContentType _ctype;
    private volatile boolean _notified;
    private final Object _secure_notify_lock;

    public KissVideoStreamServer(final MainPanel panel) {
        this._main_panel = panel;
        this._link_cache = new ConcurrentHashMap();
        this._working_threads = new ConcurrentLinkedQueue<>();
        this._ctype = new ContentType();
        this._notified = false;
        this._secure_notify_lock = new Object();

    }

    public MainPanel getMain_panel() {
        return this._main_panel;
    }

    public ConcurrentHashMap<String, HashMap<String, Object>> getLink_cache() {
        return this._link_cache;
    }

    public ConcurrentLinkedQueue<Thread> getWorking_threads() {
        return this._working_threads;
    }

    public ContentType getCtype() {
        return this._ctype;
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
                    LOG.log(SEVERE, null, ex);
                }
            }

            this._notified = false;
        }
    }

    public void start(final int port, final String context) throws IOException {

//        _main_panel.getView().updateKissStreamServerStatus(LabelTranslatorSingleton.getInstance().translate("Streaming server: ON (port ") + STREAMER_PORT + ")");

        final HttpServer httpserver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);

        httpserver.createContext(context, this);

        httpserver.setExecutor(THREAD_POOL);

        httpserver.start();
    }

    private void _updateStatus(final Integer status) {

        if (status == THREAD_START && !this.getWorking_threads().contains(Thread.currentThread())) {
            this.getWorking_threads().add(Thread.currentThread());
        } else {
            this.getWorking_threads().remove(Thread.currentThread());
        }

        this._updateStatusView();
    }

    private void _updateStatusView() {

        final String status;

        if (this.getWorking_threads().size() > 0) {

            status = LabelTranslatorSingleton.getInstance().translate("Streaming server: ON (port ") + STREAMER_PORT + " [" + this.getWorking_threads().size() + "])";

        } else {

            status = LabelTranslatorSingleton.getInstance().translate("Streaming server: ON (port ") + STREAMER_PORT + ")";
        }

        this._main_panel.getView().updateKissStreamServerStatus(status);

    }

    private String[] _getMegaFileMetadata(final String link, final MainPanelView panel) throws IOException {

        String[] file_info = null;
        int conta_error = 0;
        boolean error;

        do {

            error = false;

            try {

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {

                    final MegaAPI ma = new MegaAPI();

                    file_info = ma.getMegaFileMetadata(link);

                } else {

                    file_info = MegaCrypterAPI.getMegaFileMetadata(link, panel, this.getMain_panel().getMega_proxy_server() != null ? (this.getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + this.getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8"))) : null);

                }

            } catch (final APIException ex) {

                error = true;

                LOG.log(Level.SEVERE, ex.getMessage());

                try {
                    Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                } catch (final InterruptedException ex2) {
                    LOG.log(Level.SEVERE, ex2.getMessage());

                }

            }

        } while (error);

        return file_info;
    }

    public String getMegaFileDownloadUrl(final String link, final String pass_hash, final String noexpire_token, final String mega_account) throws Exception {

        String dl_url = null;
        int conta_error = 0;
        boolean error;

        do {

            error = false;

            try {

                MegaAPI ma = new MegaAPI();

                if (mega_account != null) {

                    ma = checkMegaAccountLoginAndShowMasterPassDialog(this._main_panel, mega_account);
                }

                if (findFirstRegex("://mega(\\.co)?\\.nz/", link, 0) != null) {
                    dl_url = ma.getMegaFileDownloadUrl(link);

                } else {
                    dl_url = MegaCrypterAPI.getMegaFileDownloadUrl(link, pass_hash, noexpire_token, ma.getSid(), this.getMain_panel().getMega_proxy_server() != null ? (this.getMain_panel().getMega_proxy_server().getPort() + ":" + Bin2BASE64(("megacrypter:" + this.getMain_panel().getMega_proxy_server().getPassword()).getBytes("UTF-8")) + ":" + MiscTools.getMyPublicIP()) : null);
                }

            } catch (final APIException ex) {

                error = true;

                LOG.log(Level.SEVERE, ex.getMessage());

                try {
                    Thread.sleep(getWaitTimeExpBackOff(conta_error++) * 1000);
                } catch (final InterruptedException ex2) {
                    LOG.log(Level.SEVERE, ex2.getMessage());
                }

            }

        } while (error);

        return dl_url;
    }

    private long[] _parseRangeHeader(final String header) {

        final Pattern pattern = Pattern.compile("bytes *\\= *([0-9]+) *\\- *([0-9]+)?");

        final Matcher matcher = pattern.matcher(header);

        final long[] ranges = new long[2];

        if (matcher.find()) {
            ranges[0] = Long.valueOf(matcher.group(1));

            if (matcher.group(2) != null) {
                ranges[1] = Long.valueOf(matcher.group(2));
            } else {
                ranges[1] = -1;
            }
        }

        return ranges;
    }

    @Override
    public void handle(final HttpExchange xchg) throws IOException {

        this._updateStatus(THREAD_START);

        StreamChunkManager chunkwriter = null;
        final ArrayList<StreamChunkDownloader> chunkworkers = new ArrayList<>();
        final PipedOutputStream pipeout = new PipedOutputStream();
        final PipedInputStream pipein = new PipedInputStream(pipeout);

        final long clength;

        final OutputStream os;

        final CipherInputStream cis;

        final String httpmethod = xchg.getRequestMethod();

        try {

            final Headers reqheaders = xchg.getRequestHeaders();

            final Headers resheaders = xchg.getResponseHeaders();

            final String url_path = xchg.getRequestURI().getPath();

            String mega_account;

            final String link;

            final String[] url_parts = new String(UrlBASE642Bin(url_path.substring(url_path.indexOf("/video/") + 7)), "UTF-8").split("\\|");

            mega_account = url_parts[0];

            if (mega_account.isEmpty()) {
                mega_account = null;
            }

            link = url_parts[1];

            LOG.log(Level.INFO, "{0} {1} {2}", new Object[]{Thread.currentThread().getName(), link, mega_account});

            final HashMap cache_info;
            final HashMap file_info;

            cache_info = this.getLink_cache().get(link);

            if (cache_info != null) {

                file_info = cache_info;

            } else {

                final String[] finfo = this._getMegaFileMetadata(link, this._main_panel.getView());

                file_info = new HashMap<>();

                file_info.put("file_name", finfo[0]);

                file_info.put("file_size", Long.parseLong(finfo[1]));

                file_info.put("file_key", finfo[2]);

                file_info.put("pass_hash", finfo.length >= 5 ? finfo[3] : null);

                file_info.put("noexpiretoken", finfo.length >= 5 ? finfo[4] : null);

                file_info.put("url", null);
            }

            final String file_name = (String) file_info.get("file_name");

            final long file_size = (long) file_info.get("file_size");

            final String file_key = (String) file_info.get("file_key");

            final String pass_hash = (String) file_info.get("pass_hash");

            final String noexpire_token = (String) file_info.get("noexpiretoken");

            final String file_ext = file_name.substring(file_name.lastIndexOf('.') + 1).toLowerCase();

            if (httpmethod.equals("HEAD")) {

                resheaders.add("Accept-Ranges", "bytes");

                resheaders.add("transferMode.dlna.org", "Streaming");

                resheaders.add("contentFeatures.dlna.org", "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000");

                resheaders.add("Content-Type", this.getCtype().getMIME(file_ext));

                resheaders.add("Content-Length", String.valueOf(file_size));

                resheaders.add("Connection", "close");

                xchg.sendResponseHeaders(200, 0);

            } else if (httpmethod.equals("GET")) {

                resheaders.add("Accept-Ranges", "bytes");

                resheaders.add("transferMode.dlna.org", "Streaming");

                resheaders.add("contentFeatures.dlna.org", "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000");

                resheaders.add("Content-Type", this.getCtype().getMIME(file_ext));

                resheaders.add("Connection", "close");

                final byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                int reads;

                String temp_url;

                if (file_info.get("url") != null) {

                    temp_url = (String) file_info.get("url");

                    if (!checkMegaDownloadUrl(temp_url)) {

                        temp_url = this.getMegaFileDownloadUrl(link, pass_hash, noexpire_token, mega_account);

                        file_info.put("url", temp_url);
                    }

                } else {

                    temp_url = this.getMegaFileDownloadUrl(link, pass_hash, noexpire_token, mega_account);

                    file_info.put("url", temp_url);
                }

                this.getLink_cache().put(link, file_info);

                long[] ranges = new long[2];

                int sync_bytes = 0;

                String header_range = null;

                final InputStream is;

                URL url;

                if (reqheaders.containsKey("Range")) {
                    header_range = "Range";

                } else if (reqheaders.containsKey("range")) {

                    header_range = "range";
                }

                if (header_range != null) {
                    final List<String> ranges_raw = reqheaders.get(header_range);

                    final String range_header = ranges_raw.get(0);

                    ranges = this._parseRangeHeader(range_header);

                    sync_bytes = (int) ranges[0] % 16;

                    if (ranges[1] >= 0 && ranges[1] >= ranges[0]) {

                        clength = ranges[1] - ranges[0] + 1;

                    } else {

                        clength = file_size - ranges[0];
                    }

                    resheaders.add("Content-Range", "bytes " + ranges[0] + "-" + (ranges[1] >= 0 ? ranges[1] : (file_size - 1)) + "/" + file_size);

                    xchg.sendResponseHeaders(206, clength);

                    chunkwriter = new StreamChunkManager(this, link, file_info, mega_account, pipeout, temp_url, ranges[0] - sync_bytes, ranges[1] >= 0 ? ranges[1] : file_size - 1);

                } else {

                    xchg.sendResponseHeaders(200, file_size);

                    chunkwriter = new StreamChunkManager(this, link, file_info, mega_account, pipeout, temp_url, 0, file_size - 1);
                }

                THREAD_POOL.execute(chunkwriter);

                for (int i = 0; i < DEFAULT_WORKERS; i++) {

                    final StreamChunkDownloader worker = new StreamChunkDownloader(i + 1, chunkwriter);

                    chunkworkers.add(worker);

                    THREAD_POOL.execute(worker);
                }

                is = pipein;

                final byte[] iv = CryptTools.initMEGALinkKeyIV(file_key);

                cis = new CipherInputStream(is, CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", CryptTools.initMEGALinkKey(file_key), (header_range != null && (ranges[0] - sync_bytes) > 0) ? CryptTools.forwardMEGALinkKeyIV(iv, ranges[0] - sync_bytes) : iv));

                os = xchg.getResponseBody();

                cis.skip(sync_bytes);

                while ((reads = cis.read(buffer)) != -1) {

                    os.write(buffer, 0, reads);
                }
            }
        } catch (final Exception ex) {

            if (!(ex instanceof IOException)) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }

        } finally {
            LOG.log(Level.INFO, "{0} KissVideoStreamerHandle: bye bye", Thread.currentThread().getName());

            if (chunkwriter != null) {

                pipeout.close();

                chunkworkers.forEach((d) -> {
                    d.setExit(true);
                });

                chunkwriter.setExit(true);

                chunkwriter.secureNotifyAll();
            }

            xchg.close();
        }

        this._updateStatus(THREAD_STOP);
    }
}
