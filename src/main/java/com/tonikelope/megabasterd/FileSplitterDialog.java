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
public class FileSplitterDialog extends javax.swing.JDialog {

    private final MainPanel _main_panel;
    private File[] _files = null;
    private File _output_dir = null;
    private volatile String _sha1 = null;
    private volatile long _progress = 0L;
    private volatile Path _current_part = null;
    private volatile int _current_file = 0;
    private volatile boolean _exit = false;

    /**
     * Creates new form FileSplitterDialog
     */
    public FileSplitterDialog(final MainPanelView parent, final boolean modal) {
        super(parent, modal);
        this._main_panel = parent.getMain_panel();

        MiscTools.GUIRunAndWait(() -> {
            this.initComponents();

//            updateFonts(this, GUI_FONT, _main_panel.getZoom_factor());
//
//            translateLabels(this);

            this.jProgressBar2.setMinimum(0);
            this.jProgressBar2.setMaximum(MAX_VALUE);
            this.jProgressBar2.setStringPainted(true);
            this.jProgressBar2.setValue(0);
            this.jProgressBar2.setVisible(false);

            this.split_size_text.addKeyListener(new java.awt.event.KeyAdapter() {

                @Override
                public void keyReleased(final java.awt.event.KeyEvent evt) {
                    try {
                        Integer.parseInt(FileSplitterDialog.this.split_size_text.getText());
                    } catch (final Exception e) {
                        FileSplitterDialog.this.split_size_text.setText(FileSplitterDialog.this.split_size_text.getText().substring(0, Math.max(0, FileSplitterDialog.this.split_size_text.getText().length() - 1)));
                    }
                }
            });

            this.split_size_text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            this.pack();
        });
    }

    private boolean _splitFile(final int i) throws IOException {

        this._sha1 = "";

        THREAD_POOL.execute(() -> {

            try {
                this._sha1 = MiscTools.computeFileSHA1(new File(this._files[i].getAbsolutePath()));
            } catch (final IOException ex) {
                Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        this._progress = 0L;

        final int mBperSplit = Integer.parseInt(this.split_size_text.getText());

        if (mBperSplit <= 0) {
            throw new IllegalArgumentException("mBperSplit must be more than zero");
        }

        final long sourceSize = Files.size(Paths.get(this._files[i].getAbsolutePath()));
        final long bytesPerSplit = 1024L * 1024L * mBperSplit;
        final long numSplits = sourceSize / bytesPerSplit;
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int conta_split = 1;

        MiscTools.GUIRunAndWait(() -> {
            this.jProgressBar2.setMinimum(0);
            this.jProgressBar2.setMaximum(MAX_VALUE);
            this.jProgressBar2.setStringPainted(true);
            this.jProgressBar2.setValue(0);
            this.file_name_label.setText(truncateText(this._files[i].getName(), 150));
            this.file_name_label.setToolTipText(this._files[i].getAbsolutePath());
            this.file_size_label.setText(MiscTools.formatBytes(this._files[i].length()));
            this.pack();

        });

        try (final RandomAccessFile sourceFile = new RandomAccessFile(this._files[i].getAbsolutePath(), "r"); final FileChannel sourceChannel = sourceFile.getChannel()) {

            for (; position < numSplits && !this._exit; position++, conta_split++) {
                this._writePartToFile(i, bytesPerSplit, position * bytesPerSplit, sourceChannel, conta_split, numSplits + (remainingBytes > 0 ? 1 : 0));
            }

            if (remainingBytes > 0 && !this._exit) {
                this._writePartToFile(i, remainingBytes, position * bytesPerSplit, sourceChannel, conta_split, numSplits + (remainingBytes > 0 ? 1 : 0));
            }
        }

        while ("".equals(this._sha1)) {
            MiscTools.GUIRunAndWait(() -> {

                this.split_button.setText(LabelTranslatorSingleton.getInstance().translate("GENERATING SHA1, please wait..."));

            });

            MiscTools.pausar(1000);
        }

        if (this._sha1 != null) {
            Files.writeString(Paths.get(this._files[i].getAbsolutePath() + ".sha1"), this._sha1);
        }

        return true;
    }

    private void monitorProgress(final int f, final long part_size) {

        THREAD_POOL.execute(() -> {

            long p = 0;

            final Path file = this._current_part;

            while (!this._exit && f == this._current_file && file == this._current_part && p < part_size) {
                try {
                    if (Files.exists(this._current_part)) {

                        p = Files.size(file);

                        final long fp = this._progress + p;

                        MiscTools.GUIRunAndWait(() -> {
                            if (this.jProgressBar2.getValue() < this.jProgressBar2.getMaximum()) {
                                this.jProgressBar2.setValue((int) Math.floor((MAX_VALUE / (double) this._files[f].length()) * fp));
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

    private void _writePartToFile(final int f, final long byteSize, final long position, final FileChannel sourceChannel, final int conta_split, final long num_splits) throws IOException {

        final Path fileName = Paths.get(this._output_dir.getAbsolutePath() + "/" + this._files[f].getName() + ".part" + String.valueOf(conta_split) + "-" + String.valueOf(num_splits));

        this._current_part = fileName;

        this._current_file = f;

        this.monitorProgress(f, byteSize);

        if (!this._exit) {
            try (final RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw"); final FileChannel toChannel = toFile.getChannel()) {
                sourceChannel.position(position);
                toChannel.transferFrom(sourceChannel, 0, byteSize);
            }
        }

        this._progress += byteSize;
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
        this.split_size_label = new javax.swing.JLabel();
        this.split_size_text = new javax.swing.JTextField();
        this.jProgressBar2 = new javax.swing.JProgressBar();
        this.split_button = new javax.swing.JButton();

        this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setTitle("File Splitter");
        this.setResizable(false);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(final java.awt.event.WindowEvent evt) {
                FileSplitterDialog.this.formWindowClosing(evt);
            }
        });

        this.file_button.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.file_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-add-file-30.png"))); // NOI18N
        this.file_button.setText("Select file/s");
        this.file_button.setDoubleBuffered(true);
        this.file_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileSplitterDialog.this.file_buttonActionPerformed(evt);
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
                FileSplitterDialog.this.output_buttonActionPerformed(evt);
            }
        });

        this.file_size_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.file_size_label.setDoubleBuffered(true);

        this.output_folder_label.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.output_folder_label.setDoubleBuffered(true);

        this.split_size_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.split_size_label.setText("Split size (MBs):");
        this.split_size_label.setDoubleBuffered(true);
        this.split_size_label.setEnabled(false);

        this.split_size_text.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.split_size_text.setBorder(null);
        this.split_size_text.setDoubleBuffered(true);
        this.split_size_text.setEnabled(false);

        this.jProgressBar2.setFont(new java.awt.Font("Dialog", 0, 18)); // NOI18N
        this.jProgressBar2.setDoubleBuffered(true);

        this.split_button.setBackground(new java.awt.Color(102, 204, 255));
        this.split_button.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        this.split_button.setForeground(new java.awt.Color(255, 255, 255));
        this.split_button.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-cut-30.png"))); // NOI18N
        this.split_button.setText("SPLIT FILE/s");
        this.split_button.setDoubleBuffered(true);
        this.split_button.setEnabled(false);
        this.split_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                FileSplitterDialog.this.split_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(this.file_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.file_name_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.output_button, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                                        .addComponent(this.file_size_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.output_folder_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(this.split_size_label)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(this.split_size_text))
                                        .addComponent(this.split_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.split_size_label)
                                        .addComponent(this.split_size_text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(this.jProgressBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(this.split_button)
                                .addContainerGap())
        );

        this.pack();
    }// </editor-fold>//GEN-END:initComponents

    private void file_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_file_buttonActionPerformed
        // TODO add your handling code here:

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Opening file..."));

        this.file_button.setEnabled(false);

        final JFileChooser filechooser = new javax.swing.JFileChooser();

        filechooser.setMultiSelectionEnabled(true);

//        updateFonts(filechooser, GUI_FONT, (float) (_main_panel.getZoom_factor() * 1.25));

        filechooser.setDialogTitle("Select file/s");

        filechooser.setAcceptAllFileFilterUsed(false);

        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && filechooser.getSelectedFile().canRead()) {

            this._files = filechooser.getSelectedFiles();
            this.file_name_label.setText(truncateText(this._files[0].getName(), 150));
            this.file_name_label.setToolTipText(this._files[0].getAbsolutePath());
            this.file_size_label.setText(MiscTools.formatBytes(this._files[0].length()));
            this.output_folder_label.setText(truncateText(this._files[0].getParentFile().getAbsolutePath(), 150));
            this.output_folder_label.setToolTipText(this._files[0].getParentFile().getAbsolutePath());
            this._output_dir = new File(this._files[0].getParentFile().getAbsolutePath());
            this.jProgressBar2.setMinimum(0);
            this.jProgressBar2.setMaximum(MAX_VALUE);
            this.jProgressBar2.setStringPainted(true);
            this.jProgressBar2.setValue(0);

            this.output_button.setEnabled(true);
            this.split_size_label.setEnabled(true);
            this.split_size_text.setEnabled(true);
            this.split_button.setEnabled(true);
        }

        this.file_button.setText(LabelTranslatorSingleton.getInstance().translate("Select file/s"));

        this.file_button.setEnabled(true);

        this.pack();

    }//GEN-LAST:event_file_buttonActionPerformed

    private void output_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_buttonActionPerformed
        // TODO add your handling code here:

        this.output_button.setText(LabelTranslatorSingleton.getInstance().translate("Changing output folder..."));

        this.file_button.setEnabled(false);

        this.output_button.setEnabled(false);

        this.split_button.setEnabled(false);

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

        this.split_button.setEnabled(true);

        this.pack();
    }//GEN-LAST:event_output_buttonActionPerformed

    private void split_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_split_buttonActionPerformed
        // TODO add your handling code here:

        if (this._output_dir != null && !"".equals(this.split_size_text.getText())) {

            this.split_button.setText(LabelTranslatorSingleton.getInstance().translate("SPLITTING FILE..."));

            this.file_button.setEnabled(false);

            this.output_button.setEnabled(false);

            this.split_button.setEnabled(false);

            this.split_size_text.setEnabled(false);

            this.jProgressBar2.setVisible(true);

            this.pack();

            final Dialog tthis = this;

            THREAD_POOL.execute(() -> {
                try {
                    for (int i = 0; i < this._files.length && !this._exit; i++) {

                        if (this._splitFile(i)) {

                            if (i == this._files.length - 1 && !this._exit) {

                                MiscTools.GUIRun(() -> {

                                    JOptionPane.showMessageDialog(tthis, LabelTranslatorSingleton.getInstance().translate("File/s successfully splitted!"));

                                    if (Desktop.isDesktopSupported()) {
                                        try {
                                            Desktop.getDesktop().open(this._output_dir);
                                        } catch (final Exception ex) {
                                            Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                                        }
                                    }

                                    this._exit = true;

                                    this.dispose();
                                });
                            }

                        } else {
                            this._files = null;
                            this._output_dir = null;
                            MiscTools.GUIRun(() -> {
                                this.file_name_label.setText("");

                                this.output_folder_label.setText("");

                                this.split_size_text.setText("");

                                this.file_size_label.setText("");

                                this.jProgressBar2.setMinimum(0);
                                this.jProgressBar2.setMaximum(MAX_VALUE);
                                this.jProgressBar2.setStringPainted(true);
                                this.jProgressBar2.setValue(0);
                                this.jProgressBar2.setVisible(false);

                                this.split_button.setText(LabelTranslatorSingleton.getInstance().translate("SPLIT FILE"));

                                this.file_button.setEnabled(true);

                                this.output_button.setEnabled(true);

                                this.split_button.setEnabled(true);

                                this.split_size_text.setEnabled(true);

                                this.pack();
                            });
                        }

                    }
                } catch (final Exception ex) {
                    Logger.getLogger(FileSplitterDialog.class.getName()).log(Level.SEVERE, ex.getMessage());
                }
            });

        }
    }//GEN-LAST:event_split_buttonActionPerformed

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

            this._main_panel.getView().getSplit_file_menu().setEnabled(this.file_button.isEnabled());

            this.dispose();
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton file_button;
    private javax.swing.JLabel file_name_label;
    private javax.swing.JLabel file_size_label;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JButton output_button;
    private javax.swing.JLabel output_folder_label;
    private javax.swing.JButton split_button;
    private javax.swing.JLabel split_size_label;
    private javax.swing.JTextField split_size_text;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(FileSplitterDialog.class.getName());
}
