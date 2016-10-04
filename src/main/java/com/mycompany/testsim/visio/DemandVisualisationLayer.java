/*
 */
package com.mycompany.testsim.visio;

import com.google.inject.Inject;
import cz.agents.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage.EntityIterator;
import cz.agents.agentpolis.simulator.visualization.visio.entity.EntityPositionUtil;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Random;
import javax.vecmath.Point2d;

/**
 *
 * @author F-I-D-O
 */
public class DemandVisualisationLayer extends AbstractLayer{
	
	private static final int DEMAND_REPRESENTATION_RADIUS = 5;
	
	
	
	
	private final EntityPositionUtil entityPostitionUtil;
    
    private final EntityStorage entityStorage;
	
	private final Random random;
    
    private final HashMap<AgentPolisEntity,Color> agentColors;

	
	
	@Inject
	public DemandVisualisationLayer(EntityPositionUtil entityPostitionUtil, EntityStorage entityStorage) {
		this.entityPostitionUtil = entityPostitionUtil;
        this.entityStorage = entityStorage;
		this.random = new Random();
        agentColors = new HashMap<>();
	}

	
	
	

	@Override
    public void paint(Graphics2D canvas) {
        Dimension dim = Vis.getDrawingDimension();
		
//		EntityPositionIterator entityPositionIterator = entityPostitionUtil.new EntityPositionIterator();
//		Point2d agentPosition;

        EntityIterator entityIterator = entityStorage.new EntityIterator();
        AgentPolisEntity agent;
        while((agent = entityIterator.getNextEntity()) != null){
            Point2d agentPosition = entityPostitionUtil.getEntityPosition(agent);
            if(agentPosition == null){
                continue;
            }
			drawAgent(agent, agentPosition, canvas, dim);
        }
    }

    private void drawAgent(AgentPolisEntity agent, Point2d agentPosition, Graphics2D canvas, Dimension dim) {
        canvas.setColor(getColor(agent));
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

    private Color getColor(AgentPolisEntity agent) {
        if(agentColors.containsKey(agent)){
            return agentColors.get(agent);
        }
        else{
            Color color = getRandomColor();
            agentColors.put(agent, color);
            return color;
        }
    }
}
