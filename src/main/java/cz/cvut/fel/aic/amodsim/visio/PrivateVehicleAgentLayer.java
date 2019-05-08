/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VehicleLayer;
import cz.cvut.fel.aic.amodsim.entity.PrivateVehicleAgent;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.awt.Color;

/**
 *
 * @author praveale
 */
public class PrivateVehicleAgentLayer extends VehicleLayer<PhysicalTransportVehicle>{
	
    private static final int STATIC_WIDTH = 7;

    private static final int STATIC_LENGTH = 10;
    
//    private static final Color NORMAL_COLOR = new Color(5, 89, 12);
    
    private static final Color REBALANCING_COLOR = new Color(20, 252, 80);
    
    private static final Color NORMAL_COLOR = Color.BLUE;

    private static final Color HIGHLIGHTED_COLOR = Color.MAGENTA;    

    private static String highlightedVehicleID;
    
	
    @Inject
    public PrivateVehicleAgentLayer(PhysicalTransportVehicleStorage physicalTransportVehicleStorage, AgentpolisConfig agentpolisConfig) {
        super(physicalTransportVehicleStorage, agentpolisConfig);
    }


    @Override
    public String getLayerDescription() {
        String description = "Layer shows drive agent vehicles";
        return buildLayersDescription(description);
    }

    /*@Override
    protected boolean skipDrawing(PhysicalTransportVehicle vehicle) {
        PrivateVehicleAgent PrivateVehicleAgent = (PrivateVehicleAgent) vehicle.getDriver();
        
        if(PrivateVehicleAgent.getState() == DriveAgentState.WAITING){
            return true;
        }
        else{
            return false;
        }
    }*/

    @Override
    protected float getVehicleWidth(PhysicalTransportVehicle vehicle) {
        return 3;
    }

    @Override
    protected float getVehicleLength(PhysicalTransportVehicle vehicle) {
        return (float) vehicle.getLength();
    }

    @Override
    protected float getVehicleStaticWidth(PhysicalTransportVehicle vehicle) {
        return 3;
    }

    @Override
    protected float getVehicleStaticLength(PhysicalTransportVehicle vehicle) {
        return (float) vehicle.getLength();
    }

    @Override
    protected Color getEntityDrawColor(PhysicalTransportVehicle vehicle) {
        PrivateVehicleAgent driveAgent = (PrivateVehicleAgent) vehicle.getDriver();
        /*if (PrivateVehicleAgent.getVehicleId().equals(this.highlightedVehicleID)) {
            return HIGHLIGHTED_COLOR;

        }

        switch(PrivateVehicleAgent.getState()){
           case REBALANCING:
               return REBALANCING_COLOR;
           default:
               return NORMAL_COLOR;
       }*/
        return NORMAL_COLOR;
    }

	
    
    public void setHighlightedID(String id) {
        this.highlightedVehicleID = id + " - vehicle";
    }
}
