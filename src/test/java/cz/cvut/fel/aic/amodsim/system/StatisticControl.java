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
package cz.cvut.fel.aic.amodsim.system;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.StationsDispatcher;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import org.junit.Assert;

/**
 * @author fido
 */
@Singleton
public class StatisticControl implements EventHandler {
	private int demandFinishDrivingCounter;

	private final Statistics statistics;

	private final StationsDispatcher onDemandVehicleStationsCentral;

//	private final SimulationCreator simulationCreator;


	@Inject
	public StatisticControl(Statistics statistics, StationsDispatcher onDemandVehicleStationsCentral,
							SimulationCreator simulationCreator, EventProcessor eventProcessor) {
		this.statistics = statistics;
		this.onDemandVehicleStationsCentral = onDemandVehicleStationsCentral;
		this.demandFinishDrivingCounter = 0;
		eventProcessor.addEventHandler(this);
	}

	public void incrementDemandFinishDrivingCounter() {
		demandFinishDrivingCounter++;
	}

	public void simulationFinished() {

		// compares demand count in results with demands which really left the station
		Assert.assertEquals(onDemandVehicleStationsCentral.getDemandsCount(),
				statistics.getNumberOfVehiclsLeftStationToServeDemand());

		// compares demand count in results with demands which MOD vehicle reached target station
		Assert.assertEquals(onDemandVehicleStationsCentral.getDemandsCount(),
				demandFinishDrivingCounter);
	}

	@Override
	public EventProcessor getEventProcessor() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void handleEvent(Event event) {
//	   if()
	}
}
