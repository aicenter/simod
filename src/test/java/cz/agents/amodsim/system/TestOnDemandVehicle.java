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
package cz.agents.amodsim.system;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.StandardTimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.activity.activityFactory.StandardDriveFactory;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.planner.TripsUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.storage.PhysicalTransportVehicleStorage;

/**
 *
 * @author fido
 */
public class TestOnDemandVehicle extends OnDemandVehicle{
	
	@Inject
	public TestOnDemandVehicle(PhysicalTransportVehicleStorage vehicleStorage,
			TripsUtil tripsUtil, StationsDispatcher onDemandVehicleStationsCentral, 
			StandardDriveFactory driveActivityFactory, VisioPositionUtil positionUtil, EventProcessor eventProcessor, 
			StandardTimeProvider timeProvider, StatisticControl statisticControl,IdGenerator rebalancingIdGenerator, 
			AmodsimConfig config, @Assisted String vehicleId, @Assisted SimulationNode startPosition) {
		super(vehicleStorage, tripsUtil, onDemandVehicleStationsCentral, driveActivityFactory, positionUtil, 
				eventProcessor, timeProvider, rebalancingIdGenerator, config, vehicleId, startPosition);

	}

	/*@Override
	public void finishedDriving() {
		OnDemandVehicleState stateBeforeActions = state;   
		
		// normal actions
		//super.finishedDriving();
		
		if(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_STATION || 
				(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION && tripToStation == null)){
			tripsToStationFinished++;
			Assert.assertEquals(droppOffCount, tripsToStationFinished);
			
			statisticControl.incrementDemandFinishDrivingCounter();
			
//			Assert.assertEquals(0, cargo.size());
		}
		if(stateBeforeActions == OnDemandVehicleState.DRIVING_TO_TARGET_LOCATION){
			demandTripsFinished++;
		}
		Assert.assertTrue(demandTripsFinished == tripsToStationFinished 
				|| demandTripsFinished == tripsToStationFinished + 1);
	}*/

	@Override
	protected void dropOffDemand() {
		super.dropOffDemand(); 
	}

	@Override
	public double getVelocity() {
		return (double) super.getVelocity();
	}
}
