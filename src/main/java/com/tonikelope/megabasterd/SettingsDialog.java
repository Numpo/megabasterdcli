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
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tonikelope.megabasterd.DBTools.insertELCAccounts;
import static com.tonikelope.megabasterd.DBTools.insertMegaAccounts;
import static com.tonikelope.megabasterd.DBTools.insertSettingValue;
import static com.tonikelope.megabasterd.DBTools.insertSettingsValues;
import static com.tonikelope.megabasterd.DBTools.selectELCAccounts;
import static com.tonikelope.megabasterd.DBTools.selectMegaAccounts;
import static com.tonikelope.megabasterd.DBTools.selectSettingsValues;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.BASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.Bin2BASE64;
import static com.tonikelope.megabasterd.MiscTools.UrlBASE642Bin;
import static com.tonikelope.megabasterd.MiscTools.createUploadLogDir;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static com.tonikelope.megabasterd.SmartMegaProxyManager.PROXY_AUTO_REFRESH_TIME;
import static com.tonikelope.megabasterd.SmartMegaProxyManager.PROXY_BLOCK_TIME;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

/**
 * @author tonikelope
 */
public class SettingsDialog extends javax.swing.JDialog {

    public static final String DEFAULT_SMART_PROXY_URL = "https://raw.githubusercontent.com/tonikelope/megabasterd/proxy_list/proxy_list.txt";
    private String _download_path;
    private String _custom_chunks_dir;
    private boolean _settings_ok;
    private final Set<String> _deleted_mega_accounts;
    private final Set<String> _deleted_elc_accounts;
    private final MainPanel _main_panel;
    private boolean _remember_master_pass;
    private volatile boolean _exit = false;

    public boolean isSettings_ok() {
        return this._settings_ok;
    }

    public Set<String> getDeleted_mega_accounts() {
        return Collections.unmodifiableSet(this._deleted_mega_accounts);
    }

    public Set<String> getDeleted_elc_accounts() {
        return Collections.unmodifiableSet(this._deleted_elc_accounts);
    }

    public boolean isRemember_master_pass() {
        return this._remember_master_pass;
    }

    public SettingsDialog(final MainPanelView parent, final boolean modal) {

        super(parent, modal);

        this._main_panel = parent.getMain_panel();

        this._remember_master_pass = true;

        this._deleted_mega_accounts = new HashSet();

        this._deleted_elc_accounts = new HashSet();

        this._settings_ok = false;

        MiscTools.GUIRunAndWait(() -> {

            this.initComponents();

//            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());

//            updateTitledBorderFont(((javax.swing.border.TitledBorder) proxy_panel.getBorder()), GUI_FONT, _main_panel.getZoom_factor());
//
//            updateTitledBorderFont(((javax.swing.border.TitledBorder) proxy_auth_panel.getBorder()), GUI_FONT, _main_panel.getZoom_factor());
//
//            translateLabels(this);

            this.panel_tabs.setTitleAt(0, LabelTranslatorSingleton.getInstance().translate("Downloads"));

            this.panel_tabs.setTitleAt(1, LabelTranslatorSingleton.getInstance().translate("Uploads"));

            this.panel_tabs.setTitleAt(2, LabelTranslatorSingleton.getInstance().translate("Accounts"));

            this.panel_tabs.setTitleAt(3, LabelTranslatorSingleton.getInstance().translate("Advanced"));

            this.downloads_scrollpane.getVerticalScrollBar().setUnitIncrement(20);

            this.downloads_scrollpane.getHorizontalScrollBar().setUnitIncrement(20);

            this.uploads_scrollpane.getVerticalScrollBar().setUnitIncrement(20);

            this.uploads_scrollpane.getHorizontalScrollBar().setUnitIncrement(20);

            this.advanced_scrollpane.getVerticalScrollBar().setUnitIncrement(20);

            this.advanced_scrollpane.getHorizontalScrollBar().setUnitIncrement(20);

            final String zoom_factor = DBTools.selectSettingValue("font_zoom");

            int int_zoom_factor = Math.round(this._main_panel.getZoom_factor() * 100);

            if (zoom_factor != null) {
                int_zoom_factor = Integer.parseInt(zoom_factor);
            }

            this.zoom_spinner.setModel(new SpinnerNumberModel(int_zoom_factor, 50, 250, 10));
            ((JSpinner.DefaultEditor) this.zoom_spinner.getEditor()).getTextField().setEditable(false);

            final String use_custom_chunks_dir = DBTools.selectSettingValue("use_custom_chunks_dir");

            if (use_custom_chunks_dir != null) {

                if (use_custom_chunks_dir.equals("yes")) {

                    this._custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");

                    this.custom_chunks_dir_current_label.setText(this._custom_chunks_dir != null ? truncateText(this._custom_chunks_dir, 80) : "");

                    this.custom_chunks_dir_checkbox.setSelected(true);

                    this.custom_chunks_dir_button.setEnabled(true);

                } else {

                    this._custom_chunks_dir = DBTools.selectSettingValue("custom_chunks_dir");

                    this.custom_chunks_dir_current_label.setText(this._custom_chunks_dir != null ? truncateText(this._custom_chunks_dir, 80) : "");

                    this.custom_chunks_dir_checkbox.setSelected(false);

                    this.custom_chunks_dir_button.setEnabled(false);

                    this.custom_chunks_dir_current_label.setEnabled(false);
                }

            } else {

                this._custom_chunks_dir = null;

                this.custom_chunks_dir_current_label.setText("");

                this.custom_chunks_dir_checkbox.setSelected(false);

                this.custom_chunks_dir_button.setEnabled(false);

                this.custom_chunks_dir_current_label.setEnabled(false);
            }

            boolean monitor_clipboard = Download.DEFAULT_CLIPBOARD_LINK_MONITOR;

            final String monitor_clipboard_string = DBTools.selectSettingValue("clipboardspy");

            if (monitor_clipboard_string != null) {
                monitor_clipboard = monitor_clipboard_string.equals("yes");
            }

            boolean thumbnails = Upload.DEFAULT_THUMBNAILS;

            final String thumbnails_string = DBTools.selectSettingValue("thumbnails");

            if (thumbnails_string != null) {
                thumbnails = thumbnails_string.equals("yes");
            }

            this.thumbnail_checkbox.setSelected(thumbnails);

            boolean upload_log = Upload.UPLOAD_LOG;

            final String upload_log_string = DBTools.selectSettingValue("upload_log");

            if (upload_log_string != null) {
                upload_log = upload_log_string.equals("yes");
            }

            this.upload_log_checkbox.setSelected(upload_log);

            boolean upload_public_folder = Upload.UPLOAD_PUBLIC_FOLDER;

            final String upload_public_folder_string = DBTools.selectSettingValue("upload_public_folder");

            if (upload_public_folder_string != null) {
                upload_public_folder = upload_public_folder_string.equals("yes");
            }

            this.upload_public_folder_checkbox.setSelected(upload_public_folder);

            this.upload_public_folder_checkbox.setBackground(this.upload_public_folder_checkbox.isSelected() ? java.awt.Color.RED : null);

            this.public_folder_panel.setVisible(this.upload_public_folder_checkbox.isSelected());

            this.clipboardspy_checkbox.setSelected(monitor_clipboard);

            String default_download_dir = DBTools.selectSettingValue("default_down_dir");

            default_download_dir = Paths.get(default_download_dir == null ? MainPanel.MEGABASTERD_HOME_DIR : default_download_dir).toAbsolutePath().normalize().toString();

            this._download_path = default_download_dir;

            this.default_dir_label.setText(truncateText(this._download_path, 80));

            String slots = DBTools.selectSettingValue("default_slots_down");

            int default_slots = Download.WORKERS_DEFAULT;

            if (slots != null) {
                default_slots = Integer.parseInt(slots);
            }

            this.default_slots_down_spinner.setModel(new SpinnerNumberModel(default_slots, Download.MIN_WORKERS, Download.MAX_WORKERS, 1));

            ((JSpinner.DefaultEditor) this.default_slots_down_spinner.getEditor()).getTextField().setEditable(false);

            slots = DBTools.selectSettingValue("default_slots_up");

            default_slots = Upload.WORKERS_DEFAULT;

            if (slots != null) {
                default_slots = Integer.parseInt(slots);
            }

            this.default_slots_up_spinner.setModel(new SpinnerNumberModel(default_slots, Upload.MIN_WORKERS, Upload.MAX_WORKERS, 1));
            ((JSpinner.DefaultEditor) this.default_slots_up_spinner.getEditor()).getTextField().setEditable(false);

            final String max_down = DBTools.selectSettingValue("max_downloads");

            int max_dl = Download.SIM_TRANSFERENCES_DEFAULT;

            if (max_down != null) {
                max_dl = Integer.parseInt(max_down);
            }

            this.max_downloads_spinner.setModel(new SpinnerNumberModel(max_dl, 1, Download.MAX_SIM_TRANSFERENCES, 1));
            ((JSpinner.DefaultEditor) this.max_downloads_spinner.getEditor()).getTextField().setEditable(false);

            final String max_up = DBTools.selectSettingValue("max_uploads");

            int max_ul = Upload.SIM_TRANSFERENCES_DEFAULT;

            if (max_up != null) {
                max_ul = Integer.parseInt(max_up);
            }

            this.max_uploads_spinner.setModel(new SpinnerNumberModel(max_ul, 1, Upload.MAX_SIM_TRANSFERENCES, 1));
            ((JSpinner.DefaultEditor) this.max_uploads_spinner.getEditor()).getTextField().setEditable(false);

            boolean limit_dl_speed = Download.LIMIT_TRANSFERENCE_SPEED_DEFAULT;

            final String limit_download_speed = DBTools.selectSettingValue("limit_download_speed");

            if (limit_download_speed != null) {
                limit_dl_speed = limit_download_speed.equals("yes");
            }

            this.limit_download_speed_checkbox.setSelected(limit_dl_speed);

            this.max_down_speed_label.setEnabled(limit_dl_speed);

            this.max_down_speed_spinner.setEnabled(limit_dl_speed);

            final String max_dl_speed = DBTools.selectSettingValue("max_download_speed");

            int max_download_speed = Download.MAX_TRANSFERENCE_SPEED_DEFAULT;

            if (max_dl_speed != null) {
                max_download_speed = Integer.parseInt(max_dl_speed);
            }

            this.max_down_speed_spinner.setModel(new SpinnerNumberModel(max_download_speed, 1, Integer.MAX_VALUE, 5));

            ((JSpinner.DefaultEditor) this.max_down_speed_spinner.getEditor()).getTextField().setEditable(true);

            boolean limit_ul_speed = Upload.LIMIT_TRANSFERENCE_SPEED_DEFAULT;

            final String limit_upload_speed = DBTools.selectSettingValue("limit_upload_speed");

            if (limit_upload_speed != null) {
                limit_ul_speed = limit_upload_speed.equals("yes");
            }

            this.limit_upload_speed_checkbox.setSelected(limit_ul_speed);

            this.max_up_speed_label.setEnabled(limit_ul_speed);

            this.max_up_speed_spinner.setEnabled(limit_ul_speed);

            final String smartproxy_auto_refresh = DBTools.selectSettingValue("smartproxy_autorefresh_time");

            int smartproxy_auto_refresh_int = PROXY_AUTO_REFRESH_TIME;

            if (smartproxy_auto_refresh != null) {
                smartproxy_auto_refresh_int = Integer.parseInt(smartproxy_auto_refresh);
            }

            this.auto_refresh_proxy_time_spinner.setModel(new SpinnerNumberModel(smartproxy_auto_refresh_int, 1, Integer.MAX_VALUE, 1));

            ((JSpinner.DefaultEditor) this.auto_refresh_proxy_time_spinner.getEditor()).getTextField().setEditable(true);

            final String smartproxy_ban_time = DBTools.selectSettingValue("smartproxy_ban_time");

            int smartproxy_ban_time_int = PROXY_BLOCK_TIME;

            if (smartproxy_ban_time != null) {
                smartproxy_ban_time_int = Integer.parseInt(smartproxy_ban_time);
            }

            this.bad_proxy_time_spinner.setModel(new SpinnerNumberModel(smartproxy_ban_time_int, 0, Integer.MAX_VALUE, 1));

            ((JSpinner.DefaultEditor) this.bad_proxy_time_spinner.getEditor()).getTextField().setEditable(true);

            final String smartproxy_timeout = DBTools.selectSettingValue("smartproxy_timeout");

            int smartproxy_timeout_int = (int) ((float) Transference.HTTP_PROXY_TIMEOUT / 1000);

            if (smartproxy_timeout != null) {
                smartproxy_timeout_int = Integer.parseInt(smartproxy_timeout);
            }

            this.proxy_timeout_spinner.setModel(new SpinnerNumberModel(smartproxy_timeout_int, 1, Integer.MAX_VALUE, 1));

            ((JSpinner.DefaultEditor) this.proxy_timeout_spinner.getEditor()).getTextField().setEditable(true);

            boolean reset_slot_proxy = SmartMegaProxyManager.RESET_SLOT_PROXY;

            final String sreset_slot_proxy = DBTools.selectSettingValue("reset_slot_proxy");

            if (sreset_slot_proxy != null) {

                reset_slot_proxy = sreset_slot_proxy.equals("yes");
            }

            this.proxy_reset_slot_checkbox.setSelected(reset_slot_proxy);

            boolean random_select = SmartMegaProxyManager.RANDOM_SELECT;

            final String srandom_select = DBTools.selectSettingValue("random_proxy");

            if (srandom_select != null) {

                random_select = srandom_select.equals("yes");
            }

            if (random_select) {
                this.proxy_random_radio.setSelected(true);
            } else {
                this.proxy_sequential_radio.setSelected(true);
            }

            boolean dark_mode = false;

            final String dark_mode_select = DBTools.selectSettingValue("dark_mode");

            if (dark_mode_select != null) {

                dark_mode = dark_mode_select.equals("yes");
            }

            this.dark_mode_checkbox.setSelected(dark_mode);

            final String max_ul_speed = DBTools.selectSettingValue("max_upload_speed");

            int max_upload_speed = Upload.MAX_TRANSFERENCE_SPEED_DEFAULT;

            if (max_ul_speed != null) {
                max_upload_speed = Integer.parseInt(max_ul_speed);
            }

            this.max_up_speed_spinner.setModel(new SpinnerNumberModel(max_upload_speed, 1, Integer.MAX_VALUE, 5));

            ((JSpinner.DefaultEditor) this.max_up_speed_spinner.getEditor()).getTextField().setEditable(true);

            boolean cbc_mac = Download.VERIFY_CBC_MAC_DEFAULT;

            final String verify_file = DBTools.selectSettingValue("verify_down_file");

            if (verify_file != null) {
                cbc_mac = (verify_file.equals("yes"));
            }

            this.verify_file_down_checkbox.setSelected(cbc_mac);

            boolean use_slots = Download.USE_SLOTS_DEFAULT;

            final String use_slots_val = DBTools.selectSettingValue("use_slots_down");

            if (use_slots_val != null) {
                use_slots = use_slots_val.equals("yes");
            }

            this.multi_slot_down_checkbox.setSelected(use_slots);

            this.default_slots_down_label.setEnabled(use_slots);
            this.default_slots_down_spinner.setEnabled(use_slots);
            this.rec_download_slots_label.setEnabled(use_slots);

            this.default_slots_up_label.setEnabled(use_slots);
            this.default_slots_up_spinner.setEnabled(use_slots);
            this.rec_upload_slots_label.setEnabled(use_slots);

            boolean use_mega_account = Download.USE_MEGA_ACCOUNT_DOWN;

            final String use_mega_acc = DBTools.selectSettingValue("use_mega_account_down");

            String mega_account = null;

            if (use_mega_acc != null) {

                use_mega_account = use_mega_acc.equals("yes");

                mega_account = DBTools.selectSettingValue("mega_account_down");
            }

            this.use_mega_label.setEnabled(use_mega_account);
            this.use_mega_account_down_checkbox.setSelected(use_mega_account);
            this.use_mega_account_down_combobox.setEnabled(use_mega_account);
            this.use_mega_account_down_combobox.setSelectedItem(mega_account);

            DefaultTableModel mega_model = (DefaultTableModel) this.mega_accounts_table.getModel();

            DefaultTableModel elc_model = (DefaultTableModel) this.elc_accounts_table.getModel();

            this.encrypt_pass_checkbox.setSelected(this._main_panel.getMaster_pass_hash() != null);

            this.remove_mega_account_button.setEnabled(mega_model.getRowCount() > 0);

            this.remove_elc_account_button.setEnabled(elc_model.getRowCount() > 0);

            if (this._main_panel.getMaster_pass_hash() != null) {

                if (this._main_panel.getMaster_pass() == null) {

                    this.encrypt_pass_checkbox.setEnabled(false);

                    this.remove_mega_account_button.setEnabled(false);

                    this.remove_elc_account_button.setEnabled(false);

                    this.add_mega_account_button.setEnabled(false);

                    this.add_elc_account_button.setEnabled(false);

                    this.unlock_accounts_button.setVisible(true);

                    for (final Object k : this._main_panel.getMega_accounts().keySet()) {

                        final String[] new_row_data = {(String) k, "**************************"};

                        mega_model.addRow(new_row_data);
                    }

                    for (final Object k : this._main_panel.getElc_accounts().keySet()) {

                        final String[] new_row_data = {(String) k, "**************************", "**************************"};

                        elc_model.addRow(new_row_data);
                    }

                    this.mega_accounts_table.setEnabled(false);

                    this.elc_accounts_table.setEnabled(false);

                } else {

                    this.unlock_accounts_button.setVisible(false);

                    for (final Map.Entry pair : this._main_panel.getMega_accounts().entrySet()) {

                        final HashMap<String, Object> data = (HashMap) pair.getValue();

                        String pass = null;

                        try {

                            pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                                       InvalidAlgorithmParameterException | IllegalBlockSizeException |
                                       BadPaddingException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        } catch (final Exception ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }

                        final String[] new_row_data = {(String) pair.getKey(), pass};

                        mega_model.addRow(new_row_data);
                    }

                    for (final Map.Entry pair : this._main_panel.getElc_accounts().entrySet()) {

                        final HashMap<String, Object> data = (HashMap) pair.getValue();

                        String user = null, apikey = null;

                        try {

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                                       InvalidAlgorithmParameterException | IllegalBlockSizeException |
                                       BadPaddingException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        } catch (final Exception ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }

                        final String[] new_row_data = {(String) pair.getKey(), user, apikey};

                        elc_model.addRow(new_row_data);
                    }

                    mega_model = (DefaultTableModel) this.mega_accounts_table.getModel();

                    elc_model = (DefaultTableModel) this.elc_accounts_table.getModel();

                    this.remove_mega_account_button.setEnabled(mega_model.getRowCount() > 0);

                    this.remove_elc_account_button.setEnabled(elc_model.getRowCount() > 0);

                }

            } else {

                this.unlock_accounts_button.setVisible(false);

                for (final Map.Entry pair : this._main_panel.getMega_accounts().entrySet()) {

                    final HashMap<String, Object> data = (HashMap) pair.getValue();

                    final String[] new_row_data = {(String) pair.getKey(), (String) data.get("password")};

                    mega_model.addRow(new_row_data);
                }

                for (final Map.Entry pair : this._main_panel.getElc_accounts().entrySet()) {

                    final HashMap<String, Object> data = (HashMap) pair.getValue();

                    final String[] new_row_data = {(String) pair.getKey(), (String) data.get("user"), (String) data.get("apikey")};

                    elc_model.addRow(new_row_data);
                }

                this.remove_mega_account_button.setEnabled((mega_model.getRowCount() > 0));

                this.remove_elc_account_button.setEnabled((elc_model.getRowCount() > 0));

            }

            this.mega_accounts_table.setAutoCreateRowSorter(true);
            final DefaultRowSorter sorter_mega = ((DefaultRowSorter) this.mega_accounts_table.getRowSorter());
            final ArrayList list_mega = new ArrayList();
            list_mega.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter_mega.setSortKeys(list_mega);
            sorter_mega.sort();

            this.elc_accounts_table.setAutoCreateRowSorter(true);
            final DefaultRowSorter sorter_elc = ((DefaultRowSorter) this.elc_accounts_table.getRowSorter());
            final ArrayList list_elc = new ArrayList();
            list_elc.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter_elc.setSortKeys(list_elc);
            sorter_elc.sort();

            boolean use_mc_reverse = false;

            final String megacrypter_reverse = DBTools.selectSettingValue("megacrypter_reverse");

            String megacrypter_reverse_p = String.valueOf(MainPanel.DEFAULT_MEGA_PROXY_PORT);

            if (megacrypter_reverse != null) {

                use_mc_reverse = megacrypter_reverse.equals("yes");

                if (megacrypter_reverse_p != null) {

                    megacrypter_reverse_p = DBTools.selectSettingValue("megacrypter_reverse_port");
                }
            }

            this.megacrypter_reverse_checkbox.setSelected(use_mc_reverse);
            this.megacrypter_reverse_port_spinner.setModel(new SpinnerNumberModel(Integer.parseInt(megacrypter_reverse_p), 1024, 65535, 1));
            ((JSpinner.DefaultEditor) this.megacrypter_reverse_port_spinner.getEditor()).getTextField().setEditable(use_mc_reverse);
            this.megacrypter_reverse_port_spinner.setEnabled(use_mc_reverse);
            this.megacrypter_reverse_warning_label.setEnabled(use_mc_reverse);

            boolean use_smart_proxy = false;

            final String smart_proxy = DBTools.selectSettingValue("smart_proxy");

            if (smart_proxy != null) {

                use_smart_proxy = smart_proxy.equals("yes");
            }

            this.smart_proxy_checkbox.setSelected(use_smart_proxy);

//            MiscTools.containerSetEnabled(smart_proxy_settings, use_smart_proxy);

            boolean force_smart_proxy = MainPanel.FORCE_SMART_PROXY;

            final String force_smart_proxy_string = DBTools.selectSettingValue("force_smart_proxy");

            if (force_smart_proxy_string != null) {

                force_smart_proxy = force_smart_proxy_string.equals("yes");
            }

            this.force_smart_proxy_checkbox.setSelected(force_smart_proxy);

            boolean run_command = false;

            final String run_command_string = DBTools.selectSettingValue("run_command");

            if (run_command_string != null) {

                run_command = run_command_string.equals("yes");
            }

            this.run_command_checkbox.setSelected(run_command);

            this.run_command_textbox.setEnabled(run_command);

            this.run_command_textbox.setText(DBTools.selectSettingValue("run_command_path"));

            boolean init_paused = false;

            final String init_paused_string = DBTools.selectSettingValue("start_frozen");

            if (init_paused_string != null) {

                init_paused = init_paused_string.equals("yes");
            }

            this.start_frozen_checkbox.setSelected(init_paused);

            boolean use_proxy = false;

            final String use_proxy_val = DBTools.selectSettingValue("use_proxy");

            if (use_proxy_val != null) {
                use_proxy = (use_proxy_val.equals("yes"));
            }

            this.use_proxy_checkbox.setSelected(use_proxy);

            this.proxy_host_textfield.setText(DBTools.selectSettingValue("proxy_host"));

            this.proxy_port_textfield.setText(DBTools.selectSettingValue("proxy_port"));

            this.proxy_user_textfield.setText(DBTools.selectSettingValue("proxy_user"));

            this.proxy_pass_textfield.setText(DBTools.selectSettingValue("proxy_pass"));

            boolean debug_file = false;

            final String debug_file_val = DBTools.selectSettingValue("debug_file");

            if (debug_file_val != null) {
                debug_file = (debug_file_val.equals("yes"));
            }

            this.debug_file_checkbox.setSelected(debug_file);

            final String font = DBTools.selectSettingValue("font");

            this.font_combo.addItem(LabelTranslatorSingleton.getInstance().translate("DEFAULT"));

            this.font_combo.addItem(LabelTranslatorSingleton.getInstance().translate("ALTERNATIVE"));

            if (font == null) {
                this.font_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("DEFAULT"));
            } else {
                this.font_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate(font));
            }

            String language = DBTools.selectSettingValue("language");

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("English"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Spanish"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Italian"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Turkish"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Chinese"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Vietnamese"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("German"));

            this.language_combo.addItem(LabelTranslatorSingleton.getInstance().translate("Hungarian"));

            if (language == null) {
                language = MainPanel.DEFAULT_LANGUAGE;
            }

            if (language.equals("EN")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("English"));
            } else if (language.equals("ES")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Spanish"));
            } else if (language.equals("IT")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Italian"));
            } else if (language.equals("TU")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Turkish"));
            } else if (language.equals("CH")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Chinese"));
            } else if (language.equals("VI")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Vietnamese"));
            } else if (language.equals("GE")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("German"));
            } else if (language.equals("HU")) {
                this.language_combo.setSelectedItem(LabelTranslatorSingleton.getInstance().translate("Hungarian"));
            }

            final String custom_proxy_list = DBTools.selectSettingValue("custom_proxy_list");

            if (custom_proxy_list != null) {
                this.custom_proxy_textarea.setText(custom_proxy_list);
            }

            this.revalidate();

            this.repaint();

            this.setPreferredSize(parent.getSize());

            this.pack();
        });

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        this.jProgressBar1 = new javax.swing.JProgressBar();
        this.save_button = new javax.swing.JButton();
        this.cancel_button = new javax.swing.JButton();
        this.panel_tabs = new javax.swing.JTabbedPane();
        this.downloads_scrollpane = new javax.swing.JScrollPane();
        this.downloads_panel = new javax.swing.JPanel();
        this.megacrypter_reverse_warning_label = new javax.swing.JLabel();
        this.rec_download_slots_label = new javax.swing.JLabel();
        this.megacrypter_reverse_checkbox = new javax.swing.JCheckBox();
        this.limit_download_speed_checkbox = new javax.swing.JCheckBox();
        this.max_downloads_label = new javax.swing.JLabel();
        this.smart_proxy_checkbox = new javax.swing.JCheckBox();
        this.max_down_speed_spinner = new javax.swing.JSpinner();
        this.verify_file_down_checkbox = new javax.swing.JCheckBox();
        this.use_mega_account_down_checkbox = new javax.swing.JCheckBox();
        this.max_downloads_spinner = new javax.swing.JSpinner();
        this.use_mega_account_down_combobox = new javax.swing.JComboBox<>();
        this.change_download_dir_button = new javax.swing.JButton();
        this.max_down_speed_label = new javax.swing.JLabel();
        this.megacrypter_reverse_port_label = new javax.swing.JLabel();
        this.default_dir_label = new javax.swing.JLabel();
        this.default_slots_down_label = new javax.swing.JLabel();
        this.use_mega_label = new javax.swing.JLabel();
        this.multi_slot_down_checkbox = new javax.swing.JCheckBox();
        this.default_slots_down_spinner = new javax.swing.JSpinner();
        this.megacrypter_reverse_port_spinner = new javax.swing.JSpinner();
        this.down_dir_label = new javax.swing.JLabel();
        this.clipboardspy_checkbox = new javax.swing.JCheckBox();
        this.smart_proxy_settings = new javax.swing.JPanel();
        this.jLabel5 = new javax.swing.JLabel();
        this.jLabel3 = new javax.swing.JLabel();
        this.jLabel4 = new javax.swing.JLabel();
        this.bad_proxy_time_spinner = new javax.swing.JSpinner();
        this.jLabel6 = new javax.swing.JLabel();
        this.jScrollPane1 = new javax.swing.JScrollPane();
        this.custom_proxy_textarea = new javax.swing.JTextArea();
        this.rec_smart_proxy_label1 = new javax.swing.JLabel();
        this.custom_proxy_list_label = new javax.swing.JLabel();
        this.rec_smart_proxy_label = new javax.swing.JLabel();
        this.proxy_timeout_spinner = new javax.swing.JSpinner();
        this.force_smart_proxy_checkbox = new javax.swing.JCheckBox();
        this.jLabel7 = new javax.swing.JLabel();
        this.jLabel8 = new javax.swing.JLabel();
        this.auto_refresh_proxy_time_spinner = new javax.swing.JSpinner();
        this.proxy_random_radio = new javax.swing.JRadioButton();
        this.proxy_sequential_radio = new javax.swing.JRadioButton();
        this.jLabel9 = new javax.swing.JLabel();
        this.proxy_reset_slot_checkbox = new javax.swing.JCheckBox();
        this.jLabel10 = new javax.swing.JLabel();
        this.jLabel11 = new javax.swing.JLabel();
        this.uploads_scrollpane = new javax.swing.JScrollPane();
        this.uploads_panel = new javax.swing.JPanel();
        this.default_slots_up_label = new javax.swing.JLabel();
        this.max_uploads_label = new javax.swing.JLabel();
        this.default_slots_up_spinner = new javax.swing.JSpinner();
        this.max_uploads_spinner = new javax.swing.JSpinner();
        this.max_up_speed_label = new javax.swing.JLabel();
        this.max_up_speed_spinner = new javax.swing.JSpinner();
        this.limit_upload_speed_checkbox = new javax.swing.JCheckBox();
        this.rec_upload_slots_label = new javax.swing.JLabel();
        this.thumbnail_checkbox = new javax.swing.JCheckBox();
        this.upload_log_checkbox = new javax.swing.JCheckBox();
        this.upload_public_folder_checkbox = new javax.swing.JCheckBox();
        this.public_folder_panel = new javax.swing.JScrollPane();
        this.public_folder_warning = new javax.swing.JTextArea();
        this.accounts_panel = new javax.swing.JPanel();
        this.mega_accounts_scrollpane = new javax.swing.JScrollPane();
        this.mega_accounts_table = new javax.swing.JTable();
        this.mega_accounts_label = new javax.swing.JLabel();
        this.remove_mega_account_button = new javax.swing.JButton();
        this.add_mega_account_button = new javax.swing.JButton();
        this.encrypt_pass_checkbox = new javax.swing.JCheckBox();
        this.delete_all_accounts_button = new javax.swing.JButton();
        this.unlock_accounts_button = new javax.swing.JButton();
        this.elc_accounts_scrollpane = new javax.swing.JScrollPane();
        this.elc_accounts_table = new javax.swing.JTable();
        this.elc_accounts_label = new javax.swing.JLabel();
        this.remove_elc_account_button = new javax.swing.JButton();
        this.add_elc_account_button = new javax.swing.JButton();
        this.jLabel1 = new javax.swing.JLabel();
        this.import_mega_button = new javax.swing.JButton();
        this.advanced_scrollpane = new javax.swing.JScrollPane();
        this.advanced_panel = new javax.swing.JPanel();
        this.proxy_panel = new javax.swing.JPanel();
        this.proxy_host_label = new javax.swing.JLabel();
        this.proxy_host_textfield = new javax.swing.JTextField();
        this.proxy_port_label = new javax.swing.JLabel();
        this.proxy_port_textfield = new javax.swing.JTextField();
        this.use_proxy_checkbox = new javax.swing.JCheckBox();
        this.proxy_warning_label = new javax.swing.JLabel();
        this.proxy_auth_panel = new javax.swing.JPanel();
        this.proxy_user_label = new javax.swing.JLabel();
        this.proxy_user_textfield = new javax.swing.JTextField();
        this.proxy_pass_label = new javax.swing.JLabel();
        this.proxy_pass_textfield = new javax.swing.JPasswordField();
        this.rec_zoom_label = new javax.swing.JLabel();
        this.custom_chunks_dir_button = new javax.swing.JButton();
        this.custom_chunks_dir_current_label = new javax.swing.JLabel();
        this.custom_chunks_dir_checkbox = new javax.swing.JCheckBox();
        this.start_frozen_checkbox = new javax.swing.JCheckBox();
        this.run_command_checkbox = new javax.swing.JCheckBox();
        this.run_command_textbox = new javax.swing.JTextField();
        this.run_command_textbox.addMouseListener(new ContextMenuMouseListener());
        this.run_command_test_button = new javax.swing.JButton();
        this.debug_file_checkbox = new javax.swing.JCheckBox();
        this.jPanel1 = new javax.swing.JPanel();
        this.jButton1 = new javax.swing.JButton();
        this.import_settings_button = new javax.swing.JButton();
        this.export_settings_button = new javax.swing.JButton();
        this.jPanel2 = new javax.swing.JPanel();
        this.jLabel2 = new javax.swing.JLabel();
        this.font_label = new javax.swing.JLabel();
        this.language_combo = new javax.swing.JComboBox<>();
        this.font_combo = new javax.swing.JComboBox<>();
        this.zoom_label = new javax.swing.JLabel();
        this.zoom_spinner = new javax.swing.JSpinner();
        this.dark_mode_checkbox = new javax.swing.JCheckBox();
        this.debug_file_path = new javax.swing.JLabel();
        this.status = new javax.swing.JLabel();

        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle("Settings");

        this.save_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.save_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-save-all-30.png"))); // NOI18N
        this.save_button.setText("SAVE");
        this.save_button.setDoubleBuffered(true);
        this.save_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.save_buttonActionPerformed(evt);
            }
        });

        this.cancel_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.cancel_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        this.cancel_button.setText("CANCEL");
        this.cancel_button.setDoubleBuffered(true);
        this.cancel_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.cancel_buttonActionPerformed(evt);
            }
        });

        this.panel_tabs.setDoubleBuffered(true);
        this.panel_tabs.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N

        this.downloads_scrollpane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        this.megacrypter_reverse_warning_label.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        this.megacrypter_reverse_warning_label.setText("Note: you MUST \"OPEN\" this port in your router/firewall.");
        this.megacrypter_reverse_warning_label.setEnabled(false);

        this.rec_download_slots_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        this.rec_download_slots_label.setText("Note: slots consume resources, so use them moderately.");
        this.rec_download_slots_label.setEnabled(false);

        this.megacrypter_reverse_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.megacrypter_reverse_checkbox.setText("Use MegaCrypter reverse mode");
        this.megacrypter_reverse_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.megacrypter_reverse_checkboxStateChanged(evt);
            }
        });

        this.limit_download_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.limit_download_speed_checkbox.setText("Limit download speed");
        this.limit_download_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.limit_download_speed_checkboxStateChanged(evt);
            }
        });

        this.max_downloads_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.max_downloads_label.setText("Max parallel downloads:");
        this.max_downloads_label.setDoubleBuffered(true);

        this.smart_proxy_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.smart_proxy_checkbox.setText("Use SmartProxy");
        this.smart_proxy_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.smart_proxy_checkboxStateChanged(evt);
            }
        });
        this.smart_proxy_checkbox.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(final java.awt.event.MouseEvent evt) {
                SettingsDialog.this.smart_proxy_checkboxMouseClicked(evt);
            }
        });

        this.max_down_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.max_down_speed_spinner.setEnabled(false);

        this.verify_file_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.verify_file_down_checkbox.setText("Verify file integrity (when download is finished)");
        this.verify_file_down_checkbox.setDoubleBuffered(true);

        this.use_mega_account_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.use_mega_account_down_checkbox.setText("Allow using MEGA accounts for download/streaming");
        this.use_mega_account_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.use_mega_account_down_checkboxStateChanged(evt);
            }
        });

        this.max_downloads_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.max_downloads_spinner.setDoubleBuffered(true);

        this.use_mega_account_down_combobox.setFont(new java.awt.Font("Dialog", 0, 20)); // NOI18N
        this.use_mega_account_down_combobox.setEnabled(false);

        this.change_download_dir_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.change_download_dir_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-folder-30.png"))); // NOI18N
        this.change_download_dir_button.setText("Change it");
        this.change_download_dir_button.setDoubleBuffered(true);
        this.change_download_dir_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.change_download_dir_buttonActionPerformed(evt);
            }
        });

        this.max_down_speed_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.max_down_speed_label.setText("Max speed (KB/s):");
        this.max_down_speed_label.setEnabled(false);

        this.megacrypter_reverse_port_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.megacrypter_reverse_port_label.setText("TCP Port:");
        this.megacrypter_reverse_port_label.setEnabled(false);

        this.default_dir_label.setBackground(new java.awt.Color(153, 255, 153));
        this.default_dir_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.default_dir_label.setForeground(new java.awt.Color(51, 0, 255));
        this.default_dir_label.setText("default dir");
        this.default_dir_label.setOpaque(true);

        this.default_slots_down_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.default_slots_down_label.setText("Default slots per file:");
        this.default_slots_down_label.setDoubleBuffered(true);
        this.default_slots_down_label.setEnabled(false);

        this.use_mega_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.use_mega_label.setText("Default account:");
        this.use_mega_label.setEnabled(false);

        this.multi_slot_down_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.multi_slot_down_checkbox.setText("Use multi slot download mode");
        this.multi_slot_down_checkbox.setDoubleBuffered(true);
        this.multi_slot_down_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.multi_slot_down_checkboxStateChanged(evt);
            }
        });

        this.default_slots_down_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.default_slots_down_spinner.setDoubleBuffered(true);
        this.default_slots_down_spinner.setEnabled(false);
        this.default_slots_down_spinner.setValue(2);

        this.megacrypter_reverse_port_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.megacrypter_reverse_port_spinner.setEnabled(false);

        this.down_dir_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.down_dir_label.setText("Download folder:");
        this.down_dir_label.setDoubleBuffered(true);

        this.clipboardspy_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.clipboardspy_checkbox.setText("Monitor clipboard looking for new links");

        this.smart_proxy_settings.setEnabled(false);

        this.jLabel5.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        this.jLabel5.setText("Proxy timeout (seconds):");

        this.jLabel3.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        this.jLabel3.setText("Proxy error ban time (seconds):");

        this.jLabel4.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        this.jLabel4.setText("(0 for permanent ban)");

        this.bad_proxy_time_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        this.bad_proxy_time_spinner.setModel(new javax.swing.SpinnerNumberModel(300, 0, null, 1));

        this.jLabel6.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        this.jLabel6.setText("(Lower values can speed up finding working proxies but it could ban slow proxies)");

        this.custom_proxy_textarea.setColumns(20);
        this.custom_proxy_textarea.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.custom_proxy_textarea.setRows(5);
        this.custom_proxy_textarea.setDoubleBuffered(true);
        this.jScrollPane1.setViewportView(this.custom_proxy_textarea);
        this.custom_proxy_textarea.addMouseListener(new ContextMenuMouseListener());

        this.rec_smart_proxy_label1.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.rec_smart_proxy_label1.setForeground(new java.awt.Color(255, 51, 0));
        this.rec_smart_proxy_label1.setText("WARNING: Using proxies or VPN to bypass MEGA's daily download limitation may violate its Terms of Use. USE THIS OPTION AT YOUR OWN RISK.");

        this.custom_proxy_list_label.setBackground(new java.awt.Color(0, 0, 0));
        this.custom_proxy_list_label.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.custom_proxy_list_label.setForeground(new java.awt.Color(255, 255, 255));
        this.custom_proxy_list_label.setText("[*]IP:PORT[@user_b64:password_b64] OR #PROXY_LIST_URL");
        this.custom_proxy_list_label.setOpaque(true);

        this.rec_smart_proxy_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        this.rec_smart_proxy_label.setText("Note1: enable it in order to mitigate bandwidth limit. (Multislot is required) ");

        this.proxy_timeout_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        this.proxy_timeout_spinner.setModel(new javax.swing.SpinnerNumberModel(10, 1, null, 1));

        this.force_smart_proxy_checkbox.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        this.force_smart_proxy_checkbox.setText("FORCE SMART PROXY");

        this.jLabel7.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        this.jLabel7.setText("Forces the use of smart proxy even if we still have direct bandwidth available (useful to test proxies)");

        this.jLabel8.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        this.jLabel8.setText("Proxy list refresh (minutes):");

        this.auto_refresh_proxy_time_spinner.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        this.auto_refresh_proxy_time_spinner.setModel(new javax.swing.SpinnerNumberModel(60, 1, null, 1));

        this.proxy_random_radio.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        this.proxy_random_radio.setText("RANDOM");
        this.proxy_random_radio.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.proxy_random_radioActionPerformed(evt);
            }
        });

        this.proxy_sequential_radio.setFont(new java.awt.Font("Noto Sans", 0, 16)); // NOI18N
        this.proxy_sequential_radio.setText("SEQUENTIAL");
        this.proxy_sequential_radio.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.proxy_sequential_radioActionPerformed(evt);
            }
        });

        this.jLabel9.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        this.jLabel9.setText("Proxy selection order:");

        this.proxy_reset_slot_checkbox.setFont(new java.awt.Font("Noto Sans", 1, 16)); // NOI18N
        this.proxy_reset_slot_checkbox.setText("Reset slot proxy after successfully downloading a chunk");

        this.jLabel10.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        this.jLabel10.setText("(Useful to avoid getting trapped in slow proxies)");

        this.jLabel11.setFont(new java.awt.Font("Noto Sans", 2, 16)); // NOI18N
        this.jLabel11.setText("(If you have a list of proxies sorted from best to worst, check sequential)");

        final javax.swing.GroupLayout smart_proxy_settingsLayout = new javax.swing.GroupLayout(this.smart_proxy_settings);
        this.smart_proxy_settings.setLayout(smart_proxy_settingsLayout);
        smart_proxy_settingsLayout.setHorizontalGroup(
                smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                .addComponent(this.force_smart_proxy_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addComponent(this.custom_proxy_list_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(this.jScrollPane1)
                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(this.rec_smart_proxy_label1)
                                                        .addComponent(this.rec_smart_proxy_label)
                                                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                                                                .addComponent(this.jLabel9)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
                                                                                .addComponent(this.proxy_random_radio)
                                                                                .addGap(18, 18, 18)
                                                                                .addComponent(this.proxy_sequential_radio))
                                                                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                                                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                        .addComponent(this.jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(this.jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(this.jLabel3, javax.swing.GroupLayout.Alignment.LEADING))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                                        .addComponent(this.bad_proxy_time_spinner, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                                                                        .addComponent(this.auto_refresh_proxy_time_spinner, javax.swing.GroupLayout.Alignment.LEADING)
                                                                                        .addComponent(this.proxy_timeout_spinner))))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(this.jLabel4)
                                                                        .addComponent(this.jLabel6)
                                                                        .addComponent(this.jLabel11)))))
                                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                                .addComponent(this.proxy_reset_slot_checkbox)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.jLabel10)))
                                .addGap(0, 0, Short.MAX_VALUE))
        );
        smart_proxy_settingsLayout.setVerticalGroup(
                smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(smart_proxy_settingsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.rec_smart_proxy_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.rec_smart_proxy_label1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.proxy_random_radio)
                                        .addComponent(this.proxy_sequential_radio)
                                        .addComponent(this.jLabel9)
                                        .addComponent(this.jLabel11))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.jLabel8)
                                        .addComponent(this.auto_refresh_proxy_time_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.jLabel3)
                                        .addComponent(this.bad_proxy_time_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.jLabel4))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.jLabel5)
                                        .addComponent(this.proxy_timeout_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.jLabel6))
                                .addGap(7, 7, 7)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.proxy_reset_slot_checkbox)
                                        .addComponent(this.jLabel10))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(smart_proxy_settingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.force_smart_proxy_checkbox)
                                        .addComponent(this.jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.custom_proxy_list_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 344, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        final javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(this.downloads_panel);
        this.downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
                downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(this.smart_proxy_checkbox)
                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                .addComponent(this.max_downloads_label)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(this.multi_slot_down_checkbox)
                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                .addComponent(this.change_download_dir_button)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.down_dir_label)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.default_dir_label))
                                                        .addComponent(this.megacrypter_reverse_checkbox)
                                                        .addComponent(this.use_mega_account_down_checkbox)
                                                        .addComponent(this.verify_file_down_checkbox)
                                                        .addComponent(this.limit_download_speed_checkbox)
                                                        .addComponent(this.clipboardspy_checkbox)
                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                .addGap(21, 21, 21)
                                                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(this.rec_download_slots_label)
                                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                                .addComponent(this.default_slots_down_label)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(this.default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                                .addComponent(this.use_mega_label)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(this.use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, 700, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addComponent(this.megacrypter_reverse_warning_label)
                                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                                .addComponent(this.megacrypter_reverse_port_label)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(this.megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                                                .addComponent(this.max_down_speed_label)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(this.max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                                .addGap(12, 12, 12)
                                                .addComponent(this.smart_proxy_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        downloads_panelLayout.setVerticalGroup(
                downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.down_dir_label)
                                        .addComponent(this.default_dir_label)
                                        .addComponent(this.change_download_dir_button))
                                .addGap(18, 18, 18)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.max_downloads_label)
                                        .addComponent(this.max_downloads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(this.multi_slot_down_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.default_slots_down_label)
                                        .addComponent(this.default_slots_down_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.rec_download_slots_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.clipboardspy_checkbox)
                                .addGap(10, 10, 10)
                                .addComponent(this.limit_download_speed_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.max_down_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.max_down_speed_label))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.verify_file_down_checkbox)
                                .addGap(18, 18, 18)
                                .addComponent(this.use_mega_account_down_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.use_mega_account_down_combobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.use_mega_label))
                                .addGap(18, 18, 18)
                                .addComponent(this.megacrypter_reverse_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.megacrypter_reverse_port_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.megacrypter_reverse_port_label))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.megacrypter_reverse_warning_label)
                                .addGap(18, 18, 18)
                                .addComponent(this.smart_proxy_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.smart_proxy_settings, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        this.downloads_scrollpane.setViewportView(this.downloads_panel);

        this.panel_tabs.addTab("Downloads", new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-download-from-ftp-30.png")), this.downloads_scrollpane); // NOI18N

        this.uploads_scrollpane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        this.default_slots_up_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.default_slots_up_label.setText("Default slots per file:");
        this.default_slots_up_label.setDoubleBuffered(true);

        this.max_uploads_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.max_uploads_label.setText("Max parallel uploads:");
        this.max_uploads_label.setDoubleBuffered(true);

        this.default_slots_up_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.default_slots_up_spinner.setDoubleBuffered(true);
        this.default_slots_up_spinner.setValue(2);

        this.max_uploads_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.max_uploads_spinner.setDoubleBuffered(true);

        this.max_up_speed_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.max_up_speed_label.setText("Max speed (KB/s):");
        this.max_up_speed_label.setEnabled(false);

        this.max_up_speed_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.max_up_speed_spinner.setEnabled(false);

        this.limit_upload_speed_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.limit_upload_speed_checkbox.setText("Limit upload speed");
        this.limit_upload_speed_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.limit_upload_speed_checkboxStateChanged(evt);
            }
        });

        this.rec_upload_slots_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        this.rec_upload_slots_label.setText("Note: slots consume resources, so use them moderately.");

        this.thumbnail_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.thumbnail_checkbox.setText("Create and upload image/video thumbnails");
        this.thumbnail_checkbox.setDoubleBuffered(true);

        this.upload_log_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.upload_log_checkbox.setText("Create upload logs");
        this.upload_log_checkbox.setDoubleBuffered(true);

        this.upload_public_folder_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.upload_public_folder_checkbox.setText("CREATE UPLOAD FOLDER PUBLIC LINK");
        this.upload_public_folder_checkbox.setDoubleBuffered(true);
        this.upload_public_folder_checkbox.setOpaque(true);
        this.upload_public_folder_checkbox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.upload_public_folder_checkboxActionPerformed(evt);
            }
        });

        this.public_folder_warning.setEditable(false);
        this.public_folder_warning.setBackground(new java.awt.Color(255, 255, 51));
        this.public_folder_warning.setColumns(20);
        this.public_folder_warning.setFont(new java.awt.Font("Noto Sans", 1, 18)); // NOI18N
        this.public_folder_warning.setForeground(new java.awt.Color(0, 51, 255));
        this.public_folder_warning.setLineWrap(true);
        this.public_folder_warning.setRows(5);
        this.public_folder_warning.setText("THIS OPTION IS NOT RECOMMENDED. Using this will cause MegaBasterd uploaded folder to appear in your account as NOT DECRYPTABLE. \n\nAt the time of writing this text, there is a method to FIX IT:\n\n1) Move first upload subfolder to the ROOT (CLOUD) folder of your account. \n\n2) Go to account settings and click RELOAD ACCOUNT. \n\nI don't know how long this method will last. USE THIS OPTION AT YOUR OWN RISK.");
        this.public_folder_warning.setWrapStyleWord(true);
        this.public_folder_panel.setViewportView(this.public_folder_warning);

        final javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(this.uploads_panel);
        this.uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
                uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(uploads_panelLayout.createSequentialGroup()
                                                .addGap(20, 20, 20)
                                                .addComponent(this.max_up_speed_label)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(this.limit_upload_speed_checkbox)
                                        .addGroup(uploads_panelLayout.createSequentialGroup()
                                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(this.default_slots_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(this.max_uploads_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(this.default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(this.max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addComponent(this.thumbnail_checkbox)
                                        .addComponent(this.upload_log_checkbox)
                                        .addComponent(this.upload_public_folder_checkbox)
                                        .addComponent(this.public_folder_panel, javax.swing.GroupLayout.PREFERRED_SIZE, 1003, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.rec_upload_slots_label))
                                .addGap(0, 0, 0))
        );
        uploads_panelLayout.setVerticalGroup(
                uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.max_uploads_label)
                                        .addComponent(this.max_uploads_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.default_slots_up_label)
                                        .addComponent(this.default_slots_up_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.rec_upload_slots_label)
                                .addGap(18, 18, 18)
                                .addComponent(this.limit_upload_speed_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.max_up_speed_label)
                                        .addComponent(this.max_up_speed_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(this.thumbnail_checkbox)
                                .addGap(18, 18, 18)
                                .addComponent(this.upload_log_checkbox)
                                .addGap(18, 18, 18)
                                .addComponent(this.upload_public_folder_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.public_folder_panel, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        this.uploads_scrollpane.setViewportView(this.uploads_panel);

        this.panel_tabs.addTab("Uploads", new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-upload-to-ftp-30.png")), this.uploads_scrollpane); // NOI18N

        this.accounts_panel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        this.mega_accounts_table.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.mega_accounts_table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "Email", "Password"
                }
        ) {
            final Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class
            };

            @Override
            public Class getColumnClass(final int columnIndex) {
                return this.types[columnIndex];
            }
        });
        this.mega_accounts_table.setDoubleBuffered(true);
        this.mega_accounts_table.setRowHeight((int) (24 * this._main_panel.getZoom_factor()));
        this.mega_accounts_scrollpane.setViewportView(this.mega_accounts_table);

        this.mega_accounts_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.mega_accounts_label.setText("Your MEGA accounts:");
        this.mega_accounts_label.setDoubleBuffered(true);

        this.remove_mega_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.remove_mega_account_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.remove_mega_account_button.setText("Remove selected");
        this.remove_mega_account_button.setDoubleBuffered(true);
        this.remove_mega_account_button.setEnabled(false);
        this.remove_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.remove_mega_account_buttonActionPerformed(evt);
            }
        });

        this.add_mega_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.add_mega_account_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-plus-30.png"))); // NOI18N
        this.add_mega_account_button.setText("Add account");
        this.add_mega_account_button.setDoubleBuffered(true);
        this.add_mega_account_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.add_mega_account_buttonActionPerformed(evt);
            }
        });

        this.encrypt_pass_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.encrypt_pass_checkbox.setText("Encrypt on disk sensitive information");
        this.encrypt_pass_checkbox.setDoubleBuffered(true);
        this.encrypt_pass_checkbox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.encrypt_pass_checkboxActionPerformed(evt);
            }
        });

        this.delete_all_accounts_button.setBackground(new java.awt.Color(255, 51, 0));
        this.delete_all_accounts_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.delete_all_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        this.delete_all_accounts_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.delete_all_accounts_button.setText("RESET ACCOUNTS");
        this.delete_all_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.delete_all_accounts_buttonActionPerformed(evt);
            }
        });

        this.unlock_accounts_button.setBackground(new java.awt.Color(0, 153, 51));
        this.unlock_accounts_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.unlock_accounts_button.setForeground(new java.awt.Color(255, 255, 255));
        this.unlock_accounts_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-key-2-30.png"))); // NOI18N
        this.unlock_accounts_button.setText("Unlock accounts");
        this.unlock_accounts_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.unlock_accounts_buttonActionPerformed(evt);
            }
        });

        this.elc_accounts_scrollpane.setDoubleBuffered(true);

        this.elc_accounts_table.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.elc_accounts_table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{
                        "Host", "User", "API-KEY"
                }
        ) {
            final Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            @Override
            public Class getColumnClass(final int columnIndex) {
                return this.types[columnIndex];
            }
        });
        this.elc_accounts_table.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        this.elc_accounts_table.setDoubleBuffered(true);
        this.elc_accounts_table.setRowHeight((int) (24 * this._main_panel.getZoom_factor()));
        this.elc_accounts_scrollpane.setViewportView(this.elc_accounts_table);

        this.elc_accounts_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.elc_accounts_label.setText("Your ELC accounts:");
        this.elc_accounts_label.setDoubleBuffered(true);

        this.remove_elc_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.remove_elc_account_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.remove_elc_account_button.setText("Remove selected");
        this.remove_elc_account_button.setDoubleBuffered(true);
        this.remove_elc_account_button.setEnabled(false);
        this.remove_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.remove_elc_account_buttonActionPerformed(evt);
            }
        });

        this.add_elc_account_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.add_elc_account_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-plus-30.png"))); // NOI18N
        this.add_elc_account_button.setText("Add account");
        this.add_elc_account_button.setDoubleBuffered(true);
        this.add_elc_account_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.add_elc_account_buttonActionPerformed(evt);
            }
        });

        this.jLabel1.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        this.jLabel1.setText("Note: you can use a (optional) alias for your email addresses -> bob@supermail.com#bob_mail (don't forget to save after entering your accounts).");
        this.jLabel1.setDoubleBuffered(true);

        this.import_mega_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.import_mega_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-import-30.png"))); // NOI18N
        this.import_mega_button.setText("IMPORT ACCOUNTS (FILE)");
        this.import_mega_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.import_mega_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout accounts_panelLayout = new javax.swing.GroupLayout(this.accounts_panel);
        this.accounts_panel.setLayout(accounts_panelLayout);
        accounts_panelLayout.setHorizontalGroup(
                accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(accounts_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.mega_accounts_scrollpane)
                                        .addGroup(accounts_panelLayout.createSequentialGroup()
                                                .addComponent(this.delete_all_accounts_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(this.unlock_accounts_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.encrypt_pass_checkbox))
                                        .addGroup(accounts_panelLayout.createSequentialGroup()
                                                .addComponent(this.remove_mega_account_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.import_mega_button)
                                                .addGap(18, 18, 18)
                                                .addComponent(this.add_mega_account_button))
                                        .addComponent(this.elc_accounts_scrollpane)
                                        .addGroup(accounts_panelLayout.createSequentialGroup()
                                                .addComponent(this.remove_elc_account_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.add_elc_account_button))
                                        .addGroup(accounts_panelLayout.createSequentialGroup()
                                                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(this.mega_accounts_label)
                                                        .addComponent(this.elc_accounts_label))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(this.jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        accounts_panelLayout.setVerticalGroup(
                accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(accounts_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.unlock_accounts_button)
                                        .addComponent(this.delete_all_accounts_button)
                                        .addComponent(this.encrypt_pass_checkbox))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.mega_accounts_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.mega_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.remove_mega_account_button)
                                        .addComponent(this.add_mega_account_button)
                                        .addComponent(this.import_mega_button))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.elc_accounts_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.elc_accounts_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accounts_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.remove_elc_account_button)
                                        .addComponent(this.add_elc_account_button))
                                .addContainerGap())
        );

        this.panel_tabs.addTab("Accounts", new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-customer-30.png")), this.accounts_panel); // NOI18N

        this.advanced_scrollpane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        this.proxy_panel.setBorder(javax.swing.BorderFactory.createTitledBorder((String) null));

        this.proxy_host_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.proxy_host_label.setText("Host:");
        this.proxy_host_label.setDoubleBuffered(true);
        this.proxy_host_label.setEnabled(false);

        this.proxy_host_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.proxy_host_textfield.setDoubleBuffered(true);
        this.proxy_host_textfield.setEnabled(false);
        this.proxy_host_textfield.addMouseListener(new ContextMenuMouseListener());

        this.proxy_port_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.proxy_port_label.setText("Port:");
        this.proxy_port_label.setDoubleBuffered(true);
        this.proxy_port_label.setEnabled(false);

        this.proxy_port_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.proxy_port_textfield.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        this.proxy_port_textfield.setDoubleBuffered(true);
        this.proxy_port_textfield.setEnabled(false);
        this.proxy_port_textfield.addMouseListener(new ContextMenuMouseListener());

        this.use_proxy_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.use_proxy_checkbox.setText("Use HTTP(S) PROXY");
        this.use_proxy_checkbox.setDoubleBuffered(true);
        this.use_proxy_checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(final javax.swing.event.ChangeEvent evt) {
                SettingsDialog.this.use_proxy_checkboxStateChanged(evt);
            }
        });

        this.proxy_warning_label.setFont(new java.awt.Font("Dialog", 2, 14)); // NOI18N
        this.proxy_warning_label.setText("Note: MegaBasterd will use this proxy for ALL connections.");
        this.proxy_warning_label.setEnabled(false);

        this.proxy_auth_panel.setBorder(javax.swing.BorderFactory.createTitledBorder((String) null));

        this.proxy_user_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.proxy_user_label.setText("Username:");
        this.proxy_user_label.setDoubleBuffered(true);
        this.proxy_user_label.setEnabled(false);

        this.proxy_user_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.proxy_user_textfield.setDoubleBuffered(true);
        this.proxy_user_textfield.setEnabled(false);
        this.proxy_user_textfield.addMouseListener(new ContextMenuMouseListener());

        this.proxy_pass_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.proxy_pass_label.setText("Password:");
        this.proxy_pass_label.setDoubleBuffered(true);
        this.proxy_pass_label.setEnabled(false);

        this.proxy_pass_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.proxy_pass_textfield.setText("jPasswordField1");
        this.proxy_pass_textfield.setEnabled(false);

        final javax.swing.GroupLayout proxy_auth_panelLayout = new javax.swing.GroupLayout(this.proxy_auth_panel);
        this.proxy_auth_panel.setLayout(proxy_auth_panelLayout);
        proxy_auth_panelLayout.setHorizontalGroup(
                proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(proxy_auth_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.proxy_user_label)
                                .addGap(6, 6, 6)
                                .addComponent(this.proxy_user_textfield)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.proxy_pass_label)
                                .addGap(6, 6, 6)
                                .addComponent(this.proxy_pass_textfield)
                                .addContainerGap())
        );
        proxy_auth_panelLayout.setVerticalGroup(
                proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(proxy_auth_panelLayout.createSequentialGroup()
                                .addGap(0, 12, Short.MAX_VALUE)
                                .addGroup(proxy_auth_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.proxy_user_label)
                                        .addComponent(this.proxy_user_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.proxy_pass_label)
                                        .addComponent(this.proxy_pass_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );

        final javax.swing.GroupLayout proxy_panelLayout = new javax.swing.GroupLayout(this.proxy_panel);
        this.proxy_panel.setLayout(proxy_panelLayout);
        proxy_panelLayout.setHorizontalGroup(
                proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(proxy_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(proxy_panelLayout.createSequentialGroup()
                                                .addComponent(this.use_proxy_checkbox)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(proxy_panelLayout.createSequentialGroup()
                                                .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proxy_panelLayout.createSequentialGroup()
                                                                .addComponent(this.proxy_host_label)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.proxy_host_textfield)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.proxy_port_label)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.proxy_port_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(this.proxy_auth_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(this.proxy_warning_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addContainerGap())))
        );
        proxy_panelLayout.setVerticalGroup(
                proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, proxy_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.use_proxy_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.proxy_warning_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(proxy_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.proxy_host_label)
                                        .addComponent(this.proxy_host_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.proxy_port_label)
                                        .addComponent(this.proxy_port_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                                .addComponent(this.proxy_auth_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        this.rec_zoom_label.setFont(new java.awt.Font("Dialog", 2, 16)); // NOI18N
        this.rec_zoom_label.setText("Note: restart might be required.");
        this.rec_zoom_label.setDoubleBuffered(true);

        this.custom_chunks_dir_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.custom_chunks_dir_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-folder-30.png"))); // NOI18N
        this.custom_chunks_dir_button.setText("Change it");
        this.custom_chunks_dir_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.custom_chunks_dir_buttonActionPerformed(evt);
            }
        });

        this.custom_chunks_dir_current_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        this.custom_chunks_dir_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.custom_chunks_dir_checkbox.setText("Use custom temporary directory for chunks storage");
        this.custom_chunks_dir_checkbox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.custom_chunks_dir_checkboxActionPerformed(evt);
            }
        });

        this.start_frozen_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.start_frozen_checkbox.setText("Freeze transferences before start");

        this.run_command_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.run_command_checkbox.setText("Execute this command when MEGA download limit is reached:");
        this.run_command_checkbox.setDoubleBuffered(true);
        this.run_command_checkbox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.run_command_checkboxActionPerformed(evt);
            }
        });

        this.run_command_textbox.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.run_command_textbox.setDoubleBuffered(true);
        this.run_command_textbox.setEnabled(false);

        this.run_command_test_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.run_command_test_button.setText("Test");
        this.run_command_test_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.run_command_test_buttonActionPerformed(evt);
            }
        });

        this.debug_file_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.debug_file_checkbox.setText("Save debug info to file -> ");

        this.jButton1.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.jButton1.setForeground(new java.awt.Color(255, 0, 0));
        this.jButton1.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        this.jButton1.setText("RESET MEGABASTERD");
        this.jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.jButton1ActionPerformed(evt);
            }
        });

        this.import_settings_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.import_settings_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-import-30.png"))); // NOI18N
        this.import_settings_button.setText("IMPORT SETTINGS");
        this.import_settings_button.setDoubleBuffered(true);
        this.import_settings_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.import_settings_buttonActionPerformed(evt);
            }
        });

        this.export_settings_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.export_settings_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-export-30.png"))); // NOI18N
        this.export_settings_button.setText("EXPORT SETTINGS");
        this.export_settings_button.setDoubleBuffered(true);
        this.export_settings_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                SettingsDialog.this.export_settings_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(this.jPanel1);
        this.jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(this.import_settings_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.export_settings_button, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.import_settings_button)
                                        .addComponent(this.export_settings_button))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jButton1)
                                .addContainerGap())
        );

        this.jLabel2.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.jLabel2.setText("Language:");

        this.font_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.font_label.setText("Font:");

        this.language_combo.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        this.font_combo.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        this.zoom_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.zoom_label.setText("Font ZOOM (%):");
        this.zoom_label.setDoubleBuffered(true);

        this.zoom_spinner.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.zoom_spinner.setDoubleBuffered(true);

        this.dark_mode_checkbox.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.dark_mode_checkbox.setText("DARK MODE");

        final javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(this.jPanel2);
        this.jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(this.zoom_label)
                                                                .addComponent(this.font_label))
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                .addComponent(this.font_combo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(this.zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                        .addComponent(this.jLabel2)
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(this.language_combo, javax.swing.GroupLayout.PREFERRED_SIZE, 351, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addComponent(this.dark_mode_checkbox))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.font_label)
                                        .addComponent(this.font_combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.zoom_label)
                                        .addComponent(this.zoom_spinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.jLabel2)
                                        .addComponent(this.language_combo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.dark_mode_checkbox)
                                .addContainerGap())
        );

        this.debug_file_path.setFont(new java.awt.Font("Noto Sans", 0, 18)); // NOI18N
        this.debug_file_path.setText(MainPanel.MEGABASTERD_HOME_DIR + "/MEGABASTERD_DEBUG.log");

        final javax.swing.GroupLayout advanced_panelLayout = new javax.swing.GroupLayout(this.advanced_panel);
        this.advanced_panel.setLayout(advanced_panelLayout);
        advanced_panelLayout.setHorizontalGroup(
                advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.proxy_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(advanced_panelLayout.createSequentialGroup()
                                                .addComponent(this.run_command_test_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.run_command_textbox))
                                        .addComponent(this.start_frozen_checkbox)
                                        .addGroup(advanced_panelLayout.createSequentialGroup()
                                                .addComponent(this.debug_file_checkbox)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.debug_file_path))
                                        .addGroup(advanced_panelLayout.createSequentialGroup()
                                                .addGap(165, 165, 165)
                                                .addComponent(this.custom_chunks_dir_current_label))
                                        .addComponent(this.rec_zoom_label)
                                        .addComponent(this.run_command_checkbox)
                                        .addGroup(advanced_panelLayout.createSequentialGroup()
                                                .addComponent(this.custom_chunks_dir_checkbox)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.custom_chunks_dir_button))
                                        .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(this.jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        advanced_panelLayout.setVerticalGroup(
                advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(advanced_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.custom_chunks_dir_checkbox)
                                        .addComponent(this.custom_chunks_dir_button))
                                .addGap(0, 0, 0)
                                .addComponent(this.custom_chunks_dir_current_label)
                                .addGap(18, 18, 18)
                                .addComponent(this.start_frozen_checkbox)
                                .addGap(18, 18, 18)
                                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.debug_file_checkbox)
                                        .addComponent(this.debug_file_path))
                                .addGap(18, 18, 18)
                                .addComponent(this.run_command_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(advanced_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.run_command_textbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(this.run_command_test_button))
                                .addGap(18, 18, 18)
                                .addComponent(this.proxy_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(this.rec_zoom_label)
                                .addGap(34, 34, 34))
        );

        this.advanced_scrollpane.setViewportView(this.advanced_panel);

        this.panel_tabs.addTab("Advanced", new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-administrative-tools-30.png")), this.advanced_scrollpane); // NOI18N

        this.status.setFont(new java.awt.Font("Dialog", 3, 14)); // NOI18N
        this.status.setForeground(new java.awt.Color(102, 102, 102));

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(this.status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.save_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(this.cancel_button))
                                        .addComponent(this.panel_tabs, javax.swing.GroupLayout.DEFAULT_SIZE, 1194, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.panel_tabs, javax.swing.GroupLayout.DEFAULT_SIZE, 1082, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(this.cancel_button)
                                                .addComponent(this.save_button))
                                        .addComponent(this.status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        this.pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancel_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_buttonActionPerformed

        if (!this.save_button.isEnabled()) {

            final Object[] options = {"No",
                    LabelTranslatorSingleton.getInstance().translate("Yes")};

            int n = 1;
            n = showOptionDialog(this,
                    LabelTranslatorSingleton.getInstance().translate("SURE?"),
                    LabelTranslatorSingleton.getInstance().translate("EXIT"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (n == 1) {
                this._exit = true;
                this.dispose();
            }

        } else {
            this._exit = true;
            this.dispose();
        }

    }//GEN-LAST:event_cancel_buttonActionPerformed

    private void save_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_buttonActionPerformed

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.panel_tabs.setEnabled(false);

        try {

            if (this.proxy_host_textfield.getText().isEmpty()) {

                this.use_proxy_checkbox.setSelected(false);
            }

            final HashMap<String, Object> settings = new HashMap<>();

            settings.put("default_down_dir", this._download_path);
            settings.put("default_slots_down", String.valueOf(this.default_slots_down_spinner.getValue()));
            settings.put("default_slots_up", String.valueOf(this.default_slots_up_spinner.getValue()));
            settings.put("use_slots_down", this.multi_slot_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_downloads", String.valueOf(this.max_downloads_spinner.getValue()));
            settings.put("max_uploads", String.valueOf(this.max_uploads_spinner.getValue()));
            settings.put("verify_down_file", this.verify_file_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("limit_download_speed", this.limit_download_speed_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_download_speed", String.valueOf(this.max_down_speed_spinner.getValue()));
            settings.put("limit_upload_speed", this.limit_upload_speed_checkbox.isSelected() ? "yes" : "no");
            settings.put("max_upload_speed", String.valueOf(this.max_up_speed_spinner.getValue()));
            settings.put("use_mega_account_down", this.use_mega_account_down_checkbox.isSelected() ? "yes" : "no");
            settings.put("mega_account_down", this.use_mega_account_down_combobox.getSelectedItem());
            settings.put("megacrypter_reverse", this.megacrypter_reverse_checkbox.isSelected() ? "yes" : "no");
            settings.put("megacrypter_reverse_port", String.valueOf(this.megacrypter_reverse_port_spinner.getValue()));
            settings.put("start_frozen", this.start_frozen_checkbox.isSelected() ? "yes" : "no");
            settings.put("use_custom_chunks_dir", this.custom_chunks_dir_checkbox.isSelected() ? "yes" : "no");
            settings.put("custom_chunks_dir", this._custom_chunks_dir);
            settings.put("run_command", this.run_command_checkbox.isSelected() ? "yes" : "no");
            settings.put("run_command_path", this.run_command_textbox.getText());
            settings.put("clipboardspy", this.clipboardspy_checkbox.isSelected() ? "yes" : "no");
            settings.put("thumbnails", this.thumbnail_checkbox.isSelected() ? "yes" : "no");
            settings.put("upload_log", this.upload_log_checkbox.isSelected() ? "yes" : "no");
            settings.put("force_smart_proxy", this.force_smart_proxy_checkbox.isSelected() ? "yes" : "no");
            settings.put("reset_slot_proxy", this.proxy_reset_slot_checkbox.isSelected() ? "yes" : "no");
            settings.put("random_proxy", this.proxy_random_radio.isSelected() ? "yes" : "no");
            settings.put("dark_mode", this.dark_mode_checkbox.isSelected() ? "yes" : "no");
            settings.put("upload_public_folder", this.upload_public_folder_checkbox.isSelected() ? "yes" : "no");
            settings.put("smartproxy_ban_time", String.valueOf(this.bad_proxy_time_spinner.getValue()));
            settings.put("smartproxy_timeout", String.valueOf(this.proxy_timeout_spinner.getValue()));
            settings.put("smartproxy_autorefresh_time", String.valueOf(this.auto_refresh_proxy_time_spinner.getValue()));

            if (this.upload_log_checkbox.isSelected()) {
                createUploadLogDir();
            }

            if (this.custom_proxy_textarea.getText().trim().length() == 0) {
                this.smart_proxy_checkbox.setSelected(false);
            }

            settings.put("smart_proxy", this.smart_proxy_checkbox.isSelected() ? "yes" : "no");
            settings.put("custom_proxy_list", this.custom_proxy_textarea.getText());

            String old_font = DBTools.selectSettingValue("font");

            if (old_font == null) {
                old_font = "DEFAULT";
            }

            String font = (String) this.font_combo.getSelectedItem();

            if (font.equals(LabelTranslatorSingleton.getInstance().translate("DEFAULT"))) {
                font = "DEFAULT";
            } else if (font.equals(LabelTranslatorSingleton.getInstance().translate("ALTERNATIVE"))) {
                font = "ALTERNATIVE";
            }

            String old_language = DBTools.selectSettingValue("language");

            if (old_language == null) {
                old_language = MainPanel.DEFAULT_LANGUAGE;
            }

            String language = (String) this.language_combo.getSelectedItem();

            if (language.equals(LabelTranslatorSingleton.getInstance().translate("English"))) {
                language = "EN";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Spanish"))) {
                language = "ES";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Italian"))) {
                language = "IT";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("German"))) {
                language = "GE";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Hungarian"))) {
                language = "HU";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Turkish"))) {
                language = "TU";
                font = "DEFAULT";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Chinese"))) {
                language = "CH";
                font = "ALTERNATIVE";
            } else if (language.equals(LabelTranslatorSingleton.getInstance().translate("Vietnamese"))) {
                language = "VI";
            }

            settings.put("font", font);

            settings.put("language", language);

            String old_zoom = DBTools.selectSettingValue("font_zoom");

            if (old_zoom == null) {

                old_zoom = String.valueOf(Math.round(100 * MainPanel.ZOOM_FACTOR));
            }

            final String zoom = String.valueOf(this.zoom_spinner.getValue());

            boolean old_dark_mode = false;

            final String dark_mode_val = DBTools.selectSettingValue("dark_mode");

            if (dark_mode_val != null) {
                old_dark_mode = (dark_mode_val.equals("yes"));
            }

            final boolean dark_mode = this.dark_mode_checkbox.isSelected();

            boolean old_use_proxy = false;

            final String use_proxy_val = DBTools.selectSettingValue("use_proxy");

            if (use_proxy_val != null) {
                old_use_proxy = (use_proxy_val.equals("yes"));
            }

            final boolean use_proxy = this.use_proxy_checkbox.isSelected();

            String old_proxy_host = DBTools.selectSettingValue("proxy_host");

            if (old_proxy_host == null) {

                old_proxy_host = "";
            }

            final String proxy_host = this.proxy_host_textfield.getText().trim();

            String old_proxy_port = DBTools.selectSettingValue("proxy_port");

            if (old_proxy_port == null) {

                old_proxy_port = "";
            }

            final String proxy_port = this.proxy_port_textfield.getText().trim();

            String old_proxy_user = DBTools.selectSettingValue("proxy_user");

            if (old_proxy_user == null) {

                old_proxy_user = "";
            }

            final String proxy_user = this.proxy_user_textfield.getText().trim();

            String old_proxy_pass = DBTools.selectSettingValue("proxy_pass");

            if (old_proxy_pass == null) {

                old_proxy_pass = "";
            }

            final String proxy_pass = new String(this.proxy_pass_textfield.getPassword());

            String old_debug_file = DBTools.selectSettingValue("debug_file");

            if (old_debug_file == null) {

                old_debug_file = "no";
            }

            final String debug_file = this.debug_file_checkbox.isSelected() ? "yes" : "no";

            settings.put("debug_file", debug_file);
            settings.put("use_proxy", use_proxy ? "yes" : "no");
            settings.put("proxy_host", proxy_host);
            settings.put("proxy_port", proxy_port);
            settings.put("proxy_user", proxy_user);
            settings.put("proxy_pass", proxy_pass);
            settings.put("font_zoom", zoom);

            insertSettingsValues(settings);

            if (!debug_file.equals(old_debug_file)
                    || !font.equals(old_font)
                    || !language.equals(old_language)
                    || !zoom.equals(old_zoom)
                    || use_proxy != old_use_proxy
                    || !proxy_host.equals(old_proxy_host)
                    || !proxy_port.equals(old_proxy_port)
                    || !proxy_user.equals(old_proxy_user)
                    || !proxy_pass.equals(old_proxy_pass)
                    || dark_mode != old_dark_mode) {

                this._main_panel.setRestart(true);
            }

            this.save_button.setEnabled(false);

            this.cancel_button.setEnabled(false);

            this.remove_mega_account_button.setEnabled(false);

            this.add_mega_account_button.setEnabled(false);

            this.delete_all_accounts_button.setEnabled(false);

            this.encrypt_pass_checkbox.setEnabled(false);

            if (this.elc_accounts_table.isEnabled()) {

                final DefaultTableModel model = (DefaultTableModel) this.elc_accounts_table.getModel();

                for (int i = 0; i < model.getRowCount(); i++) {

                    final String host_table = ((String) model.getValueAt(i, 0)).trim().replaceAll("^(https?://)?([^/]+).*$", "$2");

                    String user_table = (String) model.getValueAt(i, 1);

                    String apikey_table = (String) model.getValueAt(i, 2);

                    if (!host_table.isEmpty() && !user_table.isEmpty() && !apikey_table.isEmpty()) {

                        if (this._main_panel.getElc_accounts().get(host_table) == null) {

                            if (this._main_panel.getMaster_pass_hash() != null) {

                                user_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                apikey_table = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                            }

                            DBTools.insertELCAccount(host_table, user_table, apikey_table);

                        } else {

                            final HashMap<String, Object> elc_account_data = (HashMap) this._main_panel.getElc_accounts().get(host_table);

                            String user = (String) elc_account_data.get("user");

                            String apikey = (String) elc_account_data.get("apikey");

                            if (this._main_panel.getMaster_pass() != null) {

                                try {

                                    user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(user), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                    apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(apikey), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                } catch (final Exception ex) {
                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }
                            }

                            if (!user.equals(user_table) || !apikey.equals(apikey_table)) {

                                user = user_table;

                                apikey = apikey_table;

                                if (this._main_panel.getMaster_pass() != null) {

                                    user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user_table.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                    apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey_table.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                }

                                DBTools.insertELCAccount(host_table, user, apikey);
                            }
                        }
                    }
                }
            }

            if (this.mega_accounts_table.isEnabled()) {

                final DefaultTableModel model = (DefaultTableModel) this.mega_accounts_table.getModel();

                final int model_row_count = model.getRowCount();

                this.status.setText(LabelTranslatorSingleton.getInstance().translate("Checking your MEGA accounts, please wait..."));

                this.save_button.setEnabled(false);

                this.cancel_button.setEnabled(true);

                this.import_mega_button.setEnabled(false);

                this.remove_mega_account_button.setEnabled(false);

                this.remove_elc_account_button.setEnabled(false);

                this.add_mega_account_button.setEnabled(false);

                this.add_elc_account_button.setEnabled(false);

                this.delete_all_accounts_button.setEnabled(false);

                this.mega_accounts_table.setEnabled(false);

                this.elc_accounts_table.setEnabled(false);

                this.encrypt_pass_checkbox.setEnabled(false);

                final Dialog tthis = this;

                THREAD_POOL.execute(() -> {
                    final ArrayList<String> email_error = new ArrayList<>();
                    final ArrayList<String> new_valid_mega_accounts = new ArrayList<>();
                    for (int i = 0; i < model_row_count && !this._exit; i++) {

                        final String email = (String) model.getValueAt(i, 0);

                        final String pass = (String) model.getValueAt(i, 1);

                        final int j = i;

                        MiscTools.GUIRun(() -> {

                            this.status.setText(LabelTranslatorSingleton.getInstance().translate("Checking your MEGA accounts, please wait... ") + email + " (" + String.valueOf(j + 1) + "/" + String.valueOf(model_row_count) + ")");

                        });

                        if (!email.isEmpty() && !pass.isEmpty()) {

                            new_valid_mega_accounts.add(email);

                            final MegaAPI ma;

                            if (this._main_panel.getMega_accounts().get(email) == null) {

                                ma = new MegaAPI();

                                try {

                                    String pincode = null;

                                    boolean error_2FA = false;

                                    if (!this._main_panel.getMega_active_accounts().containsKey(email) && ma.check2FA(email)) {

                                        final Get2FACode dialog = new Get2FACode((Frame) this.getParent(), true, email, this._main_panel);

                                        dialog.setLocationRelativeTo(tthis);

                                        dialog.setVisible(true);

                                        if (dialog.isCode_ok()) {
                                            pincode = dialog.getPin_code();
                                        } else {
                                            error_2FA = true;
                                        }
                                    }

                                    if (!error_2FA) {
                                        if (!this._main_panel.getMega_active_accounts().containsKey(email)) {
                                            ma.login(email, pass, pincode);

                                            final ByteArrayOutputStream bs = new ByteArrayOutputStream();

                                            try (final ObjectOutputStream os = new ObjectOutputStream(bs)) {
                                                os.writeObject(ma);
                                            }

                                            if (this._main_panel.getMaster_pass() != null) {

                                                DBTools.insertMegaSession(email, CryptTools.aes_cbc_encrypt_pkcs7(bs.toByteArray(), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), true);

                                            } else {

                                                DBTools.insertMegaSession(email, bs.toByteArray(), false);
                                            }

                                            this._main_panel.getMega_active_accounts().put(email, ma);

                                            String password = pass, password_aes = Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash = ma.getUser_hash();

                                            if (this._main_panel.getMaster_pass_hash() != null) {

                                                password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                            }

                                            DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                        }

                                    } else {
                                        email_error.add(email);
                                    }

                                } catch (final Exception ex) {

                                    email_error.add(email);
                                    LOG.log(Level.SEVERE, ex.getMessage());
                                }

                            } else {

                                final HashMap<String, Object> mega_account_data = (HashMap) this._main_panel.getMega_accounts().get(email);

                                String password = (String) mega_account_data.get("password");

                                if (this._main_panel.getMaster_pass() != null) {

                                    try {

                                        password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin(password), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                                    } catch (final Exception ex) {
                                        LOG.log(Level.SEVERE, ex.getMessage());
                                    }
                                }

                                if (!password.equals(pass)) {

                                    ma = new MegaAPI();

                                    try {

                                        String pincode = null;

                                        boolean error_2FA = false;

                                        if (!this._main_panel.getMega_active_accounts().containsKey(email) && ma.check2FA(email)) {

                                            final Get2FACode dialog = new Get2FACode((Frame) this.getParent(), true, email, this._main_panel);

                                            dialog.setLocationRelativeTo(tthis);

                                            dialog.setVisible(true);

                                            if (dialog.isCode_ok()) {
                                                pincode = dialog.getPin_code();
                                            } else {
                                                error_2FA = true;
                                            }
                                        }

                                        if (!error_2FA) {
                                            if (!this._main_panel.getMega_active_accounts().containsKey(email)) {
                                                ma.login(email, pass, pincode);

                                                final ByteArrayOutputStream bs = new ByteArrayOutputStream();

                                                try (final ObjectOutputStream os = new ObjectOutputStream(bs)) {
                                                    os.writeObject(ma);
                                                }

                                                if (this._main_panel.getMaster_pass() != null) {

                                                    DBTools.insertMegaSession(email, CryptTools.aes_cbc_encrypt_pkcs7(bs.toByteArray(), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), true);

                                                } else {

                                                    DBTools.insertMegaSession(email, bs.toByteArray(), false);
                                                }

                                                this._main_panel.getMega_active_accounts().put(email, ma);

                                                password = pass;

                                                String password_aes = Bin2BASE64(i32a2bin(ma.getPassword_aes())), user_hash = ma.getUser_hash();

                                                if (this._main_panel.getMaster_pass() != null) {

                                                    password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(pass.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                    password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(i32a2bin(ma.getPassword_aes()), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                                                    user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(UrlBASE642Bin(ma.getUser_hash()), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                                                }

                                                DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                                            }
                                        } else {
                                            email_error.add(email);
                                        }

                                    } catch (final Exception ex) {

                                        email_error.add(email);
                                        LOG.log(Level.SEVERE, ex.getMessage());

                                    }
                                }
                            }
                        }
                    }
                    if (!this._exit) {
                        if (email_error.size() > 0) {
                            String email_error_s = "";
                            email_error_s = email_error.stream().map((s) -> s + "\n").reduce(email_error_s, String::concat);
                            final String final_email_error = email_error_s;
                            MiscTools.GUIRun(() -> {
                                this.status.setText("");

                                JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("There were errors with some accounts (email and/or password are/is wrong). Please, check them:\n\n") + final_email_error, "Mega Account Check Error", JOptionPane.ERROR_MESSAGE);

                                this.save_button.setEnabled(true);

                                this.cancel_button.setEnabled(true);

                                this.panel_tabs.setEnabled(true);

                                this.import_mega_button.setEnabled(true);

                                this.remove_mega_account_button.setEnabled(this.mega_accounts_table.getModel().getRowCount() > 0);

                                this.remove_elc_account_button.setEnabled(this.elc_accounts_table.getModel().getRowCount() > 0);

                                this.add_mega_account_button.setEnabled(true);

                                this.add_elc_account_button.setEnabled(true);

                                this.mega_accounts_table.setEnabled(true);

                                this.elc_accounts_table.setEnabled(true);

                                this.delete_all_accounts_button.setEnabled(true);

                                this.encrypt_pass_checkbox.setEnabled(true);

                                this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                            });
                        } else {
                            this._main_panel.getMega_accounts().entrySet().stream().map((entry) -> entry.getKey()).filter((email) -> (!new_valid_mega_accounts.contains(email))).forEachOrdered((email) -> {
                                this._deleted_mega_accounts.add(email);
                            });
                            MiscTools.GUIRun(() -> {
                                this.status.setText("");
                                JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("Settings successfully saved!"), LabelTranslatorSingleton.getInstance().translate("Settings saved"), JOptionPane.INFORMATION_MESSAGE);
                                this._settings_ok = true;
                                this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                                this.setVisible(false);
                            });
                        }
                    }
                });

            } else {

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully saved!"), LabelTranslatorSingleton.getInstance().translate("Settings saved"), JOptionPane.INFORMATION_MESSAGE);
                this._settings_ok = true;
                this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                this.setVisible(false);
            }

        } catch (final SQLException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                       InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        } catch (final Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }
    }//GEN-LAST:event_save_buttonActionPerformed

    private void add_elc_account_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_elc_account_buttonActionPerformed

        final DefaultTableModel model = (DefaultTableModel) this.elc_accounts_table.getModel();

        model.addRow(new Object[]{"", "", ""});

        this.elc_accounts_table.clearSelection();

        this.remove_elc_account_button.setEnabled(true);
    }//GEN-LAST:event_add_elc_account_buttonActionPerformed

    private void remove_elc_account_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_elc_account_buttonActionPerformed

        final DefaultTableModel model = (DefaultTableModel) this.elc_accounts_table.getModel();

        int selected = this.elc_accounts_table.getSelectedRow();

        while (selected >= 0) {

            final String host = (String) model.getValueAt(this.elc_accounts_table.convertRowIndexToModel(selected), 0);

            this._deleted_elc_accounts.add(host);

            model.removeRow(this.elc_accounts_table.convertRowIndexToModel(selected));

            selected = this.elc_accounts_table.getSelectedRow();
        }

        this.elc_accounts_table.clearSelection();

        if (model.getRowCount() == 0) {

            this.remove_elc_account_button.setEnabled(false);
        }
    }//GEN-LAST:event_remove_elc_account_buttonActionPerformed

    private void unlock_accounts_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlock_accounts_buttonActionPerformed

        this.unlock_accounts_button.setEnabled(false);

        final Dialog tthis = this;

        MiscTools.GUIRun(() -> {
            final GetMasterPasswordDialog dialog = new GetMasterPasswordDialog((Frame) this.getParent(), true, this._main_panel.getMaster_pass_hash(), this._main_panel.getMaster_pass_salt(), this._main_panel);

            dialog.setLocationRelativeTo(tthis);

            dialog.setVisible(true);

            if (dialog.isPass_ok()) {

                this._main_panel.setMaster_pass(dialog.getPass());

                dialog.deletePass();

                final DefaultTableModel mega_model = new DefaultTableModel(new Object[][]{}, new String[]{"Email", "Password"});

                final DefaultTableModel elc_model = new DefaultTableModel(new Object[][]{}, new String[]{"Host", "User", "API KEY"});

                this.mega_accounts_table.setModel(mega_model);

                this.elc_accounts_table.setModel(elc_model);

                this.encrypt_pass_checkbox.setEnabled(true);

                this.mega_accounts_table.setEnabled(true);

                this.elc_accounts_table.setEnabled(true);

                this.remove_mega_account_button.setEnabled(true);

                this.remove_elc_account_button.setEnabled(true);

                this.add_mega_account_button.setEnabled(true);

                this.add_elc_account_button.setEnabled(true);

                this.unlock_accounts_button.setVisible(false);

                this.delete_all_accounts_button.setEnabled(true);

                this._main_panel.getMega_accounts().entrySet().stream().map((pair) -> {
                    final HashMap<String, Object> data = (HashMap) pair.getValue();
                    String pass = null;
                    try {

                        pass = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                    } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                                   InvalidAlgorithmParameterException | IllegalBlockSizeException |
                                   BadPaddingException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    } catch (final Exception ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }
                    final String[] new_row_data = {(String) pair.getKey(), pass};
                    return new_row_data;
                }).forEachOrdered((new_row_data) -> {
                    mega_model.addRow(new_row_data);
                });
                this._main_panel.getElc_accounts().entrySet().stream().map((pair) -> {
                    final HashMap<String, Object> data = (HashMap) pair.getValue();
                    String user = null, apikey = null;
                    try {

                        user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                        apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV), "UTF-8");

                    } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                                   InvalidAlgorithmParameterException | IllegalBlockSizeException |
                                   BadPaddingException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    } catch (final Exception ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }
                    final String[] new_row_data = {(String) pair.getKey(), user, apikey};
                    return new_row_data;
                }).forEachOrdered((new_row_data) -> {
                    elc_model.addRow(new_row_data);
                });

                this.mega_accounts_table.setAutoCreateRowSorter(true);
                final DefaultRowSorter sorter_mega = ((DefaultRowSorter) this.mega_accounts_table.getRowSorter());
                final ArrayList list_mega = new ArrayList();
                list_mega.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                sorter_mega.setSortKeys(list_mega);
                sorter_mega.sort();

                this.elc_accounts_table.setAutoCreateRowSorter(true);
                final DefaultRowSorter sorter_elc = ((DefaultRowSorter) this.elc_accounts_table.getRowSorter());
                final ArrayList list_elc = new ArrayList();
                list_elc.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
                sorter_elc.setSortKeys(list_elc);
                sorter_elc.sort();

            }

            this._remember_master_pass = dialog.getRemember_checkbox().isSelected();

            dialog.dispose();

            this.unlock_accounts_button.setEnabled(true);
        });
    }//GEN-LAST:event_unlock_accounts_buttonActionPerformed

    private void delete_all_accounts_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_all_accounts_buttonActionPerformed

        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("Master password will be reset and all your accounts will be removed. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("RESET ACCOUNTS"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {

            try {
                this.encrypt_pass_checkbox.setEnabled(true);

                this.mega_accounts_table.setEnabled(true);

                this.elc_accounts_table.setEnabled(true);

                this.remove_mega_account_button.setEnabled(true);

                this.remove_elc_account_button.setEnabled(true);

                this.add_mega_account_button.setEnabled(true);

                this.add_elc_account_button.setEnabled(true);

                this.unlock_accounts_button.setVisible(false);

                this.delete_all_accounts_button.setVisible(true);

                final DefaultTableModel new_mega_model = new DefaultTableModel(new Object[][]{}, new String[]{"Email", "Password"});

                final DefaultTableModel new_elc_model = new DefaultTableModel(new Object[][]{}, new String[]{"Host", "User", "API KEY"});

                this.mega_accounts_table.setModel(new_mega_model);

                this.elc_accounts_table.setModel(new_elc_model);

                DBTools.truncateMegaAccounts();

                DBTools.truncateELCAccounts();

                DBTools.truncateMegaSessions();

                this._main_panel.setMaster_pass_hash(null);

                this._main_panel.setMaster_pass(null);

                insertSettingValue("master_pass_hash", null);

                this.encrypt_pass_checkbox.setSelected(false);

                this._main_panel.getMega_accounts().clear();

                this._main_panel.getMega_active_accounts().clear();

                this._main_panel.getElc_accounts().clear();

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Accounts successfully reset!"), LabelTranslatorSingleton.getInstance().translate("Accounts reset"), JOptionPane.INFORMATION_MESSAGE);

                this.setVisible(false);

            } catch (final SQLException ex) {
                LOG.log(Level.SEVERE, ex.getMessage());
            }
        }
    }//GEN-LAST:event_delete_all_accounts_buttonActionPerformed

    private void encrypt_pass_checkboxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encrypt_pass_checkboxActionPerformed

        this.encrypt_pass_checkbox.setEnabled(false);

        final Dialog tthis = this;

        MiscTools.GUIRun(() -> {
            final SetMasterPasswordDialog dialog = new SetMasterPasswordDialog((Frame) this.getParent(), true, this._main_panel.getMaster_pass_salt(), this._main_panel);

            dialog.setLocationRelativeTo(tthis);

            dialog.setVisible(true);

            byte[] old_master_pass = null;

            if (this._main_panel.getMaster_pass() != null) {

                old_master_pass = new byte[this._main_panel.getMaster_pass().length];

                System.arraycopy(this._main_panel.getMaster_pass(), 0, old_master_pass, 0, this._main_panel.getMaster_pass().length);
            }

            final String old_master_pass_hash = this._main_panel.getMaster_pass_hash();

            if (dialog.isPass_ok()) {

                try {

                    DBTools.truncateMegaSessions();

                    if (dialog.getNew_pass() != null && dialog.getNew_pass().length > 0) {

                        this._main_panel.setMaster_pass_hash(dialog.getNew_pass_hash());

                        this._main_panel.setMaster_pass(dialog.getNew_pass());

                    } else {

                        this._main_panel.setMaster_pass_hash(null);

                        this._main_panel.setMaster_pass(null);
                    }

                    dialog.deleteNewPass();

                    insertSettingValue("master_pass_hash", this._main_panel.getMaster_pass_hash());

                    for (final Map.Entry pair : this._main_panel.getMega_accounts().entrySet()) {

                        final HashMap<String, Object> data = (HashMap) pair.getValue();

                        final String email;
                        String password;
                        String password_aes;
                        String user_hash;

                        email = (String) pair.getKey();

                        if (old_master_pass_hash != null) {

                            password = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password")), old_master_pass, CryptTools.AES_ZERO_IV), "UTF-8");

                            password_aes = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("password_aes")), old_master_pass, CryptTools.AES_ZERO_IV));

                            user_hash = Bin2BASE64(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user_hash")), old_master_pass, CryptTools.AES_ZERO_IV));

                        } else {

                            password = (String) data.get("password");

                            password_aes = (String) data.get("password_aes");

                            user_hash = (String) data.get("user_hash");
                        }

                        if (this._main_panel.getMaster_pass() != null) {

                            password = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(password.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            password_aes = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(password_aes), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            user_hash = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(BASE642Bin(user_hash.replace('-', '+').replace('_', '/')), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                        }

                        data.put("password", password);

                        data.put("password_aes", password_aes);

                        data.put("user_hash", user_hash);

                        DBTools.insertMegaAccount(email, password, password_aes, user_hash);
                    }

                    for (final Map.Entry pair : this._main_panel.getElc_accounts().entrySet()) {

                        final HashMap<String, Object> data = (HashMap) pair.getValue();

                        final String host;
                        String user;
                        String apikey;

                        host = (String) pair.getKey();

                        if (old_master_pass_hash != null) {

                            user = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("user")), old_master_pass, CryptTools.AES_ZERO_IV), "UTF-8");

                            apikey = new String(CryptTools.aes_cbc_decrypt_pkcs7(BASE642Bin((String) data.get("apikey")), old_master_pass, CryptTools.AES_ZERO_IV), "UTF-8");

                        } else {

                            user = (String) data.get("user");

                            apikey = (String) data.get("apikey");

                        }

                        if (this._main_panel.getMaster_pass() != null) {

                            user = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(user.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));

                            apikey = Bin2BASE64(CryptTools.aes_cbc_encrypt_pkcs7(apikey.getBytes("UTF-8"), this._main_panel.getMaster_pass(), CryptTools.AES_ZERO_IV));
                        }

                        data.put("user", user);

                        data.put("apikey", apikey);

                        DBTools.insertELCAccount(host, user, apikey);
                    }

                } catch (final Exception ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }

            }

            this.encrypt_pass_checkbox.setSelected((this._main_panel.getMaster_pass_hash() != null));

            dialog.dispose();

            this.encrypt_pass_checkbox.setEnabled(true);
        });
    }//GEN-LAST:event_encrypt_pass_checkboxActionPerformed

    private void add_mega_account_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_mega_account_buttonActionPerformed

        final DefaultTableModel model = (DefaultTableModel) this.mega_accounts_table.getModel();

        model.addRow(new Object[]{"", ""});

        this.mega_accounts_table.clearSelection();
    }//GEN-LAST:event_add_mega_account_buttonActionPerformed

    private void remove_mega_account_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remove_mega_account_buttonActionPerformed

        final DefaultTableModel model = (DefaultTableModel) this.mega_accounts_table.getModel();

        int selected = this.mega_accounts_table.getSelectedRow();

        while (selected >= 0) {

            final String email = (String) model.getValueAt(this.mega_accounts_table.convertRowIndexToModel(selected), 0);

            this._deleted_mega_accounts.add(email);

            model.removeRow(this.mega_accounts_table.convertRowIndexToModel(selected));

            selected = this.mega_accounts_table.getSelectedRow();
        }

        this.mega_accounts_table.clearSelection();

        if (model.getRowCount() == 0) {

            this.remove_mega_account_button.setEnabled(false);
        }

    }//GEN-LAST:event_remove_mega_account_buttonActionPerformed

    private void limit_upload_speed_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_upload_speed_checkboxStateChanged

        this.max_up_speed_label.setEnabled(this.limit_upload_speed_checkbox.isSelected());
        this.max_up_speed_spinner.setEnabled(this.limit_upload_speed_checkbox.isSelected());
    }//GEN-LAST:event_limit_upload_speed_checkboxStateChanged

    private void run_command_test_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_command_test_buttonActionPerformed
        // TODO add your handling code here:

        if (this.run_command_textbox.getText() != null && !"".equals(this.run_command_textbox.getText().trim())) {

            try {
                Runtime.getRuntime().exec(this.run_command_textbox.getText().trim());
            } catch (final IOException ex) {
                Logger.getLogger(MiscTools.class.getName()).log(Level.SEVERE, ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_run_command_test_buttonActionPerformed

    private void run_command_checkboxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_run_command_checkboxActionPerformed
        // TODO add your handling code here:

        this.run_command_textbox.setEnabled(this.run_command_checkbox.isSelected());
    }//GEN-LAST:event_run_command_checkboxActionPerformed

    private void custom_chunks_dir_checkboxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custom_chunks_dir_checkboxActionPerformed

        if (this.custom_chunks_dir_checkbox.isSelected()) {

            this.custom_chunks_dir_button.setEnabled(true);
            this.custom_chunks_dir_current_label.setEnabled(true);

        } else {

            this.custom_chunks_dir_button.setEnabled(false);
            this.custom_chunks_dir_current_label.setEnabled(false);

        }
    }//GEN-LAST:event_custom_chunks_dir_checkboxActionPerformed

    private void custom_chunks_dir_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custom_chunks_dir_buttonActionPerformed
        final javax.swing.JFileChooser filechooser = new javax.swing.JFileChooser();
//        updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.25));

        filechooser.setCurrentDirectory(new java.io.File(this._download_path));
        filechooser.setDialogTitle("Temporary chunks directory");
        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            final File file = filechooser.getSelectedFile();

            this._custom_chunks_dir = file.getAbsolutePath();

            this.custom_chunks_dir_current_label.setText(truncateText(this._custom_chunks_dir, 80));
        }
    }//GEN-LAST:event_custom_chunks_dir_buttonActionPerformed

    private void jButton1ActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:

        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("ALL YOUR SETTINGS, ACCOUNTS AND TRANSFERENCES WILL BE REMOVED. (THIS CAN'T BE UNDONE)\n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("RESET MEGABASTERD"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {

            this.setVisible(false);
            this._main_panel.byebyenow(true, true);

        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void export_settings_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_settings_buttonActionPerformed

        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("Only SAVED settings and accounts will be exported. (If you are unsure, it is better to save your current settings and then export them).\n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("EXPORT SETTINGS"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            final JFileChooser filechooser = new JFileChooser();
//            updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.25));
            filechooser.setCurrentDirectory(new File(this._download_path));
            filechooser.setDialogTitle("Save as");

            if (filechooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

                final File file = filechooser.getSelectedFile();

                try {

                    if (file.exists()) {
                        file.delete();
                    }

                    file.createNewFile();

                    try (final BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file)); final ObjectOutputStream oos = new ObjectOutputStream(fos)) {

                        final HashMap<String, Object> settings = new HashMap<>();

                        settings.put("settings", selectSettingsValues());

                        settings.put("mega_accounts", selectMegaAccounts());

                        settings.put("elc_accounts", selectELCAccounts());

                        oos.writeObject(settings);

                        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully exported!"), LabelTranslatorSingleton.getInstance().translate("Settings exported"), JOptionPane.INFORMATION_MESSAGE);

                        this.setVisible(false);

                    } catch (final SQLException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } catch (final IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }
        }
    }//GEN-LAST:event_export_settings_buttonActionPerformed

    private void import_settings_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_settings_buttonActionPerformed

        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this,
                LabelTranslatorSingleton.getInstance().translate("All your current settings and accounts will be deleted after import. (It is recommended to export your current settings before importing). \n\nDo you want to continue?"),
                LabelTranslatorSingleton.getInstance().translate("IMPORT SETTINGS"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            final JFileChooser filechooser = new JFileChooser();
//            updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.25));
            filechooser.setCurrentDirectory(new File(this._download_path));
            filechooser.setDialogTitle("Select settings file");

            if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                final File file = filechooser.getSelectedFile();

                try {

                    try (final InputStream fis = new BufferedInputStream(new FileInputStream(file)); final ObjectInputStream ois = new ObjectInputStream(fis)) {

                        final HashMap<String, Object> settings = (HashMap<String, Object>) ois.readObject();

                        insertSettingsValues((HashMap<String, Object>) settings.get("settings"));

                        insertMegaAccounts((HashMap<String, Object>) settings.get("mega_accounts"));

                        insertELCAccounts((HashMap<String, Object>) settings.get("elc_accounts"));

                        this._main_panel.loadUserSettings();

                        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Settings successfully imported!"), LabelTranslatorSingleton.getInstance().translate("Settings imported"), JOptionPane.INFORMATION_MESSAGE);

                        this._settings_ok = true;

                        this.setVisible(false);

                    } catch (final SQLException | ClassNotFoundException ex) {
                        LOG.log(Level.SEVERE, ex.getMessage());
                    }

                } catch (final IOException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage());
                }
            }
        }
    }//GEN-LAST:event_import_settings_buttonActionPerformed

    private void use_proxy_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_use_proxy_checkboxStateChanged

        this.proxy_host_label.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_host_textfield.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_port_label.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_port_textfield.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_user_label.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_user_textfield.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_pass_label.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_pass_textfield.setEnabled(this.use_proxy_checkbox.isSelected());
        this.proxy_warning_label.setEnabled(this.use_proxy_checkbox.isSelected());

        if (this.use_proxy_checkbox.isSelected()) {
            this.smart_proxy_checkbox.setSelected(false);
        }
    }//GEN-LAST:event_use_proxy_checkboxStateChanged

    private void multi_slot_down_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_multi_slot_down_checkboxStateChanged

        if (!this.multi_slot_down_checkbox.isSelected() && !this.smart_proxy_checkbox.isSelected()) {

            this.default_slots_down_spinner.setEnabled(false);
            this.default_slots_down_label.setEnabled(false);
            this.rec_download_slots_label.setEnabled(false);

        } else {

            this.default_slots_down_spinner.setEnabled(true);
            this.default_slots_down_label.setEnabled(true);
            this.multi_slot_down_checkbox.setSelected(true);
            this.rec_download_slots_label.setEnabled(true);
        }
    }//GEN-LAST:event_multi_slot_down_checkboxStateChanged

    private void change_download_dir_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_change_download_dir_buttonActionPerformed

        final javax.swing.JFileChooser filechooser = new javax.swing.JFileChooser();
//        updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.25));

        filechooser.setCurrentDirectory(new java.io.File(this._download_path));
        filechooser.setDialogTitle("Default download directory");
        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

            final File file = filechooser.getSelectedFile();

            this._download_path = file.getAbsolutePath();

            this.default_dir_label.setText(truncateText(this._download_path, 80));
        }
    }//GEN-LAST:event_change_download_dir_buttonActionPerformed

    private void use_mega_account_down_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_use_mega_account_down_checkboxStateChanged

        if (!this.use_mega_account_down_checkbox.isSelected()) {

            this.use_mega_account_down_combobox.setEnabled(false);

            this.use_mega_label.setEnabled(false);

        } else {

            this.use_mega_account_down_combobox.setEnabled(true);

            this.use_mega_label.setEnabled(true);

            this.use_mega_account_down_combobox.removeAllItems();

            if (this._main_panel.getMega_accounts().size() > 0) {

                this._main_panel.getMega_accounts().keySet().forEach((o) -> {
                    this.use_mega_account_down_combobox.addItem(o);
                });

                final String use_mega_account_down = DBTools.selectSettingValue("mega_account_down");

                if (use_mega_account_down != null) {

                    this.use_mega_account_down_combobox.setSelectedItem(use_mega_account_down);
                }

            } else {

                this.use_mega_account_down_combobox.setEnabled(false);

                this.use_mega_label.setEnabled(false);

                this.use_mega_account_down_checkbox.setSelected(false);
            }
        }
    }//GEN-LAST:event_use_mega_account_down_checkboxStateChanged

    private void smart_proxy_checkboxMouseClicked(final java.awt.event.MouseEvent evt) {//GEN-FIRST:event_smart_proxy_checkboxMouseClicked
        // TODO add your handling code here:
        if (this.smart_proxy_checkbox.isSelected()) {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Using proxies or VPN to bypass MEGA's daily download limitation may violate its Terms of Use.\n\nUSE THIS OPTION AT YOUR OWN RISK."), LabelTranslatorSingleton.getInstance().translate("WARNING"), JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_smart_proxy_checkboxMouseClicked

    private void smart_proxy_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_smart_proxy_checkboxStateChanged

//        MiscTools.containerSetEnabled(this.smart_proxy_settings, this.smart_proxy_checkbox.isSelected());
        this.revalidate();
        this.repaint();

    }//GEN-LAST:event_smart_proxy_checkboxStateChanged

    private void limit_download_speed_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_limit_download_speed_checkboxStateChanged

        if (!this.limit_download_speed_checkbox.isSelected()) {

            this.max_down_speed_label.setEnabled(false);
            this.max_down_speed_spinner.setEnabled(false);
        } else {
            this.max_down_speed_label.setEnabled(true);
            this.max_down_speed_spinner.setEnabled(true);
        }
    }//GEN-LAST:event_limit_download_speed_checkboxStateChanged

    private void megacrypter_reverse_checkboxStateChanged(final javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_megacrypter_reverse_checkboxStateChanged

        this.megacrypter_reverse_port_label.setEnabled(this.megacrypter_reverse_checkbox.isSelected());
        this.megacrypter_reverse_port_spinner.setEnabled(this.megacrypter_reverse_checkbox.isSelected());
        this.megacrypter_reverse_warning_label.setEnabled(this.megacrypter_reverse_checkbox.isSelected());

    }//GEN-LAST:event_megacrypter_reverse_checkboxStateChanged

    private void import_mega_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_mega_buttonActionPerformed
        // TODO add your handling code here:

        if (!this.unlock_accounts_button.isVisible() || !this.unlock_accounts_button.isEnabled()) {

            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("EMAIL1#PASS1\nEMAIL2#PASS2"), "TXT FILE FORMAT", JOptionPane.INFORMATION_MESSAGE);

            final javax.swing.JFileChooser filechooser = new javax.swing.JFileChooser();

//            updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.25));

            filechooser.setDialogTitle("Select MEGA ACCOUNTS FILE");

            filechooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);

            filechooser.addChoosableFileFilter(new FileNameExtensionFilter("TXT", "txt"));

            filechooser.setAcceptAllFileFilterUsed(false);

            if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

                try {
                    final File file = filechooser.getSelectedFile();

                    final Stream<String> filter = Files.lines(file.toPath()).map(s -> s.trim()).filter(s -> !s.isEmpty());

                    final List<String> result = filter.collect(Collectors.toList());

                    final DefaultTableModel model = (DefaultTableModel) this.mega_accounts_table.getModel();

                    for (final String line : result) {

                        final String email = MiscTools.findFirstRegex("^[^#]+", line, 0).trim();
                        final String pass = MiscTools.findFirstRegex("^[^#]+#(.+)$", line, 1);
                        model.addRow(new Object[]{email, pass});
                    }

                    this.mega_accounts_table.setModel(model);

                } catch (final IOException ex) {
                    Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        } else {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("MEGA ACCOUNTS ARE LOCKED"), "ERROR", JOptionPane.ERROR_MESSAGE);

        }
    }//GEN-LAST:event_import_mega_buttonActionPerformed

    private void upload_public_folder_checkboxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upload_public_folder_checkboxActionPerformed
        // TODO add your handling code here:
        if (this.upload_public_folder_checkbox.isSelected()) {
            JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Using this option may irreversibly corrupt your uploads.\n\nUSE IT AT YOUR OWN RISK"), LabelTranslatorSingleton.getInstance().translate("WARNING"), JOptionPane.WARNING_MESSAGE);

        }

        this.upload_public_folder_checkbox.setBackground(this.upload_public_folder_checkbox.isSelected() ? java.awt.Color.RED : null);

        this.public_folder_panel.setVisible(this.upload_public_folder_checkbox.isSelected());

        this.revalidate();

        this.repaint();

    }//GEN-LAST:event_upload_public_folder_checkboxActionPerformed

    private void proxy_random_radioActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proxy_random_radioActionPerformed
        // TODO add your handling code here:
        this.proxy_random_radio.setSelected(true);
        this.proxy_sequential_radio.setSelected(false);

    }//GEN-LAST:event_proxy_random_radioActionPerformed

    private void proxy_sequential_radioActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proxy_sequential_radioActionPerformed
        // TODO add your handling code here:
        this.proxy_sequential_radio.setSelected(true);
        this.proxy_random_radio.setSelected(false);

    }//GEN-LAST:event_proxy_sequential_radioActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accounts_panel;
    private javax.swing.JButton add_elc_account_button;
    private javax.swing.JButton add_mega_account_button;
    private javax.swing.JPanel advanced_panel;
    private javax.swing.JScrollPane advanced_scrollpane;
    private javax.swing.JSpinner auto_refresh_proxy_time_spinner;
    private javax.swing.JSpinner bad_proxy_time_spinner;
    private javax.swing.JButton cancel_button;
    private javax.swing.JButton change_download_dir_button;
    private javax.swing.JCheckBox clipboardspy_checkbox;
    private javax.swing.JButton custom_chunks_dir_button;
    private javax.swing.JCheckBox custom_chunks_dir_checkbox;
    private javax.swing.JLabel custom_chunks_dir_current_label;
    private javax.swing.JLabel custom_proxy_list_label;
    private javax.swing.JTextArea custom_proxy_textarea;
    private javax.swing.JCheckBox dark_mode_checkbox;
    private javax.swing.JCheckBox debug_file_checkbox;
    private javax.swing.JLabel debug_file_path;
    private javax.swing.JLabel default_dir_label;
    private javax.swing.JLabel default_slots_down_label;
    private javax.swing.JSpinner default_slots_down_spinner;
    private javax.swing.JLabel default_slots_up_label;
    private javax.swing.JSpinner default_slots_up_spinner;
    private javax.swing.JButton delete_all_accounts_button;
    private javax.swing.JLabel down_dir_label;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JScrollPane downloads_scrollpane;
    private javax.swing.JLabel elc_accounts_label;
    private javax.swing.JScrollPane elc_accounts_scrollpane;
    private javax.swing.JTable elc_accounts_table;
    private javax.swing.JCheckBox encrypt_pass_checkbox;
    private javax.swing.JButton export_settings_button;
    private javax.swing.JComboBox<String> font_combo;
    private javax.swing.JLabel font_label;
    private javax.swing.JCheckBox force_smart_proxy_checkbox;
    private javax.swing.JButton import_mega_button;
    private javax.swing.JButton import_settings_button;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox<String> language_combo;
    private javax.swing.JCheckBox limit_download_speed_checkbox;
    private javax.swing.JCheckBox limit_upload_speed_checkbox;
    private javax.swing.JLabel max_down_speed_label;
    private javax.swing.JSpinner max_down_speed_spinner;
    private javax.swing.JLabel max_downloads_label;
    private javax.swing.JSpinner max_downloads_spinner;
    private javax.swing.JLabel max_up_speed_label;
    private javax.swing.JSpinner max_up_speed_spinner;
    private javax.swing.JLabel max_uploads_label;
    private javax.swing.JSpinner max_uploads_spinner;
    private javax.swing.JLabel mega_accounts_label;
    private javax.swing.JScrollPane mega_accounts_scrollpane;
    private javax.swing.JTable mega_accounts_table;
    private javax.swing.JCheckBox megacrypter_reverse_checkbox;
    private javax.swing.JLabel megacrypter_reverse_port_label;
    private javax.swing.JSpinner megacrypter_reverse_port_spinner;
    private javax.swing.JLabel megacrypter_reverse_warning_label;
    private javax.swing.JCheckBox multi_slot_down_checkbox;
    private javax.swing.JTabbedPane panel_tabs;
    private javax.swing.JPanel proxy_auth_panel;
    private javax.swing.JLabel proxy_host_label;
    private javax.swing.JTextField proxy_host_textfield;
    private javax.swing.JPanel proxy_panel;
    private javax.swing.JLabel proxy_pass_label;
    private javax.swing.JPasswordField proxy_pass_textfield;
    private javax.swing.JLabel proxy_port_label;
    private javax.swing.JTextField proxy_port_textfield;
    private javax.swing.JRadioButton proxy_random_radio;
    private javax.swing.JCheckBox proxy_reset_slot_checkbox;
    private javax.swing.JRadioButton proxy_sequential_radio;
    private javax.swing.JSpinner proxy_timeout_spinner;
    private javax.swing.JLabel proxy_user_label;
    private javax.swing.JTextField proxy_user_textfield;
    private javax.swing.JLabel proxy_warning_label;
    private javax.swing.JScrollPane public_folder_panel;
    private javax.swing.JTextArea public_folder_warning;
    private javax.swing.JLabel rec_download_slots_label;
    private javax.swing.JLabel rec_smart_proxy_label;
    private javax.swing.JLabel rec_smart_proxy_label1;
    private javax.swing.JLabel rec_upload_slots_label;
    private javax.swing.JLabel rec_zoom_label;
    private javax.swing.JButton remove_elc_account_button;
    private javax.swing.JButton remove_mega_account_button;
    private javax.swing.JCheckBox run_command_checkbox;
    private javax.swing.JButton run_command_test_button;
    private javax.swing.JTextField run_command_textbox;
    private javax.swing.JButton save_button;
    private javax.swing.JCheckBox smart_proxy_checkbox;
    private javax.swing.JPanel smart_proxy_settings;
    private javax.swing.JCheckBox start_frozen_checkbox;
    private javax.swing.JLabel status;
    private javax.swing.JCheckBox thumbnail_checkbox;
    private javax.swing.JButton unlock_accounts_button;
    private javax.swing.JCheckBox upload_log_checkbox;
    private javax.swing.JCheckBox upload_public_folder_checkbox;
    private javax.swing.JPanel uploads_panel;
    private javax.swing.JScrollPane uploads_scrollpane;
    private javax.swing.JCheckBox use_mega_account_down_checkbox;
    private javax.swing.JComboBox<String> use_mega_account_down_combobox;
    private javax.swing.JLabel use_mega_label;
    private javax.swing.JCheckBox use_proxy_checkbox;
    private javax.swing.JCheckBox verify_file_down_checkbox;
    private javax.swing.JLabel zoom_label;
    private javax.swing.JSpinner zoom_spinner;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(SettingsDialog.class.getName());
}
