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
 * LinkCheckBox.java
 *
 * Created on August 18, 2008, 2:17 PM
 */

package org.broad.igv.ui.util;

import java.awt.Color;
import java.awt.event.ItemListener;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.broad.igv.util.BrowserLauncher;

import com.jidesoft.swing.JideButton;

/**
 * @author eflakes
 */
public class LinkCheckBox extends javax.swing.JPanel {

    private static Logger log = Logger.getLogger(LinkCheckBox.class);
    private String hyperLink;

    /**
     * Creates new form LinkCheckBox
     */
    public LinkCheckBox() {
        initComponents();
        setOpaque(true);

        //Should we use a hand cursor to indicate clickability?
        //infoLinkButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void addItemListener(ItemListener listener) {
        jCheckBox1.addItemListener(listener);
    }

    public void showHyperLink(boolean value) {

        if (infoLinkButton != null) {
            infoLinkButton.setVisible(value);
        }
    }

    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);

        if (jCheckBox1 != null) {
            jCheckBox1.setEnabled(value);
        }
    }

    public void setFocusPainted(boolean value) {
        if (jCheckBox1 != null) {
            jCheckBox1.setFocusPainted(value);
        }
    }

    public void setSelected(boolean value) {
        if (jCheckBox1 != null) {
            jCheckBox1.setSelected(value);
        }
    }

    public boolean isSelected() {
        if (jCheckBox1 != null) {
            return jCheckBox1.isSelected();
        }
        return false;
    }

    public void setText(String value) {
        if (jCheckBox1 != null) {
            jCheckBox1.setText(value);
        }
    }

    public String getText() {
        if (jCheckBox1 != null) {
            return jCheckBox1.getText();
        } else {
            return null;
        }
    }

    public void setHyperLink(String value) {
        hyperLink = value;
    }

    public String getHyperLink() {
        return hyperLink;
    }

    public void setCheckboxBackground(Color value) {
        if (jCheckBox1 != null) {
            jCheckBox1.setBackground(value);
        }
    }

    public void setCheckboxForeground(Color value) {
        if (jCheckBox1 != null) {
            jCheckBox1.setForeground(value);
        }
    }

    @Override
    public void setBackground(Color value) {
        super.setBackground(value);
        if (jCheckBox1 != null) {
            jCheckBox1.setBackground(value);
        }
    }

    @Override
    public void setForeground(Color value) {
        super.setForeground(value);
        if (jCheckBox1 != null) {
            jCheckBox1.setForeground(value);
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBox1 = new javax.swing.JCheckBox();
        infoLinkButton = new JideButton();

        setMaximumSize(new java.awt.Dimension(32767, 26));
        setMinimumSize(new java.awt.Dimension(25, 25));
        setRequestFocusEnabled(false);
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 2, 0));

        jCheckBox1.setText("Text");
        jCheckBox1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jCheckBox1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBox1.setMaximumSize(new java.awt.Dimension(32767, 22));
        jCheckBox1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jCheckBox1.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        add(jCheckBox1);

        infoLinkButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/info.gif"))); // NOI18N
        infoLinkButton.setBorderPainted(false);
        infoLinkButton.setFocusPainted(false);
        infoLinkButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        infoLinkButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        infoLinkButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        infoLinkButton.setMaximumSize(new java.awt.Dimension(15, 15));
        infoLinkButton.setMinimumSize(new java.awt.Dimension(15, 15));
        infoLinkButton.setOpaque(false);
        infoLinkButton.setPreferredSize(new java.awt.Dimension(15, 15));
        infoLinkButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/images/info.gif"))); // NOI18N
        infoLinkButton.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        infoLinkButton.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        infoLinkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                infoLinkButtonActionPerformed(evt);
            }
        });
        infoLinkButton.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }

            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
            }

            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
                infoLinkButtonAncestorRemoved(evt);
            }
        });
        add(infoLinkButton);
    }// </editor-fold>//GEN-END:initComponents

    private void infoLinkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_infoLinkButtonActionPerformed
        try {
            BrowserLauncher.openURL(hyperLink);
        }
        catch (IOException e) {
            log.error("Error launching from hyperlink", e);
        }

    }//GEN-LAST:event_infoLinkButtonActionPerformed

    private void infoLinkButtonAncestorRemoved(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_infoLinkButtonAncestorRemoved
// TODO add your handling code here:
    }//GEN-LAST:event_infoLinkButtonAncestorRemoved


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton infoLinkButton;
    private javax.swing.JCheckBox jCheckBox1;
    // End of variables declaration//GEN-END:variables

}
