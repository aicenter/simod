package cz.cvut.fel.aic.amodsim.ridesharing.taxify;


import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import org.slf4j.LoggerFactory;


/**
 * @author olga
 * 
 */
public class SolverTaxify extends DARPSolver {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SolverTaxify.class);
    private final AmodsimConfig config;
//    private final double maxDistance;
    Graph<SimulationNode,SimulationEdge> graph;
//    private final double maxDistanceSquared;
//    private final int maxDelayTime;
    private final TripTransformTaxify tripTransform;
    
    @Inject public SolverTaxify(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, AmodsimConfig config, TimeProvider timeProvider,
        TripTransformTaxify tripTransform) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.config = config;
        this.tripTransform = tripTransform;
        graph = tripTransform.getGraph();
    };

    @Override 
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
        try {
            // Path to original .csv file with data
            List<TripTaxify<GPSLocation>>  rawDemand = tripTransform.loadTripsFromCsv(new File(config.amodsimDataDir + "/robotex2.csv"));
            Demand demand = new Demand(travelTimeProvider, config, rawDemand, graph);
            rawDemand = null;
            //demand.dumpData();
            StationCentral central = new StationCentral(tripTransform, config, travelTimeProvider, graph);
            Solution solution = new Solution(demand, travelTimeProvider,  central, config);
            solution.buildPaths();
            //demand.loadData();
            
            // Paths to save results
            Stats.writeCsv(solution.getAllCars(), demand, graph, config.amodsimExperimentDir+"result_2011.csv");
            Stats.writeEvaluationCsv(solution.getAllCars(), demand, config.amodsimExperimentDir+"eval_result_2011.csv");
        } catch (IOException ex) {
            LOGGER.error("File IO exception: "+ex);
        } catch (ParseException ex) {
            LOGGER.error("Parse exception"+ex);
        }
        return new  HashMap<>();
    }
   
    
    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

 }

