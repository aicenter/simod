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

import org.slf4j.LoggerFactory;
/**
 *
 * @author fido
 */
public class FullTest {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FullTest.class);
	
	public static void runFullTest(int duration, int startTime, long timeForFinishingEvents){
	  /*  AmodsimConfig config = Configuration.load(new AmodsimConfig());
		
		//config overwrite
		config.simulationDurationInMillis = duration;
		config.startTime = startTime;
//		config.showVisio = true;
		Common.setTestResultsDir(config, "test");
		
		// Guice configuration
		AgentPolisInitializer agentPolisInitializer = new AgentPolisInitializer(new MainModule(config));
		agentPolisInitializer.overrideModule(new TestModule());
		Injector injector = agentPolisInitializer.initialize();

		SimulationCreator creator = injector.getInstance(SimulationCreator.class);

		// prepare map, entity storages...
		creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

		List<TimeTrip<Long>> osmNodesList;
		try {
			osmNodesList = TripTransform.jsonToTrips(new File(config.preprocessedTrips), Long.class);
			RebalancingLoader rebalancingLoader = injector.getInstance(RebalancingLoader.class);
			rebalancingLoader.load(new File(config.rebalancing.policyFilePath));

			//  injector.getInstance(EntityInitializer.class).initialize(rebalancingLoader.getOnDemandVehicleStations());

			injector.getInstance(EventInitializer.class).initialize(osmNodesList,
					rebalancingLoader.getRebalancingTrips(), config);

			injector.getInstance(StatisticInitializer.class).initialize();

			// start it up
			creator.startSimulation();

			injector.getInstance(StatisticControl.class).simulationFinished();


		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}*/
	}
}
