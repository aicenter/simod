/*
 */
package cz.agents.amodsim.visio;

import com.google.inject.Inject;
import cz.agents.amodsim.entity.vehicle.OnDemandVehicle;
import cz.agents.amodsim.entity.OnDemandVehicleState;
import cz.agents.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage.EntityIterator;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.EntityLayer;
import cz.agents.alite.vis.Vis;
import cz.agents.alite.vis.layer.AbstractLayer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import javax.vecmath.Point2d;

/**
 *
 * @author F-I-D-O
 */
public class OnDemandVehicleLayer extends EntityLayer<OnDemandVehicle>{
	
	private static final int DEMAND_REPRESENTATION_RADIUS = 5;
    
//    private static final Color NORMAL_COLOR = new Color(5, 89, 12);
    
    private static final Color REBALANCING_COLOR = new Color(20, 252, 80);
    
    private static final Color NORMAL_COLOR = Color.BLUE;
    
    private static final Double TEXT_MARGIN_BOTTOM = 5.0;
    
    private static final Color TEXT_BACKGROUND_COLOR = Color.WHITE;
    
	
	


	
	
	@Inject
	public OnDemandVehicleLayer(OnDemandVehicleStorage onDemandVehicleStorage) {
        super(onDemandVehicleStorage);
	}


    @Override
    public String getLayerDescription() {
        String description = "Layer shows on-demand vehicles";
        return buildLayersDescription(description);
    }
	
	


    @Override
    protected Point2d getEntityPosition(OnDemandVehicle onDemandVehicle) {
        return positionUtil.getCanvasPositionInterpolated(onDemandVehicle);
    }

    @Override
    protected Color getEntityDrawColor(OnDemandVehicle onDemandVehicle) {
        switch(onDemandVehicle.getState()){
           case REBALANCING:
               return REBALANCING_COLOR;
           default:
               return NORMAL_COLOR;
       }
    }

    @Override
    protected int getEntityDrawRadius(OnDemandVehicle onDemandVehicle) {
    //        return DEMAND_REPRESENTATION_RADIUS;
        return (int) onDemandVehicle.getVehicle().getLength();
    }

    @Override
    protected boolean skipDrawing(OnDemandVehicle onDemandVehicle) {
        if(onDemandVehicle.getState() == OnDemandVehicleState.WAITING){
            return true;
        }
        else{
            return false;
        }
    }
    
    
}
