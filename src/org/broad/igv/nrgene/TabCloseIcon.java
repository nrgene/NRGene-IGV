/*
 * TabCloseIcon.java
 */

package org.broad.igv.nrgene;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;

import org.apache.log4j.Logger;
import org.broad.igv.ui.IGV;
import org.jfree.util.Log;

/**
 *
 * @author Herkules
 */
public class TabCloseIcon implements Icon
{
    private static Logger log = Logger.getLogger(TabCloseIcon.class);

    private final Icon mIcon;
	private JTabbedPane mTabbedPane = null;
	private transient Rectangle mPosition = null;
	
	private transient MouseAdapter mouseAdapter;
	
	/**
	 * Creates a new instance of TabCloseIcon.
	 */
	public TabCloseIcon( Icon icon )
	{
		mIcon = icon;
	}
	
	
	/**
	 * Creates a new instance of TabCloseIcon.
	 */
	public TabCloseIcon()
	{
		this( new ImageIcon( TabCloseIcon.class.getResource("/images/closeTab.gif")) );
	}
	
	
	/**
	 * when painting, remember last position painted.
	 */
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		if( null==mTabbedPane )
		{
			mTabbedPane = (JTabbedPane)c;
			mTabbedPane.addMouseListener( mouseAdapter = new MouseAdapter()
			{
				@Override public void mouseReleased( MouseEvent e )
				{
					// asking for isConsumed is *very* important, otherwise more than one tab might get closed!
					if ( !e.isConsumed()  &&   mPosition.contains( e.getX(), e.getY() ) )
					{
						final int index = mTabbedPane.getSelectedIndex();
						final Icon icon = mTabbedPane.getIconAt(index);
						if ( (icon instanceof TabCloseIcon) && ((TabCloseIcon)icon) == TabCloseIcon.this )
						{
							if ( mTabbedPane.getComponentCount() > 1 )
							{
								if ( IGV.getInstance().confirm("Close tab \"" + mTabbedPane.getTitleAt(mTabbedPane.getSelectedIndex())  + "\"?") )
								{
									TabCloseIcon.cleanupIconAt(mTabbedPane, index);
									mTabbedPane.remove(index);
									IGV.getInstance().getContentPane().tabsSwitchTo();
								}
							}
						}
						else
							log.info("ignoring zombie close icon: " + icon + " != " + TabCloseIcon.this);
						e.consume();
					}
				}
			});
		}
		
		mPosition = new Rectangle( x,y, getIconWidth(), getIconHeight() );
		mIcon.paintIcon(c, g, x, y );
	}
	
	
	public static void cleanupIconAt(JTabbedPane mTabbedPane, int index)
	{
		Icon		removedIcon = mTabbedPane.getIconAt(index);
		if ( removedIcon != null && removedIcon instanceof TabCloseIcon )
		{
			log.info("removed icon: " + removedIcon);
			mTabbedPane.removeMouseListener(((TabCloseIcon)removedIcon).mouseAdapter);
			mTabbedPane.setIconAt(index, null);
		}

	}
	
	/**
	 * just delegate
	 */
	public int getIconWidth()
	{
		return mIcon.getIconWidth();
	}
	
	/**
	 * just delegate
	 */
	public int getIconHeight()
	{
		return mIcon.getIconHeight();
	}
	
}