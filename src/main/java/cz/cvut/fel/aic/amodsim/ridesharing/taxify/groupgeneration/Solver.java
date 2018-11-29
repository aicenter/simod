///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;
//
//import com.google.inject.Inject;
//import cz.cvut.fel.aic.agentpolis.CollectionUtil;
//import gurobi.GRB;
//import gurobi.GRBEnv;
//import gurobi.GRBException;
//import gurobi.GRBLinExpr;
//import gurobi.GRBModel;
//import gurobi.GRBVar;
//import java.util.HashMap;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * This solver should choose the best groups such as each request is in exactly one group
// * @author F.I.D.O.
// */
//public class Solver {
//	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Solver.class);
//
//
////	private final PlanCostComputation planCostComputation;
//
//	private GRBEnv env;
//
//
//	int iteration;
//
//
//
//
//	@Inject
//    public Solver() {
////		this.planCostComputation = planCostComputation;
//		env = null;
//		try {
//			env = new GRBEnv("mip.log");
//		} catch (GRBException ex) {
//			Logger.getLogger(Solver.class.getName()).log(Level.SEVERE, null, ex);
//		}
//		iteration = 1;
//	}
//
//	public List<GroupPlan> getOptimalGroups(List<GroupPlan> feasiblePlans, List<Request> requests) {
//
//		try {
//			// solver init
//			GRBModel model = new GRBModel(env);
//			GRBLinExpr objetive = new GRBLinExpr();
//
//
//			// map for solution processing
//			Map<GRBVar,GroupPlan> variablePlanMap = new HashMap<>();
//
//			// map for request constraint generation
//			Map<Request,List<GRBVar>> requestVariableMap = new HashMap<>();
//
//			int groupCounter = 0;
//			for (GroupPlan groupPlan : feasiblePlans) {
//
//				// variables
//				String newVarName = String.format("group: %s", groupCounter);
//				GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);
//				variablePlanMap.put(newVar, groupPlan);
//
//				// objective
//				double cost = getPlanCost(groupPlan);
//				objetive.addTerm(cost, newVar);
//
//				// filling map for constraint
//				for (Request request: groupPlan.requests) {
//					CollectionUtil.addToListInMap(requestVariableMap, request, newVar);
//				}
//
//				groupCounter++;
//			}
//
//			// dropping variables generation (y_r)
//			int requestCounter = 0;
//			for (Request request : requests) {
//
//				// variables
//				String newVarName = String.format("droping request %s", requestCounter);
//				GRBVar newVar = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, newVarName);
//
//				// objective
//				objetive.addTerm(10000, newVar);
//
//				// filling map for constraint 2
//				CollectionUtil.addToListInMap(requestVariableMap, request, newVar);
//
//				requestCounter++;
//			}
//
//			// constraint 2 - exactly one plan for each request
//			requestCounter = 0;
//			for (Map.Entry<Request, List<GRBVar>> entry : requestVariableMap.entrySet()) {
//				Request request = entry.getKey();
//				List<GRBVar> planVariableList = entry.getValue();
//
//				GRBLinExpr requestConstraint = new GRBLinExpr();
//
//				for (GRBVar planVariable : planVariableList) {
//					requestConstraint.addTerm(1.0, planVariable);
//				}
//
//				String requestConstraintName = String.format("One plan per request - request %s", requestCounter);
//				model.addConstr(requestConstraint, GRB.EQUAL, 1.0, requestConstraintName);
//
//				requestCounter++;
//			}
//
//
//			model.setObjective(objetive, GRB.MINIMIZE);
//			LOGGER.info("solving start");
//			model.optimize();
//			LOGGER.info("solving finished");
//
//			LOGGER.info("Objective function value: {}", model.get(GRB.DoubleAttr.ObjVal));
//
//			// create output from solution
//			List<GroupPlan> optimalPlans = new LinkedList<>();
//
//			for (Map.Entry<GRBVar, GroupPlan> entry : variablePlanMap.entrySet()) {
//				GRBVar variable = entry.getKey();
//				GroupPlan plan = entry.getValue();
//
//				if(variable.get(GRB.DoubleAttr.X) == 1.0){
//					optimalPlans.add(plan);
//				}
//			}
//
//			LOGGER.info("{} requests will be optimaly served by {} group plans", requests.size(), optimalPlans.size());
//
//			return optimalPlans;
//		} catch (GRBException ex) {
//			Logger.getLogger(Solver.class.getName()).log(Level.SEVERE, null, ex);
//		}
//
//		return null;
//	}
//
//	private double getPlanCost(GroupPlan groupPlan) {
//		double serialServingTime = 0;
//		for(Request request: groupPlan.requests){
//			serialServingTime += request.minTravelTime;
//		}
//
//		return groupPlan.getDuration() - serialServingTime;
//	}
//}
