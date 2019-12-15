/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.entities;

import com.google.inject.Provider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.*;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import java.util.LinkedHashSet;

/**
 *
 * @author david
 */
public class OfflineVirtualVehicle implements IOptimalPlanVehicle{
	
	private final int capacity;
	
	private final int carLimit;
	
	private final RideSharingOnDemandVehicle exampleVehicle;
    
    private final SimulationNode position;
    
	public int getCarLimit() {
		return carLimit;
	}

	
	
	
	public OfflineVirtualVehicle(VGAVehicle vehicle,  int carLimit) {
		this.capacity = vehicle.getCapacity();
		this.carLimit = carLimit;
        this.position = vehicle.getPosition();
		exampleVehicle = vehicle.getRidesharingVehicle();
	}
	
	
	@Override
	public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard() {
		return new LinkedHashSet<>(); 
	}


	@Override
	public int getCapacity() {
		return capacity;
	}

    @Override
    public SimulationNode getPosition() {
        return position;
    }

    @Override
    public String getId() {
        return exampleVehicle.getId();
    }

    @Override
    public MovingEntity getRealVehicle() {
        return exampleVehicle;
    }




	
}
