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

import java.util.logging.Logger;

/**
 * @author tonikelope
 */
public class WarningExitMessage extends javax.swing.JDialog {

    MainPanel _main_panel;
    boolean _restart;

    /**
     * Creates new form WarningExitMessage
     */
    public WarningExitMessage(final java.awt.Frame parent, final boolean modal, final MainPanel main_panel, final boolean restart) {
        super(parent, modal);
        MiscTools.GUIRunAndWait(() -> {
            this.initComponents();

//            updateFonts(this, GUI_FONT, main_panel.getZoom_factor());
//
//            translateLabels(this);

            this._main_panel = main_panel;

            this._restart = restart;

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

        this.jPanel1 = new javax.swing.JPanel();
        this.warning_label = new javax.swing.JLabel();
        this.exit_button = new javax.swing.JButton();

        this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setTitle("Exit");
        this.setUndecorated(true);

        this.jPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        this.warning_label.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.warning_label.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("/images/icons8-error-96.png"))); // NOI18N
        this.warning_label.setText("Megabasterd is stopping transferences safely, please wait...");

        this.exit_button.setBackground(new java.awt.Color(255, 0, 0));
        this.exit_button.setFont(new java.awt.Font("Dialog", 1, 18)); // NOI18N
        this.exit_button.setForeground(new java.awt.Color(255, 255, 255));
        this.exit_button.setText("EXIT NOW");
        this.exit_button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final java.awt.event.ActionEvent evt) {
                WarningExitMessage.this.exit_buttonActionPerformed(evt);
            }
        });

        final javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(this.jPanel1);
        this.jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(this.exit_button))
                                        .addComponent(this.warning_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.warning_label, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(this.exit_button)
                                .addContainerGap())
        );

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(this.jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        this.pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exit_buttonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exit_buttonActionPerformed

        this._main_panel.byebyenow(this._restart);
    }//GEN-LAST:event_exit_buttonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton exit_button;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel warning_label;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(WarningExitMessage.class.getName());
}
