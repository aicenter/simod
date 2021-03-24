/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.amodsim;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.init.EventInitializer;
import cz.cvut.fel.aic.amodsim.init.StationsInitializer;
import cz.cvut.fel.aic.amodsim.init.StatisticInitializer;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.rebalancing.ReactiveRebalancing;
import cz.cvut.fel.aic.amodsim.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.statistics.Statistics;
import cz.cvut.fel.aic.amodsim.tripUtil.TripsUtilCached;
import java.io.File;
import java.net.MalformedURLException;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class OnDemandVehiclesSimulation {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehiclesSimulation.class);
	
	public static void main(String[] args) throws MalformedURLException {
		new OnDemandVehiclesSimulation().run(args);
	}

	public static void checkPaths(AmodsimConfig config, AgentpolisConfig agentpolisConfig){
		String[] pathsForRead = {
			config.tripsPath,
			config.stationPositionFilepath,
			agentpolisConfig.mapNodesFilepath,
			agentpolisConfig.mapEdgesFilepath
		};
		
		String[] pathsForWrite = {
			config.statistics.allEdgesLoadHistoryFilePath,
			config.statistics.darpSolverComputationalTimesFilePath,
			config.statistics.groupDataFilePath,
			config.statistics.occupanciesFilePath,
			config.statistics.resultFilePath,
			config.statistics.ridesharingFilePath,
			config.statistics.serviceFilePath,
			config.statistics.transitStatisticFilePath,
			config.statistics.tripDistancesFilePath,
			config.ridesharing.vga.groupGeneratorLogFilepath
		};
		
		try{
			for(String pathForRead: pathsForRead){
				FileUtils.checkFilePathForRead(pathForRead);
			}
			
			for(String pathForWrite: pathsForWrite){
				FileUtils.checkFilePathForWrite(pathForWrite);
			}
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void run(String[] args) {
		AmodsimConfig config = new AmodsimConfig();
		
		File localConfigFile = null;
		if(args.length > 0){
			localConfigFile = new File(args[0]);
		}               
		Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();
		
		checkPaths(config, injector.getInstance(AgentpolisConfig.class));
		
		SimulationCreator creator = injector.getInstance(SimulationCreator.class);         
		// prepare map, entity storages...
		creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

		TripTransform tripTransform = injector.getInstance(TripTransform.class);
		
		// load stations
		injector.getInstance(StationsInitializer.class).loadStations();

		if(config.rebalancing.on){

			// start rebalancing
			injector.getInstance(ReactiveRebalancing.class).start();

		}
		
		// load trips
		injector.getInstance(EventInitializer.class).initialize(
		tripTransform.loadTripsFromTxt(new File(config.tripsPath)), null);

		injector.getInstance(StatisticInitializer.class).initialize();
            
		// start it up
		creator.startSimulation();

		if (config.useTripCache) {
			injector.getInstance(TripsUtilCached.class).saveNewTrips();
		}
		injector.getInstance(Statistics.class).simulationFinished();
		injector.getInstance(TravelTimeProvider.class).printCalls();
		injector.getInstance(TravelTimeProvider.class).close();
	}
}
