package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.io.File;

import java.util.*;
import org.slf4j.LoggerFactory;

/**
 * @author F.I.D.O.
 */
public class InsertionHeuristicSolver1 extends DARPSolver {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InsertionHeuristicSolver1.class);
    private final AmodsimConfig config;
    private final double maxDistance;
    private final double maxDistanceSquared;
    private final int maxDelayTime;
    private final TripTransform tripTransform;
    private long callCount = 0;
    private long totalTime = 0;
    private long iterationTime = 0;
    private long canServeRequestCallCount = 0;
    private long vehiclePlanningAllCallCount = 0;
    private Map<Integer, Double> tripLengths;
    
    @Inject public InsertionHeuristicSolver1(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil, AmodsimConfig config, TimeProvider timeProvider,
        TripTransform tripTransform) {

        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        maxDistance = (double ) config.amodsim.ridesharing.maxWaitTime 
            * config.amodsim.ridesharing.maxSpeedEstimation / 3.6; 
        maxDistanceSquared = maxDistance * maxDistance;
        maxDelayTime = config.amodsim.ridesharing.maxWaitTime * 1000;
        tripLengths = new HashMap<>();
        LOGGER.warn("");
    }

    @Override public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
       List<TimeTripWithValue<GPSLocation>> demand = tripTransform.loadTripsFromTxt(new File(config.amodsim.tripsPath));
       System.out.println(demand.size());
       demand = computeTripLengths(demand);
       System.out.println(demand.size());
       
       return new  HashMap<>();
    }
    
    private double computeBestLength(SimulationNode start, SimulationNode target){
        return travelTimeProvider.getTravelTime(start.id, target.id);
    }
    
    private double computeBestLength(TimeTripWithValue<GPSLocation> start, TimeTripWithValue<GPSLocation> target){
        Map<Integer, Double> startNodes = start.nodes.get(1);
        Map<Integer, Double> endNodes = target.nodes.get(0);
        double bestLength = Double.MAX_VALUE;
        for(Integer sn : startNodes.keySet()){
            for(Integer en : endNodes.keySet()){
                double n2n = travelTimeProvider.getTravelTime(sn, en);
                double s2n = startNodes.get(sn);
                double e2n = endNodes.get(en);
                double pathLength = s2n + n2n + e2n;
                bestLength = bestLength <= pathLength ? bestLength : pathLength;
            }
        }
        return bestLength;
    }
    
        private double computeBestLength(TimeTripWithValue<GPSLocation> trip){
        Map<Integer, Double> startNodes = trip.nodes.get(0);
        Map<Integer, Double> endNodes = trip.nodes.get(1);
        double bestLength = Double.MAX_VALUE;
        for(Integer sn : startNodes.keySet()){
            for(Integer en : endNodes.keySet()){
                double n2n = travelTimeProvider.getTravelTime(sn, en);
                double s2n = startNodes.get(sn);
                double e2n = endNodes.get(en);
                double pathLength = s2n + n2n + e2n;
                bestLength = bestLength <= pathLength ? bestLength : pathLength;
            }
        }
        return bestLength;
    }
    
    private double computeBestLength(SimulationNode start, TimeTripWithValue<GPSLocation> target){
        Map<Integer, Double> endNodes = target.nodes.get(0);
        double bestLength = Double.MAX_VALUE;
        for(Integer en : endNodes.keySet()){
            double n2n = travelTimeProvider.getTravelTime(start.id, en);
            double e2n = endNodes.get(en);
            double pathLength = n2n + e2n;
            bestLength = bestLength <= pathLength ? bestLength : pathLength;
        }
        return bestLength;
    }
    
    private double computeBestLength(TimeTripWithValue<GPSLocation> start, SimulationNode target){
        Map<Integer, Double> startNodes = start.nodes.get(1);
        double bestLength = Double.MAX_VALUE;
        for(Integer sn : startNodes.keySet()){
            double n2n = travelTimeProvider.getTravelTime(start.id, sn);
            double s2n = startNodes.get(sn);
            double pathLength = n2n + s2n;
            bestLength = bestLength <= pathLength ? bestLength : pathLength;
        }
        return bestLength;
    }
       
    private List<TimeTripWithValue<GPSLocation>> computeTripLengths(List<TimeTripWithValue<GPSLocation>> demand ){
        int count = 0;
        List<TimeTripWithValue<GPSLocation>> filteredDemand = new ArrayList<>();
        for (TimeTripWithValue<GPSLocation> trip : demand){
            double bestLength = computeBestLength(trip);
            if (bestLength > 25000){
                count++;
            }else{
                tripLengths.put(trip.id, bestLength);
                trip.setShortestLength(bestLength);
                filteredDemand.add(trip);
            }
        }
        System.out.println("Filtered "+count);
        return filteredDemand;
    }
    

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

    }
