/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.EntityLayer;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.DemandAgentState;
import cz.cvut.fel.aic.amodsim.storage.DemandStorage;
import java.awt.Color;
import java.awt.Dimension;
import javax.vecmath.Point2d;

/**
 *
 * @author fido
 */
@Singleton
public class DemandLayer extends EntityLayer<DemandAgent>{
    
    private static final Color DEMAND_COLOR = Color.RED;
    
    private static final int SIZE = 1;

    
    
    
    @Inject
    public DemandLayer(DemandStorage demandStorage, AgentpolisConfig agentpolisConfig) {
        super(demandStorage, agentpolisConfig);
    }

    
    protected Point2d getDrivingAgentPosition(DemandAgent demandAgent){
        return positionUtil.getCanvasPositionInterpolatedForVehicle(demandAgent.getOnDemandVehicle().getVehicle());
    }
    
    protected Point2d getWaitingAgentPosition(DemandAgent demandAgent, Dimension drawingDimension){
        return positionUtil.getCanvasPosition(demandAgent.getPosition());
    }


    @Override
    protected Point2d getEntityPosition(DemandAgent demandAgent) {
        if(demandAgent.getState() == DemandAgentState.DRIVING){
            return getDrivingAgentPosition(demandAgent);
        }
        else{
            return getWaitingAgentPosition(demandAgent, dim);
        }
    }

    @Override
    protected Color getEntityDrawColor(DemandAgent demandAgent) {
        return DEMAND_COLOR;
    }

    @Override
    protected int getEntityTransformableRadius(DemandAgent demandAgent) {
        return SIZE;
    }

    @Override
    protected double getEntityStaticRadius(DemandAgent demandAgent) {
        return (double) SIZE;
    }
    
    
}
