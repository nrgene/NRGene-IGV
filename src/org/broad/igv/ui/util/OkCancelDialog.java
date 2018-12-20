/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

/*
 * PropertyDialog.java
 *
 * Created on December 19, 2007, 8:11 AM
 */

package org.broad.igv.ui.util;

import java.awt.Dialog;

import javax.swing.JPanel;

/**
 * @author eflakes
 */
abstract public class OkCancelDialog extends javax.swing.JDialog {

    private boolean isCanceled = true;

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Creates new form PropertyDialog
     */
    public OkCancelDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        if (parent != null) {
            int x = parent.getX() + (int) (parent.getWidth() / 2) -
                    (int) (getWidth() / 2);
            int y = parent.getY() + (int) (parent.getHeight() / 2) -
                    (int) (getHeight() / 2);
            setLocation(x, y);
        }
    }

    /**
     * Creates new form PropertyDialog
     */
    public OkCancelDialog(Dialog parent, boolean modal) {
        super(parent, modal);
        initComponents();

        if (parent != null) {
            int x = parent.getX() + (int) (parent.getWidth() / 2) - (getWidth() / 2);
            int y = parent.getY() + (int) (parent.getHeight() / 2) - (getHeight() / 2);
            setLocation(x, y);
        }
    }

    public JPanel getDialogPanel() {
        return contentPanel;
    }

    public void setOkButtonText(String text) {
        okButton.setText(text);
    }

    public void setCancelButtonText(String text) {
        cancelButton.setText(text);
    }

    abstract public boolean okButtonClicked(java.awt.event.ActionEvent event);

    abstract public boolean cancelButtonClicked(java.awt.event.ActionEvent event);

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentPanel = new javax.swing.JPanel();
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(440, 310));
        setModal(true);
        getContentPane().setLayout(new java.awt.BorderLayout(20, 20));

        contentPanel.setLayout(new java.awt.BorderLayout(10, 10));
        getContentPane().add(contentPanel, java.awt.BorderLayout.CENTER);

        buttonPanel.setFocusCycleRoot(true);

        okButton.setText("  Ok  ");
        okButton.setMaximumSize(new java.awt.Dimension(90, 25));
        okButton.setMinimumSize(new java.awt.Dimension(90, 25));
        okButton.setPreferredSize(new java.awt.Dimension(90, 25));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new java.awt.Dimension(90, 25));
        cancelButton.setMinimumSize(new java.awt.Dimension(90, 25));
        cancelButton.setOpaque(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(90, 25));
        cancelButton.setRequestFocusEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(cancelButton);

        getContentPane().add(buttonPanel, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        if (okButtonClicked(evt)) {
            isCanceled = false;
            setVisible(false);
        }
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed

        if (cancelButtonClicked(evt)) {
            isCanceled = true;
            setVisible(false);
        }
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    /*
    public static void batch(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                OkCancelDialog dialog = new OkCancelDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel contentPanel;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables

}
