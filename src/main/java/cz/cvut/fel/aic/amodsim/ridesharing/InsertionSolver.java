package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlan;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTask;
import cz.cvut.fel.aic.amodsim.ridesharing.plan.DriverPlanTaskType;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;


/**
 *
 * @author F.I.D.O.
 */
public class InsertionSolver extends DARPSolver{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(InsertionSolver.class);
	private static final int INFO_PERIOD = 10000;
    private final PositionUtil positionUtil;
    private final AmodsimConfig config;
	private final double maxDistance;
	private final double maxDistanceSquared;
	private final int maxDelayTime;
	private final TimeProvider timeProvider;
	private long callCount = 0;
	private long totalTime = 0;
	private long iterationTime = 0;
	private long canServeRequestCallCount = 0;
	private long vehiclePlanningAllCallCount = 0;
    
    private final long minDistanceSquared;
    private final long maxRouteTime;
            
        //counters
    int noFreeVehicleCount = 0;
    int tooBigDistanceCount = 0;
    int tooLongToStartCount = 0;
    int someOtherProblemCount = 0;
    int lessThanPickupRadius = 0;

	
	
	@Inject
	public InsertionSolver(TravelTimeProvider travelTimeProvider, TravelCostProvider travelCostProvider, 
			OnDemandVehicleStorage vehicleStorage, PositionUtil positionUtil,
			AmodsimConfig config, TimeProvider timeProvider) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider);
        this.positionUtil = positionUtil;
        this.config = config;
        this.timeProvider = timeProvider;
    	maxDistance = (double) config.amodsim.ridesharing.maxWaitTime 
				* config.amodsim.ridesharing.maxSpeedEstimation / 3600 * 1000;
        maxDistanceSquared = maxDistance * maxDistance;
        maxDelayTime = config.amodsim.ridesharing.maxWaitTime  * 1000;
        
        //move to config
        minDistanceSquared = 50*50; 
        maxRouteTime = 14400000;
	}
    
    private double getDistanceSquared(SimulationNode node1, SimulationNode node2){
        double dist_x = node2.getLatitudeProjected() - node1.getLatitudeProjected();
        double dist_y = node2.getLongitudeProjected() - node1.getLongitudeProjected();
        return dist_x * dist_x + dist_y * dist_y;
    }

	@Override
	public Map<RideSharingOnDemandVehicle, DriverPlan> solve(List<OnDemandRequest> requests) {
        callCount++;
        long startTime = System.nanoTime();
		
        Map<RideSharingOnDemandVehicle, DriverPlan> planMap = new HashMap<>();
		double minCostIncrement = Double.MAX_VALUE;
        DriverPlan bestPlan = null;
        RideSharingOnDemandVehicle servingVehicle = null;
        OnDemandRequest request = requests.get(0);
		
        long iterationStartTime = System.nanoTime();
	    for(AgentPolisEntity tVehicle: vehicleStorage.getEntitiesForIteration()){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVehicle;
            if(canServeRequest(vehicle, request)){
                vehiclePlanningAllCallCount++;
                PlanData newPlanData = getOptimalPlan(vehicle, request);
                if(newPlanData != null && newPlanData.increment < minCostIncrement){
                    minCostIncrement = newPlanData.increment;
                    bestPlan = newPlanData.plan;
                    servingVehicle = vehicle;
                }
            }
        }
		
        iterationTime += System.nanoTime() - iterationStartTime;
        if(bestPlan != null){
            planMap.put(servingVehicle, bestPlan);
			// compute scheduled pickup delay
            DriverPlanTask previousTask = null;
            long currentDelay = 0;
            for(DriverPlanTask task: bestPlan){
                if(previousTask !=  null){
                    currentDelay += travelTimeProvider.getTravelTime(servingVehicle, previousTask.getLocation(), 
                    task.getLocation());
                }
				if(task.demandAgent == request.getDemandAgent()){
                    request.getDemandAgent().setScheduledPickupDelay(currentDelay);
                    
                    break;
                }
				previousTask = task;
            }

        }else{
            debugFail(request);
		}
		totalTime += System.nanoTime() - startTime;
        if(callCount % InsertionSolver.INFO_PERIOD == 0){
            System.out.println(getCurrentStatisics());
            
        }
        
        return planMap;
	}
	
	private boolean canServeRequest(RideSharingOnDemandVehicle vehicle, OnDemandRequest request){
        canServeRequestCallCount++;
		
        // do not mess with rebalancing
        if(vehicle.getState() == OnDemandVehicleState.REBALANCING){
            return false;
        }

		//node identity
        if(vehicle.getPosition() == request.getPosition()){
            return true;
        }

        double distanceSquared = getDistanceSquared(vehicle.getPosition(), request.getPosition());
        //pick_up radius
        if(distanceSquared <= minDistanceSquared){
            return true;
        }
        if(distanceSquared > maxDistanceSquared){
            return false;
        }
		
		// real feasibility check 
		// TODO compute from interpolated position
        
        //max_waiting_time
        if(travelTimeProvider.getTravelTime(vehicle.getPosition(), request.getPosition()) > maxDelayTime){
            return false;
        }
		//boolean canServe = travelTimeProvider.getTravelTime(vehicle, vehicle.getPosition(), request.getPosition()) 
		//		< maxDelayTime;
        
               
		return true;
		//return canServe;
	}

	private PlanData getOptimalPlan(RideSharingOnDemandVehicle vehicle, OnDemandRequest request) {
		long minTimeFromProvider = (long) travelTimeProvider.getTravelTime(
				vehicle, request.getDemandAgent().getPosition(), request.getTargetLocation());
//		request.getDemandAgent().setMinDemandServiceDuration(minTime);
		
		double minCostIncrement = Double.MAX_VALUE;
		DriverPlan bestPlan = null;
		DriverPlan currentPlan = vehicle.getCurrentPlan();
		int freeCapacity = vehicle.getFreeCapacity();
		
		for(int pickupOptionIndex = 1; pickupOptionIndex <= currentPlan.getLength(); pickupOptionIndex++){
            // continue if the vehicle is full
			if(freeCapacity == 0){
				continue;
			}
			
			for(int dropoffOptionIndex = pickupOptionIndex + 1; dropoffOptionIndex <= currentPlan.getLength() + 1; 
					dropoffOptionIndex++){
				DriverPlan potentialPlan = insertIntoPlan(currentPlan, pickupOptionIndex, dropoffOptionIndex, request,
						vehicle, minTimeFromProvider);
				if(potentialPlan != null){
//					computePlanCost(vehicle, currentPlan, potentialPlan, request);
					double costIncrement = potentialPlan.totalTime - currentPlan.totalTime;
					if(costIncrement < minCostIncrement){
						minCostIncrement = costIncrement;
						bestPlan = potentialPlan;
					}
				}
			}
			
			// change free capacity for next index
			if(pickupOptionIndex < currentPlan.getLength()){
				if(currentPlan.plan.get(pickupOptionIndex).getTaskType() == DriverPlanTaskType.PICKUP){
					freeCapacity--;
				}else{
					freeCapacity++;
				}
			}
		}
		return new PlanData(bestPlan, minCostIncrement);
	}

	private DriverPlan insertIntoPlan(DriverPlan currentPlan, int pickupOptionIndex, int dropoffOptionIndex, 
			OnDemandRequest request, RideSharingOnDemandVehicle vehicle, long minTimeFromProvider) {
		List<DriverPlanTask> newPlan = new LinkedList<>();
		
		Iterator<DriverPlanTask> oldPlanIterator = currentPlan.iterator();
		DriverPlanTask previousTask = null;

		Set<DemandAgent> unfinishedDemands = new HashSet<>(currentPlan.getDemands());
		unfinishedDemands.add(request.getDemandAgent());
		long planTravelTime = 0;
		int freeCapacity = vehicle.getFreeCapacity();
		
		for(int newPlanIndex = 0; newPlanIndex <= currentPlan.getLength() + 1; newPlanIndex++){
			DemandAgent finishedDemand = null;
			
			/* get new task */
			DriverPlanTask newTask = null;
			if(newPlanIndex == pickupOptionIndex){
				newTask = new DriverPlanTask(DriverPlanTaskType.PICKUP, request.getDemandAgent(), 
						request.getDemandAgent().getPosition());
			}else if(newPlanIndex == dropoffOptionIndex){
				newTask = new DriverPlanTask(DriverPlanTaskType.DROPOFF, request.getDemandAgent(), 
						request.getTargetLocation());
				finishedDemand = request.getDemandAgent();
			}else{
				newTask = oldPlanIterator.next();
				if(newTask.getTaskType() == DriverPlanTaskType.DROPOFF){
					finishedDemand = newTask.getDemandAgent();
				}
			}
			
			/* chceck capacity */
			if(newTask.getTaskType() == DriverPlanTaskType.DROPOFF){
				freeCapacity++;
			}else if(newTask.getTaskType() == DriverPlanTaskType.PICKUP){
				if(freeCapacity == 0){
					return null;
				}
				freeCapacity--;
			}
					
			/* check max time for all demands */
			if(previousTask != null){
				planTravelTime += travelTimeProvider.getTravelTime(vehicle, previousTask.getLocation(), 
							newTask.getLocation());
			}
			// min estimated simulation time to finish this task
			long curentTaskTime = timeProvider.getCurrentSimTime() + planTravelTime;
			for(DemandAgent demandAgent: unfinishedDemands){
				long minServiceDuration = curentTaskTime - demandAgent.getDemandTime();
				if(minServiceDuration - minTimeFromProvider > maxDelayTime){
					return null;
				}
			}
			
			// do not check time for finished demands
			if(finishedDemand != null){
				unfinishedDemands.remove(finishedDemand);
			}
			
			newPlan.add(newTask);
			previousTask = newTask;
		}
		
		return new DriverPlan(newPlan, planTravelTime);
	}

	// TODO passenger time constraints
	private boolean planFeasible(DriverPlan potentialPlan, RideSharingOnDemandVehicle vehicle) {
		int occupancy = vehicle.getOnBoardCount();
		int capacity = vehicle.getVehicle().getCapacity();
        long planTravelTime = potentialPlan.totalTime;
		if(planTravelTime > maxRouteTime){
            LOGGER.info("Route is too long");
            return false;
            }
        
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
        //potentialPlan.plan = list of tasks
        //potentialPlan.getLength() = number of tasks in the plan
        
//        List<DemandAgent> pickUpTasks = new ArrayList<>();
//        LOGGER.info("Checking max_ride_value for passengers");
//        for(DriverPlanTask pickUpTask: potentialPlan){
//			if(pickUpTask.getTaskType() == DriverPlanTaskType.PICKUP){
//                
//                DemandAgent currentClient = pickUpTask.getDemandAgent();
//                for(DriverPlanTask dropOffTask: potentialPlan){
//                    if(dropOffTask.getTaskType() == DriverPlanTaskType.DROPOFF){
//                        if(dropOffTask.getDemandAgent() == currentClient){
//                            continue;
//                            //
//                            
//                        }
//                    }
//                }
//			}
//        }

        
        
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
					}else{
						costAddition -=
								travelTimeProvider.getTravelTime(vehicle, previousTask.getLocation(), nextTask.getLocation());
						if(task.getTaskType() == DriverPlanTaskType.DROPOFF){
							break;
						}
						previousTask = nextTask;
					}
				}
			}else{
				previousTask = task;
			}
		}
	}

	private void debugFail(OnDemandRequest request) {
		boolean freeVehicle = false;
		double bestCartesianDistance = Double.MAX_VALUE;
		double bestTravelTime = Double.MAX_VALUE;
	
		for(OnDemandVehicle tVehicle: vehicleStorage){
			RideSharingOnDemandVehicle vehicle = (RideSharingOnDemandVehicle) tVehicle;
			if(vehicle.hasFreeCapacity()){
				freeVehicle = true;
				// cartesian distance check
				double distance = positionUtil.getPosition(vehicle.getPosition())
						.distance(positionUtil.getPosition(request.getPosition()));
    			bestCartesianDistance = distance < bestCartesianDistance ? distance : bestCartesianDistance;
				
				if(distance < maxDistance){
					// real feasibility check 
					// TODO compute from interpolated position
					double travelTime = 
							travelTimeProvider.getTravelTime(vehicle, vehicle.getPosition(), request.getPosition());

					bestTravelTime = travelTime < bestTravelTime ? travelTime : bestTravelTime;
    			}
            }
        }//for
			      
        
		String requestId = request.getDemandAgent().getId();
		if(!freeVehicle){
             noFreeVehicleCount++;
			System.out.println("Request " + requestId + ": Cannot serve request - No free vehicle ("
                +noFreeVehicleCount+").");
		}
		else if(bestCartesianDistance > maxDistance){
            tooBigDistanceCount++;
			System.out.println("Request " + requestId + ": Cannot serve request - Too big distance: " 
                + bestCartesianDistance + "m (max distance: " + maxDistance + ") (" + tooBigDistanceCount+").");
		}
		else if(bestTravelTime > maxDelayTime){
            tooLongToStartCount++;
			System.out.println("Request " + requestId 
                + ": Cannot serve request - Too big traveltime to start location: " 
                + bestTravelTime+"("+tooLongToStartCount+").");
		}
		else{
            someOtherProblemCount++;
			System.out.println("Request " + requestId +
                ": Cannot serve request - Some other problem - all nearby vehicle plans infeasible?"+
                "("+someOtherProblemCount+").");
		}
	}

	private String readableTime(long nanoTime) {
		long milisTotal = (long) nanoTime / 1000000;
		long millis = milisTotal % 1000;
		long second = (milisTotal / 1000) % 60;
		long minute = (milisTotal / (1000 * 60)) % 60;
		long hour = (milisTotal / (1000 * 60 * 60)) % 24;
		
		return String.format(" (%02d:%02d:%02d:%d)", hour, minute, second, millis);
	}
    
    public String getCurrentStatisics(){
        StringBuilder sb = new StringBuilder();
        sb.append("Insetrion Heuristic Stats: \n");
        sb.append("Real time: ").append(System.nanoTime()).append(readableTime(System.nanoTime())).append("\n");
        sb.append("Simulation time: ").append(timeProvider.getCurrentSimTime())
                 .append(readableTime(timeProvider.getCurrentSimTime() * 1000000)).append("\n");
        sb.append("Call count: ").append(callCount).append("\n");
        sb.append("Total time: ").append(totalTime).append(readableTime(totalTime)).append("\n");
        sb.append("Iteration time: ").append(iterationTime).append(readableTime(iterationTime)).append("\n");
        sb.append("Can serve call count: ").append(canServeRequestCallCount).append("\n");
        sb.append("Vehicle planning call count: ").append(vehiclePlanningAllCallCount).append("\n");
        sb.append("Traveltime call count: ").append(((EuclideanTravelTimeProvider) travelTimeProvider).getCallCount()).append("\n");
        return sb.toString();
    }

    @Override
    public Map<RideSharingOnDemandVehicle, DriverPlan> solve() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
	
	private class PlanData{
		final DriverPlan plan;
		final double increment;

	public PlanData(DriverPlan plan, double increment) {
			this.plan = plan;
			this.increment = increment;
		}
		
	}
	
}