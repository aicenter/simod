package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
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
	
//	private static 

	private final PositionUtil positionUtil;
	
	private final AmodsimConfig config;
	
	private final double maxDistance;
	
	private final double maxDistanceSquared;
	
	private final int maxWaitTime;
	
	@Inject
	public InsertionHeuristicSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
			AmodsimConfig config) {
		super(vehicleStorage, travelTimeProvider, travelCostProvider);
		this.positionUtil = positionUtil;
		this.config = config;
		maxDistance = config.amodsim.ridesharing.maxWaitTime 
				* config.amodsim.ridesharing.maxSpeedEstimation / 3600 * 1000;
		maxDistanceSquared = maxDistance * maxDistance;
		maxWaitTime = config.amodsim.ridesharing.maxWaitTime  * 1000;
	}

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
		Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new HashMap<>();
		
		double minCost = Double.MAX_VALUE;
		DriverPlan bestPlan = null;
		RideSharingOnDemandVehicle servingVehicle = null;
		OnDemandRequest request = requests.get(0);
		
		for(OnDemandVehicle tVvehicle: vehicleStorage){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
			if(canServeRequest(vehicle, request)){
				DriverPlan newPlan = getOptimalPlan(vehicle, request);
				if(newPlan.getPlannedTraveltime() < minCost){
					minCost = newPlan.getPlannedTraveltime();
					bestPlan = newPlan;
					servingVehicle = vehicle;
				}
			}
		}
		
		if(bestPlan != null){
			planMap.put(servingVehicle, bestPlan);
		}
		else{
			debugFail(request);
		}
		
		
		return planMap;
	}
	
	private boolean canServeRequest(RideSharingOnDemandVehicle vehicle, OnDemandRequest request){
		
		// do not mess with rebalancing
		if(vehicle.getState() == OnDemandVehicleState.REBALANCING){
			return false;
		}
		
		if(!vehicle.hasFreeCapacity()){
			return false;
		}
		
		// node identity
		if(vehicle.getPosition() == request.getPosition()){
			return true;
		}
		
		// euclidean distance check
		double dist_x = vehicle.getPosition().getLatitudeProjected() - request.getPosition().getLatitudeProjected();
		double dist_y = vehicle.getPosition().getLongitudeProjected() - request.getPosition().getLongitudeProjected();
		double distanceSquared = dist_x * dist_x + dist_y * dist_y;
		if(distanceSquared > maxDistanceSquared){
			return false;
		}
//		double distance = GPSLocationTools.computeDistanceAsDouble(vehicle.getPosition(), request.getPosition());
//		if(distance > maxDistance){
//			return false;
//		}
		
		// real feasibility check 
		// TODO compute from interpolated position
		return travelTimeProvider.getTravelTime(vehicle, vehicle.getPosition(), request.getPosition()) 
				< maxWaitTime;
	}

	private DriverPlan getOptimalPlan(RideSharingOnDemandVehicle vehicle, OnDemandRequest request) {
		double minCostIncrement = Double.MAX_VALUE;
		DriverPlan bestPlan = null;
		DriverPlan currentPlan = vehicle.getCurrentPlan();
		
		for(int pickupOptionIndex = 1; pickupOptionIndex <= currentPlan.getLength(); pickupOptionIndex++){
			for(int dropoffOptionIndex = pickupOptionIndex + 1; dropoffOptionIndex <= currentPlan.getLength() + 1; 
					dropoffOptionIndex++){
				DriverPlan potentialPlan = insertIntoPlan(currentPlan, pickupOptionIndex, dropoffOptionIndex, request);
				if(planFeasible(potentialPlan, vehicle)){
					computePlanCost(vehicle, currentPlan, potentialPlan, request);
					double costIncrement = potentialPlan.getPlannedTraveltime() - currentPlan.getPlannedTraveltime();
					if(costIncrement < minCostIncrement){
						minCostIncrement = costIncrement;
						bestPlan = potentialPlan;
					}
				}
			}
		}
		return bestPlan;
	}

	private DriverPlan insertIntoPlan(DriverPlan currentPlan, int pickupOptionIndex, int dropoffOptionIndex, 
			OnDemandRequest request) {
		List<DriverPlanTask> newPlan = new LinkedList<>();
		
		Iterator<DriverPlanTask> oldPlanIterator = currentPlan.iterator();
		
		for(int newPlanIndex = 0; newPlanIndex <= currentPlan.getLength() + 1; newPlanIndex++){
			if(newPlanIndex == pickupOptionIndex){
				newPlan.add(new DriverPlanTask(DriverPlanTaskType.PICKUP, request.getDemandAgent(), 
						request.getDemandAgent().getPosition()));
			}
			else if(newPlanIndex == dropoffOptionIndex){
				newPlan.add(new DriverPlanTask(DriverPlanTaskType.DROPOFF, request.getDemandAgent(), 
						request.getTargetLocation()));
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

	private void computePlanCost(RideSharingOnDemandVehicle vehicle, DriverPlan currentPlan, DriverPlan potentialPlan, 
			OnDemandRequest request) {
		double costAddition = 0;
		Iterator<DriverPlanTask> planIterator = potentialPlan.iterator();
		
		DriverPlanTask previousTask = null;
		while (planIterator.hasNext()) {
			DriverPlanTask task = planIterator.next();
			if(task.getDemandAgent() == request.getDemandAgent()){
				costAddition += 
						travelTimeProvider.getTravelTime(vehicle, previousTask.getLocation(), task.getLocation());
				if(planIterator.hasNext()){
					DriverPlanTask nextTask = planIterator.next();
				
					costAddition += travelTimeProvider.getTravelTime(vehicle, task.getLocation(), nextTask.getLocation());
					// drop off next to pickup
					if(task.getTaskType() == DriverPlanTaskType.PICKUP
							&& nextTask.getDemandAgent() == request.getDemandAgent()){
						if(planIterator.hasNext()){
							DriverPlanTask nextNextTask = planIterator.next();
							costAddition += 
									travelTimeProvider.getTravelTime(vehicle, nextTask.getLocation(), nextNextTask.getLocation());
							costAddition -= 
									travelTimeProvider.getTravelTime(vehicle, previousTask.getLocation(), nextNextTask.getLocation());
							break;
						}
					}
					else{
						costAddition -=
								travelTimeProvider.getTravelTime(vehicle, previousTask.getLocation(), nextTask.getLocation());
						if(task.getTaskType() == DriverPlanTaskType.DROPOFF){
							break;
						}
						previousTask = nextTask;
					}
				}
			}
			else{
				previousTask = task;
			}
		}
		
		potentialPlan.setPlannedTraveltime(currentPlan.getPlannedTraveltime() + costAddition);
	}

	private void debugFail(OnDemandRequest request) {
		boolean freeVehicle = false;
		double bestCartesianDistance = Double.MAX_VALUE;
		double bestTravelTimne = Double.MAX_VALUE;
		
		for(OnDemandVehicle tVvehicle: vehicleStorage){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVvehicle;
			if(vehicle.hasFreeCapacity()){
				freeVehicle = true;
				
				// cartesian distance check
				double distance = positionUtil.getPosition(vehicle.getPosition())
						.distance(positionUtil.getPosition(request.getPosition()));

				if(distance < bestCartesianDistance){
					bestCartesianDistance = distance;
				}
				
				
				if(distance < maxDistance){
						// real feasibility check 
					// TODO compute from interpolated position
					double travelTime = 
							travelTimeProvider.getTravelTime(vehicle, vehicle.getPosition(), request.getPosition());


					if(travelTime < bestTravelTimne){
						bestTravelTimne = travelTime;
					}
				}
			}
		}	
		if(!freeVehicle){
			System.out.println("Cannot serve request - No free vehicle");
		}
		else if(bestCartesianDistance > maxDistance){
			System.out.println("Cannot serve request - Too big distance: " + bestCartesianDistance + "m (max distance: " + maxDistance + ")");
		}
		else{
			System.out.println("Cannot serve request - Too big traveltime: " + bestTravelTimne);
		}
	}
	
}
