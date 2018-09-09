/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.alite.common.event.Event;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.event.OnDemandVehicleStationsCentralEvent;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;


/**
 *
 * @author fiedlda1
 */
@Singleton
public class RidesharingStationsCentral extends OnDemandVehicleStationsCentral{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RidesharingStationsCentral.class);
    
	private final DARPSolver solver;
    private final TravelTimeProvider travelTimeProvider;
    
    private final long  maxRideTime = 1800000; // 30 min in ms??
    
    
    int tooLongTripsCount = 0;
    int demandCount = 0;
    int acceptedDemandCount = 0;
    double totalValue = 0;
    double droppedValue = 0;
    
	
	
	@Inject
	public RidesharingStationsCentral(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			EventProcessor eventProcessor, AmodsimConfig config, TravelTimeProvider travelTimeProvider, DARPSolver solver, @Named("mapSrid") int srid) {
		super(onDemandvehicleStationStorage, eventProcessor, config, srid);
		this.solver = solver;
        this.travelTimeProvider = travelTimeProvider;
	}
	
	@Override
    public void handleEvent(Event event) {
        OnDemandVehicleStationsCentralEvent eventType = (OnDemandVehicleStationsCentralEvent) event.getType();
        
        switch(eventType){
            case DEMAND:
                processDemand(event);
                break;
            case REBALANCING:
                super.serveRebalancing(event);
                break;
        }
        
    }
    
    private void processDemand(Event event) {
        demandCount++;
        if(demandCount%10000 == 0){
            System.out.println(getStatistics());
        }
        DemandData demandData = (DemandData) event.getContent();
        List<SimulationNode> locations = demandData.locations;
        Node startNode = locations.get(0);
        Node endNode = locations.get(locations.size() -1 );
        double travelTime = travelTimeProvider.getTravelTime((SimulationNode) startNode, (SimulationNode) endNode);
	    if(travelTime >= maxRideTime){
            tooLongTripsCount++;
            //LOGGER.info("Demand is too long, count {}", tooLongTripsCount);
        }else{
            serveDemand(startNode, demandData);
        }
    
    } 
    
	@Override
	protected void serveDemand(Node startNode, DemandData demandData) {
        acceptedDemandCount++;
        double rideValue = demandData.demandAgent.getRideValue();
        totalValue += rideValue;
        //LOGGER.info("Demand is accepted, count {}",  acceptedDemandCount);
		List<OnDemandRequest> requests = new LinkedList<>();
		requests.add(new OnDemandRequest(demandData.demandAgent, demandData.locations.get(1)));
		Map<RideSharingOnDemandVehicle,DriverPlan> newPlans = solver.solve(requests);
		
		if(newPlans.isEmpty()){
			numberOfDemandsDropped++;
            droppedValue += rideValue;
            //LOGGER.info("Demand is dropped, count {}", numberOfDemandsDropped);
			demandData.demandAgent.setDropped(true);
		}
		else{
			for(Entry<RideSharingOnDemandVehicle,DriverPlan> entry: newPlans.entrySet()){
				RideSharingOnDemandVehicle vehicle = entry.getKey();
				DriverPlan plan = entry.getValue();
				vehicle.replan(plan);
			}
		}
	}
    
    public String getStatistics(){
        StringBuilder sb = new StringBuilder("RideSharingCentral stats: \n");
        sb.append("Total demands received: ").append(demandCount).append("\n");
        sb.append("Accepted demands: ").append(acceptedDemandCount).append(" , ").
            append(100*acceptedDemandCount/demandCount).append("% of received \n");
        sb.append("Total demands dropped: ").append(numberOfDemandsDropped).append(" , ").
            append(100*numberOfDemandsDropped/acceptedDemandCount).append("% of accepted \n");
        sb.append("Total value of received demands: ").append(totalValue).append("\n");
        sb.append("Total value of dropped demands: ").append(droppedValue).append(" ,").
            append(100*droppedValue/totalValue).append("% of accepted \n");
        sb.append("Total value earnde: ").append(totalValue - droppedValue).append(" ,").
            append(100*(totalValue - droppedValue)/totalValue).append("% of accepted \n");
        return sb.toString();
    }
    


	
	
	
}
