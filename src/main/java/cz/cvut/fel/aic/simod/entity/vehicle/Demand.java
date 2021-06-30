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
package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.cvut.fel.aic.simod.entity.DemandAgent;

/**
 *
 * @author fido
 */
public class Demand {
	final DemandAgent demandAgent;

	final VehicleTrip<TripItem> demandTrip;

	public DemandAgent getDemandAgent() {
		return demandAgent;
	}

	public VehicleTrip getDemandTrip() {
		return demandTrip;
	}



	public Demand(DemandAgent demandAgent, VehicleTrip demandTrip) {
		this.demandAgent = demandAgent;
		this.demandTrip = demandTrip;
	}

	public int getStartNodeId(){
		return demandTrip.getLocations()[0].tripPositionByNodeId;
	}

	public int getTargetNodeId(){
		return demandTrip.getLocations()[demandTrip.getLocations().length - 1].tripPositionByNodeId;
	}
}
