package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.OnDemandRequest;
import cz.cvut.fel.aic.amodsim.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.RideSharingOnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author F.I.D.O.
 */
public class InsertionHeuristicSolver extends DARPSolver{

	private final PositionUtil positionUtil;
	
	private final AmodsimConfig config;
	
	@Inject
	public InsertionHeuristicSolver(OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
			AmodsimConfig config) {
		super(vehicleStorage);
		this.positionUtil = positionUtil;
		this.config = config;
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests, 
			TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider) {
		Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new HashMap<>();
		
		double minCost = Double.MAX_VALUE;
		OnDemandRequest request = requests.get(0);
		
		for(OnDemandVehicle tVvehicle: vehicleStorage){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
			if(canServeRequest(vehicle, request, travelTimeProvider)){
				DriverPlan newPlan = getOptimalPlan(vehicle, request);
				6. if [ , ] != [ , ] and < * then
				7. [ , ] = [ , ]
				8. =
				9. end if
				10. end if
				11. end for
				12. Insert( , , )
				13. Update( )
			}
		}
		
		
		return planMap;
	}
	
	private boolean canServeRequest(RideSharingOnDemandVehicle vehicle, OnDemandRequest request, 
			TravelTimeProvider travelTimeProvider){
		if(!vehicle.hasFreeCapacity()){
			return false;
		}
		
		// cartesian distance check
		double distance = positionUtil.getPosition(vehicle.getPosition())
				.distance(positionUtil.getPosition(request.getPosition()));
		double maxDistance = config.amodsim.ridesharing.maxWaitTime 
				* config.amodsim.ridesharing.maxSpeedEstimation / 3600;
		if(distance > maxDistance){
			return false;
		}
		
		// real feasibilitz check 
		// TODO compute from interpolated position
		return travelTimeProvider.getTravelTime(vehicle.getPosition(), request.getPosition()) < maxDistance;
		
		
		
	}

	private DriverPlan getOptimalPlan(RideSharingOnDemandVehicle vehicle, OnDemandRequest request, 
			TravelTimeProvider travelTimeProvider) {
		double minCostIncrement = Double.MAX_VALUE;
		DriverPlan currentPlan = vehicle.getCurrentPlan();
		
		for(int pickupOptionIndex = 1; pickupOptionIndex <= currentPlan.getLength() + 2; pickupOptionIndex++){
			for(int dropoffOptionIndex = 2; dropoffOptionIndex <= currentPlan.getLength() + 3; dropoffOptionIndex++){
				DriverPlan potentialPlan = insertIntoPlan(currentPlan, pickupOptionIndex, dropoffOptionIndex, request,
						travelTimeProvider);
				if(planFeasible(potentialPlan, vehicle)){
					
				}
			}
		}

		6. if FeasibleTour( , , ) then
		7. = Objective( , , ) - Objective( )
		8. if < * then
		9. * =
		10. [ , ] = [ , ]
		11. end if
		12. end if
		13. end for
		14. end for
		15. Return [ , ], *
	}

	private DriverPlan insertIntoPlan(DriverPlan currentPlan, int pickupOptionIndex, int dropoffOptionIndex, 
			OnDemandRequest request, TravelTimeProvider travelTimeProvider) {
		List<DriverPlanTask> newPlan = new LinkedList<>();
		double newTravelTime = currentPlan.getPlannedTraveltime();
		
		Iterator<DriverPlanTask> oldPlanIterator = currentPlan.iterator();
		
		for(int newPlanIndex = 1; newPlanIndex <= currentPlan.getLength() + 3; newPlanIndex++){
			if(newPlanIndex == pickupOptionIndex){
				newPlan.add(new DriverPlanTask(DriverPlanTaskType.PICKUP, request.getDemandAgent()));
			}
			else if(newPlanIndex == dropoffOptionIndex){
				newPlan.add(new DriverPlanTask(DriverPlanTaskType.DROPOFF, request.getDemandAgent()));
			}
			else{
				newPlan.add(oldPlanIterator.next());
			}
		}
		
		return new DriverPlan(newPlan);
	}

	// TODO passanger time constraints
	private boolean planFeasible(DriverPlan potentialPlan, RideSharingOnDemandVehicle vehicle) {
		int occupancy = vehicle.getOnBoardCount();
		int capacity = vehicle.getVehicle().getCapacity();
		
		for(DriverPlanTask planTask: potentialPlan){
			if(planTask.getTaskType() == DriverPlanTaskType.DROPOFF){
				occupancy--;
			}
			else{
				if(++occupancy > capacity){
					return false;
				}
			}
		}
		return true;
	}
	
}
