/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.io.Rtree;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * Keeps information about stations.
 * Returns nearest station for simulation nodes by id.
 * 
 * @author olga
 */
public class StationCentral {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StationCentral.class);
    ConfigTaxify config;
    TravelTimeProvider travelTimeProvider;
    Graph<SimulationNode,SimulationEdge> graph;
    private final Map<Integer, int[]> nearestStation;
    private final int N;
    Map<Integer, Integer> depoIds;
    private Rtree rtree;

    /**
     * Loads station coordinates from .csv, maps stations to graph nodes,
     * create rtree for search.
     * 
     * @param config
     * @param travelTimeProvider
     * @param graph 
     */
    public StationCentral(ConfigTaxify config, TravelTimeProvider travelTimeProvider,
                          Graph<SimulationNode,SimulationEdge> graph){
        this.config = config;
        this.graph = graph; 
        this.travelTimeProvider = travelTimeProvider;
        nearestStation = new HashMap<>();
        depoIds = new HashMap<>();
        N = config.maxStations;
        loadStations();
    }
    
    /**
     * Finds nearest station by SimulationNode id.
     * 
     * @param nodeId id of SimulationNode
     * @return id of Simulation node where the nearest station is located
     */
    public int[] findNearestStation(int nodeId){
        //TODO the map is completely filled, destroy the rtree.
        if(nearestStation.containsKey(nodeId)){
            return nearestStation.get(nodeId);
        }
        List<Integer> stations = rtree.findNearestStations(graph.getNode(nodeId));
        nearestStation.put(nodeId, new int[2]);
        int bestTime = Integer.MAX_VALUE;
        for(Integer station: stations){
            int time = travelTimeProvider.getTravelTimeInMillis(nodeId, station);
            if(time < bestTime){
                bestTime = time;
                nearestStation.get(nodeId)[0] = station;
                nearestStation.get(nodeId)[1] = bestTime;
            }
        }
        return nearestStation.get(nodeId);
    }
    /**
     * Finds nearest station for the location with one or two possible nodes.
     * 
     * @param nodes array of simulationNodes ids and distances 
     * [node1, dist1, node2, dist2]
     * array length is either 2 (one node, ie gps location was mapped to the node),
     * or 4 (2 nodes, mapped to edge)
     * @return 
     */
    public int[] findNearestStation(int[] nodes){
    
//        LOGGER.debug(Arrays.toString(nodes));
        if(nodes.length == 2){ // 
            return findNearestStation(nodes[0]);
        }
        int[] node1Result = findNearestStation(nodes[0]);
        int[] node2Result = findNearestStation(nodes[2]);
        if(node1Result[1] <= node2Result[1]){
            return node1Result;
        }else{
            return node2Result;
        }
    }
    /**
     * returns depo id (from 0 to maxDepo) by SimulationNode id
     * 
     * @param nodeId
     * @return 
     */
    public int getDepoId(int nodeId){
        nodeId = nodeId >=0 ? nodeId : -nodeId;
        return depoIds.get(nodeId);
    }
            
    private void loadStations(){
        int radius = 4*config.pickupRadius;
        //List<SimulationNode> stationNodes = new ArrayList<>();
        rtree = new Rtree(this.graph.getAllNodes(), this.graph.getAllEdges());
        try (BufferedReader br = new BufferedReader(new FileReader(config.depoFileName))) {
            String line = br.readLine();
            for(int i= 0; i < N; i++){
                line = br.readLine();
                String[] parts = line.split(",");
                //System.out.println(count+Arrays.toString(parts));
                GPSLocation loc = GPSLocationTools.createGPSLocation(Double.parseDouble(parts[0]),
                                                                    Double.parseDouble(parts[1]),
                                                                    0, config.SRID);
                Object[] result = rtree.findNode(loc, radius);
                if (result == null){
                    LOGGER.error("Node not found for station " + i);
                }else{
                    depoIds.put((int) result[0], i);
                }
            }
            rtree = null;
            if(depoIds.size() != N){
                LOGGER.error("Number of stations loaded differs from config");
            }
            rtree = new Rtree(depoIds.keySet().stream()
                                .map(nodeId->graph.getNode(nodeId))
                                .collect(Collectors.toList()));
        } catch (IOException ex) {
            LOGGER.error("Error loading stations: "+ex);
        }
    }
}


