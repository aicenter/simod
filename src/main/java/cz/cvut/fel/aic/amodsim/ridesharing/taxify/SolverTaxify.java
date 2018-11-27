package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.StationCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTransformTaxify;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.DARPSolver;
import cz.cvut.fel.aic.amodsim.ridesharing.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.Stats;
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
    private final ConfigTaxify config;
    Graph<SimulationNode,SimulationEdge> graph;
    private final TripTransformTaxify tripTransform;
    
    @Inject 
    public SolverTaxify(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, TimeProvider timeProvider,
        TripTransformTaxify tripTransform) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        config = new ConfigTaxify();
        this.tripTransform = tripTransform;
        graph = tripTransform.getGraph();
    };

    @Override 
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {

        
        try {
            List<TripTaxify<GPSLocation>>  rawDemand = tripTransform.loadTripsFromCsv(new File(config.tripFileName));
            // Path to original .csv file with data
            Demand demand = new Demand(travelTimeProvider, config, rawDemand, graph);
            rawDemand = null;
            StationCentral central = new StationCentral(config, travelTimeProvider, graph);
            //demand.dumpData();
            Solution sol = new Solution(demand, travelTimeProvider, central, config);
            sol.buildPaths();

//          demand.loadData();
            //uncomment to save results
            Date timeStamp = Calendar.getInstance().getTime();
            Stats.writeEvaluationCsv(sol.getAllCars(), demand, config, central, timeStamp);
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

//n1 = graph.getNode(4139);
//        n2  = graph.getNode(8073);
//        System.out.println(n1.id+": "+n1.getLatitude()+", "+n1.getLongitude());
//        System.out.println(n2.id+": "+n2.getLatitude()+", "+n2.getLongitude());
//        System.out.println("    Time in millis ="+travelTimeProvider.getTravelTimeInMillis(n1.id, n2.id));
//        n1 = graph.getNode(32);
//        n2  = graph.getNode(4833);
//        System.out.println(n1.id+": "+n1.getLatitude()+", "+n1.getLongitude());
//        System.out.println(n2.id+": "+n2.getLatitude()+", "+n2.getLongitude());
//        System.out.println("    Time in millis ="+travelTimeProvider.getTravelTimeInMillis(n1.id, n2.id));
//
//        SimulationNode n1 = graph.getNode(539);
//        SimulationNode n2  = graph.getNode(8471);
//        System.out.println(n1.id+": "+n1.getLatitude()+", "+n1.getLongitude());
//        System.out.println(n2.id+": "+n2.getLatitude()+", "+n2.getLongitude());
//        System.out.println("    Time in millis ="+travelTimeProvider.getTravelTimeInMillis(n1.id, n2.id));
//        

//            int[] sample = new int[] {16003,25538, 138230, 159386};
//            for(int tripId : sample){
//                System.out.println("Id "+ tripId);
//                int tripInd = demand.id2ind(tripId);
//                int[] starts = demand.getStartNodes(tripInd);
//                int[] ends = demand.getEndNodes(tripInd);
//                System.out.println("    Starts "+Arrays.toString(starts));
//                System.out.println("    Ends "+Arrays.toString(ends));
//            }