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
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.CryptTools.decryptMegaDownloaderLink;
import static com.tonikelope.megabasterd.DBTools.deleteELCAccount;
import static com.tonikelope.megabasterd.DBTools.deleteMegaAccount;
import static com.tonikelope.megabasterd.DBTools.selectSettingValue;
import static com.tonikelope.megabasterd.MainPanel.ICON_FILE;
import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MainPanel.VERSION;
import static com.tonikelope.megabasterd.MiscTools.findAllRegex;
import static com.tonikelope.megabasterd.MiscTools.findFirstRegex;
import static com.tonikelope.megabasterd.MiscTools.genID;
import static com.tonikelope.megabasterd.MiscTools.i32a2bin;
import static java.awt.event.WindowEvent.WINDOW_CLOSING;
import static java.util.logging.Level.SEVERE;
import static javax.swing.JOptionPane.DEFAULT_OPTION;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

/**
 * @author tonikelope
 */
public final class MainPanelView extends javax.swing.JFrame {

    private final MainPanel _main_panel;

    private static volatile MainPanelView INSTANCE = null;

    public JProgressBar getDownload_status_bar() {
        return this.download_status_bar;
    }

    public JProgressBar getUpload_status_bar() {
        return this.upload_status_bar;
    }

    public static MainPanelView getINSTANCE() {
        return INSTANCE;
    }

    public JMenuItem getMerge_file_menu() {
        return this.merge_file_menu;
    }

    public JMenuItem getSplit_file_menu() {
        return this.split_file_menu;
    }

    public JLabel getKiss_server_status() {
        return this.kiss_server_status;
    }

    public JMenuItem getClean_all_down_menu() {
        return this.clean_all_down_menu;
    }

    public JMenuItem getClean_all_up_menu() {
        return this.clean_all_up_menu;
    }

    public JButton getClose_all_finished_down_button() {
        return this.close_all_finished_down_button;
    }

    public JButton getClose_all_finished_up_button() {
        return this.close_all_finished_up_button;
    }

    public JLabel getMemory_status() {
        return this.memory_status;
    }

    public JLabel getGlobal_speed_down_label() {
        return this.global_speed_down_label;
    }

    public JLabel getDown_remtime_label() {
        return this.down_remtime_label;
    }

    public JLabel getUp_remtime_label() {
        return this.up_remtime_label;
    }

    public JLabel getGlobal_speed_up_label() {
        return this.global_speed_up_label;
    }

    public JPanel getjPanel_scroll_down() {
        return this.jPanel_scroll_down;
    }

    public JPanel getjPanel_scroll_up() {
        return this.jPanel_scroll_up;
    }

    public JMenuItem getNew_download_menu() {
        return this.new_download_menu;
    }

    public JMenuItem getNew_upload_menu() {
        return this.new_upload_menu;
    }

    public JButton getPause_all_down_button() {
        return this.pause_all_down_button;
    }

    public JButton getPause_all_up_button() {
        return this.pause_all_up_button;
    }

    public JLabel getStatus_down_label() {
        return this.status_down_label;
    }

    public JLabel getStatus_up_label() {
        return this.status_up_label;
    }

    public JButton getForce_chunk_reset_button() {
        return this.force_chunk_reset_button;
    }

    public JButton getUnfreeze_transferences_button() {
        return this.unfreeze_transferences_button;
    }

    public MainPanel getMain_panel() {
        return this._main_panel;
    }

    public JTabbedPane getjTabbedPane1() {
        return this.jTabbedPane1;
    }

    public JLabel getSmart_proxy_status() {
        return this.smart_proxy_status;
    }

    public JCheckBoxMenuItem getAuto_close_menu() {
        return this.auto_close_menu;
    }

    public JMenuItem getCancel_all_downloads_menu() {
        return this.cancel_all_downloads_menu;
    }

    public void updateKissStreamServerStatus(final String status) {

        MiscTools.GUIRun(() -> {
            final String old_status = this.getKiss_server_status().getText();

            if (!old_status.equals(status + " ")) {

                this.getKiss_server_status().setText(status + " ");

            }
        });
    }

    public void updateSmartProxyStatus(final String status) {

        MiscTools.GUIRun(() -> {
            final String old_status = this.getSmart_proxy_status().getText();

            if (!old_status.equals(status + " ")) {

                this.getSmart_proxy_status().setText(status + " ");

            }
        });
    }

    private void _new_upload_dialog(final FileGrabberDialog dialog) {

        try {

            dialog.setLocationRelativeTo(this);

            dialog.setVisible(true);

            if (dialog.isUpload() && dialog.getFiles().size() > 0) {

                this.getMain_panel().resumeUploads();

                this.getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().addAll(dialog.getFiles());

                this.getMain_panel().getUpload_manager().secureNotify();

                MiscTools.GUIRun(() -> {
                    this.upload_status_bar.setIndeterminate(true);
                    this.upload_status_bar.setValue(this.upload_status_bar.getMinimum());
                    this.upload_status_bar.setMaximum(this.getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().size() + this.getMain_panel().getUpload_manager().getTransference_preprocess_queue().size() + this.getMain_panel().getUpload_manager().getTransference_provision_queue().size());
                    this.upload_status_bar.setVisible(true);
                });

                final String mega_account = (String) dialog.getAccount_combobox().getSelectedItem();

                final String base_path = dialog.getBase_path();

                final String dir_name = dialog.getDir_name_textfield().getText();

                this.jTabbedPane1.setSelectedIndex(1);

                final Runnable run = () -> {

                    final MegaAPI ma = this.getMain_panel().getMega_active_accounts().get(mega_account);

                    try {

                        final byte[] parent_key = ma.genFolderKey();

                        final byte[] share_key = ma.genShareKey();

                        final String root_name = dir_name != null ? dir_name : dialog.getFiles().get(0).getName() + "_" + genID(10);

                        HashMap<String, Object> res = ma.createDir(root_name, ma.getRoot_id(), parent_key, i32a2bin(ma.getMaster_key()));

                        final String parent_node = (String) ((Map) ((List) res.get("f")).get(0)).get("h");

                        LOG.log(Level.INFO, "{0} Dir {1} created", new Object[]{Thread.currentThread().getName(), parent_node});

                        final String upload_folder_string = DBTools.selectSettingValue("upload_public_folder");

                        final boolean folder_share = "yes".equals(upload_folder_string);

                        String folder_link = null;

                        if (folder_share) {

                            ma.shareFolder(parent_node, parent_key, share_key);

                            folder_link = ma.getPublicFolderLink(parent_node, share_key);

                        }

                        if (dialog.getUpload_log_checkbox().isSelected()) {

                            final File upload_log = new File(MiscTools.UPLOAD_LOGS_DIR + "/megabasterd_upload_" + parent_node + ".log");
                            upload_log.createNewFile();

                            final FileWriter fr;
                            try {
                                fr = new FileWriter(upload_log, true);
                                fr.write("***** MegaBasterd UPLOAD LOG FILE *****\n\n");
                                fr.write(MiscTools.getFechaHoraActual() + "\n\n");
                                fr.write(ma.getEmail() + "\n\n");
                                fr.write(dir_name + "   " + folder_link + "\n\n");
                                fr.close();
                            } catch (final IOException ex) {
                                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, ex.getMessage());
                            }
                        }

                        if (folder_share) {
                            res = ma.createDirInsideAnotherSharedDir(root_name, parent_node, ma.genFolderKey(), i32a2bin(ma.getMaster_key()), parent_node, share_key);
                        } else {
                            res = ma.createDir(root_name, parent_node, ma.genFolderKey(), i32a2bin(ma.getMaster_key()));

                        }

                        final String file_paths_2_node = (String) ((Map) ((List) res.get("f")).get(0)).get("h");

                        MegaDirNode file_paths = new MegaDirNode(parent_node);

                        final MegaDirNode file_paths_2 = new MegaDirNode(file_paths_2_node);

                        file_paths.getChildren().put(root_name, file_paths_2);

                        file_paths = file_paths_2;

                        for (final File f : dialog.getFiles()) {

                            final String file_path = f.getParentFile().getAbsolutePath().replace(base_path, "");

                            try {

                                if (!file_path.isEmpty()) {

                                    LOG.log(Level.INFO, "{0} FILE_PATH -> {1}", new Object[]{Thread.currentThread().getName(), file_path});

                                }

                                final String[] dirs = file_path.split("\\" + File.separator);

                                MegaDirNode current_node = file_paths;

                                String file_parent = current_node.getNode_id();

                                for (final String d : dirs) {

                                    if (!d.isEmpty()) {

                                        LOG.log(Level.INFO, "{0} DIR -> {1}", new Object[]{Thread.currentThread().getName(), d});

                                        if (current_node.getChildren().get(d) != null) {

                                            current_node = current_node.getChildren().get(d);

                                            file_parent = current_node.getNode_id();

                                        } else {

                                            if (folder_share) {
                                                res = ma.createDirInsideAnotherSharedDir(d, current_node.getNode_id(), ma.genFolderKey(), i32a2bin(ma.getMaster_key()), parent_node, share_key);
                                            } else {
                                                res = ma.createDir(d, current_node.getNode_id(), ma.genFolderKey(), i32a2bin(ma.getMaster_key()));

                                            }
                                            file_parent = (String) ((Map) ((List) res.get("f")).get(0)).get("h");

                                            current_node.getChildren().put(d, new MegaDirNode(file_parent));

                                            current_node = current_node.getChildren().get(d);
                                        }
                                    }
                                }

                                while (this.getMain_panel().getUpload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || this.getMain_panel().getUpload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {

                                    synchronized (this.getMain_panel().getUpload_manager().getWait_queue_lock()) {
                                        this.getMain_panel().getUpload_manager().getWait_queue_lock().wait(1000);
                                    }
                                }

                                final Upload upload = new Upload(this.getMain_panel(), ma, f.getAbsolutePath(), file_parent, null, null, parent_node, share_key, folder_link, dialog.getPriority_checkbox().isSelected());

                                this.getMain_panel().getUpload_manager().getTransference_provision_queue().add(upload);

                                this.getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().remove(f);

                                this.getMain_panel().getUpload_manager().secureNotify();

                            } catch (final Exception ex) {

                                this.getMain_panel().getUpload_manager().getTransference_preprocess_global_queue().remove(f);

                                this.getMain_panel().getUpload_manager().secureNotify();

                                LOG.log(SEVERE, null, ex);
                            }
                        }

                    } catch (final Exception ex) {

                        LOG.log(SEVERE, null, ex);
                    }
                };

                this.getMain_panel().getUpload_manager().getTransference_preprocess_queue().add(run);

                this.getMain_panel().getUpload_manager().secureNotify();

            }

        } catch (final Exception ex) {
        }

        if (!dialog.isRemember_master_pass()) {

            this._main_panel.setMaster_pass(null);
        }

        dialog.dispose();

    }

    private void _file_drop_notify(final List<File> files) {

        final MainPanelView tthis = this;

        THREAD_POOL.execute(() -> {
            final int n;

            if (files.size() > 1) {

                final Object[] options = {LabelTranslatorSingleton.getInstance().translate("Split content in different uploads"), LabelTranslatorSingleton.getInstance().translate("Merge content in the same upload")};

                n = showOptionDialog(this._main_panel.getView(),
                        LabelTranslatorSingleton.getInstance().translate("How do you want to proceed?"),
                        LabelTranslatorSingleton.getInstance().translate("File Grabber"), DEFAULT_OPTION, INFORMATION_MESSAGE,
                        null,
                        options,
                        null);

            } else {

                n = 1;

            }

            if (n == 0) {

                files.stream().map((file) -> {
                    final List<File> aux = new ArrayList<>();
                    aux.add(file);
                    return aux;
                }).map((aux) -> new FileGrabberDialog(tthis, true, aux)).forEachOrdered((dialog) -> {
                    this._new_upload_dialog(dialog);
                });

            } else if (n == 1) {

                final FileGrabberDialog dialog = new FileGrabberDialog(tthis, true, files);

                this._new_upload_dialog(dialog);

            }
        });
    }

    public MainPanelView(final MainPanel main_panel) {

        this._main_panel = main_panel;

        MiscTools.GUIRunAndWait(() -> {

            this.initComponents();

//            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());
//
//            translateLabels(this);

            for (final JComponent c : new JComponent[]{this.download_status_bar, this.upload_status_bar, this.force_chunk_reset_button, this.unfreeze_transferences_button, this.global_speed_down_label, this.global_speed_up_label, this.down_remtime_label, this.up_remtime_label, this.close_all_finished_down_button, this.close_all_finished_up_button, this.pause_all_down_button, this.pause_all_up_button}) {

                c.setVisible(false);
            }

            this.download_status_bar.setMinimum(0);
            this.upload_status_bar.setMinimum(0);

            this.download_status_bar.setValue(this.download_status_bar.getMinimum());
            this.upload_status_bar.setValue(this.upload_status_bar.getMinimum());

            this.clean_all_down_menu.setEnabled(false);
            this.clean_all_up_menu.setEnabled(false);
            this.cancel_all_downloads_menu.setEnabled(false);

            this.jScrollPane_down.getVerticalScrollBar().setUnitIncrement(20);
            this.jScrollPane_up.getVerticalScrollBar().setUnitIncrement(20);

            this.jTabbedPane1.setTitleAt(0, LabelTranslatorSingleton.getInstance().translate("Downloads"));
            this.jTabbedPane1.setTitleAt(1, LabelTranslatorSingleton.getInstance().translate("Uploads"));
            this.jTabbedPane1.setDropTarget(new DropTarget() {

                                           public boolean canImport(final DataFlavor[] flavors) {
                                               for (final DataFlavor flavor : flavors) {
                                                   if (flavor.isFlavorJavaFileListType()) {
                                                       return true;
                                                   }
                                               }
                                               return false;
                                           }

                                           @Override
                                           public synchronized void drop(final DropTargetDropEvent dtde) {
                                               this.changeToNormal();
                                               dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                                               final List<File> files;

                                               try {

                                                   if (this.canImport(dtde.getTransferable().getTransferDataFlavors())) {
                                                       files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

                                                       THREAD_POOL.execute(() -> {
                                                           MainPanelView.this._file_drop_notify(files);
                                                       });
                                                   }

                                               } catch (final Exception ex) {
                                                   JOptionPane.showMessageDialog(main_panel.getView(), LabelTranslatorSingleton.getInstance().translate("ERROR DOING DRAG AND DROP WITH THIS FILE (use button method)"), "Error", JOptionPane.ERROR_MESSAGE);

                                               }
                                           }

                                           @Override
                                           public synchronized void dragEnter(final DropTargetDragEvent dtde) {
                                               this.changeToDrop();
                                           }

                                           @Override
                                           public synchronized void dragExit(final DropTargetEvent dtde) {
                                               this.changeToNormal();
                                           }

                                           private void changeToDrop() {
                                               MainPanelView.this.jTabbedPane1.setBorder(BorderFactory.createLineBorder(Color.green, 5));

                                           }

                                           private void changeToNormal() {
                                               MainPanelView.this.jTabbedPane1.setBorder(null);
                                           }
                                       }
            );

            final String auto_close = selectSettingValue("auto_close");

            if (auto_close != null) {
                this.getAuto_close_menu().setSelected(auto_close.equals("yes"));
            } else {
                this.getAuto_close_menu().setSelected(false);
            }

            this.pack();
        });

        INSTANCE = this;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        this.logo_label = new javax.swing.JLabel();
        this.kiss_server_status = new javax.swing.JLabel();
        this.smart_proxy_status = new javax.swing.JLabel();
        this.memory_status = new javax.swing.JLabel();
        this.jTabbedPane1 = new javax.swing.JTabbedPane();
        this.downloads_panel = new javax.swing.JPanel();
        this.global_speed_down_label = new javax.swing.JLabel();
        this.status_down_label = new javax.swing.JLabel();
        this.close_all_finished_down_button = new javax.swing.JButton();
        this.jScrollPane_down = new javax.swing.JScrollPane();
        this.jPanel_scroll_down = new javax.swing.JPanel();
        this.pause_all_down_button = new javax.swing.JButton();
        this.down_remtime_label = new javax.swing.JLabel();
        this.jButton1 = new javax.swing.JButton();
        this.force_chunk_reset_button = new javax.swing.JButton();
        this.download_status_bar = new javax.swing.JProgressBar();
        this.uploads_panel = new javax.swing.JPanel();
        this.global_speed_up_label = new javax.swing.JLabel();
        this.status_up_label = new javax.swing.JLabel();
        this.close_all_finished_up_button = new javax.swing.JButton();
        this.jScrollPane_up = new javax.swing.JScrollPane();
        this.jPanel_scroll_up = new javax.swing.JPanel();
        this.pause_all_up_button = new javax.swing.JButton();
        this.up_remtime_label = new javax.swing.JLabel();
        this.upload_status_bar = new javax.swing.JProgressBar();
        this.copy_all_uploads = new javax.swing.JButton();
        this.unfreeze_transferences_button = new javax.swing.JButton();
        this.main_menubar = new javax.swing.JMenuBar();
        this.file_menu = new javax.swing.JMenu();
        this.new_download_menu = new javax.swing.JMenuItem();
        this.new_upload_menu = new javax.swing.JMenuItem();
        this.new_stream_menu = new javax.swing.JMenuItem();
        this.jSeparator5 = new javax.swing.JPopupMenu.Separator();
        this.split_file_menu = new javax.swing.JMenuItem();
        this.merge_file_menu = new javax.swing.JMenuItem();
        this.jSeparator4 = new javax.swing.JPopupMenu.Separator();
        this.clean_all_down_menu = new javax.swing.JMenuItem();
        this.clean_all_up_menu = new javax.swing.JMenuItem();
        this.cancel_all_downloads_menu = new javax.swing.JMenuItem();
        this.jSeparator2 = new javax.swing.JPopupMenu.Separator();
        this.hide_tray_menu = new javax.swing.JMenuItem();
        this.auto_close_menu = new javax.swing.JCheckBoxMenuItem();
        this.exit_menu = new javax.swing.JMenuItem();
        this.edit_menu = new javax.swing.JMenu();
        this.settings_menu = new javax.swing.JMenuItem();
        this.help_menu = new javax.swing.JMenu();
        this.about_menu = new javax.swing.JMenuItem();

        this.setTitle("MegaBasterd " + VERSION);
        this.setIconImage(new ImageIcon(this.getClass().getResource(ICON_FILE)).getImage());
        this.setPreferredSize(new java.awt.Dimension(1024, 650));
        this.setSize(new java.awt.Dimension(1024, 650));

        this.logo_label.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/mbasterd_logo_nuevo-picsay.png"))); // NOI18N
        this.logo_label.setDoubleBuffered(true);

        this.kiss_server_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        this.kiss_server_status.setForeground(new java.awt.Color(102, 102, 102));
        this.kiss_server_status.setDoubleBuffered(true);

        this.smart_proxy_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        this.smart_proxy_status.setForeground(new java.awt.Color(102, 102, 102));
        this.smart_proxy_status.setDoubleBuffered(true);

        this.memory_status.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        this.memory_status.setForeground(new java.awt.Color(102, 102, 102));
        this.memory_status.setDoubleBuffered(true);

        this.jTabbedPane1.setDoubleBuffered(true);
        this.jTabbedPane1.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N

        this.global_speed_down_label.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        this.global_speed_down_label.setText("Speed");
        this.global_speed_down_label.setDoubleBuffered(true);

        this.status_down_label.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        this.status_down_label.setForeground(new java.awt.Color(102, 102, 102));
        this.status_down_label.setDoubleBuffered(true);

        this.close_all_finished_down_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.close_all_finished_down_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-ok-30.png"))); // NOI18N
        this.close_all_finished_down_button.setText("Clear finished");
        this.close_all_finished_down_button.setDoubleBuffered(true);
        this.close_all_finished_down_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.close_all_finished_down_buttonActionPerformed(evt);
            }
        });

        this.jScrollPane_down.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        this.jPanel_scroll_down.setLayout(new javax.swing.BoxLayout(this.jPanel_scroll_down, javax.swing.BoxLayout.Y_AXIS));
        this.jScrollPane_down.setViewportView(this.jPanel_scroll_down);

        this.pause_all_down_button.setBackground(new java.awt.Color(255, 153, 0));
        this.pause_all_down_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.pause_all_down_button.setForeground(new java.awt.Color(255, 255, 255));
        this.pause_all_down_button.setText("PAUSE ALL");
        this.pause_all_down_button.setDoubleBuffered(true);
        this.pause_all_down_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.pause_all_down_buttonActionPerformed(evt);
            }
        });

        this.down_remtime_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        this.jButton1.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.jButton1.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-copy-to-clipboard-30.png"))); // NOI18N
        this.jButton1.setText("COPY ALL DOWNLOAD LINKS");
        this.jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.jButton1ActionPerformed(evt);
            }
        });

        this.force_chunk_reset_button.setBackground(new java.awt.Color(255, 0, 153));
        this.force_chunk_reset_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.force_chunk_reset_button.setForeground(new java.awt.Color(255, 255, 255));
        this.force_chunk_reset_button.setText("FORCE ALL CURRENT CHUNKS RESET");
        this.force_chunk_reset_button.setDoubleBuffered(true);
        this.force_chunk_reset_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.force_chunk_reset_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout downloads_panelLayout = new javax.swing.GroupLayout(this.downloads_panel);
        this.downloads_panel.setLayout(downloads_panelLayout);
        downloads_panelLayout.setHorizontalGroup(
                downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(this.global_speed_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.force_chunk_reset_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.pause_all_down_button))
                        .addGroup(downloads_panelLayout.createSequentialGroup()
                                .addComponent(this.status_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18)
                                .addComponent(this.close_all_finished_down_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jButton1)
                                .addContainerGap())
                        .addComponent(this.jScrollPane_down)
                        .addComponent(this.down_remtime_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(this.download_status_bar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        downloads_panelLayout.setVerticalGroup(
                downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, downloads_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(this.close_all_finished_down_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.jButton1))
                                        .addComponent(this.status_down_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.download_status_bar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jScrollPane_down, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.down_remtime_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(downloads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.global_speed_down_label)
                                        .addComponent(this.pause_all_down_button)
                                        .addComponent(this.force_chunk_reset_button)))
        );

        this.jTabbedPane1.addTab("Downloads", new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-download-from-ftp-30.png")), this.downloads_panel); // NOI18N

        this.global_speed_up_label.setFont(new java.awt.Font("Dialog", 1, 54)); // NOI18N
        this.global_speed_up_label.setText("Speed");
        this.global_speed_up_label.setDoubleBuffered(true);

        this.status_up_label.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        this.status_up_label.setForeground(new java.awt.Color(102, 102, 102));

        this.close_all_finished_up_button.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.close_all_finished_up_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-ok-30.png"))); // NOI18N
        this.close_all_finished_up_button.setText("Clear finished");
        this.close_all_finished_up_button.setDoubleBuffered(true);
        this.close_all_finished_up_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.close_all_finished_up_buttonActionPerformed(evt);
            }
        });

        this.jScrollPane_up.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        this.jPanel_scroll_up.setLayout(new javax.swing.BoxLayout(this.jPanel_scroll_up, javax.swing.BoxLayout.Y_AXIS));
        this.jScrollPane_up.setViewportView(this.jPanel_scroll_up);

        this.pause_all_up_button.setBackground(new java.awt.Color(255, 153, 0));
        this.pause_all_up_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.pause_all_up_button.setForeground(new java.awt.Color(255, 255, 255));
        this.pause_all_up_button.setText("PAUSE ALL");
        this.pause_all_up_button.setDoubleBuffered(true);
        this.pause_all_up_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.pause_all_up_buttonActionPerformed(evt);
            }
        });

        this.up_remtime_label.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N

        this.copy_all_uploads.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.copy_all_uploads.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-copy-to-clipboard-30.png"))); // NOI18N
        this.copy_all_uploads.setText("COPY ALL UPLOAD LINKS");
        this.copy_all_uploads.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.copy_all_uploadsActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout uploads_panelLayout = new javax.swing.GroupLayout(this.uploads_panel);
        this.uploads_panel.setLayout(uploads_panelLayout);
        uploads_panelLayout.setHorizontalGroup(
                uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addComponent(this.global_speed_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, 839, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.pause_all_up_button))
                        .addGroup(uploads_panelLayout.createSequentialGroup()
                                .addComponent(this.status_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(3, 3, 3)
                                .addComponent(this.close_all_finished_up_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.copy_all_uploads)
                                .addContainerGap())
                        .addComponent(this.jScrollPane_up)
                        .addComponent(this.up_remtime_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(this.upload_status_bar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        uploads_panelLayout.setVerticalGroup(
                uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, uploads_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(this.close_all_finished_up_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.copy_all_uploads))
                                        .addComponent(this.status_up_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.upload_status_bar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jScrollPane_up, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.up_remtime_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(uploads_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.global_speed_up_label)
                                        .addComponent(this.pause_all_up_button)))
        );

        this.jTabbedPane1.addTab("Uploads", new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-upload-to-ftp-30.png")), this.uploads_panel); // NOI18N

        this.unfreeze_transferences_button.setBackground(new java.awt.Color(255, 255, 204));
        this.unfreeze_transferences_button.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        this.unfreeze_transferences_button.setForeground(new java.awt.Color(0, 153, 255));
        this.unfreeze_transferences_button.setText("UNFREEZE WAITING TRANSFERENCES");
        this.unfreeze_transferences_button.setDoubleBuffered(true);
        this.unfreeze_transferences_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.unfreeze_transferences_buttonActionPerformed(evt);
            }
        });

        this.file_menu.setText("File");
        this.file_menu.setDoubleBuffered(true);
        this.file_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        this.new_download_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.new_download_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-download-from-ftp-30.png"))); // NOI18N
        this.new_download_menu.setText("New download");
        this.new_download_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.new_download_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.new_download_menu);

        this.new_upload_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.new_upload_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-upload-to-ftp-30.png"))); // NOI18N
        this.new_upload_menu.setText("New upload");
        this.new_upload_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.new_upload_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.new_upload_menu);

        this.new_stream_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.new_stream_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-video-playlist-30.png"))); // NOI18N
        this.new_stream_menu.setText("New streaming");
        this.new_stream_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.new_stream_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.new_stream_menu);
        this.file_menu.add(this.jSeparator5);

        this.split_file_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.split_file_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-cut-30.png"))); // NOI18N
        this.split_file_menu.setText("Split file");
        this.split_file_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.split_file_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.split_file_menu);

        this.merge_file_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.merge_file_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-glue-30.png"))); // NOI18N
        this.merge_file_menu.setText("Merge file");
        this.merge_file_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.merge_file_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.merge_file_menu);
        this.file_menu.add(this.jSeparator4);

        this.clean_all_down_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.clean_all_down_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.clean_all_down_menu.setText("Remove all no running downloads");
        this.clean_all_down_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.clean_all_down_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.clean_all_down_menu);

        this.clean_all_up_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.clean_all_up_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.clean_all_up_menu.setText("Remove all no running uploads");
        this.clean_all_up_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.clean_all_up_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.clean_all_up_menu);

        this.cancel_all_downloads_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.cancel_all_downloads_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-minus-30.png"))); // NOI18N
        this.cancel_all_downloads_menu.setText("CANCEL ALL DOWNLOADS");
        this.cancel_all_downloads_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.cancel_all_downloads_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.cancel_all_downloads_menu);
        this.file_menu.add(this.jSeparator2);

        this.hide_tray_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.hide_tray_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/pica_roja_menu.png"))); // NOI18N
        this.hide_tray_menu.setText("Hide to tray");
        this.hide_tray_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.hide_tray_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.hide_tray_menu);

        this.auto_close_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.auto_close_menu.setText("Close MegaBasterd when all transfers finish");
        this.auto_close_menu.setDoubleBuffered(true);
        this.auto_close_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-cancel-30.png"))); // NOI18N
        this.auto_close_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.auto_close_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.auto_close_menu);

        this.exit_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.exit_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-shutdown-30.png"))); // NOI18N
        this.exit_menu.setText("Exit");
        this.exit_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.exit_menuActionPerformed(evt);
            }
        });
        this.file_menu.add(this.exit_menu);

        this.main_menubar.add(this.file_menu);

        this.edit_menu.setText("Edit");
        this.edit_menu.setDoubleBuffered(true);
        this.edit_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        this.settings_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.settings_menu.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-services-30.png"))); // NOI18N
        this.settings_menu.setText("Settings");
        this.settings_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.settings_menuActionPerformed(evt);
            }
        });
        this.edit_menu.add(this.settings_menu);

        this.main_menubar.add(this.edit_menu);

        this.help_menu.setText("Help");
        this.help_menu.setDoubleBuffered(true);
        this.help_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N

        this.about_menu.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.about_menu.setText("About");
        this.about_menu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                MainPanelView.this.about_menuActionPerformed(evt);
            }
        });
        this.help_menu.add(this.about_menu);

        this.main_menubar.add(this.help_menu);

        this.setJMenuBar(this.main_menubar);

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(this.unfreeze_transferences_button)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(this.kiss_server_status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.smart_proxy_status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.memory_status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.logo_label)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.unfreeze_transferences_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.jTabbedPane1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(this.logo_label)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(this.smart_proxy_status)
                                                        .addComponent(this.memory_status))
                                                .addComponent(this.kiss_server_status)))
                                .addGap(2, 2, 2))
        );

        this.pack();
    }// </editor-fold>//GEN-END:initComponents

    private void new_download_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_download_menuActionPerformed

        final LinkGrabberDialog dialog = new LinkGrabberDialog(this, true, this._main_panel.getDefault_download_path(), this._main_panel.getClipboardspy());

        this._main_panel.getClipboardspy().attachObserver(dialog);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);

        this._main_panel.getClipboardspy().detachObserver(dialog);

        final String dl_path = dialog.getDownload_path();

        final MegaAPI ma;

        if (this.getMain_panel().isUse_mega_account_down()) {
            final String mega_account = (String) dialog.getUse_mega_account_down_combobox().getSelectedItem();

            if ("".equals(mega_account)) {

                ma = new MegaAPI();

            } else {

                ma = this.getMain_panel().getMega_active_accounts().get(mega_account);
            }

        } else {

            ma = new MegaAPI();
        }

        this.jTabbedPane1.setSelectedIndex(0);

        if (dialog.isDownload()) {

            this.getMain_panel().resumeDownloads();

            final MainPanelView tthis = this;

            final Runnable run = () -> {

                final String link_data = MiscTools.extractMegaLinksFromString(dialog.getLinks_textarea().getText());

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

                dlc.stream().map((d) -> CryptTools.decryptDLC(d, this._main_panel)).forEachOrdered((links) -> {
                    links.stream().filter((link) -> (findFirstRegex("(?:https?|mega)://[^\r\n](#[^\r\n!]*?)?![^\r\n!]+![^\\?\r\n/]+", link, 0) != null)).forEachOrdered((link) -> {
                        urls.add(link);
                    });
                });

                if (!urls.isEmpty()) {

                    final Set<String> folder_file_links = new HashSet(findAllRegex("(?:https?|mega)://[^\r\n]+#F\\*[^\r\n!]*?![^\r\n!]+![^\\?\r\n/]+", link_data, 0));

                    this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().addAll(folder_file_links);

                    this.getMain_panel().getDownload_manager().secureNotify();

                    MiscTools.GUIRun(() -> {
                        this.download_status_bar.setIndeterminate(true);
                        this.download_status_bar.setValue(this.download_status_bar.getMinimum());
                        this.download_status_bar.setMaximum(this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().size() + this.getMain_panel().getDownload_manager().getTransference_preprocess_queue().size() + this.getMain_panel().getDownload_manager().getTransference_provision_queue().size());
                        this.download_status_bar.setVisible(true);
                    });

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

                            Download download;

                            if (findFirstRegex("#F!", url, 0) != null) {

                                final FolderLinkDialog fdialog = new FolderLinkDialog(this._main_panel.getView(), true, url);

                                if (fdialog.isMega_error() == 0) {

                                    fdialog.setLocationRelativeTo(this._main_panel.getView());

                                    fdialog.setVisible(true);

                                    if (fdialog.isDownload()) {

                                        final List<HashMap> folder_links = fdialog.getDownload_links();

                                        fdialog.dispose();

                                        for (final HashMap folder_link : folder_links) {

                                            while (this.getMain_panel().getDownload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || this.getMain_panel().getDownload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {

                                                if (!link_warning) {
                                                    link_warning = true;

                                                    JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("There are a lot of files in this folder.\nNot all links will be provisioned at once to avoid saturating MegaBasterd"), "Warning", JOptionPane.WARNING_MESSAGE);
                                                }

                                                synchronized (this.getMain_panel().getDownload_manager().getWait_queue_lock()) {
                                                    this.getMain_panel().getDownload_manager().getWait_queue_lock().wait(1000);
                                                }
                                            }

                                            if (!this.getMain_panel().getDownload_manager().getTransference_preprocess_global_queue().isEmpty()) {

                                                if (!((String) folder_link.get("url")).equals("*")) {

                                                    download = new Download(this.getMain_panel(), ma, (String) folder_link.get("url"), dl_path, (String) folder_link.get("filename"), (String) folder_link.get("filekey"), (long) folder_link.get("filesize"), null, null, this.getMain_panel().isUse_slots_down(), false, this.getMain_panel().isUse_custom_chunks_dir() ? this.getMain_panel().getCustom_chunks_dir() : null, dialog.getPriority_checkbox().isSelected());

                                                    this.getMain_panel().getDownload_manager().getTransference_provision_queue().add(download);

                                                    this.getMain_panel().getDownload_manager().secureNotify();

                                                } else {
                                                    //Directorio vacío
                                                    final String filename = dl_path + "/" + (String) folder_link.get("filename");

                                                    final File file = new File(filename);

                                                    if (file.getParent() != null) {
                                                        final File path = new File(file.getParent());

                                                        path.mkdirs();
                                                    }

                                                    if (((int) folder_link.get("type")) == 1) {

                                                        file.mkdir();

                                                    } else {
                                                        try {
                                                            file.createNewFile();
                                                        } catch (final IOException ex) {
                                                            Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, null, ex);
                                                        }
                                                    }
                                                }
                                            } else {
                                                break;
                                            }
                                        }
                                    }

                                }

                                fdialog.dispose();

                            } else {

                                while (this.getMain_panel().getDownload_manager().getTransference_waitstart_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE || this.getMain_panel().getDownload_manager().getTransference_waitstart_aux_queue().size() >= TransferenceManager.MAX_WAIT_QUEUE) {

                                    synchronized (this.getMain_panel().getDownload_manager().getWait_queue_lock()) {
                                        this.getMain_panel().getDownload_manager().getWait_queue_lock().wait(1000);
                                    }
                                }

                                download = new Download(this.getMain_panel(), ma, url, dl_path, null, null, null, null, null, this.getMain_panel().isUse_slots_down(), false, this.getMain_panel().isUse_custom_chunks_dir() ? this.getMain_panel().getCustom_chunks_dir() : null, dialog.getPriority_checkbox().isSelected());

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

        dialog.dispose();

    }//GEN-LAST:event_new_download_menuActionPerformed

    private void settings_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settings_menuActionPerformed

        final SettingsDialog dialog = new SettingsDialog(this, true);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);

        if (dialog.isSettings_ok()) {

            dialog.getDeleted_mega_accounts().stream().map((email) -> {
                try {
                    deleteMegaAccount(email);
                } catch (final SQLException ex) {
                    LOG.log(SEVERE, null, ex);
                }
                return email;
            }).map((email) -> {
                this._main_panel.getMega_accounts().remove(email);
                return email;
            }).forEachOrdered((email) -> {
                this._main_panel.getMega_active_accounts().remove(email);
            });
            dialog.getDeleted_elc_accounts().stream().map((host) -> {
                try {
                    deleteELCAccount(host);
                } catch (final SQLException ex) {
                    LOG.log(SEVERE, null, ex);
                }
                return host;
            }).forEachOrdered((host) -> {
                this._main_panel.getElc_accounts().remove(host);
            });

            if (this._main_panel.isRestart()) {

                this._main_panel.byebye(true);
            } else {
                this._main_panel.loadUserSettings();

                if (this._main_panel.isLimit_download_speed()) {

                    this._main_panel.getStream_supervisor().setMaxBytesPerSecInput(this._main_panel.getMax_dl_speed() * 1024);

                    this.global_speed_down_label.setForeground(new Color(255, 0, 0));

                } else {

                    this._main_panel.getStream_supervisor().setMaxBytesPerSecInput(0);

                    this.global_speed_down_label.setForeground(new Color(0, 128, 255));

                }

                if (this._main_panel.isLimit_upload_speed()) {

                    this._main_panel.getStream_supervisor().setMaxBytesPerSecOutput(this._main_panel.getMax_up_speed() * 1024);

                    this.global_speed_up_label.setForeground(new Color(255, 0, 0));

                } else {

                    this._main_panel.getStream_supervisor().setMaxBytesPerSecOutput(0);

                    this.global_speed_up_label.setForeground(new Color(0, 128, 255));

                }

                this._main_panel.getDownload_manager().setMax_running_trans(this._main_panel.getMax_dl());

                this._main_panel.getUpload_manager().setMax_running_trans(this._main_panel.getMax_ul());

                this._main_panel.getDownload_manager().secureNotify();

                this._main_panel.getUpload_manager().secureNotify();

                if (this._main_panel.isMegacrypter_reverse()) {

                    if (this._main_panel.getMega_proxy_server() == null) {

                        this._main_panel.setMega_proxy_server(new MegaProxyServer(this._main_panel, UUID.randomUUID().toString(), this._main_panel.getMegacrypter_reverse_port()));

                        THREAD_POOL.execute(this._main_panel.getMega_proxy_server());

                    } else if (this._main_panel.getMega_proxy_server().getPort() != this._main_panel.getMegacrypter_reverse_port()) {

                        try {

                            this._main_panel.getMega_proxy_server().stopServer();
                            this._main_panel.setMega_proxy_server(new MegaProxyServer(this._main_panel, UUID.randomUUID().toString(), this._main_panel.getMegacrypter_reverse_port()));
                            THREAD_POOL.execute(this._main_panel.getMega_proxy_server());

                        } catch (final IOException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }
                    }

                } else {

                    if (this._main_panel.getMega_proxy_server() != null) {

                        try {
                            this._main_panel.getMega_proxy_server().stopServer();
                        } catch (final IOException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage());
                        }
                    }

                    this._main_panel.setMega_proxy_server(null);
                }

                this.force_chunk_reset_button.setVisible(MainPanel.isUse_smart_proxy());

                if (MainPanel.isUse_smart_proxy()) {

                    if (MainPanel.getProxy_manager() == null) {

                        final String lista_proxy = DBTools.selectSettingValue("custom_proxy_list");

                        final String url_list = MiscTools.findFirstRegex("^#(http.+)$", lista_proxy.trim(), 1);

                        MainPanel.setProxy_manager(new SmartMegaProxyManager(url_list, this._main_panel));
                    } else {
                        final String lista_proxy = DBTools.selectSettingValue("custom_proxy_list");
                        final String url_list = MiscTools.findFirstRegex("^#(http.+)$", lista_proxy.trim(), 1);
                        MainPanel.getProxy_manager().refreshProxyList(url_list);
                    }

                    MainPanel.getProxy_manager().refreshSmartProxySettings();

                } else {

                    this.updateSmartProxyStatus("SmartProxy: OFF");
                }
            }

            if (!dialog.isRemember_master_pass()) {

                this._main_panel.setMaster_pass(null);
            }

            dialog.dispose();

        }
    }//GEN-LAST:event_settings_menuActionPerformed

    private void hide_tray_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hide_tray_menuActionPerformed

        this.dispatchEvent(new WindowEvent(this, WINDOW_CLOSING));
    }//GEN-LAST:event_hide_tray_menuActionPerformed

    private void about_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_about_menuActionPerformed

        final AboutDialog dialog = new AboutDialog(this, true);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }//GEN-LAST:event_about_menuActionPerformed

    private void exit_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exit_menuActionPerformed

        this._main_panel.byebye(false);
    }//GEN-LAST:event_exit_menuActionPerformed

    private void close_all_finished_down_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finished_down_buttonActionPerformed

        this._main_panel.getDownload_manager().closeAllFinished();
    }//GEN-LAST:event_close_all_finished_down_buttonActionPerformed

    private void clean_all_down_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clean_all_down_menuActionPerformed

        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this._main_panel.getView(),
                LabelTranslatorSingleton.getInstance().translate("Remove all no running downloads?"),
                LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            this._main_panel.getDownload_manager().closeAllPreProWaiting();
        }
    }//GEN-LAST:event_clean_all_down_menuActionPerformed

    private void pause_all_down_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_all_down_buttonActionPerformed

        this.pause_all_down_button.setEnabled(false);

        if (!this._main_panel.getDownload_manager().isPaused_all()) {

            this._main_panel.getDownload_manager().pauseAll();

        } else {

            this._main_panel.getDownload_manager().resumeAll();
        }

    }//GEN-LAST:event_pause_all_down_buttonActionPerformed

    private void new_stream_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_stream_menuActionPerformed

        final StreamerDialog dialog = new StreamerDialog(this, true, this._main_panel.getClipboardspy());

        this._main_panel.getClipboardspy().attachObserver(dialog);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);

        this._main_panel.getClipboardspy().detachObserver(dialog);
    }//GEN-LAST:event_new_stream_menuActionPerformed

    private void new_upload_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_upload_menuActionPerformed

        final FileGrabberDialog dialog = new FileGrabberDialog(this, true, null);

        this._new_upload_dialog(dialog);
    }//GEN-LAST:event_new_upload_menuActionPerformed

    private void close_all_finished_up_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_close_all_finished_up_buttonActionPerformed

        this._main_panel.getUpload_manager().closeAllFinished();
    }//GEN-LAST:event_close_all_finished_up_buttonActionPerformed

    private void pause_all_up_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pause_all_up_buttonActionPerformed

        this.pause_all_up_button.setEnabled(false);

        if (!this._main_panel.getUpload_manager().isPaused_all()) {

            this._main_panel.getUpload_manager().pauseAll();

        } else {

            this._main_panel.getUpload_manager().resumeAll();
        }
    }//GEN-LAST:event_pause_all_up_buttonActionPerformed

    private void clean_all_up_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clean_all_up_menuActionPerformed

        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this._main_panel.getView(),
                LabelTranslatorSingleton.getInstance().translate("Remove all no running uploads?"),
                LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            this._main_panel.getUpload_manager().closeAllPreProWaiting();
        }
    }//GEN-LAST:event_clean_all_up_menuActionPerformed

    private void split_file_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_file_menuActionPerformed
        // TODO add your handling code here:
        final FileSplitterDialog dialog = new FileSplitterDialog(this, false);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }//GEN-LAST:event_split_file_menuActionPerformed

    private void merge_file_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_merge_file_menuActionPerformed
        // TODO add your handling code here:

        final FileMergerDialog dialog = new FileMergerDialog(this, false);

        dialog.setLocationRelativeTo(this);

        dialog.setVisible(true);
    }//GEN-LAST:event_merge_file_menuActionPerformed

    private void auto_close_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_auto_close_menuActionPerformed
        try {
            DBTools.insertSettingValue("auto_close", this.getAuto_close_menu().isSelected() ? "yes" : "no");
        } catch (final SQLException ex) {
            Logger.getLogger(MainPanelView.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }//GEN-LAST:event_auto_close_menuActionPerformed

    private void unfreeze_transferences_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unfreeze_transferences_buttonActionPerformed
        // TODO add your handling code here:

        this.unfreeze_transferences_button.setVisible(false);

        THREAD_POOL.execute(this._main_panel.getDownload_manager()::unfreezeTransferenceWaitStartQueue);

        THREAD_POOL.execute(this._main_panel.getUpload_manager()::unfreezeTransferenceWaitStartQueue);
    }//GEN-LAST:event_unfreeze_transferences_buttonActionPerformed

    private void jButton1ActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:

        final int total = this._main_panel.getDownload_manager().copyAllLinksToClipboard();

        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate(total > 0 ? "ALL DOWNLOAD LINKS COPIED!" : "NO DOWNLOAD LINKS TO COPY"));
    }//GEN-LAST:event_jButton1ActionPerformed

    private void cancel_all_downloads_menuActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel_all_downloads_menuActionPerformed
        // TODO add your handling code here:
        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        final int n = showOptionDialog(this._main_panel.getView(),
                LabelTranslatorSingleton.getInstance().translate("CANCEL ALL DOWNLOADS?"),
                LabelTranslatorSingleton.getInstance().translate("Warning!"), YES_NO_CANCEL_OPTION, QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {
            this._main_panel.getDownload_manager().closeAllPreProWaiting();
            this._main_panel.getDownload_manager().cancelAllTransferences();
        }
    }//GEN-LAST:event_cancel_all_downloads_menuActionPerformed

    private void force_chunk_reset_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_force_chunk_reset_buttonActionPerformed
        // TODO add your handling code here:

        this.force_chunk_reset_button.setEnabled(false);

        this._main_panel.getDownload_manager().forceResetAllChunks();

    }//GEN-LAST:event_force_chunk_reset_buttonActionPerformed

    private void copy_all_uploadsActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_all_uploadsActionPerformed
        // TODO add your handling code here:
        final int total = this._main_panel.getUpload_manager().copyAllLinksToClipboard();

        JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate(total > 0 ? "ALL UPLOAD LINKS COPIED!" : "NO UPLOAD LINKS TO COPY"));

    }//GEN-LAST:event_copy_all_uploadsActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem about_menu;
    private javax.swing.JCheckBoxMenuItem auto_close_menu;
    private javax.swing.JMenuItem cancel_all_downloads_menu;
    private javax.swing.JMenuItem clean_all_down_menu;
    private javax.swing.JMenuItem clean_all_up_menu;
    private javax.swing.JButton close_all_finished_down_button;
    private javax.swing.JButton close_all_finished_up_button;
    private javax.swing.JButton copy_all_uploads;
    private javax.swing.JLabel down_remtime_label;
    private javax.swing.JProgressBar download_status_bar;
    private javax.swing.JPanel downloads_panel;
    private javax.swing.JMenu edit_menu;
    private javax.swing.JMenuItem exit_menu;
    private javax.swing.JMenu file_menu;
    private javax.swing.JButton force_chunk_reset_button;
    private javax.swing.JLabel global_speed_down_label;
    private javax.swing.JLabel global_speed_up_label;
    private javax.swing.JMenu help_menu;
    private javax.swing.JMenuItem hide_tray_menu;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel_scroll_down;
    private javax.swing.JPanel jPanel_scroll_up;
    private javax.swing.JScrollPane jScrollPane_down;
    private javax.swing.JScrollPane jScrollPane_up;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel kiss_server_status;
    private javax.swing.JLabel logo_label;
    private javax.swing.JMenuBar main_menubar;
    private javax.swing.JLabel memory_status;
    private javax.swing.JMenuItem merge_file_menu;
    private javax.swing.JMenuItem new_download_menu;
    private javax.swing.JMenuItem new_stream_menu;
    private javax.swing.JMenuItem new_upload_menu;
    private javax.swing.JButton pause_all_down_button;
    private javax.swing.JButton pause_all_up_button;
    private javax.swing.JMenuItem settings_menu;
    private javax.swing.JLabel smart_proxy_status;
    private javax.swing.JMenuItem split_file_menu;
    private javax.swing.JLabel status_down_label;
    private javax.swing.JLabel status_up_label;
    private javax.swing.JButton unfreeze_transferences_button;
    private javax.swing.JLabel up_remtime_label;
    private javax.swing.JProgressBar upload_status_bar;
    private javax.swing.JPanel uploads_panel;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(MainPanelView.class.getName());

}
