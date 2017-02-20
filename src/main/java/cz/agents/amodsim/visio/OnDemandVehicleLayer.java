/*
 */
package cz.agents.amodsim.visio;

import com.google.inject.Inject;
import cz.agents.amodsim.entity.OnDemandVehicle;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage.EntityIterator;
import cz.agents.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.agents.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.vecmath.Point2d;

/**
 *
 * @author F-I-D-O
 */
public class OnDemandVehicleLayer extends AbstractLayer{
	
	private static final int DEMAND_REPRESENTATION_RADIUS = 5;
    
//    private static final Color NORMAL_COLOR = new Color(5, 89, 12);
    
    private static final Color REBALANCING_COLOR = new Color(20, 252, 80);
    
    private static final Color NORMAL_COLOR = Color.BLUE;
    
    private static final Double TEXT_MARGIN_BOTTOM = 5.0;
    
    private static final Color TEXT_BACKGROUND_COLOR = Color.WHITE;
    
	
	
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;
    
    private final PositionUtil positionUtil;

	
	
	@Inject
	public OnDemandVehicleLayer(OnDemandVehicleStorage onDemandVehicleStorage, PositionUtil positionUtil) {
        this.onDemandVehicleStorage = onDemandVehicleStorage;
        this.positionUtil = positionUtil;
	}

	
	
	

	@Override
    public void paint(Graphics2D canvas) {
        Dimension dim = Vis.getDrawingDimension();

        OnDemandVehicleStorage.EntityIterator entityIterator = onDemandVehicleStorage.new EntityIterator();
        OnDemandVehicle agent;
        while((agent = entityIterator.getNextEntity()) != null){
            if(agent.getState() == OnDemandVehicleState.WAITING){
                continue;
            }
            
            Point2d agentPosition = positionUtil.getCanvasPositionInterpolated(agent);
//            if(agentPosition == null){
//                continue;
//            }
			drawAgent(agent, agentPosition, canvas, dim);
        }
    }

    private void drawAgent(OnDemandVehicle agent, Point2d agentPosition, Graphics2D canvas, Dimension dim) {
        Color color = getColor(agent);
        canvas.setColor(color);
        int radius = DEMAND_REPRESENTATION_RADIUS;
		int width = radius * 2;

        int x1 = (int) (agentPosition.getX() - radius);
        int y1 = (int) (agentPosition.getY() - radius);
        int x2 = (int) (agentPosition.getX() + radius);
        int y2 = (int) (agentPosition.getY() + radius);
        if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
            canvas.fillOval(x1, y1, width, width);
            if(agent.getCargo().size() > 1){
                VisioUtils.printTextWithBackgroud(canvas, Integer.toString(agent.getCargo().size()), 
                    new Point((int) (x1 - TEXT_MARGIN_BOTTOM), y1 - (y2 - y1) / 2), color, 
                    TEXT_BACKGROUND_COLOR);
            }
        }

    }

    @Override
    public String getLayerDescription() {
        String description = "Layer shows demannds as randomly colored points";
        return buildLayersDescription(description);
    }
	
	

    protected Color getColor(OnDemandVehicle agent) {
       switch(agent.getState()){
           case REBALANCING:
               return REBALANCING_COLOR;
           default:
               return NORMAL_COLOR;
       }
    }
}
