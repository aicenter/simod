/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.IHPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Olga Kholkovskaia
 */
public class IHSolution {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(IHSolution.class);

    private final int maxProlongation;
    
    private final int timeToStart;
    
    private final Demand demand;
    
    private final TravelTimeProvider travelTimeProvider;
    
    private final AmodsimConfig config;
    
    private final List<IHPlan> plans;
    
    private final int maxCapacity;
    
//    private final Map<Integer, Map<Integer,Integer>> carPlanTimeMap;

    /**
     *
     * @param demand {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.Demand}
     * @param travelTimeProvider {@link cz.cvut.fel.aic.amodsim.ridesharing.offline.search.TravelTimeProviderTaxify}
     * @param config {@link cz.cvut.fel.aic.amodsim.ridesharing.taxify.ConfigTaxify}
     */
    public IHSolution(Demand demand, TravelTimeProvider travelTimeProvider,  AmodsimConfig config) {
        this.demand = demand;
        this.travelTimeProvider = travelTimeProvider;
        this.config = config;
        maxProlongation = config.ridesharing.maxProlongationInSeconds * 1000;
        timeToStart = config.ridesharing.offline.timeToStart;
        maxCapacity = config.ridesharing.vga.maxGroupSize;
        plans = new LinkedList<>();
    }
           
    /**
     * 
     * @return 
     */
//    public List<IHPlan> getAllPlans(){
//        Collections.sort(plans, Comparator.comparing(IHPlan::getId));
//        return plans;
//    }

    /**
     * Insertion Heuristic 
     * 
     */
    public void buildPaths(){
                
        for(int trip = 0; trip < demand.N; trip++){
            int bestResult = Integer.MAX_VALUE;
            int bestPlanInd = -1;
            IHPlan bestPlan = null;
           
            for(int pInd = 0; pInd < plans.size(); pInd++){
                IHPlan plan = plans.get(pInd);
                IHPlan updatedPlan = tryInsert(plan, trip);
                if (updatedPlan == null){
                    continue;
                }
                int costInc = updatedPlan.getPlanCost() - plan.getPlanCost();
                if (costInc < bestResult){
                    bestPlanInd = pInd;
                    bestResult = costInc;
                    bestPlan = updatedPlan;
                }
                
            }
            if(bestPlan != null){
                plans.remove(bestPlanInd);
                plans.add(bestPlanInd, new IHPlan(bestPlanInd, bestPlan));
            } else {
                bestPlan = new IHPlan();
                bestPlan.addFirstTrip(trip, demand.getStartTime(trip) + timeToStart, demand.getBestTime(trip));
                plans.add(bestPlan);
            }
        }
        
  
        int totaTrips = plans.stream().map(c->c.getSize()).mapToInt(Integer::intValue).sum()/2;
        LOGGER.info("Total trip nodes in paths: "+totaTrips);
        LOGGER.info("Cars used: "+ plans.size());
    }
    
    
    private IHPlan tryInsert(IHPlan plan, int trip){
        
        List<Integer> planActions = plan.getActions();
        int l = plan.getSize();
        int bestTime = Integer.MAX_VALUE;
        IHPlan bestPlan  = null;
        
        for(int i = 0; i <= l; i++){
            planActions.add(i, trip);
            for (int j = i + 1; j <= l + 1; j++){
                planActions.add(j, -trip-1);
                IHPlan newPlan = makePlan(planActions);
                if(newPlan == null){
                    continue;
                }
                int newCost = newPlan.getPlanCost();
                if(newCost < bestTime){
                    bestTime = newCost;
                    bestPlan = newPlan;
                }
                planActions.remove(j);
            }
            planActions.remove(i);
        }
        return bestPlan;
    }
    
    private IHPlan makePlan(List<Integer> actions){
        IHPlan plan = new IHPlan(-1);
        int firstPickup = actions.get(0);
        int startTime = demand.getStartTime(firstPickup) + timeToStart;
        plan.addAction(firstPickup, startTime, demand.getStartNodeId(firstPickup));
        int cap = 1;
     
        for(int i = 1; i < actions.size(); i++){
           int nextAction = actions.get(i);
           SimulationNode planLastNode = demand.getNodeById(plan.getLastActionLocation());
           int planLastTime = plan.getLastActionTime();
           //feasibility checks
           
           // pickup
           if (nextAction > 0){
               //capacity check
               if(cap == maxCapacity){
                   return null;
               }
               SimulationNode actionNode = demand.getStartNode(nextAction);
               int travelTime = (int) travelTimeProvider.getExpectedTravelTime(planLastNode, actionNode);
               int arrivalTime = planLastTime + travelTime;
               int originTime = demand.getStartTime(nextAction);
               //pickup time window
               arrivalTime = arrivalTime > originTime ? arrivalTime : originTime;
               if(arrivalTime > originTime + maxProlongation){
                   return null;
               }
               plan.addAction(nextAction, arrivalTime, actionNode.id);
               cap++;
            }   
           else if (nextAction < 0){
               nextAction = -nextAction - 1;
               SimulationNode actionNode = demand.getEndNode(nextAction);
               int travelTime = (int) travelTimeProvider.getExpectedTravelTime(planLastNode, actionNode);
               int arrivalTime = planLastTime + travelTime;
               int plannedArrival = demand.getEndTime(nextAction);
               if(arrivalTime > plannedArrival + maxProlongation){
                   return null;
               }
              plan.addAction((-nextAction-1), arrivalTime, actionNode.id);
              cap--;
           }
        }
        return plan;
    }
    
    
  }















//    private Car getCar(int trip, int[] depo){
//        Car theCar = null;
//        // check among waiting cars
//        List<Car> sortedCars = cars[W].stream().sorted(Comparator.comparingInt(Car::getLastActionTime))
//            .collect(Collectors.toList());
//        for (Car car: sortedCars){
//            if(canServe(car.getLastDemandNode(), car.getLastActionTime(), trip)){
//                theCar = car;
////                int currentNode = car.getLastDemandNode();
////                SimulationNode startSimNode = demand.getEndNode(currentNode);
////                SimulationNode endSimNode = demand.getStartNode(trip);
////                int timeToTripStart = (int) travelTimeProvider.getExpectedTravelTime(startSimNode, endSimNode);
//                break;
//                }
//        }
//        if(theCar != null){
//            
//            cars[D].add(theCar);
//            cars[W].remove(theCar);
//            return theCar;
//        }
//        //second, search for cars in the nearest Depo
//        int latestPossibleArrival = demand.getStartTime(trip) + maxWaitTime;
////        sortedCars = cars[C].stream().sorted(Comparator.comparingInt(Car::getLastActionTime))
////            .collect(Collectors.toList());
////        for(Car car: sortedCars){
////            SimulationNode startSimNode = demand.getNodeById(-car.getLastDemandNode());
////            SimulationNode endSimNode = demand.getStartNode(trip);
////            int timeFromeDepo = (int) travelTimeProvider.getExpectedTravelTime(startSimNode, endSimNode);
////            if (car.getLastActionTime() + timeFromeDepo + timeBuffer <= latestPossibleArrival){
////                theCar = car;
////                break;
////                }
////            }
////        if(theCar != null){
////            cars[D].add(theCar);
////            cars[C].remove(theCar);
////            return theCar;
////           
////        }
//        if(depo[1] < latestPossibleArrival){
//         
//            theCar = new Car(depo[0]);
//            cars[D].add(theCar);
//            return theCar;
//        }
//        return null;
//    }









    