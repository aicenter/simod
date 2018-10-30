package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

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
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
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
public class SolverTaxify extends DARPSolver {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SolverTaxify.class);
    private final AmodsimConfig config;
    private final double maxDistance;
    private final double maxDistanceSquared;
    private final int maxDelayTime;
    private final TripTransform tripTransform;
   // private long callCount = 0;
    //private long totalTime = 0;
   // private long iterationTime = 0;
   // private long canServeRequestCallCount = 0;
   // private long vehiclePlanningAllCallCount = 0;
    //private Map<Integer, Double> tripLengths;
    
    @Inject public SolverTaxify(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil, AmodsimConfig config, TimeProvider timeProvider,
        TripTransform tripTransform) {

        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        maxDistance = (double ) config.amodsim.ridesharing.maxWaitTime 
            * config.amodsim.ridesharing.maxSpeedEstimation / 3.6; 
        maxDistanceSquared = maxDistance * maxDistance;
        maxDelayTime = config.amodsim.ridesharing.maxWaitTime * 1000;
        //tripLengths = new HashMap<>();
        LOGGER.warn("");
    }

    @Override public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
       List<TimeTripWithValue<GPSLocation>> rawDemand = tripTransform.loadTripsFromTxt(new File(config.amodsim.tripsPath));
       Demand demand = new Demand(travelTimeProvider, config, rawDemand.size(), rawDemand.get(rawDemand.size()-1).id+1);
       demand.prepareDemand(rawDemand);
       int[][] paths = demand.buildPaths(5);
       LOGGER.info("Number of  paths " + paths.length);
       
       //int[][] adj = demand.buildAdjacency(5);
       //int[][] rAdj = demand.reverseAdjacency(adj);
       
       return new  HashMap<>();
    }
    
    private void savePath(List<double[]> path){
        
        
    }
       
    

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

    }
