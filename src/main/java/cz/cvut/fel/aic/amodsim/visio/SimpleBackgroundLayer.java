package cz.cvut.fel.aic.amodsim.visio;

import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.alite.vis.layer.AbstractLayer;

import java.awt.*;

public class SimpleBackgroundLayer extends AbstractLayer {

	@Override
	public void paint(Graphics2D canvas) {
		canvas.setColor(new Color(255, 255, 255, 255));
		canvas.fillRect(0, 0, Vis.getDrawingDimension().width, Vis.getDrawingDimension().height);
	}

}
