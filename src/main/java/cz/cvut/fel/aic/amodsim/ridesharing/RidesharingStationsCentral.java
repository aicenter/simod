/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.amodsim.DemandData;
import cz.cvut.fel.aic.amodsim.OnDemandVehicleStationsCentral;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import cz.cvut.fel.aic.geographtools.Node;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class RidesharingStationsCentral extends OnDemandVehicleStationsCentral{
	
	private final DARPSolver solver;
	
	
	
	
	@Inject
	public RidesharingStationsCentral(OnDemandvehicleStationStorage onDemandvehicleStationStorage, 
			EventProcessor eventProcessor, AmodsimConfig config, DARPSolver solver, @Named("mapSrid") int srid) {
		super(onDemandvehicleStationStorage, eventProcessor, config, srid);
		this.solver = solver;
	}

	
	
	
	@Override
	protected void serveDemand(Node startNode, DemandData demandData) {
		List<OnDemandRequest> requests = new LinkedList<>();
		requests.add(new OnDemandRequest(demandData.demandAgent, demandData.locations.get(1)));
		Map<RideSharingOnDemandVehicle,DriverPlan> newPlans = solver.solve(requests);
		
		if(newPlans.isEmpty()){
			numberOfDemandsDropped++;
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
	
	
	
}
