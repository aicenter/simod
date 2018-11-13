/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.config.Stations;
import cz.cvut.fel.aic.amodsim.io.Rtree;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class StationCentral {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StationCentral.class);
    AmodsimConfig config;
    TravelTimeProvider travelTimeProvider;
    Graph<SimulationNode,SimulationEdge> graph;
    private final Map<Integer, int[]> nearestStation;
    int N;
    int[] nodes; 
    private Rtree rtree;

    public StationCentral(TripTransformTaxify tripTransform,  AmodsimConfig config, TravelTimeProvider travelTimeProvider,
                            Graph<SimulationNode,SimulationEdge> graph){
        this.config = config;
        this.graph = graph; 
        this.travelTimeProvider = travelTimeProvider;
        nearestStation = new HashMap<>();
        loadStations(tripTransform);
    }
    
    public int[] findNearestStation(int nodeId){
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
    
    public int[] findNearestStation(int[] nodes){
//        LOGGER.debug(Arrays.toString(nodes));
        if(nodes.length == 2){
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
    
    

            
    private void loadStations(TripTransformTaxify tripTransform){
        try {
            List<SimulationNode> stationsList = tripTransform.loadStations();
            N = stationsList.size();
            nodes = stationsList.stream().map(n->n.id).mapToInt(Integer::intValue).toArray();
            LOGGER.info("Stations loaded: "+N);
            System.out.println(Arrays.toString(nodes));
            rtree = new Rtree(stationsList);
            System.out.println("rtree for stations, size "+rtree.size());
        } catch (IOException ex) {
            LOGGER.error("Error loading stations: "+ex);
        }
    }
}
