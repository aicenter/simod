/*
 */
package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VehicleLayer;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;
import java.awt.Color;

/**
 *
 * @author F-I-D-O
 */
@Singleton
public class OnDemandVehicleLayer extends VehicleLayer<PhysicalTransportVehicle>{
	
	private static final int STATIC_WIDTH = 7;
	
	private static final int STATIC_LENGTH = 10;
	
//	private static final Color NORMAL_COLOR = new Color(5, 89, 12);
	
	private static final Color REBALANCING_COLOR = new Color(20, 252, 80);
	
	private static final Color NORMAL_COLOR = Color.BLUE;

	private static final Color HIGHLIGHTED_COLOR = Color.MAGENTA;
	

	private static String highlightedVehicleID;

	
	
	@Inject
	public OnDemandVehicleLayer(PhysicalTransportVehicleStorage physicalTransportVehicleStorage, AgentpolisConfig agentpolisConfig) {
		super(physicalTransportVehicleStorage, agentpolisConfig);
	}


	@Override
	public String getLayerDescription() {
		String description = "Layer shows on-demand vehicles";
		return buildLayersDescription(description);
	}

	@Override
	protected boolean skipDrawing(PhysicalTransportVehicle vehicle) {
		OnDemandVehicle onDemandVehicle = (OnDemandVehicle) vehicle.getDriver();
		
		if(onDemandVehicle.getParkedIn() != null){
			return true;
		}
		else{
			return false;
		}
	}

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
		OnDemandVehicle onDemandVehicle = (OnDemandVehicle) vehicle.getDriver();
		if (onDemandVehicle.getVehicleId().equals(this.highlightedVehicleID)) {
			return HIGHLIGHTED_COLOR;

		}

		switch(onDemandVehicle.getState()){
		   case REBALANCING:
			   return REBALANCING_COLOR;
		   default:
			   return NORMAL_COLOR;
	   }
	}

	
	
	public void setHighlightedID(String id) {
		this.highlightedVehicleID = id + " - vehicle";
	}
}
