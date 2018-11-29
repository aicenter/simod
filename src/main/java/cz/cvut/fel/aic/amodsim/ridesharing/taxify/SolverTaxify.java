package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTaxify;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.entities.StationCentral;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.TripTransformTaxify;
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
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration.GroupGenerator;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration.GroupPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration.Request;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.io.Stats;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.search.TravelTimeProviderTaxify;
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
public class SolverTaxify extends DARPSolver<TravelTimeProviderTaxify> {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SolverTaxify.class);
    private final ConfigTaxify config;
    Graph<SimulationNode,SimulationEdge> graph;
    private final TripTransformTaxify tripTransform;
	
	private final GroupGenerator groupGenerator;
	
	private final AmodsimConfig amodsimConfig;
    
    @Inject 
    public SolverTaxify(TravelTimeProviderTaxify travelTimeProvider, TravelCostProvider travelCostProvider, 
        OnDemandVehicleStorage vehicleStorage, TimeProvider timeProvider,
        TripTransformTaxify tripTransform, GroupGenerator groupGenerator, AmodsimConfig amodsimConfig) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
		this.groupGenerator = groupGenerator;
		this.amodsimConfig = amodsimConfig;
        config = new ConfigTaxify();
        this.tripTransform = tripTransform;
        graph = tripTransform.getGraph();
    };

    @Override 
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {

        
        try {
            List<TripTaxify<GPSLocation>>  rawDemand = tripTransform.loadTripsFromCsv(new File(config.tripFileName));
			
			Set<GroupPlan> groupPlans = buildGroupPlans(rawDemand);
			
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

	private Set<GroupPlan> buildGroupPlans(List<TripTaxify<GPSLocation>> rawDemand) {
		List<Request> requests = new LinkedList<>();
		int counter = 0;
		for (TripTaxify<GPSLocation> trip : rawDemand) {
			// TODO  - change for multiple nodes
			Map<Integer, Double> startNodes = trip.nodes.get(0);
			int fromId = 0;
			for(Integer nodeId: startNodes.keySet()){
				fromId = nodeId;
				break;
			}
			
			Map<Integer, Double> endNodes = trip.nodes.get(1);
			int toId = 0;
			for(Integer nodeId: endNodes.keySet()){
				toId = nodeId;
				break;
			}
			
			requests.add(new Request(trip.getStartTime(), fromId, toId, travelTimeProvider, amodsimConfig));
			
			counter++;
			if(counter >= 500000){
				break;
			}
		}
		
		return groupGenerator.generateGroups(requests);
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