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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tonikelope.megabasterd.MainPanel.VERSION;

/**
 * @author tonikelope
 */
public class MiscTools {

    public static final int EXP_BACKOFF_BASE = 2;
    public static final int EXP_BACKOFF_SECS_RETRY = 1;
    public static final int EXP_BACKOFF_MAX_WAIT_TIME = 8;
    public static final Object PASS_LOCK = new Object();
    public static final int HTTP_TIMEOUT = 30;
    public static final String UPLOAD_LOGS_DIR = System.getProperty("user.home") + File.separator + "MEGABASTERD_UPLOAD_LOGS";

    //    private static final Comparator<DefaultMutableTreeNode> TREE_NODE_COMPARATOR = (DefaultMutableTreeNode a, DefaultMutableTreeNode b) -> {
//        if (a.isLeaf() && !b.isLeaf()) {
//            return 1;
//        } else if (!a.isLeaf() && b.isLeaf()) {
//            return -1;
//        } else {
//
//            Object ca = a.getUserObject();
//
//            Object cb = b.getUserObject();
//
//            if (ca instanceof String) {
//                return MiscTools.naturalCompare((String) ca, (String) cb, true);
//            } else {
//                return MiscTools.naturalCompare((String) ((Map) ca).get("name"), (String) ((Map) cb).get("name"), true);
//            }
//        }
//    };
    private static final Logger LOG = Logger.getLogger(MiscTools.class.getName());

    public static String computeFileSHA1(final File file) throws IOException {

        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            final BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            int n = 0;
            final byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
            return new HexBinaryAdapter().marshal(digest.digest()).toLowerCase().trim();
        } catch (final NoSuchAlgorithmException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static void createUploadLogDir() {

        if (!Files.exists(Paths.get(UPLOAD_LOGS_DIR))) {
            try {
                Files.createDirectory(Paths.get(UPLOAD_LOGS_DIR));

                final File dir = new File(System.getProperty("user.home"));

                for (final File file : dir.listFiles()) {
                    if (!file.isDirectory() && file.getName().startsWith("megabasterd_upload_")) {
                        Files.move(file.toPath(), Paths.get(UPLOAD_LOGS_DIR + File.separator + file.getName()));
                    }
                }

            } catch (final IOException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static void purgeFolderCache() {
        final File directory = new File(System.getProperty("java.io.tmpdir"));

        for (final File f : directory.listFiles()) {
            if (f.isFile() && f.getName().startsWith("megabasterd_folder_cache_")) {
                f.delete();
                Logger.getLogger(MiscTools.class.getName()).log(Level.INFO, "REMOVING FOLDER CACHE FILE {0}", f.getAbsolutePath());
            }
        }
    }

//    public static void containerSetEnabled(final Container panel, final boolean enabled) {
//
//        for (final Component cp : panel.getComponents()) {
//
//            if (cp instanceof Container) {
//                containerSetEnabled((Container) cp, enabled);
//            }
//
//            cp.setEnabled(enabled);
//        }
//    }

    public static String getFechaHoraActual() {

        final String format = "dd-MM-yyyy HH:mm:ss";

        return getFechaHoraActual(format);
    }

    public static boolean isVideoFile(final String filename) {

        try {

            final String part_file = MiscTools.findFirstRegex("\\.part[0-9]+-[0-9]+$", filename, 0);

            return part_file == null && Files.probeContentType(Paths.get(filename)).startsWith("video/");
        } catch (final IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public static boolean isImageFile(final String filename) {

        try {

            final String part_file = MiscTools.findFirstRegex("\\.part[0-9]+-[0-9]+$", filename, 0);

            return part_file == null && Files.probeContentType(Paths.get(filename)).startsWith("image/");
        } catch (final IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public static String getFechaHoraActual(final String format) {

        final Date currentDate = new Date(System.currentTimeMillis());

        final DateFormat df = new SimpleDateFormat(format);

        return df.format(currentDate);
    }

    public static void deleteDirectoryRecursion(final Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (final DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (final Path entry : entries) {
                    deleteDirectoryRecursion(entry);
                }
            }
        }
        Files.delete(path);
    }

//    public static Font createAndRegisterFont(final String name) {
//
//        Font font = null;
//
//        try {
//
//            font = Font.createFont(Font.TRUETYPE_FONT, MiscTools.class.getResourceAsStream(name));
//
//            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//
//            ge.registerFont(font);
//
//        } catch (final FontFormatException | IOException ex) {
//            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
//        }
//
//        return font;
//    }

    public static void setNimbusLookAndFeel(final boolean dark) {
//
//        try {
//            for (final javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//
//                if ("Nimbus".equals(info.getName())) {
//
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//
//                    if (dark) {
//                        // Dark LAF
//                        UIManager.put("control", new Color(128, 128, 128));
//                        UIManager.put("info", new Color(128, 128, 128));
//                        UIManager.put("nimbusBase", new Color(18, 30, 49));
//                        UIManager.put("nimbusAlertYellow", new Color(248, 187, 0));
//                        UIManager.put("nimbusDisabledText", new Color(100, 100, 100));
//                        UIManager.put("nimbusFocus", new Color(115, 164, 209));
//                        UIManager.put("nimbusGreen", new Color(176, 179, 50));
//                        UIManager.put("nimbusInfoBlue", new Color(66, 139, 221));
//                        UIManager.put("nimbusLightBackground", new Color(18, 30, 49));
//                        UIManager.put("nimbusOrange", new Color(191, 98, 4));
//                        UIManager.put("nimbusRed", new Color(169, 46, 34));
//                        UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
//                        UIManager.put("nimbusSelectionBackground", new Color(104, 93, 156));
//                        UIManager.put("text", new Color(230, 230, 230));
//
//                    } else {
//                        final UIDefaults defaults = UIManager.getLookAndFeelDefaults();
//                        defaults.put("nimbusOrange", defaults.get("nimbusFocus"));
//                    }
//
//                    break;
//                }
//            }
//        } catch (final Exception ex) {
//            java.util.logging.Logger.getLogger(MiscTools.class.getName()).log(java.util.logging.Level.SEVERE, ex.getMessage());
//        }
    }

    public static int[] bin2i32a(final byte[] bin) {
        final int l = (int) (4 * Math.ceil((double) bin.length / 4));

        final IntBuffer intBuf = ByteBuffer.wrap(bin, 0, l).order(ByteOrder.BIG_ENDIAN).asIntBuffer();

        final int[] array = new int[intBuf.remaining()];

        intBuf.get(array);

        return array;
    }

    public static byte[] i32a2bin(final int[] values) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final DataOutputStream dos = new DataOutputStream(baos);

        for (int i = 0; i < values.length; ++i) {
            try {
                dos.writeInt(values[i]);
            } catch (final IOException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return baos.toByteArray();
    }

    public static BigInteger mpi2big(final byte[] s) {

        final byte[] ns = Arrays.copyOfRange(s, 2, s.length);

        final BigInteger bigi = new BigInteger(1, ns);

        return bigi;
    }

//    public static BufferedImage toBufferedImage(final Image img) {
//        if (img instanceof BufferedImage) {
//            return (BufferedImage) img;
//        }
//
//        // Create a buffered image with transparency
//        final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
//
//        // Draw the image on to the buffered image
//        final Graphics2D bGr = bimage.createGraphics();
//        bGr.drawImage(img, 0, 0, null);
//        bGr.dispose();
//
//        // Return the buffered image
//        return bimage;
//    }

    public static String genID(final int length) {

        final String pos = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        String res = "";

        final Random randomno = new Random();

        for (int i = 0; i < length; i++) {

            res += pos.charAt(randomno.nextInt(pos.length()));
        }

        return res;
    }

    public static byte[] long2bytearray(long val) {

        final byte[] b = new byte[8];

        for (int i = 7; i >= 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }

        return b;
    }

    public static long bytearray2long(final byte[] val) {

        long l = 0;

        for (int i = 0; i <= 7; i++) {
            l += val[i];
            l <<= 8;
        }

        return l;
    }

    public static String findFirstRegex(final String regex, final String data, final int group) {
        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        final Matcher matcher = pattern.matcher(data);

        return matcher.find() ? matcher.group(group) : null;
    }

    public static ArrayList<String> findAllRegex(final String regex, final String data, final int group) {
        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

        final Matcher matcher = pattern.matcher(data);

        final ArrayList<String> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(matcher.group(group));
        }

        return matches;
    }

//    public static void updateFonts(final Component component, final Font font, final float zoom_factor) {
//
//        if (component != null) {
//
//            if (component instanceof javax.swing.JMenu) {
//
//                for (final Component child : ((javax.swing.JMenu) component).getMenuComponents()) {
//                    if (child instanceof JMenuItem) {
//
//                        updateFonts(child, font, zoom_factor);
//                    }
//                }
//
//            } else if (component instanceof Container) {
//
//                for (final Component child : ((Container) component).getComponents()) {
//                    if (child instanceof Container) {
//
//                        updateFonts(child, font, zoom_factor);
//                    }
//                }
//            }
//
//            final Font old_font = component.getFont();
//
//            final Font new_font = font.deriveFont(old_font.getStyle(), Math.round(old_font.getSize() * zoom_factor));
//
//            boolean error;
//
//            do {
//                try {
//                    component.setFont(new_font);
//                    error = false;
//                } catch (final Exception ex) {
//                    error = true;
//                }
//            } while (error);
//
//        }
//    }

//    public static void translateLabels(final Component component) {
//
//        if (component != null) {
//
//            if (component instanceof javax.swing.JLabel) {
//
//                ((JLabel) component).setText(LabelTranslatorSingleton.getInstance().translate(((JLabel) component).getText()));
//
//            } else if (component instanceof javax.swing.JButton) {
//
//                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));
//
//            } else if (component instanceof javax.swing.JCheckBox) {
//
//                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));
//
//            } else if ((component instanceof JMenuItem) && !(component instanceof JMenu)) {
//
//                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));
//
//            } else if (component instanceof JMenu) {
//
//                for (final Component child : ((JMenu) component).getMenuComponents()) {
//                    if (child instanceof JMenuItem) {
//                        translateLabels(child);
//                    }
//                }
//
//                ((AbstractButton) component).setText(LabelTranslatorSingleton.getInstance().translate(((AbstractButton) component).getText()));
//
//            } else if (component instanceof Container) {
//
//                for (final Component child : ((Container) component).getComponents()) {
//                    if (child instanceof Container) {
//
//                        translateLabels(child);
//                    }
//                }
//
//                if ((component instanceof JPanel) && (((JComponent) component).getBorder() instanceof TitledBorder)) {
//                    ((TitledBorder) ((JComponent) component).getBorder()).setTitle(LabelTranslatorSingleton.getInstance().translate(((TitledBorder) ((JComponent) component).getBorder()).getTitle()));
//                }
//
//                if (component instanceof JDialog) {
//                    ((Dialog) component).setTitle(LabelTranslatorSingleton.getInstance().translate(((Dialog) component).getTitle()));
//                }
//            }
//        }
//    }

    public static Sequencer midiLoopPlay(final String midi, final int volume) {
        try {
            final Sequencer sequencer = MidiSystem.getSequencer();

            if (sequencer == null) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, "MIDI Sequencer device not supported");
                return null;
            }

            final Synthesizer synthesizer = MidiSystem.getSynthesizer();

            final Sequence sequence = MidiSystem.getSequence(MiscTools.class.getResource(midi));

            for (final Track t : sequence.getTracks()) {

                for (int k = 0; k < synthesizer.getChannels().length; k++) {
                    t.add(new MidiEvent(new ShortMessage(ShortMessage.CONTROL_CHANGE, k, 7, volume), t.ticks()));
                }
            }

            sequencer.open();
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            return sequencer;
        } catch (final MidiUnavailableException | InvalidMidiDataException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

//    public static void updateTitledBorderFont(final TitledBorder border, final Font font, final float zoom_factor) {
//
//        final Font old_title_font = border.getTitleFont();
//
//        final Font new_title_font = font.deriveFont(old_title_font.getStyle(), Math.round(old_title_font.getSize() * zoom_factor));
//
//        border.setTitleFont(new_title_font);
//    }

    public static String HashString(final String algo, final String data) {
        try {
            final MessageDigest md = MessageDigest.getInstance(algo);

            final byte[] thedigest = md.digest(data.getBytes("UTF-8"));

            return bin2hex(thedigest);
        } catch (final NoSuchAlgorithmException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        } catch (final UnsupportedEncodingException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static String HashString(final String algo, final byte[] data) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance(algo);

        final byte[] thedigest = md.digest(data);

        return bin2hex(thedigest);
    }

    public static byte[] HashBin(final String algo, final String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest md = MessageDigest.getInstance(algo);

        return md.digest(data.getBytes("UTF-8"));
    }

    public static byte[] HashBin(final String algo, final byte[] data) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance(algo);

        return md.digest(data);
    }

    public static byte[] BASE642Bin(final String data) {
        return Base64.getDecoder().decode(data);
    }

    public static String Bin2BASE64(final byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static byte[] UrlBASE642Bin(final String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    public static String Bin2UrlBASE64(final byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    public static void pausar(final long pause) {
        try {
            Thread.sleep(pause);
        } catch (final InterruptedException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void GUIRun(final Runnable r) {
//
//        if (!SwingUtilities.isEventDispatchThread()) {
//            SwingUtilities.invokeLater(r);
//        } else {
//            r.run();
//        }
//
    }

    public static void GUIRunAndWait(final Runnable r) {
//
//        try {
//            if (!SwingUtilities.isEventDispatchThread()) {
//                SwingUtilities.invokeAndWait(r);
//            } else {
//                r.run();
//            }
//        } catch (final Exception ex) {
//
//            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
//
//        }
//
    }

    public static FutureTask futureRun(final Callable c) {

        final FutureTask f = new FutureTask(c);

        final Thread hilo = new Thread(f);

        hilo.start();

        return f;
    }

    public static long getWaitTimeExpBackOff(final int retryCount) {

        final long waitTime = ((long) Math.pow(EXP_BACKOFF_BASE, retryCount) * EXP_BACKOFF_SECS_RETRY);

        return Math.min(waitTime, EXP_BACKOFF_MAX_WAIT_TIME);
    }

    public static String bin2hex(final byte[] b) {

        final BigInteger bi = new BigInteger(1, b);

        return String.format("%0" + (b.length << 1) + "x", bi);
    }

    public static byte[] hex2bin(final String s) {
        return DatatypeConverter.parseHexBinary(s);
    }

    public static void copyTextToClipboard(final String text) {
//
//        final StringSelection stringSelection = new StringSelection(text);
//        final Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
//        clpbrd.setContents(stringSelection, null);
//
    }

    public static String formatBytes(Long bytes) {

        final String[] units = {"B", "KB", "MB", "GB", "TB"};

        bytes = Math.max(bytes, 0L);

        final int pow = Math.min((int) ((bytes > 0L ? Math.log(bytes) : 0) / Math.log(1024)), units.length - 1);

        final Double bytes_double = (double) bytes / (1L << (10 * pow));

        final DecimalFormat df = new DecimalFormat("#.##");

        return df.format(bytes_double) + ' ' + units[pow];
    }

    public static MegaMutableTreeNode calculateTreeFolderSizes(final MegaMutableTreeNode node) {

        final int n = node.getChildCount();

        for (int i = 0; i < n; i++) {

            if (node.getChildAt(i).isLeaf()) {
                node.setMega_node_size(node.getMega_node_size() + ((MegaMutableTreeNode) node.getChildAt(i)).getMega_node_size());
            } else {
                calculateTreeFolderSizes((MegaMutableTreeNode) node.getChildAt(i));
                node.setMega_node_size(node.getMega_node_size() + ((MegaMutableTreeNode) node.getChildAt(i)).getMega_node_size());
            }
        }

        return node;

    }

    public static MegaMutableTreeNode resetTreeFolderSizes(final MegaMutableTreeNode node) {

        if (!node.isLeaf()) {

            node.setMega_node_size(0);

            final int n = node.getChildCount();

            for (int i = 0; i < n; i++) {

                if (!node.getChildAt(i).isLeaf()) {
                    resetTreeFolderSizes((MegaMutableTreeNode) node.getChildAt(i));
                }
            }
        }

        return node;

    }

    public static MegaMutableTreeNode findMegaTreeNodeByID(final MegaMutableTreeNode root, final String node_id) {

        final Enumeration e = root.depthFirstEnumeration();

        while (e.hasMoreElements()) {

            final MegaMutableTreeNode node = (MegaMutableTreeNode) e.nextElement();

            final HashMap<String, Object> mega_node = (HashMap<String, Object>) node.getUserObject();

            if (mega_node.get("h").equals(node_id)) {
                return node;
            }
        }

        return null;
    }

//    public static DefaultMutableTreeNode sortTree(final DefaultMutableTreeNode root) {
//
//        final Enumeration e = root.depthFirstEnumeration();
//
//        while (e.hasMoreElements()) {
//
//            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
//
//            if (!node.isLeaf()) {
//
//                _sortTreeNode(node);
//
//            }
//        }
//
//        return root;
//
//    }

//    private static void _sortTreeNode(final DefaultMutableTreeNode parent) {
//
//        final int n = parent.getChildCount();
//
//        final List<DefaultMutableTreeNode> children = new ArrayList<>(n);
//
//        for (int i = 0; i < n; i++) {
//
//            children.add((DefaultMutableTreeNode) parent.getChildAt(i));
//        }
//
//        Collections.sort(children, TREE_NODE_COMPARATOR);
//
//        parent.removeAllChildren();
//
//        children.forEach((node) -> {
//            parent.add(node);
//        });
//    }

//    public static boolean deleteSelectedTreeItems(final JTree tree) {
//
//        final TreePath[] paths = tree.getSelectionPaths();
//
//        if (paths != null) {
//
//            final DefaultTreeModel tree_model = (DefaultTreeModel) tree.getModel();
//
//            MutableTreeNode node;
//
//            for (final TreePath path : paths) {
//
//                node = (MutableTreeNode) path.getLastPathComponent();
//
//                if (node != null) {
//
//                    if (node != tree_model.getRoot()) {
//
//                        MutableTreeNode parent = (MutableTreeNode) node.getParent();
//
//                        tree_model.removeNodeFromParent(node);
//
//                        while (parent != null && parent.isLeaf()) {
//
//                            if (parent != tree_model.getRoot()) {
//
//                                final MutableTreeNode parent_aux = (MutableTreeNode) parent.getParent();
//
//                                tree_model.removeNodeFromParent(parent);
//
//                                parent = parent_aux;
//
//                            } else {
//
//                                parent = null;
//                            }
//                        }
//
//                    } else {
//
//                        final MutableTreeNode new_root;
//
//                        try {
//
//                            new_root = (MutableTreeNode) tree_model.getRoot().getClass().newInstance();
//
//                            tree.setModel(new DefaultTreeModel(new_root));
//
//                            tree.setRootVisible(new_root.getChildCount() > 0);
//
//                            tree.setEnabled(true);
//
//                        } catch (final InstantiationException | IllegalAccessException ex) {
//                            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
//                        }
//
//                        return true;
//                    }
//                }
//            }
//
//            tree.setRootVisible(((TreeNode) tree_model.getRoot()).getChildCount() > 0);
//            tree.setEnabled(true);
//
//            return true;
//        }
//
//        return false;
//    }

    public static boolean isDirEmpty(final Path path) {
        if (Files.isDirectory(path)) {
            try (final DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            } catch (final IOException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return false;
    }

//    public static boolean deleteAllExceptSelectedTreeItems(final JTree tree) {
//
//        final TreePath[] paths = tree.getSelectionPaths();
//
//        final HashMap<MutableTreeNode, MutableTreeNode> hashmap_old = new HashMap<>();
//
//        final DefaultTreeModel tree_model = (DefaultTreeModel) tree.getModel();
//
//        if (paths != null) {
//
//            final Class node_class = tree_model.getRoot().getClass();
//
//            Object new_root = null;
//
//            try {
//
//                new_root = node_class.getDeclaredConstructor().newInstance();
//
//                ((DefaultMutableTreeNode) new_root).setUserObject(((DefaultMutableTreeNode) tree_model.getRoot()).getUserObject());
//
//            } catch (final InstantiationException | IllegalAccessException ex) {
//                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
//            } catch (final NoSuchMethodException | SecurityException | IllegalArgumentException |
//                           InvocationTargetException ex) {
//                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//            for (final TreePath path : paths) {
//
//                if ((MutableTreeNode) path.getLastPathComponent() != (MutableTreeNode) tree_model.getRoot()) {
//                    Object parent = new_root;
//
//                    for (final Object path_element : path.getPath()) {
//
//                        if ((DefaultMutableTreeNode) path_element != (DefaultMutableTreeNode) tree_model.getRoot()) {
//
//                            if (hashmap_old.get(path_element) == null) {
//
//                                Object node = null;
//
//                                if ((DefaultMutableTreeNode) path_element == (DefaultMutableTreeNode) path.getLastPathComponent()) {
//
//                                    node = path_element;
//
//                                } else {
//
//                                    try {
//
//                                        node = node_class.newInstance();
//
//                                        ((DefaultMutableTreeNode) node).setUserObject(((DefaultMutableTreeNode) path_element).getUserObject());
//
//                                    } catch (final InstantiationException | IllegalAccessException ex) {
//                                        Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
//                                    }
//                                }
//
//                                if (parent != null) {
//
//                                    ((DefaultMutableTreeNode) parent).add((DefaultMutableTreeNode) node);
//
//                                    if (!((TreeNode) path_element).isLeaf()) {
//
//                                        hashmap_old.put((DefaultMutableTreeNode) path_element, (DefaultMutableTreeNode) node);
//
//                                        parent = node;
//                                    }
//                                }
//
//                            } else {
//
//                                parent = hashmap_old.get(path_element);
//                            }
//                        }
//                    }
//
//                } else {
//
//                    return false;
//                }
//            }
//
//            tree.setModel(new DefaultTreeModel(sortTree((DefaultMutableTreeNode) new_root)));
//
//            tree.setRootVisible(new_root != null ? ((TreeNode) new_root).getChildCount() > 0 : false);
//
//            tree.setEnabled(true);
//
//            return true;
//        }
//
//        return false;
//    }

    public static String truncateText(final String text, int max_length) {

        final String separator = " ... ";

        max_length -= separator.length();

        if (max_length % 2 != 0) {

            max_length--;
        }

        return (text.length() > max_length) ? text.replaceAll("^(.{1," + (max_length / 2) + "}).*?(.{1," + (max_length / 2) + "})$", "$1" + separator + "$2") : text;
    }

    public static String cleanFilename(final String filename) {

        return (System.getProperty("os.name").toLowerCase().contains("win") ? filename.replaceAll("[<>:\"/\\\\\\|\\?\\*\t]+", "") : filename).replaceAll("\\" + File.separator, "").replaceAll("\\.\\.+", "__").replaceAll("[\\x00-\\x1F]", "").trim();
    }

    public static String cleanFilePath(final String path) {

        return !path.equals(".") ? ((System.getProperty("os.name").toLowerCase().contains("win") ? path.replaceAll("[<>:\"\\|\\?\\*\t]+", "") : path).replaceAll(" +\\" + File.separator, "\\" + File.separator).replaceAll("\\.\\.+", "__").replaceAll("[\\x00-\\x1F]", "").trim()) : path;
    }

    public static byte[] genRandomByteArray(final int length) {

        final byte[] the_array = new byte[length];

        final Random randomno = new Random();

        randomno.nextBytes(the_array);

        return the_array;
    }

//    public static String extractStringFromClipboardContents(final Transferable contents) {
//
//        String ret = null;
//
//        if (contents != null) {
//
//            try {
//
//                final Object o = contents.getTransferData(DataFlavor.stringFlavor);
//
//                if (o instanceof String) {
//
//                    ret = (String) o;
//                }
//
//            } catch (final Exception ex) {
//            }
//        }
//
//        return ret;
//
//    }

    public static String extractMegaLinksFromString(String data) {

        String res = "";

        if (data != null) {

            if (data.startsWith("moz-extension") || data.startsWith("chrome-extension")) {
                data = extensionURL2NormalLink(data);
            }

            final ArrayList<String> links = new ArrayList<>();
            String url_decoded;
            try {
                url_decoded = URLDecoder.decode(data, "UTF-8");
            } catch (final Exception ex) {
                url_decoded = data;
            }
            final ArrayList<String> base64_chunks = findAllRegex("[A-Za-z0-9+/_-]+=*", url_decoded, 0);
            if (!base64_chunks.isEmpty()) {

                for (final String chunk : base64_chunks) {

                    try {

                        final String clean_data = MiscTools.newMegaLinks2Legacy(new String(Base64.getDecoder().decode(chunk)));

                        final String decoded = MiscTools.findFirstRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", clean_data, 0);

                        if (decoded != null) {
                            links.add(decoded);
                        }

                    } catch (final Exception e) {
                    }
                    ;
                }
            }
            try {
                url_decoded = URLDecoder.decode(data, "UTF-8");
            } catch (final Exception ex) {
                url_decoded = data;
            }
            final String clean_data = MiscTools.newMegaLinks2Legacy(url_decoded);
            links.addAll(findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", clean_data, 0));
            links.addAll(findAllRegex("mega://e(n|l)c[^\r\n]+", clean_data, 0));
            res = links.stream().map((s) -> s + "\n").reduce(res, String::concat);
        }

        return res.trim();
    }

    public static String extractFirstMegaLinkFromString(final String data) {

        String res = "";

        if (data != null) {

            try {
                final String clean_data = MiscTools.newMegaLinks2Legacy(URLDecoder.decode(data, "UTF-8"));

                final ArrayList<String> links = findAllRegex("(?:https?|mega)://[^\r\n]+(#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", clean_data, 0);

                links.addAll(findAllRegex("mega://e(n|l)c[^\r\n]+", clean_data, 0));

                if (links.size() > 0) {

                    res = links.get(0);
                }
            } catch (final UnsupportedEncodingException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        }

        return res;
    }

    public static boolean checkMegaDownloadUrl(final String string_url) {

        if (string_url == null || "".equals(string_url)) {
            return false;
        }

        HttpURLConnection con = null;

        int http_status = 0, http_error = 0;

        String current_smart_proxy = null;

        boolean smart_proxy_socks = false;

        final ArrayList<String> excluded_proxy_list = new ArrayList<>();

        do {

            final SmartMegaProxyManager proxy_manager = MainPanel.getProxy_manager();

            if (MainPanel.isUse_smart_proxy() && proxy_manager != null && proxy_manager.isForce_smart_proxy()) {

                final String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                current_smart_proxy = smart_proxy[0];

                smart_proxy_socks = smart_proxy[1].equals("socks");

            }

            try {

                final URL url = new URL(string_url + "/0-0");

                if ((current_smart_proxy != null || http_error == 509) && MainPanel.isUse_smart_proxy() && proxy_manager != null && !MainPanel.isUse_proxy()) {

                    if (current_smart_proxy != null && http_error != 0) {

                        proxy_manager.blockProxy(current_smart_proxy, "HTTP " + String.valueOf(http_error));

                        final String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                        current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");

                    } else if (current_smart_proxy == null) {

                        final String[] smart_proxy = proxy_manager.getProxy(excluded_proxy_list);

                        current_smart_proxy = smart_proxy[0];

                        smart_proxy_socks = smart_proxy[1].equals("socks");
                    }

                    if (current_smart_proxy != null) {

                        final String[] proxy_info = current_smart_proxy.split(":");

                        final Proxy proxy = new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxy_info[0], Integer.parseInt(proxy_info[1])));

                        con = (HttpURLConnection) url.openConnection(proxy);

                    } else {

                        if (MainPanel.isUse_proxy()) {

                            con = (HttpURLConnection) url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                            if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                                con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                            }
                        } else {

                            con = (HttpURLConnection) url.openConnection();
                        }
                    }

                } else {

                    if (MainPanel.isUse_proxy()) {

                        con = (HttpURLConnection) url.openConnection(new Proxy(smart_proxy_socks ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                        if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                            con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                        }
                    } else {

                        con = (HttpURLConnection) url.openConnection();
                    }
                }

                if (current_smart_proxy != null && proxy_manager != null) {
                    con.setConnectTimeout(proxy_manager.getProxy_timeout());
                    con.setReadTimeout(proxy_manager.getProxy_timeout() * 2);
                }

                con.setUseCaches(false);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                http_status = con.getResponseCode();

                if (http_status != 200) {
                    http_error = http_status;
                } else {
                    http_error = 0;
                }

            } catch (final Exception ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
            } finally {

                if (con != null) {
                    con.disconnect();
                }

            }

        } while (http_error == 509);

        return http_status != 403;
    }

    public static String getMyPublicIP() {

        String public_ip = null;
        HttpURLConnection con = null;

        try {

            final URL url_api = new URL("http://whatismyip.akamai.com/");

            if (MainPanel.isUse_proxy()) {

                con = (HttpURLConnection) url_api.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                }
            } else {

                con = (HttpURLConnection) url_api.openConnection();
            }

            con.setUseCaches(false);

            try (final InputStream is = con.getInputStream(); final ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                final byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                int reads;

                while ((reads = is.read(buffer)) != -1) {

                    byte_res.write(buffer, 0, reads);
                }

                public_ip = new String(byte_res.toByteArray(), "UTF-8");
            }

        } catch (final MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } catch (final IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return public_ip;
    }

    public static String checkNewVersion(final String url) {

        String new_version_major = null, new_version_minor = null, current_version_major = null, current_version_minor = null;

        final URL mb_url;

        HttpURLConnection con = null;

        try {

            mb_url = new URL(url);

            if (MainPanel.isUse_proxy()) {

                con = (HttpURLConnection) mb_url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                }
            } else {

                con = (HttpURLConnection) mb_url.openConnection();
            }

            con.setUseCaches(false);

            try (final InputStream is = con.getInputStream(); final ByteArrayOutputStream byte_res = new ByteArrayOutputStream()) {

                final byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                int reads;

                while ((reads = is.read(buffer)) != -1) {

                    byte_res.write(buffer, 0, reads);
                }

                final String latest_version_res = new String(byte_res.toByteArray(), "UTF-8");

                final String latest_version = findFirstRegex("releases\\/tag\\/v?([0-9]+\\.[0-9]+)", latest_version_res, 1);

                new_version_major = findFirstRegex("([0-9]+)\\.[0-9]+", latest_version, 1);

                new_version_minor = findFirstRegex("[0-9]+\\.([0-9]+)", latest_version, 1);

                current_version_major = findFirstRegex("([0-9]+)\\.[0-9]+$", VERSION, 1);

                current_version_minor = findFirstRegex("[0-9]+\\.([0-9]+)$", VERSION, 1);

                if (new_version_major != null && (Integer.parseInt(current_version_major) < Integer.parseInt(new_version_major) || (Integer.parseInt(current_version_major) == Integer.parseInt(new_version_major) && Integer.parseInt(current_version_minor) < Integer.parseInt(new_version_minor)))) {

                    return new_version_major + "." + new_version_minor;

                }
            }

        } catch (final MalformedURLException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } catch (final IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return null;
    }

    public static void openBrowserURL(final String url) {
//        THREAD_POOL.execute(() -> {
//            try {
//                Logger.getLogger(MiscTools.class.getName()).log(Level.INFO, "Trying to open URL in external browser: {0}", url);
//
//                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
//                    Desktop.getDesktop().browse(new URI(url));
//                    return;
//                }
//                if (System.getProperty("os.name").toLowerCase().contains("nux")) {
//                    final Process p = Runtime.getRuntime().exec(new String[]{"xdg-open", url});
//                    p.waitFor();
//                    p.destroy();
//                    return;
//                }
//                Logger.getLogger(MiscTools.class.getName()).log(Level.WARNING, "Unable to open URL: Unsupported platform.", url);
//            } catch (final Exception ex) {
//                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
//            }
//        });
    }

    public static byte[] recReverseArray(final byte[] arr, final int start, final int end) {

        final byte temp;

        if (start < end) {
            temp = arr[start];

            arr[start] = arr[end];

            arr[end] = temp;

            return recReverseArray(arr, start + 1, end - 1);

        } else {
            return arr;
        }
    }

    public static String getCurrentJarParentPath() {
        try {
            final CodeSource codeSource = MainPanel.class.getProtectionDomain().getCodeSource();

            final File jarFile = new File(codeSource.getLocation().toURI().getPath());

            return jarFile.getParentFile().getAbsolutePath();

        } catch (final URISyntaxException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    public static void restartApplication() {

        final StringBuilder cmd = new StringBuilder();

        cmd.append(System.getProperty("java.home")).append(File.separator).append("bin").append(File.separator).append("java ");

        ManagementFactory.getRuntimeMXBean().getInputArguments().forEach((jvmArg) -> {
            cmd.append(jvmArg).append(" ");
        });

        cmd.append("-cp ").append(ManagementFactory.getRuntimeMXBean().getClassPath()).append(" ");

        cmd.append(MainPanel.class.getName()).append(" native 1");

        try {
            Runtime.getRuntime().exec(cmd.toString());
        } catch (final IOException ex) {
            Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        System.exit(2);
    }

    /*
        Thanks -> https://stackoverflow.com/a/26884326
     */
    public static int naturalCompare(String a, String b, final boolean ignoreCase) {

        if (a == null) {
            a = "";
        }

        if (b == null) {
            b = "";
        }

        if (ignoreCase) {
            a = a.toLowerCase();
            b = b.toLowerCase();
        }
        final int aLength = a.length();
        final int bLength = b.length();
        final int minSize = Math.min(aLength, bLength);
        char aChar, bChar;
        boolean aNumber, bNumber;
        boolean asNumeric = false;
        int lastNumericCompare = 0;
        for (int i = 0; i < minSize; i++) {
            aChar = a.charAt(i);
            bChar = b.charAt(i);
            aNumber = aChar >= '0' && aChar <= '9';
            bNumber = bChar >= '0' && bChar <= '9';
            if (asNumeric) {
                if (aNumber && bNumber) {
                    if (lastNumericCompare == 0) {
                        lastNumericCompare = aChar - bChar;
                    }
                } else if (aNumber) {
                    return 1;
                } else if (bNumber) {
                    return -1;
                } else if (lastNumericCompare == 0) {
                    if (aChar != bChar) {
                        return aChar - bChar;
                    }
                    asNumeric = false;
                } else {
                    return lastNumericCompare;
                }
            } else if (aNumber && bNumber) {
                asNumeric = true;
                if (lastNumericCompare == 0) {
                    lastNumericCompare = aChar - bChar;
                }
            } else if (aChar != bChar) {
                return aChar - bChar;
            }
        }
        if (asNumeric) {
            if (aLength > bLength && a.charAt(bLength) >= '0' && a.charAt(bLength) <= '9') // as number
            {
                return 1;  // a has bigger size, thus b is smaller
            } else if (bLength > aLength && b.charAt(aLength) >= '0' && b.charAt(aLength) <= '9') // as number
            {
                return -1;  // b has bigger size, thus a is smaller
            } else if (lastNumericCompare == 0) {
                return aLength - bLength;
            } else {
                return lastNumericCompare;
            }
        } else {
            return aLength - bLength;
        }
    }

    public static MegaAPI checkMegaAccountLoginAndShowMasterPassDialog(final MainPanel main_panel, final String email) throws Exception {

        final boolean remember_master_pass = true;

        final HashMap<String, Object> account_info = (HashMap) main_panel.getMega_accounts().get(email);

        MegaAPI ma = main_panel.getMega_active_accounts().get(email);

        if (ma == null) {

            ma = new MegaAPI();

            final String password_aes;
            final String user_hash;
            synchronized (PASS_LOCK) {

                if (main_panel.getMaster_pass_hash() != null) {

//                    if (main_panel.getMaster_pass() == null) {
//
//                        GetMasterPasswordDialog pdialog = new GetMasterPasswordDialog((Frame) container.getParent(), true, main_panel.getMaster_pass_hash(), main_panel.getMaster_pass_salt(), main_panel);
//
//                        pdialog.setLocationRelativeTo(container);
//
//                        pdialog.setVisible(true);
//
//                        if (pdialog.isPass_ok()) {
//
//                            main_panel.setMaster_pass(pdialog.getPass());
//
//                            pdialog.deletePass();
//
//                            remember_master_pass = pdialog.getRemember_checkbox().isSelected();
//
//                            pdialog.dispose();
//
//                            password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("password_aes")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
//
//                            user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("user_hash")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
//
//                        } else {
//
//                            pdialog.dispose();
//
//                            return null;
//                        }
//
//                    } else {

                    password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("password_aes")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                    user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) account_info.get("user_hash")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

//                    }

                } else {

                    password_aes = (String) account_info.get("password_aes");

                    user_hash = (String) account_info.get("user_hash");
                }

                try {

                    final HashMap<String, Object> old_session_data = DBTools.selectMegaSession(email);

                    boolean unserialization_error = false;

                    if (old_session_data != null) {

                        Logger.getLogger(MiscTools.class.getName()).log(Level.INFO, "Reutilizando sesión de MEGA guardada para {0}", email);

                        MegaAPI old_ma = new MegaAPI();

                        if ((boolean) old_session_data.get("crypt")) {

                            final ByteArrayInputStream bs = new ByteArrayInputStream(CryptTools.aes_cbc_decrypt_pkcs7((byte[]) old_session_data.get("ma"), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            try (final ObjectInputStream is = new ObjectInputStream(bs)) {

                                old_ma = (MegaAPI) is.readObject();

                            } catch (final Exception ex) {
                                unserialization_error = true;
                            }

                        } else {

                            final ByteArrayInputStream bs = new ByteArrayInputStream((byte[]) old_session_data.get("ma"));

                            try (final ObjectInputStream is = new ObjectInputStream(bs)) {
                                old_ma = (MegaAPI) is.readObject();
                            } catch (final Exception ex) {
                                unserialization_error = true;
                            }

                        }

                        if (old_ma.getQuota() == null) {

                            unserialization_error = true;

                        } else {

                            ma = old_ma;
                        }
                    }

                    if (old_session_data == null || unserialization_error) {

                        final String pincode = null;

//                        if (ma.check2FA(email)) {
//
//                            Get2FACode dialog = new Get2FACode((Frame) container.getParent(), true, email, main_panel);
//
//                            dialog.setLocationRelativeTo(container);
//
//                            dialog.setVisible(true);
//
//                            if (dialog.isCode_ok()) {
//                                pincode = dialog.getPin_code();
//                            } else {
//                                throw new MegaAPIException(-26);
//                            }
//                        }

                        ma.fastLogin(email, bin2i32a(BASE642Bin(password_aes)), user_hash, pincode);

                        final ByteArrayOutputStream bs = new ByteArrayOutputStream();

                        try (final ObjectOutputStream os = new ObjectOutputStream(bs)) {
                            os.writeObject(ma);
                        }

                        if (main_panel.getMaster_pass() != null) {

                            DBTools.insertMegaSession(email, CryptTools.aes_cbc_encrypt_pkcs7(bs.toByteArray(), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), true);

                        } else {

                            DBTools.insertMegaSession(email, bs.toByteArray(), false);
                        }
                    }

                    main_panel.getMega_active_accounts().put(email, ma);

                } catch (final MegaAPIException exception) {

//                    if (exception.getCode() == -6) {
//                        JOptionPane.showMessageDialog(container.getParent(), LabelTranslatorSingleton.getInstance().translate("You've tried to login too many times. Wait an hour."), "Error", JOptionPane.ERROR_MESSAGE);
//                    }

                    throw exception;
                }

            }
        }

        if (!remember_master_pass) {

            main_panel.setMaster_pass(null);
        }

        return ma;

    }

    public static String newMegaLinks2Legacy(String data) {

        data = MiscTools.addBackSlashToLinks(MiscTools.addHTTPSToMegaLinks(data));

        data = data.replaceAll("(?:https://)?mega(?:\\.co)?\\.nz/folder/([^#]+)#([^\r\n/]+)/file/([^\r\n/]+)", "https://mega.nz/#F*$3!$1!$2");

        data = data.replaceAll("(?:https://)?mega(?:\\.co)?\\.nz/folder/([^#]+)#([^\r\n/]+)/folder/([^\r\n/]+)", "https://mega.nz/#F!$1@$3!$2");

        return data.replaceAll("(?:https://)?mega(?:\\.co)?\\.nz/folder/([^#]+)#([^\r\n]+)", "https://mega.nz/#F!$1!$2").replaceAll("(?:https://)?mega(?:\\.co)?\\.nz/file/([^#]+)#([^\r\n]+)", "https://mega.nz/#!$1!$2");
    }

    public static String addHTTPSToMegaLinks(final String data) {

        return data.replaceAll("(?<!https?://)mega(?:\\.co)?\\.nz", "https://mega.nz");
    }

    public static String addBackSlashToLinks(final String data) {

        return data.replaceAll("https?://", "\n$0");
    }

    /* This method changes the MEGA extension URL to a ordinary MEGA URL,
    so copying the extension URL from Firefox or Chrome also works as a normal URL */
    public static String extensionURL2NormalLink(final String data) {

        final String toReplace = data.substring(0, data.indexOf('#') + 1);

        return data.replace(toReplace, "https://mega.nz");
    }

    private MiscTools() {
    }

}
