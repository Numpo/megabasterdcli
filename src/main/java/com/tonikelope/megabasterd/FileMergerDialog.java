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
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tonikelope.megabasterd.MainPanel.THREAD_POOL;
import static com.tonikelope.megabasterd.MiscTools.truncateText;
import static java.lang.Integer.MAX_VALUE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

/**
 * @author tonikelope
 */
public class FileMergerDialog extends javax.swing.JDialog {

    private final MainPanel _main_panel;
    private File _output_dir = null;
    private volatile long _progress = 0L;
    private final ArrayList<String> _file_parts = new ArrayList<>();
    private String _file_name = null;
    private long _file_size = 0L;
    private volatile boolean _exit = false;
    private volatile String _file_name_full;

    /**
     * Creates new form FileSplitterDialog
     */
    public FileMergerDialog(final MainPanelView parent, final boolean modal) {
        super(parent, modal);
        this._main_panel = parent.getMain_panel();

        MiscTools.GUIRunAndWait(() -> {
            this.initComponents();
//            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());
//            translateLabels(this);
            this.jProgressBar2.setMinimum(0);
            this.jProgressBar2.setMaximum(MAX_VALUE);
            this.jProgressBar2.setStringPainted(true);
            this.jProgressBar2.setValue(0);
            this.jProgressBar2.setVisible(false);

            this.pack();
        });
    }

    private void monitorProgress(final Path file) {

        THREAD_POOL.execute(() -> {

            long p = 0;

            while (!this._exit && p < this._file_size) {

                try {

                    if (Files.exists(file)) {

                        p = Files.size(file);

                        final long fp = p;

                        MiscTools.GUIRunAndWait(() -> {
                            if (this.jProgressBar2.getValue() < this.jProgressBar2.getMaximum()) {
                                this.jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) this._file_size) * fp));
                            }
                        });
                    }

                    MiscTools.pausar(2000);

                } catch (final IOException ex) {
                    Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });

    }

    private boolean _mergeFile() throws IOException {

        try (final RandomAccessFile targetFile = new RandomAccessFile(this._file_name_full, "rw")) {

            final FileChannel targetChannel = targetFile.getChannel();

            this.monitorProgress(Paths.get(this._file_name));

            for (final String file_path : this._file_parts) {

                if (this._exit) {
                    break;
                }

                final RandomAccessFile rfile = new RandomAccessFile(file_path, "r");

                targetChannel.transferFrom(rfile.getChannel(), this._progress, rfile.length());

                this._progress += rfile.length();

                MiscTools.GUIRun(() -> {
                    this.jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) this._file_size) * this._progress));
                });
            }
        }

        if (Files.exists(Paths.get(this._file_name_full + ".sha1"))) {

            final String sha1 = Files.readString(Paths.get(this._file_name_full + ".sha1")).toLowerCase().trim();

            MiscTools.GUIRunAndWait(() -> {
                this.merge_button.setText(LabelTranslatorSingleton.getInstance().translate("CHECKING FILE INTEGRITY, please wait..."));
            });

            if (sha1.equals(MiscTools.computeFileSHA1(new File(this._file_name_full)))) {
                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("FILE INTEGRITY IS OK"));
                return true;
            } else {
                JOptionPane.showMessageDialog(this, LabelTranslatorSingleton.getInstance().translate("FILE INTEGRITY CHECK FAILED"), "ERROR", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        return true;
    }

    private void _deleteParts() {

        try {
            this._file_parts.stream().map((file_path) -> new File(file_path)).forEachOrdered((file) -> {
                file.delete();
            });

            Files.deleteIfExists(Paths.get(this._file_name_full + ".sha1"));
        } catch (final IOException ex) {
            Logger.getLogger(FileMergerDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        this.file_button = new javax.swing.JButton();
        this.file_name_label = new javax.swing.JLabel();
        this.output_button = new javax.swing.JButton();
        this.file_size_label = new javax.swing.JLabel();
        this.output_folder_label = new javax.swing.JLabel();
        this.jProgressBar2 = new javax.swing.JProgressBar();
        this.merge_button = new javax.swing.JButton();
        this.delete_parts_checkbox = new javax.swing.JCheckBox();

        this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setTitle("File Merger");
        this.setResizable(false);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(final java.awt.event.WindowEvent evt) {
                FileMergerDialog.this.formWindowClosing(evt);
            }
        });

        this.file_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.file_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-add-file-30.png"))); // NOI18N
        this.file_button.setText("Select (any) file part");
        this.file_button.setDoubleBuffered(true);
        this.file_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileMergerDialog.this.file_buttonActionPerformed(evt);
            }
        });

        this.file_name_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.file_name_label.setDoubleBuffered(true);

        this.output_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.output_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-add-folder-30.png"))); // NOI18N
        this.output_button.setText("Change output folder");
        this.output_button.setDoubleBuffered(true);
        this.output_button.setEnabled(false);
        this.output_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileMergerDialog.this.output_buttonActionPerformed(evt);
            }
        });

        this.file_size_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.file_size_label.setDoubleBuffered(true);

        this.output_folder_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.output_folder_label.setDoubleBuffered(true);

        this.jProgressBar2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.jProgressBar2.setDoubleBuffered(true);

        this.merge_button.setBackground(new java.awt.Color(102, 204, 255));
        this.merge_button.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        this.merge_button.setForeground(new java.awt.Color(255, 255, 255));
        this.merge_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-glue-30.png"))); // NOI18N
        this.merge_button.setText("MERGE FILE");
        this.merge_button.setDoubleBuffered(true);
        this.merge_button.setEnabled(false);
        this.merge_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileMergerDialog.this.merge_buttonActionPerformed(evt);
            }
        });

        this.delete_parts_checkbox.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        this.delete_parts_checkbox.setSelected(true);
        this.delete_parts_checkbox.setText("Delete parts after merge");
        this.delete_parts_checkbox.setDoubleBuffered(true);
        this.delete_parts_checkbox.setEnabled(false);

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(this.delete_parts_checkbox)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(this.file_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.file_name_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.output_button, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                                        .addComponent(this.file_size_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.output_folder_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.merge_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.file_button)
                                .addGap(9, 9, 9)
                                .addComponent(this.file_name_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.file_size_label)
                                .addGap(18, 18, 18)
                                .addComponent(this.output_button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.output_folder_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, Short.MAX_VALUE)
                                .addComponent(this.delete_parts_checkbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(this.merge_button)
                                .addContainerGap())
        );

        this.pack();
    }// </editor-fold>//GEN-END:initComponents

    private void file_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_file_buttonActionPerformed
        // TODO add your handling code here:

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Selecting file..."));

        this.file_button.setEnabled(false);

        final JFileChooser filechooser = new javax.swing.JFileChooser();

//        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Select any part of the original file");

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._file_name = MiscTools.findFirstRegex("^(.+)\\.part[0-9]+\\-[0-9]+$", filechooser.getSelectedFile().getName(), 1);

            this._file_name_full = MiscTools.findFirstRegex("^(.+)\\.part[0-9]+\\-[0-9]+$", filechooser.getSelectedFile().getAbsolutePath(), 1);

            if (this._file_name != null) {

                this.file_name_label.setText(truncateText(this._file_name, 150));

                this.file_name_label.setToolTipText(filechooser.getSelectedFile().getParentFile().getAbsolutePath() + "/" + this._file_name);

                this.output_folder_label.setText(truncateText(filechooser.getSelectedFile().getParentFile().getAbsolutePath(), 150));

                this.output_folder_label.setToolTipText(filechooser.getSelectedFile().getParentFile().getAbsolutePath());

                this._output_dir = new File(filechooser.getSelectedFile().getParentFile().getAbsolutePath());

                final File directory = filechooser.getSelectedFile().getParentFile();

                final File[] fList = directory.listFiles();

                this._file_size = 0L;

                for (final File file : fList) {

                    if (file.isFile() && file.canRead() && file.getName().startsWith(this._file_name + ".part")) {

                        this._file_parts.add(file.getAbsolutePath());

                        this._file_size += file.length();

                    }
                }

                Collections.sort(this._file_parts);

                this.file_size_label.setText(MiscTools.formatBytes(this._file_size));

                this.output_button.setEnabled(true);

                this.delete_parts_checkbox.setEnabled(true);

                this.merge_button.setEnabled(true);
            }

        }

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Select (any) file part"));

        this.file_button.setEnabled(true);

        this.pack();

    }//GEN-LAST:event_file_buttonActionPerformed

    private void output_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_buttonActionPerformed
        // TODO add your handling code here:

        this.output_button.setText(LabelTranslatorSingleton.getInstance().translate("Changing output folder..."));

        this.file_button.setEnabled(false);

        this.output_button.setEnabled(false);

        this.merge_button.setEnabled(false);

        this.delete_parts_checkbox.setEnabled(false);

        final JFileChooser filechooser = new javax.swing.JFileChooser();

//        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Add directory");

        filechooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._output_dir = filechooser.getSelectedFile();

            this.output_folder_label.setText(truncateText(this._output_dir.getAbsolutePath(), 100));

            this.output_folder_label.setToolTipText(this._output_dir.getAbsolutePath());
        }

        this.output_button.setText(LabelTranslatorSingleton.getInstance().translate("Change output folder"));

        this.file_button.setEnabled(true);

        this.output_button.setEnabled(true);

        this.merge_button.setEnabled(true);

        this.delete_parts_checkbox.setEnabled(true);

        this.pack();
    }//GEN-LAST:event_output_buttonActionPerformed

    private void merge_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_merge_buttonActionPerformed
        // TODO add your handling code here:

        if (this._output_dir != null) {

            this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            this.merge_button.setText(LabelTranslatorSingleton.getInstance().translate("MERGING FILE..."));

            this.file_button.setEnabled(false);

            this.output_button.setEnabled(false);

            this.merge_button.setEnabled(false);

            this.delete_parts_checkbox.setEnabled(false);

            this.jProgressBar2.setVisible(true);

            this.pack();

            final Dialog tthis = this;

            THREAD_POOL.execute(() -> {
                try {
                    if (this._mergeFile()) {
                        if (this.delete_parts_checkbox.isSelected()) {
                            this._deleteParts();
                        }

                        if (!this._exit) {
                            MiscTools.GUIRun(() -> {
                                this.jProgressBar2.setValue(this.jProgressBar2.getMaximum());

                                JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("File successfully merged!"));

                                if (Desktop.isDesktopSupported()) {
                                    try {
                                        Desktop.getDesktop().open(this._output_dir);
                                    } catch (final Exception ex) {
                                        Logger.getLogger(FileMergerDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                                    }
                                }

                                this._exit = true;
                                this.dispose();
                            });
                        }
                    } else {
                        this._file_parts.clear();
                        MiscTools.GUIRun(() -> {
                            this.file_name_label.setText("");

                            this.file_size_label.setText("");

                            this.output_folder_label.setText("");

                            this._output_dir = null;

                            this._file_name = null;

                            this._file_size = 0L;

                            this._progress = 0L;

                            this.jProgressBar2.setMinimum(0);
                            this.jProgressBar2.setMaximum(MAX_VALUE);
                            this.jProgressBar2.setStringPainted(true);
                            this.jProgressBar2.setValue(0);
                            this.jProgressBar2.setVisible(false);

                            this.merge_button.setText(LabelTranslatorSingleton.getInstance().translate("MERGE FILE"));

                            this.file_button.setEnabled(true);

                            this.output_button.setEnabled(true);

                            this.merge_button.setEnabled(true);

                            this.delete_parts_checkbox.setEnabled(true);

                            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

                            this.pack();
                        });
                    }
                } catch (final Exception ex) {
                    Logger.getLogger(FileMergerDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            });

        }
    }//GEN-LAST:event_merge_buttonActionPerformed

    private void formWindowClosing(final java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        final Object[] options = {"No",
                LabelTranslatorSingleton.getInstance().translate("Yes")};

        int n = 1;

        if (!this.file_button.isEnabled()) {
            n = showOptionDialog(this,
                    LabelTranslatorSingleton.getInstance().translate("SURE?"),
                    LabelTranslatorSingleton.getInstance().translate("EXIT"), YES_NO_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);
        }

        if (n == 1) {
            this._exit = true;

            this._main_panel.getView().getMerge_file_menu().setEnabled(this.file_button.isEnabled());

            this.dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox delete_parts_checkbox;
    private javax.swing.JButton file_button;
    private javax.swing.JLabel file_name_label;
    private javax.swing.JLabel file_size_label;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JButton merge_button;
    private javax.swing.JButton output_button;
    private javax.swing.JLabel output_folder_label;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(FileMergerDialog.class.getName());
}
