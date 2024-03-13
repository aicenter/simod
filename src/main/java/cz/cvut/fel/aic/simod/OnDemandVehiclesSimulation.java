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
package cz.cvut.fel.aic.simod;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.MapData;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.init.RequestsInitializer;
import cz.cvut.fel.aic.simod.init.StationsInitializer;
import cz.cvut.fel.aic.simod.init.StatisticInitializer;
import cz.cvut.fel.aic.simod.init.VehicleInitializer;
import cz.cvut.fel.aic.simod.traveltimecomputation.AstarTravelTimeProvider;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.statistics.Statistics;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.PrimitiveIterator;
import java.util.Random;

import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class OnDemandVehiclesSimulation {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehiclesSimulation.class);

	public static boolean gurobiAvailable(){
		try {
			Class.forName("com.gurobi.gurobi.GRBEnv");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static boolean hdf5Available(){
		try {
			Class.forName("as.hdfql.HDFql");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public static void main(String[] args) throws MalformedURLException {
		new OnDemandVehiclesSimulation().run(args);
	}

	public static void checkPaths(SimodConfig config, AgentpolisConfig agentpolisConfig){
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

	private static void checkTravelTimeProvider(Injector injector) {
		TravelTimeProvider travelTimeProvider = injector.getInstance(TravelTimeProvider.class);
		if(!(travelTimeProvider instanceof AstarTravelTimeProvider)){
			AstarTravelTimeProvider astarTravelTimeProvider = injector.getInstance(AstarTravelTimeProvider.class);

			MapData map = injector.getInstance(MapInitializer.class).getMap();
			PrimitiveIterator.OfInt ris = new Random().ints(0, map.nodesFromAllGraphs.size()).iterator();
			boolean fail = false;
			for (int i = 0; i < 5; i++) {
				SimulationNode from = map.nodesFromAllGraphs.get(ris.nextInt());
				SimulationNode to = map.nodesFromAllGraphs.get(ris.nextInt());
				long time = travelTimeProvider.getExpectedTravelTime(from, to);
				long timeAstar = astarTravelTimeProvider.getExpectedTravelTime(from, to);
				if (time != timeAstar) {
					LOGGER.error("Travel time provider is not consistent with AstarTravelTimeProvider.\n"
							+ "Time from {} to {}\n" +
								"AstarTravelTimeProvider: {}\n" +
								"{}: {}", from, to, timeAstar, travelTimeProvider.getClass().getSimpleName(), time);
					fail = true;
				}
			}
			if (fail) {
				throw new RuntimeException("Travel time provider is not consistent with AstarTravelTimeProvider.");
			}
		}
	}

	public void run(String[] args) {
		SimodConfig config = new SimodConfig();
		
		File localConfigFile = null;
		if(args.length > 0){
			localConfigFile = new File(args[0]);
		}               
		Injector injector = new AgentPolisInitializer(new MainModule(config, localConfigFile)).initialize();
		
		checkPaths(config, injector.getInstance(AgentpolisConfig.class));
		
		// overide the disfunctional old no ridesharing setup
		if(!config.ridesharing.on){
			config.ridesharing.on = true;
			config.ridesharing.vehicleCapacity = 1;
		}
		
		SimulationCreator creator = injector.getInstance(SimulationCreator.class);        
		
		// prepare map, entity storages...
		creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());

		// check that travel time provider provides the same results as AstarTravelTimeProvider
		// Need to be done after map initialization in SimulationCreator.prepareSimulation()
//		checkTravelTimeProvider(injector);

		// load stations
		injector.getInstance(StationsInitializer.class).loadStations();

		// load vehicles if they are in a separate file
		if(!config.vehiclesFilePath.isBlank()){
			injector.getInstance(VehicleInitializer.class).run();
		}

		if(config.rebalancing.on){

			if(!gurobiAvailable()) {
				LOGGER.error("Gurobi not available, rebalancing will not be used");
			}
			else {
				// start rebalancing
                try {
                    Class<?> rebalancingStationClass
                            = Class.forName("cz.cvut.fel.aic.rebalancing.ReactiveRebalancing");
					Object reactiveRebalancing = injector.getInstance(rebalancingStationClass);
					Method startMethod = rebalancingStationClass.getMethod("start");
					startMethod.invoke(reactiveRebalancing);
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

		}
		
		// load trips
		injector.getInstance(RequestsInitializer.class).initialize();

		injector.getInstance(StatisticInitializer.class).initialize();
            
		// start it up
		creator.startSimulation();

//		if (config.useTripCache) {
//			injector.getInstance(TripsUtilCached.class).saveNewTrips();
//		}
		injector.getInstance(Statistics.class).simulationFinished();
		injector.getInstance(TravelTimeProvider.class).printCalls();
		injector.getInstance(TravelTimeProvider.class).close();
	}


}
