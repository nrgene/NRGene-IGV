package org.broad.igv.ui.panel;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class FloaterPanel extends JPanel {
	
    static public FloaterPanel create(JComponent parent) 
    {
    	parent.setBackground(Color.green);
    	
    	FloaterPanel		floater = new FloaterPanel();
    	floater.setBackground(new Color(0, 0, 0, 128));
    	floater.setPreferredSize(new Dimension(40, 40));
    	floater.setMinimumSize(new Dimension(20, 20));
    	floater.setMaximumSize(new Dimension(60, 60));
    	parent.add(floater);
    	
    	return floater;
	}




}
