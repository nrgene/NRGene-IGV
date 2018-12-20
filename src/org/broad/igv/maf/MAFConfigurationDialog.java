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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MAFConfigurationDialog.java
 *
 * Created on Feb 19, 2009, 8:59:46 AM
 */
package org.broad.igv.maf;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.broad.igv.PreferenceManager;

/**
 * @author jrobinso
 */
public class MAFConfigurationDialog extends javax.swing.JDialog {

    enum Category {

        Primate, Mammal, Vertebrate
    }

    ;
    static String[] primates = {
            "panTro2", "gorGor1", "ponAbe2", "rheMac2", "calJac1",
            "tarSyr1", "micMur1", "otoGar1"
    };
    static String[] mammals = {
            "tupBel1", "mm9", "rn4", "dipOrd1",
            "cavPor3", "speTri1", "oryCun1", "ochPri2", "vicPac1", "turTru1",
            "bosTau4", "equCab2", "felCat3", "canFam2", "myoLuc1", "pteVam1",
            "eriEur1", "sorAra1", "loxAfr2", "proCap1", "echTel1", "dasNov2",
            "choHof1"
    };
    static String[] vertebrates = {
            "monDom4", "ornAna1", "galGal3", "taeGut1", "anoCar1",
            "xenTro2", "tetNig1", "fr2", "gasAcu1", "oryLat28", "danRer5", "petMar1"
    };
    boolean cancelled = false;

    MAFManager mgr;

    /**
     * Creates new form MAFConfigurationDialog
     */
    public MAFConfigurationDialog(java.awt.Frame parent, boolean modal, MAFManager mgr) {
        super(parent, modal);
        this.mgr = mgr;
        initComponents();
        ((SpeciesSelectionPanel) primatePanel).checkForAllSelections();
        ((SpeciesSelectionPanel) mammalsPanel).checkForAllSelections();
        ((SpeciesSelectionPanel) vertebratePanel).checkForAllSelections();


    }

    public List<String> getSelectedSpecies() {
        List<String> selectedSpecies = new ArrayList(44);
        selectedSpecies.addAll(((SpeciesSelectionPanel) primatePanel).getSelectedSpecies());
        selectedSpecies.addAll(((SpeciesSelectionPanel) mammalsPanel).getSelectedSpecies());
        selectedSpecies.addAll(((SpeciesSelectionPanel) vertebratePanel).getSelectedSpecies());
        return selectedSpecies;
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

        primatePanel = new SpeciesSelectionPanel(Category.Primate);
        mammalsPanel = new SpeciesSelectionPanel(Category.Mammal);
        vertebratePanel = new SpeciesSelectionPanel(Category.Vertebrate);
        primateCheckbox = new javax.swing.JCheckBox();
        mammalCheckbox = new javax.swing.JCheckBox();
        vertebrateCheckbox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        primatePanel.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.shadow")));
        primatePanel.setMaximumSize(new java.awt.Dimension(32767, 60));
        primatePanel.setMinimumSize(new java.awt.Dimension(2, 60));
        primatePanel.setPreferredSize(new java.awt.Dimension(727, 60));
        primatePanel.setLayout(new java.awt.GridLayout(2, 5));

        mammalsPanel.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.shadow")));
        mammalsPanel.setMaximumSize(new java.awt.Dimension(32767, 60));
        mammalsPanel.setMinimumSize(new java.awt.Dimension(2, 60));
        mammalsPanel.setPreferredSize(new java.awt.Dimension(727, 60));
        mammalsPanel.setLayout(new java.awt.GridLayout(6, 5));

        vertebratePanel.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.shadow")));
        vertebratePanel.setMaximumSize(new java.awt.Dimension(32767, 60));
        vertebratePanel.setMinimumSize(new java.awt.Dimension(2, 60));
        vertebratePanel.setPreferredSize(new java.awt.Dimension(727, 60));
        vertebratePanel.setLayout(new java.awt.GridLayout(3, 5));

        primateCheckbox.setText("Primates");
        primateCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                primateCheckboxActionPerformed(evt);
            }
        });

        mammalCheckbox.setText("Mammals");
        mammalCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mammalCheckboxActionPerformed(evt);
            }
        });

        vertebrateCheckbox.setText("Verterbrates");
        vertebrateCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vertebrateCheckboxActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 18)); // NOI18N
        jLabel1.setText("Multiz Species Selection");

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(layout.createSequentialGroup()
                                .add(vertebrateCheckbox)
                                .add(630, 630, 630))
                        .add(layout.createSequentialGroup()
                                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 293, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(454, Short.MAX_VALUE))
                        .add(layout.createSequentialGroup()
                                .add(mammalCheckbox)
                                .addContainerGap(661, Short.MAX_VALUE))
                        .add(layout.createSequentialGroup()
                                .add(primateCheckbox)
                                .addContainerGap(667, Short.MAX_VALUE))
                        .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, vertebratePanel, 0, 0, Short.MAX_VALUE)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, mammalsPanel, 0, 0, Short.MAX_VALUE)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, primatePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(layout.createSequentialGroup()
                                .add(451, 451, 451)
                                .add(okButton)
                                .add(18, 18, 18)
                                .add(cancelButton)))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(primateCheckbox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(primatePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(46, 46, 46)
                        .add(mammalCheckbox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(mammalsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 221, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(28, 28, 28)
                        .add(vertebrateCheckbox)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(vertebratePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(34, 34, 34)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(cancelButton)
                                .add(okButton))
                        .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void primateCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_primateCheckboxActionPerformed
        ((SpeciesSelectionPanel) primatePanel).setSelectedAll(primateCheckbox.isSelected());
    }//GEN-LAST:event_primateCheckboxActionPerformed

    private void mammalCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mammalCheckboxActionPerformed
        ((SpeciesSelectionPanel) mammalsPanel).setSelectedAll(mammalCheckbox.isSelected());
    }//GEN-LAST:event_mammalCheckboxActionPerformed

    private void vertebrateCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vertebrateCheckboxActionPerformed
        ((SpeciesSelectionPanel) vertebratePanel).setSelectedAll(vertebrateCheckbox.isSelected());
    }//GEN-LAST:event_vertebrateCheckboxActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        cancelled = true;
        setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_okButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JCheckBox mammalCheckbox;
    private javax.swing.JPanel mammalsPanel;
    private javax.swing.JButton okButton;
    private javax.swing.JCheckBox primateCheckbox;
    private javax.swing.JPanel primatePanel;
    private javax.swing.JCheckBox vertebrateCheckbox;
    private javax.swing.JPanel vertebratePanel;
    // End of variables declaration//GEN-END:variables

    class SpeciesSelectionPanel extends JPanel {

        int nColumns = 4;
        String[] speciesIds;
        JCheckBox[] checkboxes;
        Category category;

        public SpeciesSelectionPanel(Category category) {
            this.category = category;
            switch (category) {
                case Primate:
                    speciesIds = primates;
                    break;
                case Mammal:
                    speciesIds = mammals;
                    break;
                case Vertebrate:
                    speciesIds = vertebrates;
                    break;
            }
            checkboxes = new JCheckBox[speciesIds.length];
            init();
        }

        private void init() {

            Set<String> currentSelections = new HashSet(
                    PreferenceManager.getInstance().getMafSpecies());

            int nRows = speciesIds.length / nColumns + 1;

            //GridLayout layout = new GridLayout();
            //layout.setColumns(nColumns);
            //layout.setRows(nRows);
            //setLayout(layout);

            int height = nRows * 20;
            this.setSize(600, height);
            this.setMaximumSize(new Dimension(600, height));

            for (int i = 0; i < speciesIds.length; i++) {
                String sp = speciesIds[i];
                String label = mgr.getSpeciesName(sp);
                if (label == null) {
                    label = sp;
                }

                checkboxes[i] = new JCheckBox(label);
                checkboxes[i].setSelected(currentSelections.contains(sp));
                checkboxes[i].addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        checkboxAction(evt);
                    }
                });

                add(checkboxes[i]);
            }
            //checkForAllSelections();
        }

        private void checkboxAction(java.awt.event.ActionEvent evt) {
            checkForAllSelections();
        }

        public void checkForAllSelections() {
            JCheckBox masterCheckbox = getMasterCheckbox();
            if (isAllSelected()) {
                if (!masterCheckbox.isSelected()) {
                    masterCheckbox.setSelected(true);
                }
            } else {
                if (masterCheckbox.isSelected()) {
                    masterCheckbox.setSelected(false);
                }
            }
        }

        private JCheckBox getMasterCheckbox() {
            switch (category) {
                case Primate:
                    return primateCheckbox;
                case Mammal:
                    return mammalCheckbox;
                case Vertebrate:
                    return vertebrateCheckbox;
            }
            return null;

        }

        public void setSelectedAll(boolean selected) {
            for (JCheckBox cb : checkboxes) {
                cb.setSelected(selected);
            }
        }

        private boolean isAllSelected() {
            for (JCheckBox cb : checkboxes) {
                if (!cb.isSelected()) {
                    return false;
                }
            }
            return true;

        }

        public List<String> getSelectedSpecies() {
            List<String> selections = new ArrayList(speciesIds.length);
            for (int i = 0; i < speciesIds.length; i++) {
                if (checkboxes[i].isSelected()) {
                    selections.add(speciesIds[i]);
                }
            }
            return selections;
        }
    }
}
