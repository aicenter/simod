/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.offline.demand;

import com.opencsv.CSVWriter;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.offline.entities.IHPlan;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import java.io.FileWriter;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FilenameUtils;
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
    boolean dbg = false;
    public void buildPaths(){
        
        ProgressBar pb = new ProgressBar("Insertion Heuristic", demand.N);
        for(int trip = 0; trip < demand.N; trip++){
             if (dbg) LOGGER.debug("Trip "+trip + ", num of plans "+plans.size());
            int bestResult = Integer.MAX_VALUE;
            int bestPlanInd = -1;
            IHPlan bestPlan = null;
           
            for(int pInd = 0; pInd < plans.size(); pInd++){
                IHPlan plan = plans.get(pInd);
                if (dbg) LOGGER.debug("Plan "+plan.id+", at position "+pInd);
                IHPlan updatedPlan = tryInsert(plan, trip);
                if (updatedPlan == null){
                     if (dbg)LOGGER.debug("Failed to build plan.");
                    continue;
                }
                int costInc = updatedPlan.getPlanCost() - plan.getPlanCost();
                if (dbg) LOGGER.debug("updated plan "+updatedPlan.id +", cost "+updatedPlan.getPlanCost());
                if (costInc < bestResult){
                    if (dbg) LOGGER.debug("new best plan "+plan.printPlan());
                    bestPlanInd = pInd;
                    bestResult = costInc;
                    bestPlan = updatedPlan;
                }
                
            }
           // pb.step();
            if(bestPlan != null){
                if (dbg) LOGGER.debug("Old plan at position "+bestPlanInd+", id "+plans.get(bestPlanInd).id+", cost"+plans.get(bestPlanInd).getPlanCost());
                IHPlan oldPlan = plans.remove(bestPlanInd);
                IHPlan newPlan = new IHPlan(bestPlanInd, bestPlan);
                if (dbg) LOGGER.debug("old plan "+oldPlan.printPlan());
                    if (dbg) LOGGER.debug("new plan "+newPlan.printPlan());
                if (dbg)LOGGER.debug("New plan at position "+bestPlanInd+", id "+newPlan.id+", cost"+newPlan.getPlanCost());
                 plans.add(bestPlanInd, newPlan);
            } else {
                if (dbg) LOGGER.debug("Adding new plan");
                bestPlan = new IHPlan();
                bestPlan.addFirstTrip(trip, demand.getStartTime(trip) + timeToStart, demand.getBestTime(trip));
                plans.add(bestPlan);
                if (dbg)LOGGER.debug("Adding new plan " + bestPlan.id + ", position " + (plans.size()-1)+"; "+bestPlan.printPlan());
            }
            pb.step();
        }
        pb.close();
        
        int totaTrips = plans.stream().map(p->p.getSize()).mapToInt(Integer::intValue).sum()/2;
        LOGGER.info("Total trip nodes in paths: "+totaTrips);
        int totalCost = plans.stream().mapToInt(p -> p.getPlanCost()/1000).sum();
        
        LOGGER.info("Cars used: "+ plans.size()+", total cost "+(totalCost/3600)+" hours");
        
        //TODO move to statistics
        int[] plansBysize = new int[config.ridesharing.vga.maxGroupSize+1];
        for(int i =0; i < plansBysize.length; i++)         plansBysize[i]=0;
        List<String[]> result = new ArrayList<>();
         result.add(new String[]{ "car_id", "demand_id", "action",  "ept", 
             "action time",  "lpt",  "time-ept", "lpt-time" , "node_id", "lat",  "lon"});
        Set<Integer> seenTrips = new HashSet<>();
        List<double[]> occupancy = new ArrayList<>();

        int tripCounter = 0;
        for (IHPlan plan: plans){
            if(dbg) LOGGER.debug("plan "+plan.id+", size "+plan.getSize() +", n  req "+plan.getRequestsNum()+", cost "+plan.getPlanCost());
            List<Integer> planActions = plan.getActions();
            int cap = 0;
            double[] timeWitnNPassengers = new double[config.ridesharing.vehicleCapacity + 1];
            for(int planInd = 0; planInd < plan.getSize(); planInd++){
               if(dbg)  LOGGER.debug("i=" +planInd+", action "+planActions.get(planInd));
                int action = planActions.get(planInd);
                int actionTime = plan.times.get(planInd);
                if(action >= 0){
                    if(seenTrips.contains(action)){
                        LOGGER.error("duplicate pickup "+ action);
                    }
                    if(planInd == 0){
                        timeWitnNPassengers[cap]+= (timeToStart/1000.0);
                    }else{
                        int travelTime = actionTime - plan.getPreviousActionTime(planInd);
                         timeWitnNPassengers[cap]+= (travelTime/1000.0);
                    }
                    cap++;
                    if(cap > maxCapacity){
                        LOGGER.error("Max capacity " + cap);
                    }
                    tripCounter++;
                    seenTrips.add(action);
                    int ept = demand.getStartTime(action);
                    int lpt = ept+ maxProlongation;
                    if(actionTime > lpt || actionTime < ept){
                    LOGGER.error("pickup "+action+"; "+ ept + " < "
                        + actionTime + " < "+ lpt);  
                    }
                    result.add(new String[]{String.valueOf(plan.id),
                        String.valueOf(action)," Pickup",
                        String.valueOf(Math.round(ept/1000.0)),
                        String.valueOf(Math.round(actionTime/1000.0)),
                        String.valueOf(Math.round(lpt/1000.0)),
                        String.valueOf(Math.round((actionTime-ept)/1000.0)),
                        String.valueOf(Math.round((lpt-actionTime)/1000.0)),
                        String.valueOf(demand.getStartNodeId(action)),
                        String.valueOf(demand.getStartLatitude(action)), 
                        String.valueOf(demand.getStartLongitude(action)) });
                }
//              "car_id", "demand_id", "action",  "ept",  "action time",  "lpt",  "time-ept", "lpt-time" , "node_id", "lat",  "lon"
                else if (action < 0){
                    int travelTime = actionTime - plan.getPreviousActionTime(planInd);
                    timeWitnNPassengers[cap]+= (travelTime/1000.0);
                    cap--;
                    action = -action - 1;
                    if(! seenTrips.contains(action)){
                        LOGGER.error("Dropoff before pickup");
                    }
                    seenTrips.remove(action);
                    int ept = demand.getEndTime(action);
                    int lpt = ept+ maxProlongation;
                    if(actionTime > lpt || actionTime < ept){
                        LOGGER.error("pickup "+action+"; "+ ept + " < "
                            + actionTime + " < "+ lpt);  
                    }
                    result.add(new String[]{String.valueOf(plan.id),
                        String.valueOf(action), "Dropoff", 
                        String.valueOf(Math.round(ept/1000.0)),
                        String.valueOf(Math.round(actionTime/1000.0)),
                        String.valueOf(Math.round(lpt/1000.0)),
                        String.valueOf(Math.round((actionTime-ept)/1000.0)),
                        String.valueOf(Math.round((lpt-actionTime)/1000.0)),
                        String.valueOf(demand.getStartNodeId(action)),
                        String.valueOf(demand.getStartLatitude(action)), 
                        String.valueOf(demand.getStartLongitude(action)) });
                
                }
            }
            occupancy.add(timeWitnNPassengers);
        }
        LOGGER.debug("Trip counter " + tripCounter);
        LOGGER.debug("seen trips size "+ seenTrips.size());
        occupancyResults(occupancy);
        writeCsv(config.amodsimExperimentDir, "eval_result", result);
    }
    
    
    private IHPlan tryInsert(IHPlan plan, int trip){
       if (dbg)  LOGGER.debug("Inserting " +trip +" into "+ plan.printPlan());
        List<Integer> planActions = plan.getActions();
       if (dbg)  LOGGER.debug("plan actions: ");
        if (dbg) for(int action: planActions) System.out.print(action + ", ");
        if (dbg) LOGGER.debug("\n");
        int l = plan.getSize();
        int bestTime = Integer.MAX_VALUE;
        IHPlan bestPlan  = null;
        
        for(int i = 0; i <= l; i++){
//            if(i < (l - 1) && plan.times.get(i+1) > (demand.getStartTime(trip) + maxProlongation)){
//                continue;
//            }
            planActions.add(i, trip);
             
            if (dbg)LOGGER.debug("p ins pos " +i);
             if (dbg)  LOGGER.debug("plan actions: ");
             if (dbg)   for(int action: planActions) System.out.print(action + ", ");
             if (dbg)  LOGGER.debug("\n");
           
           
            for (int j = i + 1; j <= l + 1; j++){
//                if(j < l  && plan.times.get(j+1) > (demand.getEndTime(trip) + maxProlongation)){
//                continue;
//            }
                planActions.add(j, -trip-1);
                
                if (dbg)   LOGGER.debug("d ins pos " +j);
                if (dbg)   LOGGER.debug("plan actions: ");
                if (dbg)   for(int action: planActions) System.out.print(action + ", ");
                if (dbg)    LOGGER.debug("\n");
                
                IHPlan newPlan = makePlan(planActions);
                if(newPlan != null){
                    int newCost = newPlan.getPlanCost();
                    if(newCost < bestTime){
                        bestTime = newCost;
                        bestPlan = newPlan;
                    }
                }
                planActions.remove(j);
            }
            planActions.remove(i);
        }
        return bestPlan;
    }
    
    private IHPlan makePlan(List<Integer> actions){
         if (dbg)LOGGER.debug("TRY make plan from :");
        if (dbg) for(int action: actions) System.out.print(action + ", ");
       if (dbg)  LOGGER.debug("\n");
        IHPlan plan = new IHPlan(-1);
        int firstPickup = actions.get(0);
        int startTime = demand.getStartTime(firstPickup) + timeToStart;
        plan.addAction(firstPickup, startTime, demand.getStartNodeId(firstPickup));
        int cap = 1;
        int time = startTime;
        for(int i = 1; i < actions.size(); i++){
           int nextAction = actions.get(i);
           SimulationNode planLastNode = demand.getNodeById(plan.getLastActionLocation());
           int planLastTime = plan.getLastActionTime();
           //feasibility checks
           
           // pickup
           if (nextAction >= 0){
               //capacity check
               if(cap == maxCapacity){
                   return null;
               }
               int originTime = demand.getStartTime(nextAction);
//               if(time > originTime){
//                   return null;
//               }
               SimulationNode actionNode = demand.getStartNode(nextAction);
               int travelTime = (int) travelTimeProvider.getExpectedTravelTime(planLastNode, actionNode);
               int arrivalTime = planLastTime + travelTime;
               
                //pickup time window
               arrivalTime = arrivalTime > originTime ? arrivalTime : originTime;
               if (cap == 0 && arrivalTime > (originTime + timeToStart)){
                   return null;
               }
               if(arrivalTime > originTime + maxProlongation){
                   return null;
               }
               plan.addAction(nextAction, arrivalTime, actionNode.id);
               cap++;
               time = arrivalTime;
            }   
           else if (nextAction < 0){
               nextAction = -nextAction - 1;
               int plannedArrival = demand.getEndTime(nextAction);
//                if(time > plannedArrival){
//                   return null;
//               }
               SimulationNode actionNode = demand.getEndNode(nextAction);
               int travelTime = (int) travelTimeProvider.getExpectedTravelTime(planLastNode, actionNode);
               int arrivalTime = planLastTime + travelTime;
               
               if(arrivalTime > plannedArrival + maxProlongation){
                   return null;
               }
              plan.addAction((-nextAction-1), arrivalTime, actionNode.id);
              cap--;
           }
        }
        return plan;
    }
    
    private void occupancyResults(List<double[]> occupancy){
        int capacities = config.ridesharing.vehicleCapacity +1;
        int columns = capacities + 1;
        int[] total = new int[columns];
        Arrays.fill(total, 0);
        
        List<String[]> result = new ArrayList<>();
        String[] header = new String[columns];
        header[0] = "car_id";
        for(int i = 1; i < columns; i++){
            header[i] = String.format("onboard_%s", (i-1)); 
        }
        result.add(header);
        for(int carId = 0; carId < occupancy.size(); carId++){
            double car[] = occupancy.get(carId);
            String[] entry = new String[columns];
            entry[0] = String.valueOf(carId);
            for(int count = 0; count < capacities; count++){
                int col = count  + 1;
                total[count]+=car[count];
                entry[col] = String.valueOf(car[count]);
            }
            result.add(entry);
        }
        writeCsv(config.amodsimExperimentDir, "passenger_stats", result);
    }
       
    private void writeCsv(String dir, String name, List<String[]> result){
        Date timeStamp = Calendar.getInstance().getTime();
     
        String filename = makeFilename(dir, name , timeStamp);
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(filename))) {
            csvWriter.writeAll(result);
        }
		catch(Exception ex){
			ex.printStackTrace();
		}
    }
       
       
    private static String makeFilename(String dir, String name, Date timeStamp){
         
        //dir = dir.endsWith("/") ? dir : dir + "/";
        String timeString = new SimpleDateFormat("dd-MM-HH-mm").format(timeStamp);
        name = name + "_"+ timeString + ".csv";
        return FilenameUtils.concat(dir, name);
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









    