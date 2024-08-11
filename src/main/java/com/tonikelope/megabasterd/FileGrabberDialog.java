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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.checkMegaAccountLoginAndShowMasterPassDialog;
import static com.tonikelope.megabasterd.MiscTools.copyTextToClipboard;
import static com.tonikelope.megabasterd.MiscTools.formatBytes;

/**
 * @author tonikelope
 */
public class FileGrabberDialog extends javax.swing.JDialog {

    private boolean _upload;
    private final ArrayList<File> _files;
    private String _base_path;
    private long _total_space;
    private final MainPanel _main_panel;
    private final boolean _remember_master_pass;
    private boolean _inserting_mega_accounts;
    private boolean _quota_ok;
    private int _last_selected_index;
    private final List<File> _drag_drop_files;

    @Override
    public void dispose() {
        this.file_tree.setModel(null);
        super.dispose();
    }

    public JCheckBox getPriority_checkbox() {
        return this.priority_checkbox;
    }

    public JCheckBox getUpload_log_checkbox() {
        return this.upload_log_checkbox;
    }

    public boolean isUpload() {
        return this._upload;
    }

    public ArrayList<File> getFiles() {
        return this._files;
    }

    public String getBase_path() {
        return this._base_path;
    }

    public JComboBox<String> getAccount_combobox() {
        return this.account_combobox;
    }

    public JTextField getDir_name_textfield() {
        return this.dir_name_textfield;
    }

    public boolean isRemember_master_pass() {
        return this._remember_master_pass;
    }

    public FileGrabberDialog(final MainPanelView parent, final boolean modal, final List<File> files) {

        super(parent, modal);

        this._main_panel = parent.getMain_panel();

        this._drag_drop_files = files;

        this._quota_ok = false;

        this._total_space = 0L;
        this._base_path = null;
        this._upload = false;
        this._inserting_mega_accounts = false;
        this._remember_master_pass = true;
        this._files = new ArrayList<>();
        this._last_selected_index = -1;

        MiscTools.GUIRunAndWait(() -> {
            this.initComponents();

            final String upload_log_string = DBTools.selectSettingValue("upload_log");

            this.upload_log_checkbox.setSelected("yes".equals(upload_log_string));

//            updateFonts(this, GUI_FONT, this._main_panel.getZoom_factor());
//
//            translateLabels(this);

            this.jPanel1.setDropTarget(
                    new DropTarget() {

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
                                        FileGrabberDialog.this._file_drop_notify(files);
                                    });
                                }

                            } catch (final Exception ex) {
                                JOptionPane.showMessageDialog(parent, LabelTranslatorSingleton.getInstance().translate("ERROR DOING DRAG AND DROP WITH THIS FILE (use button method)"), "Error", JOptionPane.ERROR_MESSAGE);
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
                            FileGrabberDialog.this.jPanel1.setBorder(BorderFactory.createLineBorder(Color.green, 5));

                        }

                        private void changeToNormal() {
                            FileGrabberDialog.this.jPanel1.setBorder(null);
                        }
                    }
            );

            this.dir_name_textfield.addMouseListener(new ContextMenuMouseListener());

            this.pack();

        });

        THREAD_POOL.execute(() -> {

            if (this._drag_drop_files != null) {

                this._file_drop_notify(this._drag_drop_files);
            }

            if (this._main_panel.getMega_accounts().size() > 0) {

                final ArrayList<String> cuentas = new ArrayList<>();

                this._main_panel.getMega_accounts().keySet().forEach((o) -> {
                    cuentas.add(o);
                });

                Collections.sort(cuentas);

                MiscTools.GUIRunAndWait(() -> {
                    if (!this._main_panel.getMega_active_accounts().isEmpty()) {
                        this._inserting_mega_accounts = true;

                        cuentas.forEach((o) -> {
                            this.account_combobox.addItem(o);
                        });

                        this._inserting_mega_accounts = false;

                        for (final Object o : this._main_panel.getMega_active_accounts().keySet()) {

                            this.account_combobox.setSelectedItem(o);

                            this.account_comboboxItemStateChanged(null);

                            break;
                        }

                    } else {

                        cuentas.forEach((o) -> {
                            this.account_combobox.addItem(o);
                        });
                    }

                    this.pack();
                });
            } else {
                MiscTools.GUIRunAndWait(() -> {
                    this.used_space_label.setForeground(Color.red);
                    this.used_space_label.setEnabled(true);
                    this.used_space_label.setText(LabelTranslatorSingleton.getInstance().translate("No MEGA accounts available (Go to Settings > Accounts)"));
                });
            }
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

        this.jPanel1 = new javax.swing.JPanel();
        this.file_tree_scrollpane = new javax.swing.JScrollPane();
        this.file_tree = new javax.swing.JTree();
        this.jPanel2 = new javax.swing.JPanel();
        this.dir_name_label = new javax.swing.JLabel();
        this.dir_name_textfield = new javax.swing.JTextField();
        this.account_label = new javax.swing.JLabel();
        this.account_combobox = new javax.swing.JComboBox<>();
        this.used_space_label = new javax.swing.JLabel();
        this.add_folder_button = new javax.swing.JButton();
        this.add_files_button = new javax.swing.JButton();
        this.upload_log_checkbox = new javax.swing.JCheckBox();
        this.priority_checkbox = new javax.swing.JCheckBox();
        this.copy_email_button = new javax.swing.JButton();
        this.dance_button = new javax.swing.JButton();
        this.total_file_size_label = new javax.swing.JLabel();
        this.warning_label = new javax.swing.JLabel();
        this.skip_rest_button = new javax.swing.JButton();
        this.skip_button = new javax.swing.JButton();

        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle("File Grabber");

        this.file_tree.setBorder(null);
        this.file_tree.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        final javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        this.file_tree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        this.file_tree.setDoubleBuffered(true);
        this.file_tree.setEnabled(false);
        this.file_tree.setRootVisible(false);
        this.file_tree_scrollpane.setViewportView(this.file_tree);

        final javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(this.jPanel1);
        this.jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(this.file_tree_scrollpane)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(this.file_tree_scrollpane, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
        );

        this.dir_name_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.dir_name_label.setText("Upload name:");
        this.dir_name_label.setDoubleBuffered(true);
        this.dir_name_label.setEnabled(false);

        this.dir_name_textfield.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.dir_name_textfield.setDoubleBuffered(true);
        this.dir_name_textfield.setEnabled(false);
        this.dir_name_textfield.setMargin(new java.awt.Insets(2, 2, 2, 2));

        this.account_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.account_label.setText("Account:");
        this.account_label.setDoubleBuffered(true);
        this.account_label.setEnabled(false);

        this.account_combobox.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.account_combobox.setDoubleBuffered(true);
        this.account_combobox.setEnabled(false);
        this.account_combobox.addItemListener(new java.awt.event.ItemListener() {
            @Override
            public void itemStateChanged(final java.awt.event.ItemEvent evt) {
                FileGrabberDialog.this.account_comboboxItemStateChanged(evt);
            }
        });

        this.used_space_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.used_space_label.setText("Used space: 0.00GB");
        this.used_space_label.setDoubleBuffered(true);
        this.used_space_label.setEnabled(false);

        this.add_folder_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.add_folder_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-add-folder-30.png"))); // NOI18N
        this.add_folder_button.setText("Add folder");
        this.add_folder_button.setDoubleBuffered(true);
        this.add_folder_button.setEnabled(false);
        this.add_folder_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileGrabberDialog.this.add_folder_buttonActionPerformed(evt);
            }
        });

        this.add_files_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.add_files_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-add-file-30.png"))); // NOI18N
        this.add_files_button.setText("Add files");
        this.add_files_button.setDoubleBuffered(true);
        this.add_files_button.setEnabled(false);
        this.add_files_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileGrabberDialog.this.add_files_buttonActionPerformed(evt);
            }
        });

        this.upload_log_checkbox.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.upload_log_checkbox.setText("Enable log file");
        this.upload_log_checkbox.setDoubleBuffered(true);
        this.upload_log_checkbox.setEnabled(false);

        this.priority_checkbox.setFont(new java.awt.Font("Dialog", 1, 16)); // NOI18N
        this.priority_checkbox.setText("Put on TOP of waiting queue");
        this.priority_checkbox.setDoubleBuffered(true);
        this.priority_checkbox.setEnabled(false);

        this.copy_email_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.copy_email_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-copy-to-clipboard-30.png"))); // NOI18N
        this.copy_email_button.setText("Copy email");
        this.copy_email_button.setEnabled(false);
        this.copy_email_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileGrabberDialog.this.copy_email_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(this.jPanel2);
        this.jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, 0)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(this.upload_log_checkbox)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.priority_checkbox))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(this.dir_name_label)
                                                        .addComponent(this.account_label))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(this.add_files_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(this.add_folder_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                        .addComponent(this.dir_name_textfield)
                                                        .addComponent(this.used_space_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(this.account_combobox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(this.copy_email_button)))))
                                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.dir_name_label)
                                        .addComponent(this.dir_name_textfield, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.account_label)
                                        .addComponent(this.account_combobox)
                                        .addComponent(this.copy_email_button, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(this.used_space_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.add_files_button)
                                        .addComponent(this.add_folder_button))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.upload_log_checkbox)
                                        .addComponent(this.priority_checkbox))
                                .addContainerGap())
        );

        this.dance_button.setBackground(new java.awt.Color(102, 204, 255));
        this.dance_button.setFont(new java.awt.Font("Dialog", 1, 22)); // NOI18N
        this.dance_button.setForeground(new java.awt.Color(255, 255, 255));
        this.dance_button.setText("Let's dance, baby");
        this.dance_button.setDoubleBuffered(true);
        this.dance_button.setEnabled(false);
        this.dance_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileGrabberDialog.this.dance_buttonActionPerformed(evt);
            }
        });

        this.total_file_size_label.setFont(new java.awt.Font("Dialog", 1, 32)); // NOI18N
        this.total_file_size_label.setForeground(new java.awt.Color(0, 0, 255));
        this.total_file_size_label.setText("[---]");
        this.total_file_size_label.setDoubleBuffered(true);
        this.total_file_size_label.setEnabled(false);

        this.warning_label.setFont(new java.awt.Font("Dialog", 0, 16)); // NOI18N
        this.warning_label.setText("If you DO NOT want to transfer some folder or file you can REMOVE it (to select several items at the same time use CTRL + LMOUSE).");
        this.warning_label.setDoubleBuffered(true);
        this.warning_label.setEnabled(false);

        this.skip_rest_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.skip_rest_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.skip_rest_button.setText("REMOVE ALL EXCEPT THIS");
        this.skip_rest_button.setDoubleBuffered(true);
        this.skip_rest_button.setEnabled(false);
        this.skip_rest_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileGrabberDialog.this.skip_rest_buttonActionPerformed(evt);
            }
        });

        this.skip_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.skip_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-trash-can-30.png"))); // NOI18N
        this.skip_button.setText("REMOVE THIS");
        this.skip_button.setDoubleBuffered(true);
        this.skip_button.setEnabled(false);
        this.skip_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileGrabberDialog.this.skip_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(this.warning_label)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(this.total_file_size_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(this.skip_rest_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(this.skip_button)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(this.dance_button)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.total_file_size_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.warning_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.dance_button)
                                        .addComponent(this.skip_rest_button)
                                        .addComponent(this.skip_button))
                                .addContainerGap())
        );

        this.pack();
    }// </editor-fold>//GEN-END:initComponents

    private void add_files_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_files_buttonActionPerformed

        this.add_files_button.setText(LabelTranslatorSingleton.getInstance().translate("Adding files, please wait..."));
        this.add_files_button.setEnabled(false);
        this.add_folder_button.setEnabled(false);
        this.warning_label.setEnabled(false);
        this.skip_button.setEnabled(false);
        this.skip_rest_button.setEnabled(false);
        this.dance_button.setEnabled(false);
        this.dir_name_textfield.setEnabled(false);
        this.dir_name_label.setEnabled(false);
        this.upload_log_checkbox.setEnabled(false);
        this.priority_checkbox.setEnabled(false);

        final JFileChooser filechooser = new javax.swing.JFileChooser();

//        updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Add files");

        filechooser.setAcceptAllFileFilterUsed(false);

        filechooser.setMultiSelectionEnabled(true);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this.total_file_size_label.setText("[0 B]");

            final File[] files_selected = filechooser.getSelectedFiles();

            this._base_path = files_selected[0].getParentFile().getAbsolutePath();

            this.dir_name_textfield.setText(files_selected[0].getParentFile().getName());

            this.dir_name_textfield.setEnabled(true);

            this.dir_name_label.setEnabled(true);

            final DefaultMutableTreeNode root = new DefaultMutableTreeNode(filechooser.getSelectedFile().getParent());

            for (final File file : files_selected) {

                final DefaultMutableTreeNode current_file = new DefaultMutableTreeNode(file.getName() + (file.isFile() ? " [" + formatBytes(file.length()) + "]" : ""));

                root.add(current_file);
            }

//            final DefaultTreeModel tree_model = new DefaultTreeModel(sortTree(root));

//            this.file_tree.setModel(tree_model);

            THREAD_POOL.execute(() -> {
                this._genFileList();
                MiscTools.GUIRun(() -> {
                    this.add_files_button.setEnabled(true);

                    this.add_folder_button.setEnabled(true);

                    this.add_files_button.setText(LabelTranslatorSingleton.getInstance().translate("Add files"));

//                    final boolean root_childs = ((TreeNode) tree_model.getRoot()).getChildCount() > 0;

//                    this.file_tree.setRootVisible(root_childs);
//                    this.file_tree.setEnabled(root_childs);
//                    this.warning_label.setEnabled(root_childs);
//                    this.dance_button.setEnabled(root_childs);
//                    this.total_file_size_label.setEnabled(root_childs);
//                    this.skip_button.setEnabled(root_childs);
//                    this.skip_rest_button.setEnabled(root_childs);
//                    this.upload_log_checkbox.setEnabled(root_childs);
//                    this.priority_checkbox.setEnabled(root_childs);
                });
            });

        } else {

            if (filechooser.getSelectedFile() != null && !filechooser.getSelectedFile().canRead()) {

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("File is not readable!"), "Error", JOptionPane.ERROR_MESSAGE);
            }

            final boolean root_childs = ((TreeNode) this.file_tree.getModel().getRoot()).getChildCount() > 0;

            this.add_files_button.setText(LabelTranslatorSingleton.getInstance().translate("Add files"));
            this.add_files_button.setEnabled(true);
            this.add_folder_button.setEnabled(true);
            this.file_tree.setRootVisible(root_childs);
            this.file_tree.setEnabled(root_childs);
            this.warning_label.setEnabled(root_childs);
            this.dance_button.setEnabled(root_childs);
            this.total_file_size_label.setEnabled(root_childs);
            this.skip_button.setEnabled(root_childs);
            this.skip_rest_button.setEnabled(root_childs);
            this.dir_name_textfield.setEnabled(root_childs);
            this.dir_name_label.setEnabled(root_childs);
            this.upload_log_checkbox.setEnabled(root_childs);
            this.priority_checkbox.setEnabled(root_childs);

        }
    }//GEN-LAST:event_add_files_buttonActionPerformed

    private void add_folder_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_folder_buttonActionPerformed

        this.add_folder_button.setText(LabelTranslatorSingleton.getInstance().translate("Adding folder, please wait..."));

        this.add_files_button.setEnabled(false);
        this.add_folder_button.setEnabled(false);
        this.warning_label.setEnabled(false);
        this.skip_button.setEnabled(false);
        this.skip_rest_button.setEnabled(false);
        this.dance_button.setEnabled(false);
        this.dir_name_textfield.setEnabled(false);
        this.dir_name_label.setEnabled(false);
        this.upload_log_checkbox.setEnabled(false);
        this.priority_checkbox.setEnabled(false);

        final JFileChooser filechooser = new javax.swing.JFileChooser();

//        updateFonts(filechooser, GUI_FONT, (float) (this._main_panel.getZoom_factor() * 1.2));

        filechooser.setDialogTitle("Add directory");

        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            THREAD_POOL.execute(() -> {
                MiscTools.GUIRunAndWait(() -> {
                    this.total_file_size_label.setText("[0 B]");

                    this._base_path = filechooser.getSelectedFile().getAbsolutePath();

                    this.dir_name_textfield.setText(filechooser.getSelectedFile().getName());

                    this.dir_name_textfield.setEnabled(true);

                    this.dir_name_label.setEnabled(true);
                });

                final DefaultMutableTreeNode root = new DefaultMutableTreeNode(filechooser.getSelectedFile().getAbsolutePath());

                this._genFileTree(filechooser.getSelectedFile().getAbsolutePath(), root, null);

//                final DefaultTreeModel tree_model = new DefaultTreeModel(sortTree(root));

                MiscTools.GUIRunAndWait(() -> {
//                    this.file_tree.setModel(tree_model);
                });

                this._genFileList();

                MiscTools.GUIRunAndWait(() -> {
                    this.add_files_button.setEnabled(true);

                    this.add_folder_button.setEnabled(true);

                    this.add_folder_button.setText(LabelTranslatorSingleton.getInstance().translate("Add folder"));

//                    final boolean root_childs = ((TreeNode) tree_model.getRoot()).getChildCount() > 0;

//                    this.file_tree.setRootVisible(root_childs);
//                    this.file_tree.setEnabled(root_childs);
//                    this.warning_label.setEnabled(root_childs);
//                    this.dance_button.setEnabled(root_childs);
//                    this.total_file_size_label.setEnabled(root_childs);
//                    this.skip_button.setEnabled(root_childs);
//                    this.skip_rest_button.setEnabled(root_childs);
//                    this.upload_log_checkbox.setEnabled(root_childs);
//                    this.priority_checkbox.setEnabled(root_childs);
                });
            });

        } else {

            if (filechooser.getSelectedFile() != null && !filechooser.getSelectedFile().canRead()) {

                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("Folder is not readable!"), "Error", JOptionPane.ERROR_MESSAGE);
            }

            final boolean root_childs = ((TreeNode) this.file_tree.getModel().getRoot()).getChildCount() > 0;

            this.add_folder_button.setText(LabelTranslatorSingleton.getInstance().translate("Add folder"));
            this.add_files_button.setEnabled(true);
            this.add_folder_button.setEnabled(true);
            this.file_tree.setRootVisible(root_childs);
            this.file_tree.setEnabled(root_childs);
            this.warning_label.setEnabled(root_childs);
            this.dance_button.setEnabled(root_childs);
            this.total_file_size_label.setEnabled(root_childs);
            this.skip_button.setEnabled(root_childs);
            this.skip_rest_button.setEnabled(root_childs);
            this.dir_name_textfield.setEnabled(root_childs);
            this.dir_name_label.setEnabled(root_childs);
            this.upload_log_checkbox.setEnabled(root_childs);
            this.priority_checkbox.setEnabled(root_childs);

        }

    }//GEN-LAST:event_add_folder_buttonActionPerformed

    private void dance_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dance_buttonActionPerformed

        this._upload = true;

        this.setVisible(false);
    }//GEN-LAST:event_dance_buttonActionPerformed

    private void account_comboboxItemStateChanged(final java.awt.event.ItemEvent evt) {//GEN-FIRST:event_account_comboboxItemStateChanged

        final String selected_item = (String) this.account_combobox.getSelectedItem();

        if (!this._inserting_mega_accounts && selected_item != null && this.account_combobox.getSelectedIndex() != this._last_selected_index) {

            this._last_selected_index = this.account_combobox.getSelectedIndex();

            final String email = selected_item;

            final Dialog tthis = this;

            this.used_space_label.setForeground(new Color(102, 102, 102));

            this.used_space_label.setText(LabelTranslatorSingleton.getInstance().translate("Checking account quota, please wait..."));

            this.account_combobox.setEnabled(false);
            this.copy_email_button.setEnabled(false);
            this.account_label.setEnabled(false);
            this.dance_button.setEnabled(false);
            this.add_files_button.setEnabled(false);
            this.add_folder_button.setEnabled(false);
            this.dir_name_textfield.setEnabled(false);
            this.dir_name_label.setEnabled(false);
            this.total_file_size_label.setEnabled(false);
            this.skip_button.setEnabled(false);
            this.skip_rest_button.setEnabled(false);
            this.warning_label.setEnabled(false);
            this.file_tree.setEnabled(false);

            THREAD_POOL.execute(() -> {
                MegaAPI ma = null;
                try {
                    this._quota_ok = false;
                    ma = checkMegaAccountLoginAndShowMasterPassDialog(this._main_panel, email);
                    final Long[] quota = ma.getQuota();
                    if (this.isDisplayable()) {
                        if (quota != null) {
                            final Color used_space_color;
                            if (quota[0] <= Math.round((double) quota[1] / 2)) {

                                used_space_color = new Color(0, 170, 0);

                            } else if (quota[0] < quota[1]) {

                                used_space_color = new Color(230, 115, 0);

                            } else {

                                used_space_color = Color.red;
                            }
                            final String quota_m = LabelTranslatorSingleton.getInstance().translate("Quota used: ") + formatBytes(quota[0]) + "/" + formatBytes(quota[1]);
                            this._quota_ok = true;
                            MiscTools.GUIRun(() -> {
                                final boolean root_childs = ((TreeNode) this.file_tree.getModel().getRoot()).getChildCount() > 0;

                                this.used_space_label.setText(quota_m);

                                this.used_space_label.setForeground(used_space_color);

                                for (final JComponent c : new JComponent[]{this.copy_email_button, this.used_space_label, this.add_files_button, this.add_folder_button, this.account_combobox, this.account_label, this.upload_log_checkbox, this.priority_checkbox}) {

                                    c.setEnabled(true);
                                }

                                for (final JComponent c : new JComponent[]{this.dir_name_textfield, this.dir_name_label, this.warning_label, this.dance_button, this.file_tree, this.total_file_size_label, this.skip_button, this.skip_rest_button}) {

                                    c.setEnabled(root_childs);
                                }
                            });
                        } else {
                            MiscTools.GUIRun(() -> {
                                this.account_combobox.setEnabled(true);
                                this.account_label.setEnabled(true);
                                this.account_combobox.setSelectedIndex(-1);
                                this.copy_email_button.setEnabled(true);
                                this.used_space_label.setForeground(Color.red);
                                this.used_space_label.setText(LabelTranslatorSingleton.getInstance().translate("ERROR checking account quota!"));
                                this.used_space_label.setEnabled(true);
                                this._last_selected_index = this.account_combobox.getSelectedIndex();
                                this.dance_button.setEnabled(false);
                                this.total_file_size_label.setEnabled(false);
                                this.skip_button.setEnabled(false);
                                this.skip_rest_button.setEnabled(false);
                                this.warning_label.setEnabled(false);
                                this.file_tree.setEnabled(false);
                                this.add_files_button.setEnabled(false);
                                this.add_folder_button.setEnabled(false);
                                this.upload_log_checkbox.setEnabled(false);
                                this.priority_checkbox.setEnabled(false);
                                this.dir_name_textfield.setEnabled(false);
                                this.dir_name_label.setEnabled(false);
                            });
                        }
                    }
                } catch (final Exception ex) {
                    MiscTools.GUIRun(() -> {
                        this.account_combobox.setEnabled(true);
                        this.account_label.setEnabled(true);
                        this.account_combobox.setSelectedIndex(-1);
                        this.copy_email_button.setEnabled(true);
                        this.used_space_label.setForeground(Color.red);
                        this.used_space_label.setText(LabelTranslatorSingleton.getInstance().translate("ERROR checking account quota!"));
                        this.used_space_label.setEnabled(true);
                        this._last_selected_index = this.account_combobox.getSelectedIndex();
                        this.dance_button.setEnabled(false);
                        this.total_file_size_label.setEnabled(false);
                        this.skip_button.setEnabled(false);
                        this.skip_rest_button.setEnabled(false);
                        this.warning_label.setEnabled(false);
                        this.file_tree.setEnabled(false);
                        this.add_files_button.setEnabled(false);
                        this.add_folder_button.setEnabled(false);
                        this.upload_log_checkbox.setEnabled(false);
                        this.priority_checkbox.setEnabled(false);
                        this.dir_name_textfield.setEnabled(false);
                        this.dir_name_label.setEnabled(false);
                    });
                }
            });

        }
    }//GEN-LAST:event_account_comboboxItemStateChanged

    private void skip_rest_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skip_rest_buttonActionPerformed

//        if (deleteAllExceptSelectedTreeItems(this.file_tree)) {
//
//            this._genFileList();
//
//            this.warning_label.setEnabled(true);
//            this.dance_button.setEnabled(true);
//            this.total_file_size_label.setEnabled(true);
//            this.skip_button.setEnabled(true);
//            this.skip_rest_button.setEnabled(true);
//            this.dir_name_textfield.setEnabled(true);
//            this.dir_name_label.setEnabled(true);
//        }
    }//GEN-LAST:event_skip_rest_buttonActionPerformed

    private void skip_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_skip_buttonActionPerformed

//        if (deleteSelectedTreeItems(this.file_tree)) {
//
//            this._genFileList();
//
//            final boolean root_childs = ((TreeNode) this.file_tree.getModel().getRoot()).getChildCount() > 0;
//
//            this.warning_label.setEnabled(root_childs);
//            this.dance_button.setEnabled(root_childs);
//            this.total_file_size_label.setEnabled(root_childs);
//            this.skip_button.setEnabled(root_childs);
//            this.skip_rest_button.setEnabled(root_childs);
//            this.dir_name_textfield.setEnabled(root_childs);
//            this.dir_name_label.setEnabled(root_childs);
//
//            if (!root_childs) {
//
//                this.dir_name_textfield.setText("");
//            }
//        }
    }//GEN-LAST:event_skip_buttonActionPerformed

    private void copy_email_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_email_buttonActionPerformed
        // TODO add your handling code here:
        if (this.account_combobox.getSelectedIndex() >= 0) {
            this.copy_email_button.setEnabled(false);

            this.copy_email_button.setText(LabelTranslatorSingleton.getInstance().translate("Please wait..."));

            copyTextToClipboard((String) this.account_combobox.getSelectedItem());

            this.copy_email_button.setText(LabelTranslatorSingleton.getInstance().translate("Copy email"));

            this.copy_email_button.setEnabled(true);
        }
    }//GEN-LAST:event_copy_email_buttonActionPerformed

    private void _genFileTree(final String directoryName, final DefaultMutableTreeNode root, final File[] files) {

        final File directory = new File(directoryName);

        final File[] fList = files == null ? directory.listFiles() : files;

        if (fList != null) {

            for (final File file : fList) {

                if (file.isFile() && file.canRead()) {

                    final DefaultMutableTreeNode current_file = new DefaultMutableTreeNode(file.getName() + " [" + formatBytes(file.length()) + "]");

                    root.add(current_file);

                } else if (file.isDirectory() && file.canRead() && file.listFiles().length > 0) {

                    if (files == null || files.length > 1) {

                        final DefaultMutableTreeNode current_dir = new DefaultMutableTreeNode(file.getName());

                        root.add(current_dir);

                        this._genFileTree(file.getAbsolutePath(), current_dir, null);

                    } else {
                        this._genFileTree(file.getAbsolutePath(), root, null);
                    }

                }
            }

        }
    }

    private void _genFileList() {

        try {
            this._files.clear();

            this._total_space = 0L;

            final DefaultTreeModel tree_model = (DefaultTreeModel) (MiscTools.futureRun((Callable) this.file_tree::getModel).get());

            final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree_model.getRoot();

            final Enumeration files_tree = root.depthFirstEnumeration();

            while (files_tree.hasMoreElements()) {

                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) files_tree.nextElement();

                if (node.isLeaf() && node != root) {

                    String path = "";

                    final Object[] object_path = node.getUserObjectPath();

                    for (final Object p : object_path) {

                        path += File.separator + p;
                    }

                    path = path.replaceAll("^/+", "/").replaceAll("^\\+", "\\").trim().replaceAll(" \\[[0-9,.]+ [A-Z]+\\]$", "");

                    final File file = new File(path);

                    if (file.isFile()) {

                        this._total_space += file.length();

                        this._files.add(file);
                    }
                }
            }

            MiscTools.GUIRun(() -> {
                this.total_file_size_label.setText("[" + formatBytes(this._total_space) + "]");
            });
        } catch (final InterruptedException ex) {
            Logger.getLogger(FileGrabberDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (final ExecutionException ex) {
            Logger.getLogger(FileGrabberDialog.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> account_combobox;
    private javax.swing.JLabel account_label;
    private javax.swing.JButton add_files_button;
    private javax.swing.JButton add_folder_button;
    private javax.swing.JButton copy_email_button;
    private javax.swing.JButton dance_button;
    private javax.swing.JLabel dir_name_label;
    private javax.swing.JTextField dir_name_textfield;
    private javax.swing.JTree file_tree;
    private javax.swing.JScrollPane file_tree_scrollpane;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JCheckBox priority_checkbox;
    private javax.swing.JButton skip_button;
    private javax.swing.JButton skip_rest_button;
    private javax.swing.JLabel total_file_size_label;
    private javax.swing.JCheckBox upload_log_checkbox;
    private javax.swing.JLabel used_space_label;
    private javax.swing.JLabel warning_label;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(FileGrabberDialog.class.getName());

    private void _file_drop_notify(final List<File> files) {

        MiscTools.GUIRunAndWait(() -> {
            this.add_files_button.setEnabled(false);
            this.add_folder_button.setEnabled(false);
            this.warning_label.setEnabled(false);
            this.skip_button.setEnabled(false);
            this.skip_rest_button.setEnabled(false);
            this.dance_button.setEnabled(false);
            this.dir_name_textfield.setEnabled(false);
            this.dir_name_label.setEnabled(false);
            this.upload_log_checkbox.setEnabled(false);
            this.priority_checkbox.setEnabled(false);
            this.total_file_size_label.setText("[0 B]");
        });

        this._base_path = (files.size() == 1 && files.get(0).isDirectory()) ? files.get(0).getAbsolutePath() : files.get(0).getParentFile().getAbsolutePath();

        MiscTools.GUIRunAndWait(() -> {
            this.dir_name_textfield.setText(((files.size() == 1 && files.get(0).isDirectory()) ? files.get(0).getName() : files.get(0).getParentFile().getName()));

            this.dir_name_textfield.setEnabled(true);

            this.dir_name_label.setEnabled(true);
        });

        final DefaultMutableTreeNode root = new DefaultMutableTreeNode(this._base_path);

        MiscTools.GUIRunAndWait(() -> {
            this.dance_button.setText(LabelTranslatorSingleton.getInstance().translate("Loading files, please wait..."));
        });

        this._genFileTree(this._base_path, root, files.toArray(new File[files.size()]));

//        final DefaultTreeModel tree_model = new DefaultTreeModel(sortTree(root));

        MiscTools.GUIRunAndWait(() -> {
//            this.file_tree.setModel(tree_model);
        });

        this._genFileList();

        MiscTools.GUIRunAndWait(() -> {
            this.dance_button.setText(LabelTranslatorSingleton.getInstance().translate("Let's dance, baby"));

            if (this._last_selected_index != -1 && this._quota_ok) {
                this.add_files_button.setEnabled(true);
                this.add_folder_button.setEnabled(true);
                this.file_tree.setRootVisible(true);
                this.file_tree.setEnabled(true);
                this.warning_label.setEnabled(true);
                this.dance_button.setEnabled(true);
                this.total_file_size_label.setEnabled(true);
                this.skip_button.setEnabled(true);
                this.skip_rest_button.setEnabled(true);
                this.dir_name_textfield.setEnabled(true);
                this.dir_name_label.setEnabled(true);
                this.upload_log_checkbox.setEnabled(true);
                this.priority_checkbox.setEnabled(true);
            }
        });
    }
}
