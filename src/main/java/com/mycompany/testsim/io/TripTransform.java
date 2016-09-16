/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mycompany.testsim.NodeExtendedFunction;
import com.vividsolutions.jts.geom.Coordinate;
import cz.agents.agentpolis.simmodel.environment.AgentPolisEnvironmentModule;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.importer.init.VehicleDataModelFactory;
import cz.agents.agentpolis.simulator.creator.initializator.InitModuleFactory;
import cz.agents.agentpolis.utils.config.ConfigReader;
import cz.agents.agentpolis.utils.nearestelement.NearestElementUtil;
import cz.agents.alite.common.event.EventProcessor;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.GTDGraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.javatuples.Pair;

/**
 *
 * @author F-I-D-O
 */
public class TripTransform {
	
	public static List<Trip<Long>> gpsTripsToOsmNodeTrips(List<Trip<GPSLocation>> gpsTrips, 
			File osmFile, int srid){
		Transformer transformer = new Transformer(srid);
		
		GTDGraphBuilder gtdBuilder = new GTDGraphBuilder(transformer, osmFile, 
				Sets.immutableEnumSet(ModeOfTransport.CAR), null, null);
        Graph<RoadNode, RoadEdge> highwayGraph = gtdBuilder.buildSimplifiedRoadGraph();

		List<Pair<Coordinate, RoadNode>> pairs = new ArrayList<>();
		
		for (RoadNode roadNode : highwayGraph.getAllNodes()) {
			pairs.add(new Pair<>(new Coordinate(roadNode.getLongitude(), roadNode.getLatitude()), roadNode));
		}
		
		NearestElementUtil<RoadNode> nearestElementUtil = new NearestElementUtil<>(pairs, new Transformer(4326), 
				new RoadNodeArrayConstructor());
		
		ArrayList<Trip<Long>> osmNodeTrips = new ArrayList<>();
		
		for (Trip<GPSLocation> trip : gpsTrips) {
			ArrayList<Long> osmNodesList = new ArrayList<>();
			
			long lastNodeId = 0;
			
			for (GPSLocation gpsLocation : trip.getLocations()) {
				RoadNode nearestNode = nearestElementUtil.getNearestElement(gpsLocation);
				
				long nodeId = nearestNode.getSourceId();
				if(nodeId != lastNodeId){
					osmNodesList.add(nearestNode.getSourceId());
					lastNodeId = nodeId;
				}
			}
			
			osmNodeTrips.add(new Trip<>(osmNodesList, trip.getStartTime(), trip.getEndTime()));
		}
		
		return osmNodeTrips;
	}
	
	public static <T> void tripsToJson(List<Trip<T>> trips, File outputFile) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.writeValue(outputFile, trips);
	}
	
	public static <T> List<Trip<T>> jsonToTrips(File inputFile, Class<T> locationType) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		
//		return (List<Trip<T>>) mapper.readValue(inputFile, new TypeReference<ArrayList<Trip<Long>>>(){});
		return mapper.readValue(inputFile, typeFactory.constructCollectionType(
				List.class, typeFactory.constructParametricType(Trip.class, locationType)));
	}
	
//	private static void nearestNodesTestbed(){
//		Injector injector = createInjector("C:\\AIC data\\cr", new File("C:\\AIC data\\cr\\czech-republic-streets-tertiary-ele.osm"));
//		NodeExtendedFunction nearestNodeFinder = injector.getInstance(NodeExtendedFunction.class);
//	}
//	
//	protected static Injector createInjector(String benchmarkDir, File osmMap) {
//        try {
//            Injector injector = Guice.createInjector();
//
//
//            // select the benchmark directory
//            File experiment = new File(benchmarkDir);
//
//            ConfigReader scenario = ConfigReader.initConfigReader(new File(experiment, "config/scenario.groovy").toURL());
//
//            // extract the filenames from scenario.groovy config file
//
//            int epsg = scenario.getInteger("epsg");
//            String vehicledatamodelPath = scenario.getString("vehicledatamodelPath");
//
//            TestbedMapInit mapInitFactory = new TestbedMapInit(epsg);
//
//            //TODO need to be set properly
//            long simulationDurationInMilisec = 0;
//            osmDTO = mapInitFactory.initMap(osmMap, injector, simulationDurationInMilisec);
//
//            EventProcessor eventProcessor = new EventProcessor();
//            ZonedDateTime initDate = ZonedDateTime.now();
//            injector = injector.createChildInjector(new AgentPolisEnvironmentModule(
//                    eventProcessor, new Random(4),
//                    osmDTO.graphByType, osmDTO.nodesFromAllGraphs, initDate));
//
//            List<InitModuleFactory> initModuleFactories = new ArrayList<>();
//
//            initModuleFactories.add(new VehicleDataModelFactory(new File(vehicledatamodelPath)));
//            initModuleFactories.add(new NearestNodeInitModuleFactory(epsg));
//            initModuleFactories.add(new TestbedPlannerModuleFactory());
//
//            injector = injector.createChildInjector(new TestbedEnvironmentModul(eventProcessor));
//
//            // add agents into the environment
////            List<AgentInitFactory> agentInits = new ArrayList<>();
////            agentInits.add(new DriverForBenchmarkInitFactory(new File(driverPopulationPath)));
//
//            for (InitModuleFactory initFactory : initModuleFactories) {
//                AbstractModule module = initFactory.injectModule(injector);
//                injector = injector.createChildInjector(module);
//            }
//
////            initModuleFactories.add(new DispatchingAndTimersInitFactory());
//            // initialize the dispatching and timers
//
//
//            return injector;
//
//        } catch (Exception e) {
//            System.out.println(e.toString());
//            return null;
//        }
//
//    }
}
