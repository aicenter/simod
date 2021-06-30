/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VehicleLayer;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;
import java.awt.Color;

/**
 *
 * @author F-I-D-O
 */
@Singleton
public class OnDemandVehicleLayer extends VehicleLayer<PhysicalTransportVehicle>{
	
	private static final int STATIC_WIDTH = 24;
	
//	private static final Color NORMAL_COLOR = new Color(5, 89, 12);
	
	private static final Color REBALANCING_COLOR = new Color(88, 196, 178);
	
	public static final Color NORMAL_COLOR = new Color(76, 82, 156);

	public static final Color HIGHLIGHTED_COLOR = new Color(0, 19, 255);
	

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
		return (float) vehicle.getLengthM();
	}

	@Override
	protected float getVehicleStaticWidth(PhysicalTransportVehicle vehicle) {
		return STATIC_WIDTH;
	}

	@Override
	protected float getVehicleStaticLength(PhysicalTransportVehicle vehicle) {
		return (float) vehicle.getLengthM() * 8;
	}

	@Override
	protected Color getEntityDrawColor(PhysicalTransportVehicle vehicle) {
		OnDemandVehicle onDemandVehicle = (OnDemandVehicle) vehicle.getDriver();
		if (onDemandVehicle.getVehicleId().equals(OnDemandVehicleLayer.highlightedVehicleID) || vehicle.isHighlited()) {
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
		OnDemandVehicleLayer.highlightedVehicleID = id + " - vehicle";
	}

	@Override
	public boolean checkIfTransformSize(PhysicalTransportVehicle representative) {
		if(representative.getId().equals(OnDemandVehicleLayer.highlightedVehicleID) || representative.isHighlited()){
			return false;
		}
		else{
			return super.checkIfTransformSize(representative);
		}
	}
	
	
}
