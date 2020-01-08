/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.io;


import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.search.Rtree;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
    
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.LoggerFactory;


/**
 *
 * @author Olga Kholkovskaia
 */
@Singleton
public class TripTransformTaxify {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripTransformTaxify.class);
    
    private final AmodsimConfig config;
  
    private final Graph<SimulationNode,SimulationEdge> graph;
    
    private Rtree rtree;
    
    private int tooFarCount = 0;
    
    private int sameNodeCount = 0;
    
    
    
    /**
     *
     * @param highwayNetwork
     * {@link  cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork};
     * @param nearestElementUtils 
     *          {@link cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils}
     * @param travelTimeProvider {@link cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider}
     * @param config {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify}
     */
    @Inject
    public TripTransformTaxify(HighwayNetwork highwayNetwork, NearestElementUtils nearestElementUtils,
        AmodsimConfig config, TravelTimeProvider travelTimeProvider) {
        this.graph = highwayNetwork.getNetwork();
        this.config = config;

    }    
   
       	
    /**
     * Reads demand data from .csv, and prepares it for solver.
     * 
     * @param inputFile
     * @return
     */
    public List<TripTaxify<SimulationNode>> loadTripsFromCsv(){
        
        //TODO srid from parent config? 
        int SRID = 32116;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        File inputFile = new File(config.tripsPath);
        LocalDateTime startDateTime = LocalDateTime.parse(config.ridesharing.offline.startFrom ,formatter);
        int pickupRadius = config.ridesharing.offline.pickupRadius;
        int count = 0;
        rtree = new Rtree(this.graph.getAllNodes());
        List<TripTaxify<SimulationNode>> trips = new LinkedList<>();
  
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line = br.readLine(); //header
  
            while ((line = br.readLine()) != null) {
                count++;
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                
                GPSLocation startLocation = GPSLocationTools.createGPSLocation(
                    Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), 0, SRID);
                GPSLocation targetLocation = GPSLocationTools.createGPSLocation(
                    Double.parseDouble(parts[4]), Double.parseDouble(parts[5]), 0, SRID);

                long millisFromStart = parseTime(LocalDateTime.parse(parts[1].substring(0,19) ,formatter),
                    startDateTime);
                
                Integer startNodeId = rtree.findNodeId(startLocation, pickupRadius);
                if(startNodeId == null){
                    tooFarCount++;
                    continue;
                }
                Integer endNodeId = rtree.findNodeId(targetLocation, pickupRadius);
                if(endNodeId == null){
                    tooFarCount++;
                    continue;
                }
                if(startNodeId.equals(endNodeId)){
                    sameNodeCount++;
                    continue;
                }
                trips.add(new TripTaxify<>(id, millisFromStart, graph.getNode(startNodeId),
                    graph.getNode(endNodeId)));
            }
        }catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
        
        if(trips.isEmpty()){
            LOGGER.error("Empty trip list ");
            System.exit(-1);
        }
        
        LOGGER.info("{} trips in csv file", count);
        LOGGER.info("{} trips remained", trips.size());
        LOGGER.info("{} nodes not found in node tree", tooFarCount);
        LOGGER.info("{} same start and target node", sameNodeCount);
        Collections.sort(trips, Comparator.comparing(TripTaxify::getStartTime));
        return trips;
    }
   
        
    private long parseTime(LocalDateTime dateTime, LocalDateTime startDateTime){
        int startDay = startDateTime.getDayOfMonth();
        int startHour = startDateTime.getHour();
        int day = dateTime.getDayOfMonth() - startDay;
        int hour = day*24 + dateTime.getHour() - startHour;
        int min = hour*60 + dateTime.getMinute();
        int sec = min*60 + dateTime.getSecond();
        long millisFromStart = sec*1000;
        return millisFromStart;
    }
}
   