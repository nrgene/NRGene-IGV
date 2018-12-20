package org.broad.igv.track;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.UUID;

import org.broad.igv.renderer.DataRenderer;
import org.broad.igv.renderer.Renderer;

public class EmptyTrack extends AbstractTrack {

	static private String		idPrefix = "emptyTrack_";
	
    private DataRenderer renderer;

    public EmptyTrack(String id)
	{
		super(id);
		setNameColor(Color.WHITE);
	}
	
    
    @Override
    public void renderName(Graphics2D g2D, Rectangle trackRectangle, Rectangle visibleRectangle) {
    }
    
	@Override
	public void render(RenderContext context, Rectangle rect) 
	{
		Color			color = getNameColor();
		if ( color == null )
			return;
		
        Graphics2D borderGraphics = context.getGraphic2DForColor(color);
        borderGraphics.fill(rect);
	}

	@Override
	public Renderer getRenderer() 
	{
        if (renderer == null) 
        {
            setRendererClass(getDefaultRendererClass());
        }
        return renderer;
	}


	public static String createEmptyTrackId()
	{
		return idPrefix + UUID.randomUUID().toString();
	}
	
	public static boolean isEmptyTrackId(String id) 
	{
		return (id != null) && id.startsWith(idPrefix) && (id.indexOf(".") < 0);
	}

}
