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

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static com.tonikelope.megabasterd.MiscTools.BASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.Bin2UrlBASE64;
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.bin2i32a;
import static com.tonikelope.megabasterd.MiscTools.findAllRegex;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static com.tonikelope.megabasterd.MiscTools.hex2bin;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static com.tonikelope.megabasterd.MiscTools.long2bytearray;
import static com.tonikelope.megabasterd.MiscTools.recReverseArray;

/**
 * @author tonikelope
 */
public class CryptTools {

    public static final int[] AES_ZERO_IV_I32A = {0, 0, 0, 0};

    public static final byte[] AES_ZERO_IV = i32a2bin(AES_ZERO_IV_I32A);

    public static final int MASTER_PASSWORD_PBKDF2_SALT_BYTE_LENGTH = 16;

    public static final int MASTER_PASSWORD_PBKDF2_OUTPUT_BIT_LENGTH = 256;

    public static final int MASTER_PASSWORD_PBKDF2_ITERATIONS = 65536;
    private static final Logger LOG = Logger.getLogger(CryptTools.class.getName());

    public static Cipher genDecrypter(final String algo, final String mode, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        final SecretKeySpec skeySpec = new SecretKeySpec(key, algo);

        final Cipher decryptor = Cipher.getInstance(mode);

        if (iv != null) {

            final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            decryptor.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);

        } else {

            decryptor.init(Cipher.DECRYPT_MODE, skeySpec);
        }

        return decryptor;
    }

    public static Cipher genCrypter(final String algo, final String mode, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        final SecretKeySpec skeySpec = new SecretKeySpec(key, algo);

        final Cipher cryptor = Cipher.getInstance(mode);

        if (iv != null) {

            final IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            cryptor.init(Cipher.ENCRYPT_MODE, skeySpec, ivParameterSpec);

        } else {

            cryptor.init(Cipher.ENCRYPT_MODE, skeySpec);
        }

        return cryptor;
    }

    public static byte[] aes_cbc_encrypt_nopadding(final byte[] data, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher cryptor = CryptTools.genCrypter("AES", "AES/CBC/NoPadding", key, iv);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_cbc_encrypt_pkcs7(final byte[] data, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher cryptor = CryptTools.genCrypter("AES", "AES/CBC/PKCS5Padding", key, iv);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_cbc_decrypt_nopadding(final byte[] data, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CBC/NoPadding", key, iv);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_cbc_decrypt_pkcs7(final byte[] data, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CBC/PKCS5Padding", key, iv);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_ecb_encrypt_nopadding(final byte[] data, final byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher cryptor = CryptTools.genCrypter("AES", "AES/ECB/NoPadding", key, null);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_ecb_decrypt_nopadding(final byte[] data, final byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher decryptor = CryptTools.genDecrypter("AES", "AES/ECB/NoPadding", key, null);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_ecb_encrypt_pkcs7(final byte[] data, final byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher cryptor = CryptTools.genCrypter("AES", "AES/ECB/PKCS5Padding", key, null);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_ecb_decrypt_pkcs7(final byte[] data, final byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher decryptor = CryptTools.genDecrypter("AES", "AES/ECB/PKCS5Padding", key, null);

        return decryptor.doFinal(data);
    }

    public static byte[] aes_ctr_encrypt_nopadding(final byte[] data, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher cryptor = CryptTools.genCrypter("AES", "AES/CTR/NoPadding", key, iv);

        return cryptor.doFinal(data);
    }

    public static byte[] aes_ctr_decrypt_nopadding(final byte[] data, final byte[] key, final byte[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        final Cipher decryptor = CryptTools.genDecrypter("AES", "AES/CTR/NoPadding", key, iv);

        return decryptor.doFinal(data);
    }

    public static int[] aes_cbc_encrypt_ia32(final int[] data, final int[] key, final int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        return bin2i32a(CryptTools.aes_cbc_encrypt_nopadding(i32a2bin(data), i32a2bin(key), i32a2bin(iv)));
    }

    public static int[] aes_cbc_decrypt_ia32(final int[] data, final int[] key, final int[] iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        return bin2i32a(CryptTools.aes_cbc_decrypt_nopadding(i32a2bin(data), i32a2bin(key), i32a2bin(iv)));
    }

    public static byte[] rsaDecrypt(final BigInteger enc_data, final BigInteger p, final BigInteger q, final BigInteger d) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        final RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(p.multiply(q), d);

        final KeyFactory factory = KeyFactory.getInstance("RSA");

        final Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");

        final RSAPrivateKey privKey = (RSAPrivateKey) factory.generatePrivate(privateSpec);

        cipher.init(Cipher.DECRYPT_MODE, privKey);

        byte[] enc_data_byte = enc_data.toByteArray();

        if (enc_data_byte[0] == 0) {

            enc_data_byte = Arrays.copyOfRange(enc_data_byte, 1, enc_data_byte.length);
        }

        byte[] plainText = cipher.doFinal(enc_data_byte);

        if (plainText[0] == 0) {

            plainText = Arrays.copyOfRange(plainText, 1, plainText.length);
        }

        return plainText;
    }

    public static byte[] initMEGALinkKey(final String key_string) {
        final int[] int_key = bin2i32a(UrlBASE642Bin(key_string));
        final int[] k = new int[4];

        k[0] = int_key[0] ^ int_key[4];
        k[1] = int_key[1] ^ int_key[5];
        k[2] = int_key[2] ^ int_key[6];
        k[3] = int_key[3] ^ int_key[7];

        return i32a2bin(k);
    }

    public static byte[] initMEGALinkKeyIV(final String key_string) throws Exception {
        final int[] int_key = bin2i32a(UrlBASE642Bin(key_string));
        final int[] iv = new int[4];

        iv[0] = int_key[4];
        iv[1] = int_key[5];
        iv[2] = 0;
        iv[3] = 0;

        return i32a2bin(iv);
    }

    public static byte[] forwardMEGALinkKeyIV(final byte[] iv, final long forward_bytes) {
        final byte[] new_iv = new byte[iv.length];

        System.arraycopy(iv, 0, new_iv, 0, iv.length / 2);

        final byte[] ctr = long2bytearray(forward_bytes / iv.length);

        System.arraycopy(ctr, 0, new_iv, iv.length / 2, ctr.length);

        return new_iv;
    }

    public static String decryptMegaDownloaderLink(final String link) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, Exception, IllegalBlockSizeException, BadPaddingException {
        final String[] keys = {"6B316F36416C2D316B7A3F217A30357958585858585858585858585858585858", "ED1F4C200B35139806B260563B3D3876F011B4750F3A1A4A5EFD0BBE67554B44"};
        final String iv = "79F10A01844A0B27FF5B2D4E0ED3163E";

        final String enc_type;
        final String folder;
        final String dec_link;

        if ((enc_type = findFirstRegex("mega://f?(enc[0-9]*)\\?", link, 1)) != null) {
            final Cipher decrypter;

            String the_key = null;

            switch (enc_type.toLowerCase()) {
                case "enc":
                    the_key = keys[0];
                    break;
                case "enc2":
                    the_key = keys[1];
                    break;
            }

            folder = findFirstRegex("mega://(f)?enc[0-9]*\\?", link, 1);

            decrypter = CryptTools.genDecrypter("AES", "AES/CBC/NoPadding", hex2bin(the_key), hex2bin(iv));

            final byte[] decrypted_data = decrypter.doFinal(UrlBASE642Bin(findFirstRegex("mega://f?enc[0-9]*\\?([\\da-zA-Z_,-]*)", link, 1)));

            dec_link = new String(decrypted_data, "UTF-8").trim();

            return "https://mega.nz/#" + (folder != null ? "f" : "") + dec_link;

        } else {
            return link;
        }
    }

    public static HashSet<String> decryptELC(final String link, final MainPanel main_panel) {

        final String elc;

        final HashSet<String> links = new HashSet<>();

        if ((elc = findFirstRegex("mega://elc\\?([0-9a-zA-Z,_-]+)", link, 1)) != null) {

            HttpURLConnection con = null;

            try {

                byte[] elc_byte = UrlBASE642Bin(elc);

                final boolean compression;

                if (((int) elc_byte[0] & 0xFF) != 112 && ((int) elc_byte[0] & 0xFF) != 185) {

                    throw new Exception("BAD ELC!");

                } else {

                    compression = (((int) elc_byte[0] & 0xFF) == 112);
                }

                elc_byte = Arrays.copyOfRange(elc_byte, 1, elc_byte.length);

                if (compression) {

                    try (final InputStream is = new GZIPInputStream(new ByteArrayInputStream(elc_byte)); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                        final byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                        int reads;

                        while ((reads = is.read(buffer)) != -1) {

                            out.write(buffer, 0, reads);
                        }

                        elc_byte = out.toByteArray();

                    }
                }

                final int bin_links_length = ByteBuffer.wrap(recReverseArray(Arrays.copyOfRange(elc_byte, 0, 4), 0, 3)).getInt();

                final byte[] bin_links = Arrays.copyOfRange(elc_byte, 4, 4 + bin_links_length);

                final short url_bin_length = ByteBuffer.wrap(recReverseArray(Arrays.copyOfRange(elc_byte, 4 + bin_links_length, 4 + bin_links_length + 2), 0, 1)).getShort();

                final byte[] url_bin = Arrays.copyOfRange(elc_byte, 4 + bin_links_length + 2, 4 + bin_links_length + 2 + url_bin_length);

                if (!new String(url_bin, "UTF-8").contains("http")) {

                    throw new Exception("BAD ELC HOST URL!");
                }

                final short pass_bin_length = ByteBuffer.wrap(recReverseArray(Arrays.copyOfRange(elc_byte, 4 + bin_links_length + 2 + url_bin_length, 4 + bin_links_length + 2 + url_bin_length + 2), 0, 1)).getShort();

                final byte[] pass_bin = Arrays.copyOfRange(elc_byte, 4 + bin_links_length + 2 + url_bin_length + 2, 4 + bin_links_length + 2 + url_bin_length + 2 + pass_bin_length);

                final URL url = new URL(new String(url_bin, "UTF-8").trim());

                if (MainPanel.isUse_proxy()) {

                    con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                    if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                        con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                    }
                } else {

                    con = (HttpURLConnection) url.openConnection();
                }

                con.setRequestMethod("POST");

                con.setDoOutput(true);

                con.setUseCaches(false);

                con.setRequestProperty("User-Agent", MainPanel.DEFAULT_USER_AGENT);

                final HashMap<String, String> elc_account_data;

                final String user;
                final String api_key;

                final boolean remember_master_pass;

                if (main_panel.getElc_accounts().get(url.getHost()) != null) {

                    elc_account_data = (HashMap) main_panel.getElc_accounts().get(url.getHost());

                    if (main_panel.getMaster_pass_hash() != null) {

                        if (main_panel.getMaster_pass() == null) {

                            final GetMasterPasswordDialog dialog = new GetMasterPasswordDialog(main_panel.getView(), true, main_panel.getMaster_pass_hash(), main_panel.getMaster_pass_salt(), main_panel);

                            dialog.setLocationRelativeTo(main_panel.getView());

                            dialog.setVisible(true);

                            if (dialog.isPass_ok()) {

                                main_panel.setMaster_pass(dialog.getPass());

                                dialog.deletePass();

                                remember_master_pass = dialog.getRemember_checkbox().isSelected();

                                dialog.dispose();

                                user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(elc_account_data.get("user")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                api_key = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(elc_account_data.get("apikey")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                if (!remember_master_pass) {

                                    main_panel.setMaster_pass(null);
                                }

                            } else {

                                dialog.dispose();

                                throw new Exception("NO valid ELC account available!");
                            }

                        } else {

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(elc_account_data.get("user")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                            api_key = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(elc_account_data.get("apikey")), main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        }

                    } else {

                        user = elc_account_data.get("user");

                        api_key = elc_account_data.get("apikey");
                    }

                } else {

                    throw new Exception("NO valid ELC account available!");
                }

                final String postdata = "OPERATION_TYPE=D&DATA=" + new String(pass_bin, "UTF-8") + "&USER=" + user + "&APIKEY=" + api_key;

                con.getOutputStream().write(postdata.getBytes("UTF-8"));

                con.getOutputStream().close();

                try (final InputStream is = con.getInputStream(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                    final byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];

                    int reads;

                    while ((reads = is.read(buffer)) != -1) {

                        out.write(buffer, 0, reads);
                    }

                    final ObjectMapper objectMapper = new ObjectMapper();

                    final HashMap res_map = objectMapper.readValue(new String(out.toByteArray(), "UTF-8"), HashMap.class);

                    String dec_pass = (String) res_map.get("d");

                    if (dec_pass != null && dec_pass.length() > 0) {

                        dec_pass = (String) res_map.get("d");

                        final byte[] pass_dec_byte = BASE642Bin(dec_pass);

                        final byte[] key = Arrays.copyOfRange(pass_dec_byte, 0, 16);

                        final byte[] iv = new byte[16];

                        Arrays.fill(iv, (byte) 0);

                        System.arraycopy(pass_dec_byte, 16, iv, 0, 8);

                        final byte[] bin_links_dec = CryptTools.aes_cbc_decrypt_nopadding(bin_links, key, iv);

                        final String[] links_string = (new String(bin_links_dec, "UTF-8").trim()).split("\\|");

                        for (final String s : links_string) {

                            links.add("https://mega.nz/" + s);
                        }

                    } else {
                        throw new Exception(url.getAuthority() + " ELC SERVER ERROR " + new String(out.toByteArray(), "UTF-8"));
                    }

                }

            } catch (final Exception ex) {
                Logger.getLogger(CryptTools.class.getName()).log(Level.SEVERE, ex.getMessage());
//                JOptionPane.showMessageDialog(main_panel.getView(), ex.getMessage(), "ELC ERROR", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }

        }

        return links;
    }

    public static HashSet<String> decryptDLC(final String data, final MainPanel main_panel) {

        final HashSet<String> links = new HashSet<>();

        final String dlc_url = "http://service.jdownloader.org/dlcrypt/service.php";

        final String dlc_rev = "34065";

        final String dlc_master_key = "447E787351E60E2C6A96B3964BE0C9BD";

        final String dlc_id = data.substring(data.length() - 88);

        final String enc_dlc_data = data.substring(0, data.length() - 88).trim();

        HttpURLConnection con = null;

        try {

            final URL url = new URL(dlc_url);

            if (MainPanel.isUse_proxy()) {

                con = (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(MainPanel.getProxy_host(), MainPanel.getProxy_port())));

                if (MainPanel.getProxy_user() != null && !"".equals(MainPanel.getProxy_user())) {

                    con.setRequestProperty("Proxy-Authorization", "Basic " + MiscTools.Bin2BASE64((MainPanel.getProxy_user() + ":" + MainPanel.getProxy_pass()).getBytes("UTF-8")));
                }
            } else {

                con = (HttpURLConnection) url.openConnection();
            }

            con.setRequestMethod("POST");

            con.setDoOutput(true);

            con.setUseCaches(false);

            con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux amd64; rv:44.0) Gecko/20100101 Firefox/44.0");

            con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            con.setRequestProperty("Accept-Language", "de,en-gb;q=0.7, en;q=0.3");

            con.setRequestProperty("Accept-Encoding", "gzip, deflate");

            con.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

            con.setRequestProperty("Cache-Control", "no-cache");

            con.setRequestProperty("rev", dlc_rev);

            final String postdata = "destType=jdtc6&b=JD&srcType=dlc&data=" + dlc_id + "&v=" + dlc_rev;

            con.getOutputStream().write(postdata.getBytes("UTF-8"));

            con.getOutputStream().close();

            final String enc_dlc_key;

            try (final InputStream is = con.getInputStream(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                final byte[] buffer = new byte[MainPanel.DEFAULT_BYTE_BUFFER_SIZE];
                int reads;
                while ((reads = is.read(buffer)) != -1) {

                    out.write(buffer, 0, reads);
                }
                enc_dlc_key = findFirstRegex("< *rc *>(.+?)< */ *rc *>", new String(out.toByteArray(), "UTF-8"), 1);
            }

            final String dec_dlc_key = new String(CryptTools.aes_ecb_decrypt_nopadding(BASE642Bin(enc_dlc_key), hex2bin(dlc_master_key)), "UTF-8").trim();

            final String dec_dlc_data = new String(CryptTools.aes_cbc_decrypt_nopadding(BASE642Bin(enc_dlc_data), BASE642Bin(dec_dlc_key), BASE642Bin(dec_dlc_key)), "UTF-8").trim();

            final ArrayList<String> files = findAllRegex("< *file *>(.+?)< */ *file *>", new String(BASE642Bin(dec_dlc_data), "UTF-8"), 1);

            for (final String f : files) {

                final ArrayList<String> urls = findAllRegex("< *url *>(.+?)< */ *url *>", f, 1);

                for (final String s : urls) {

                    links.add(new String(BASE642Bin(s), "UTF-8"));
                }
            }

        } catch (final Exception ex) {

            Logger.getLogger(CryptTools.class.getName()).log(Level.SEVERE, ex.getMessage());

//            JOptionPane.showMessageDialog(main_panel.getView(), ex.getMessage(), "DLC ERROR", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return links;
    }

    public static String MEGAUserHash(final byte[] str, final int[] aeskey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, Exception {

        final int[] s32 = bin2i32a(str);

        int[] h32 = {0, 0, 0, 0};

        final int[] iv = {0, 0, 0, 0};

        for (int i = 0; i < s32.length; i++) {

            h32[i % 4] ^= s32[i];
        }

        for (int i = 0; i < 0x4000; i++) {

            h32 = CryptTools.aes_cbc_encrypt_ia32(h32, aeskey, iv);
        }

        final int[] res = {h32[0], h32[2]};

        return Bin2UrlBASE64(i32a2bin(res));
    }

    public static int[] MEGAPrepareMasterKey(final int[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        int[] pkey = {0x93C467E3, 0x7DB0C7A4, 0xD1BE3F81, 0x0152CB56};

        final int[] iv = {0, 0, 0, 0};

        for (int r = 0; r < 0x10000; r++) {

            for (int j = 0; j < key.length; j += 4) {

                final int[] k = {0, 0, 0, 0};

                for (int i = 0; i < 4; i++) {

                    if (i + j < key.length) {

                        k[i] = key[i + j];
                    }
                }

                pkey = CryptTools.aes_cbc_encrypt_ia32(pkey, k, iv);
            }
        }

        return pkey;
    }

    public static byte[] PBKDF2HMACSHA256(final String password, final byte[] salt, final int iterations, final int output_length) throws NoSuchAlgorithmException, InvalidKeySpecException {

        final SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        final KeySpec ks = new PBEKeySpec(password.toCharArray(), salt, iterations, output_length);

        return f.generateSecret(ks).getEncoded();
    }

    public static byte[] PBKDF2HMACSHA512(final String password, final byte[] salt, final int iterations, final int output_length) throws NoSuchAlgorithmException, InvalidKeySpecException {

        final SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

        final KeySpec ks = new PBEKeySpec(password.toCharArray(), salt, iterations, output_length);

        return f.generateSecret(ks).getEncoded();
    }

    private CryptTools() {
    }
}
