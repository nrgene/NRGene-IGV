package org.broad.igv.ui.util;

import java.awt.Component;
import java.awt.Container;

import org.apache.log4j.Logger;
import org.broad.igv.ui.panel.MainPanel;

public class NrgeneDumpFrameHierarchy {
	
    private static Logger log = Logger.getLogger(MainPanel.class);

	public void dump(Component component)
	{
		dump(component, 0);
	}
	
	private void dump(Component component, int level)
	{
		// print line for this frame
		String		format = "[%d]%s %" + (level + 1) * 2 + "s %s";
		String		info = component.getClass().getName();
		String		code = component.getClass().getName().startsWith("org") ? "*" : " ";
		String		line = String.format(format, level, code, "", info);
		
		line += " visible:" + component.isVisible();
		
		log.debug(line);
		
		// next into children
		if ( component instanceof Container )
		{
			Container		container = (Container)component;
			for ( Component child : container.getComponents() )
				dump(child, level + 1);
		}
		
	}

}
