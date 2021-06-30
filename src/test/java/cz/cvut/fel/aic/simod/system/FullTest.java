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
package cz.cvut.fel.aic.simod.system;

import org.slf4j.LoggerFactory;
/**
 *
 * @author fido
 */
public class FullTest {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FullTest.class);
	
	public static void runFullTest(int duration, int startTime, long timeForFinishingEvents){
	  /*  SimodConfig config = Configuration.load(new SimodConfig());
		
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
