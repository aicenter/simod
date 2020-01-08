package cz.cvut.fel.aic.amodsim.ridesharing.offline;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Solution;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.NormalDemand;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.demand.Demand;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import cz.cvut.fel.aic.geographtools.Graph;

import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventHandler;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;

import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.DroppedDemandsAnalyzer;
import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.Car;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;

import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.TripTransformTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.io.Statistics;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;

/**
 * @author olga
 * 
 */

@Singleton
public class OfflineSolver extends DARPSolver implements EventHandler{
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OfflineSolver.class);
    
    private final AmodsimConfig config;
    
    private final Graph<SimulationNode,SimulationEdge> graph;
    
    private final TripTransformTaxify tripTransform;

    
    /*
     * @param travelTimeProvider {@link cz.cvut.fel.aic.amodsim.config.AmodsimConfig}
     * @param travelCostProvider  {@link cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider}
     * @param vehicleStorage {@link cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage}
     * @param timeProvider {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.TravelTimeProviderTaxify}
     * @param tripTransform  {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTransformTaxify}
     * @param groupGenerator {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration.GroupGenerator}
     * @param amodsimConfig {@link cz.cvut.fel.aic.amodsim.config.AmodsimConfig}
     */
    @Inject 
    public OfflineSolver(TravelTimeProvider travelTimeProvider, PlanCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, TimeProvider timeProvider,
        TripTransformTaxify tripTransform, GroupGenerator groupGenerator, AmodsimConfig config,
        DroppedDemandsAnalyzer droppedAnalyzer, StandardPlanCostProvider spcProvider,
        DefaultPlanComputationRequestFactory requestFactory, TypedSimulation eventProcessor, 
		OnDemandvehicleStationStorage onDemandvehicleStationStorage,  HighwayNetwork highwayNetwork

    ){
        super(vehicleStorage, travelTimeProvider, travelCostProvider, requestFactory);
        this.config = config;
        this.tripTransform = tripTransform;
        graph = highwayNetwork.getNetwork();
    };

    /**
     *  Takes two empty lists and returns empty list!!
     *  
     * @param newRequests
     * @param waitingRequests
     * @return 
     */
    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan>  solve(List<PlanComputationRequest> newRequests,
        List<PlanComputationRequest> waitingRequests) {
        List<TripTaxify<SimulationNode>>  trips = tripTransform.loadTripsFromCsv();
        Collections.sort(trips, Comparator.comparing(TripTaxify::getStartTime));
        
        Demand demand = new NormalDemand(travelTimeProvider, config, trips, graph);
        Solution sol = new Solution(demand, travelTimeProvider, config);
        sol.buildPaths();
        
        List<Car> cars = sol.getAllCars();
        Statistics stats = new Statistics(demand, cars, travelTimeProvider, config);
        stats.writeResults(config.amodsimExperimentDir);
        return new  HashMap<>();
    }

    @Override
    public EventProcessor getEventProcessor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleEvent(Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
 

