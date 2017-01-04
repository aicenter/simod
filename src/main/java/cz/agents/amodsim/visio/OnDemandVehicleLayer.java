/*
 */
package cz.agents.amodsim.visio;

import com.google.inject.Inject;
import cz.agents.amodsim.entity.OnDemandVehicle;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.EntityStorage.EntityIterator;
import cz.agents.agentpolis.simulator.visualization.visio.entity.VehiclePositionUtil;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
    
	
	
	
	private final VehiclePositionUtil vehiclePositionUtil;
    
    private final OnDemandVehicleStorage onDemandVehicleStorage;
    
    

	
	
	@Inject
	public OnDemandVehicleLayer(VehiclePositionUtil vehiclePositionUtil, OnDemandVehicleStorage onDemandVehicleStorage) {
		this.vehiclePositionUtil = vehiclePositionUtil;
        this.onDemandVehicleStorage = onDemandVehicleStorage;
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
            
            Point2d agentPosition = vehiclePositionUtil.getVehicleCanvasPositionInterpolated(agent.getVehicle(), agent);
            if(agentPosition == null){
                continue;
            }
			drawAgent(agent, agentPosition, canvas, dim);
        }
    }

    private void drawAgent(OnDemandVehicle agent, Point2d agentPosition, Graphics2D canvas, Dimension dim) {
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
