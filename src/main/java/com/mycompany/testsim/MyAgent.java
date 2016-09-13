/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.google.inject.Injector;
import cz.agents.agentpolis.siminfrastructure.description.DescriptionImpl;
import cz.agents.agentpolis.siminfrastructure.planner.TripPlannerException;
import cz.agents.agentpolis.siminfrastructure.planner.path.ShortestPathPlanner;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.agents.agentpolis.siminfrastructure.planner.trip.TripItem;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trips;
import cz.agents.agentpolis.siminfrastructure.planner.trip.VehicleTrip;
import cz.agents.agentpolis.simmodel.agent.Agent;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.MovementActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.callback.DrivingFinishedActivityCallback;
import cz.agents.agentpolis.simmodel.agent.activity.movement.callback.MovementActivityCallback;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.entity.vehicle.VehicleType;
import cz.agents.agentpolis.simmodel.environment.model.VehiclePositionModel;
import cz.agents.agentpolis.simmodel.environment.model.VehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.action.driving.DriveMovingAction;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.EGraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.HighwayNetwork;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.agentpolis.simmodel.environment.model.entityvelocitymodel.EntityVelocityModel;
import cz.agents.agentpolis.simmodel.environment.model.query.AgentPositionQuery;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.VehicleDataModel;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.VehicleTemplate;
import cz.agents.agentpolis.simmodel.environment.model.vehiclemodel.VehicleTemplateId;
import cz.agents.agentpolis.utils.VelocityConverter;
import cz.agents.basestructures.Node;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author david
 */
public class MyAgent extends Agent implements DrivingFinishedActivityCallback{
	
	private final Injector injector;


	MyAgent(String agentId, MyEntityType agentType, Injector injector) {
		super(agentId, agentType);
		this.injector = injector;
	}

	@Override
	public void born() {
		AgentPositionQuery positionQuery = injector.getInstance(AgentPositionQuery.class);
		VehicleDataModel vehicleDataModel = injector.getInstance(VehicleDataModel.class);
		Set<GraphType> allowedGraphTypes = new HashSet<>();
		allowedGraphTypes.add(EGraphType.HIGHWAY);
		ShortestPathPlanner planner = ShortestPathPlanner.createShortestPathPlanner(injector, allowedGraphTypes);
		
		Integer currentPos = positionQuery.getCurrentPositionByNodeId(this.getId());
		
		Random random = new Random();
		Collection<SimulationNode> possibleNodesCollection = injector.getInstance(HighwayNetwork.class).getNetwork().getAllNodes();
		List<Integer> allNetworkNodes = possibleNodesCollection.stream().map(Node::getId).collect(Collectors.toList());
		
		Integer sourcePos = allNetworkNodes.get(random.nextInt(allNetworkNodes.size()));
		Integer destPos = allNetworkNodes.get(random.nextInt(allNetworkNodes.size()));
//		MovementActivity moveActivity = injector.getInstance(MovementActivity.class);

		DriveVehicleActivity driveActivity = injector.getInstance(DriveVehicleActivity.class);
		
		Vehicle vehicle = new Vehicle("testv", VehicleType.CAR, 5, 5, EGraphType.HIGHWAY);
		
		VehicleTemplate vehicleTemplate = selectVehicleTemplate(vehicleDataModel, VehicleType.CAR, random);
		vehicleDataModel.assignVehicleTemplate(vehicle.getId(), vehicleTemplate.vehicleTemplateId);
		
//		int initialLocation = findNearestNode(driver.driverInitPosition, nearestNodeFinder);

		double velocityOfVehicle = VelocityConverter.kmph2mps(15);
		
		injector.getInstance(VehicleStorage.class).addEntity(vehicle);
		injector.getInstance(VehiclePositionModel.class).setNewEntityPosition(vehicle.getId(), sourcePos);
		injector.getInstance(EntityVelocityModel.class).addEntityMaxVelocity(vehicle.getId(), velocityOfVehicle);
		
		try {
			Trips trips = planner.findTrip(vehicle.getId(), sourcePos, destPos);
			driveActivity.drive(getId(), vehicle, (Trip<TripItem>) trips.getAndRemoveFirstTrip(), this);
			
//		DriveMovingAction action = new DriveMovingAction(drivenAction, vehiclePlanNotifyAction, 0, vehicleId, 0, vehicleBeforePlanNotifyAction);
		
//		LinkedList<TripItem> path = new LinkedList<>();
//		path.add(new TripItem(currentPos));
//		path.add(new TripItem(currentPos));
//
//		Trip<TripItem> trip = new VehicleTrip(path, graphType, vehicleId);
		} catch (TripPlannerException ex) {
			Logger.getLogger(MyAgent.class.getName()).log(Level.SEVERE, null, ex);
		}

		

//		moveActivity.move(getId(), this, movingAction, trip);
		
	}

	@Override
	public DescriptionImpl getDescription() {
		return new DescriptionImpl();
	}

	@Override
	public void finishedDriving() {
		
	}
	
	private VehicleTemplate selectVehicleTemplate(VehicleDataModel vehicleDataModel, VehicleType vehicleType,
                                                  Random random) {

        TreeMap<Double, VehicleTemplateId> dist = vehicleDataModel.getDistributionForVehicleType(vehicleType);
        Double key = dist.ceilingKey(random.nextDouble());
        VehicleTemplateId vehicleTemplateId = dist.get(key);

        return vehicleDataModel.getVehicleTemplate(vehicleTemplateId);

    }
}
