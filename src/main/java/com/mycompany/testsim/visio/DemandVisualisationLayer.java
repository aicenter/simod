/*
 */
package com.mycompany.testsim.visio;

import cz.agents.agentpolis.simulator.visualization.visio.entity.EntityPositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.entity.EntityPositionUtil.EntityPositionIterator;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Random;
import javafx.geometry.Point2D;

/**
 *
 * @author F-I-D-O
 */
public class DemandVisualisationLayer extends AbstractLayer{
	
	private static final int DEMAND_REPRESENTATION_RADIUS = 5;
	
	
	
	
	private final EntityPositionUtil entityPostitionUtil;
	
	private final Random random;

	
	
	
	public DemandVisualisationLayer(EntityPositionUtil entityPostitionUtil, Random random) {
		this.entityPostitionUtil = entityPostitionUtil;
		this.random = random;
	}

	
	
	

	@Override
    public void paint(Graphics2D canvas) {
//        canvas.setStroke(new BasicStroke(1));
        Dimension dim = Vis.getDrawingDimension();
		
		EntityPositionIterator entityPositionIterator = entityPostitionUtil.new EntityPositionIterator();
		Point2D agentPosition;
        while((agentPosition = entityPositionIterator.getNextEntityPosition()) != null){
			drawAgent(agentPosition, canvas, dim);
        }
    }

    private void drawAgent(Point2D agentPosition, Graphics2D canvas, Dimension dim) {
        canvas.setColor(getRandomColor());
        int radius = DEMAND_REPRESENTATION_RADIUS;
		int width = radius * 2;

        int x1 = (int) (agentPosition.getX() - radius);
        int y1 = (int) (agentPosition.getY() - radius);
        int x2 = (int) (agentPosition.getX() + radius);
        int y2 = (int) (agentPosition.getY() + radius);
        if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
            canvas.fillOval(x1, y1, width, width);
        }

    }

    @Override
    public String getLayerDescription() {
        String description = "Layer shows demannds as raandomlz colored points";
        return buildLayersDescription(description);
    }
	
	private Color getRandomColor(){
		float r = random.nextFloat();
		float g = random.nextFloat();
		float b = random.nextFloat();
		
		return new Color(r, g, b);
	}
}
