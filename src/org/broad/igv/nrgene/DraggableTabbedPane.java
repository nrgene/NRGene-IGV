package org.broad.igv.nrgene;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.broad.igv.ui.IGV;


public class DraggableTabbedPane extends JTabbedPane {

  private boolean dragging = false;
  private Image tabImage = null;
  private Point currentMouseLocation = null;
  private int draggedTabIndex = 0;
  private boolean setCurrentAfterDrop = false;

  public DraggableTabbedPane() {
    super();
    addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent e) {

        if(!dragging) {
          // Gets the tab index based on the mouse position
          int tabNumber = getUI().tabForCoordinate(DraggableTabbedPane.this, e.getX(), e.getY());

          if(tabNumber >= 0) {
            draggedTabIndex = tabNumber;
            Rectangle bounds = getUI().getTabBounds(DraggableTabbedPane.this, tabNumber);


            // Paint the tabbed pane to a buffer
            Image totalImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics totalGraphics = totalImage.getGraphics();
            totalGraphics.setClip(bounds);
            // Don't be double buffered when painting to a static image.
            setDoubleBuffered(false);
            paintComponent(totalGraphics);

            // Paint just the dragged tab to the buffer
            tabImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = tabImage.getGraphics();
            graphics.drawImage(totalImage, 0, 0, bounds.width, bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y+bounds.height, DraggableTabbedPane.this);

            dragging = true;
            repaint();
          }
        } else {
          currentMouseLocation = e.getPoint();

          // Need to repaint
          repaint();
        }

        super.mouseDragged(e);
      }
    });

    addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {

        if(dragging) {
          int tabNumber = getUI().tabForCoordinate(DraggableTabbedPane.this, e.getX(), 10);

          // this is only applicable if there is more then a single tab
          if( getComponentCount() > 1 && tabNumber >= 0 && draggedTabIndex != tabNumber ) 
          {
        	IGV.getInstance().getContentPane().tabsDumpState("before drop manipulation");
        	  
            Component comp = getComponentAt(draggedTabIndex);
            
            String title = getTitleAt(draggedTabIndex);
            Icon icon = getIconAt(draggedTabIndex);
            
			TabCloseIcon.cleanupIconAt(DraggableTabbedPane.this, draggedTabIndex);
            removeTabAt(draggedTabIndex);
            insertTab(title, null, comp, null, tabNumber);
            setIconAt(tabNumber, icon);
            
            if ( isSetCurrentAfterDrop() )
            	setSelectedIndex(tabNumber);
            
            IGV.getInstance().getContentPane().tabsUpdateShowGenome();
            
        	IGV.getInstance().getContentPane().tabsDumpState("after drop manipulation");

        	e.consume();
          }
        }

        dragging = false;
        tabImage = null;
        
        repaint();
      }
    });
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Are we dragging?
    if(dragging && currentMouseLocation != null && tabImage != null) {
      // Draw the dragged tab
      g.drawImage(tabImage, currentMouseLocation.x, currentMouseLocation.y, this);
    }
  }

  public static void main(String[] args) {
    JFrame test = new JFrame("Tab test");
    test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    test.setSize(400, 400);

    DraggableTabbedPane tabs = new DraggableTabbedPane();
    tabs.addTab("One", new JButton("One"));
    tabs.addTab("Two", new JButton("Two"));
    tabs.addTab("Three", new JButton("Three"));
    tabs.addTab("Four", new JButton("Four"));

    test.add(tabs);
    test.setVisible(true);
  }

public boolean isSetCurrentAfterDrop() {
	return setCurrentAfterDrop;
}

public void setSetCurrentAfterDrop(boolean setCurrentAfterDrop) {
	this.setCurrentAfterDrop = setCurrentAfterDrop;
}
}