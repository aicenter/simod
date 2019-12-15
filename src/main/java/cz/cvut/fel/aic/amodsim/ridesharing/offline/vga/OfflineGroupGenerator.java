/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.vga;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.NormalDemand;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;

import cz.cvut.fel.aic.geographtools.Graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;


/**
 * Group generator for MyOfflineVGASolver.
 * 
 * @author Olga Kholkovskaia <olga.kholkovskaya@gmail.com>
 */


public class OfflineGroupGenerator {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OfflineGroupGenerator.class);
    
    private final AmodsimConfig config;
    
    private final NormalDemand demand;
    
    private final TravelTimeProvider travelTimeProvider;
    
    private final Graph<SimulationNode, SimulationEdge> graph;
    
    private final int maxProlongation;
    
    private final int maxGroupSize;
    
    private final int timeToStart;

    
    
    public OfflineGroupGenerator(NormalDemand demand, TravelTimeProvider travelTimeProvider, AmodsimConfig config,
         Graph<SimulationNode, SimulationEdge> graph){
        
        this.config = config;
        this.travelTimeProvider = travelTimeProvider;
        this.demand = demand;
        this.graph = graph;
        this.maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        this.maxGroupSize = config.ridesharing.vga.maxGroupSize;
        this.timeToStart = config.ridesharing.offline.timeToStart;
     
    }
    
    /**
     * Generates all feasible groups up to maxGrupSize
     * for trips from input array.
     * 
     * @param tripIds Original trips' ids.
     * @return 
     */
    public List<int[]> generateGroups(int[] tripIds){
        //convert trip ids to index positions
//        List<Integer> tripIndices = tripIds.stream().map((id)->demand.tripIdToInd(id)).collect(Collectors.toList());
          List<Integer> tripIndices = Arrays.stream(tripIds).map((id)->demand.tripIdToInd(id))
              .boxed().collect(Collectors.toList());
        // (planTime, ind1, ind2, ind3, -(ind2+1), -(ind1+1), -(ind3+1))
        List<int[]>[] groups = new List[maxGroupSize];
//        LOGGER.debug("groups length " + groups.length);
           
        //size 1 groups for each trip
        groups[0] = new ArrayList<>();
        for (int ind : tripIndices){
            groups[0].add(new int[]{demand.getBestTime(ind) + timeToStart, ind, -(ind + 1)});
        }
        LOGGER.debug("GroupSize 1  : "+ groups[0].size());
        
        //size 2+ groups
        
        int currentGroupSize = 1;
        
        while (currentGroupSize < maxGroupSize  ){ // && !groups[k-1].isEmpty()
//            LOGGER.debug("Current group  size: "+ (currentGroupSize+1));
            int currentGroupLen = currentGroupSize*2 +1;
            int nextGroupLen = currentGroupLen + 2;
            groups[currentGroupSize] = new ArrayList<>();

            for(int[] group: groups[currentGroupSize-1]){
//             LOGGER.debug("group "+Arrays.toString(group));
               
                for (int i = 0; i < tripIndices.size(); i++){
                    int newTripInd = tripIndices.get(i);
                  
//                LOGGER.debug("new trip ind "+newTripInd);

                    // trip is already in that route, or it's id is smaller than  any trip id already in that route
                    if(inRouteOrBefore(group, newTripInd)){
//                         LOGGER.debug("in route or before ");
                        continue;
                    }
                
                    int[] nextGroup = tryInsertTrip(group, newTripInd);
//                    LOGGER.debug("\n RETURNED  " +Arrays.toString(nextGroup));
                    if(nextGroup.length == nextGroupLen ){
                        //if (nextGroup[0] > (group[0] + demand.getStartTime(newTripInd) + timeToStart)){
//                         LOGGER.debug("\n ADD  group " +Arrays.toString(nextGroup));
                        groups[currentGroupSize].add(nextGroup);
                        //}
                    }
//                    LOGGER.debug("insertion failed.");
                }//for requests
            }//for groups
            LOGGER.debug("GroupSize " + (currentGroupSize+1)+": "+ groups[currentGroupSize].size());
            currentGroupSize++;
        }//whileStream.of(Stream.of(title), Stream.of(desc), IntStream.of(thumb).mapToObj(i -> i))
        
        LOGGER.debug("Group generation finished.");
        List<int[]> allGroups = new ArrayList<>();
        for(int lev = 0; lev < groups.length; lev++){
           // allGroups.addAll(groups[lev]);
            List<int[]> gs = groups[lev];
            if (gs != null){
                allGroups.addAll(gs);
               LOGGER.debug(gs.size()+" groups of size " + (lev +1));
               //  for(int[] g : gs)  LOGGER.debug(Arrays.toString(g));
                }
            }
             
    return allGroups;
    }
    
    
   
    private int[] tryInsertTrip(int[] group, int trip){
        
        List<Integer> newGroup = Arrays.stream(group).boxed().collect(Collectors.toList());
        int bestCost = Integer.MAX_VALUE;
        int bestPosition[] = new int[]{0, 0};
   
        for(int pickupInsPosition = 1; pickupInsPosition <= group.length/2 + 1; pickupInsPosition++){
            newGroup.add(pickupInsPosition, trip);
            for(int dropoffInsPosition = newGroup.size()/2 + 1; dropoffInsPosition<= newGroup.size(); dropoffInsPosition++){
                newGroup.add(dropoffInsPosition,  -(trip+1));
                int cost = isFeasible(newGroup);
                if(cost < bestCost){
//                LOGGER.debug(" new best time" );
                    bestCost = cost;
                    bestPosition = new int[]{pickupInsPosition, dropoffInsPosition};
                }
                newGroup.remove(dropoffInsPosition);
            }//drop
            newGroup.remove(pickupInsPosition);
        }//pick
        
        if(bestCost < Integer.MAX_VALUE){
            newGroup.add(bestPosition[0], trip);
            newGroup.add(bestPosition[1], -(trip+1));
            newGroup.remove(0);
            newGroup.add(0, bestCost);
            return newGroup.stream().mapToInt(i -> i).toArray();
        }
        return group;
    }
    
    
    private int isFeasible(List<Integer> group){
         
         int ind0 = group.get(1);
         int time = demand.getStartTime(ind0) + timeToStart;
         SimulationNode node = demand.getStartNode(ind0);
         for (int pos = 2; pos < group.size(); pos++){
            int nextInd = group.get(pos);
            //pickup
            if(nextInd >= 0){
                SimulationNode nextNode = demand.getStartNode(nextInd);
                int requestTime = demand.getStartTime(nextInd);
                int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, nextNode);
                int actionTime = time + travelTime;
                actionTime = actionTime >= requestTime ? actionTime : requestTime;
                if((actionTime - requestTime) > maxProlongation){
                    return Integer.MAX_VALUE;
                }
                node = nextNode;
                time = actionTime;
            //dropoff
            }else { 
                nextInd = -nextInd - 1;
                SimulationNode nextNode = demand.getEndNode(nextInd);
                int bestTime = demand.getEndTime(nextInd);
                int travelTime = (int) travelTimeProvider.getExpectedTravelTime(node, nextNode);
                int actionTime = time + travelTime;
                if((actionTime - bestTime) > maxProlongation){
                    return Integer.MAX_VALUE;
                }
                node = nextNode;
                time = actionTime;
            }
        }
        int cost = time - demand.getStartTime(ind0);
        return cost;
    }
        
    
    private boolean inRouteOrBefore(int[] group, int trip){
        for(int i = 1; i <= group.length/2; i++){
            if(trip <= group[i]){
                return true;
            }
        }
        return false;
    }
}
