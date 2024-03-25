/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.ridesharing.vga.planBuilder;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.simod.action.PlanActionDropoff;
import cz.cvut.fel.aic.simod.action.PlanActionPickup;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.action.PlanRequestAction;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.ridesharing.vga.calculations.IOptimalPlanVehicle;
import java.util.*;

/**
 * Vehicle plan with current plan state.
 * @author David Prochazka
 */
public class VGAVehiclePlan {

	private double discomfort;
	
	public final IOptimalPlanVehicle vgaVehicle;
	
	private final Set<PlanComputationRequest> requests;
	private final Set<PlanComputationRequest> waitingRequests;
	private final Set<PlanComputationRequest> onboardRequests;
	private final List<PlanRequestAction> actions;
	
	private final double startTime;
	
	private final TravelTimeProvider travelTimeProvider;
	
	private double endTime;
	
	
	
	
	

	public VGAVehiclePlan(IOptimalPlanVehicle vgaVehicle, Set<PlanComputationRequest> group, double startTime,
			TravelTimeProvider travelTimeProvider){
		this.vgaVehicle = vgaVehicle;
		this.travelTimeProvider = travelTimeProvider;
		this.discomfort = 0;
		this.actions = new ArrayList<>();
		this.requests = new LinkedHashSet<>(group);
		this.waitingRequests = new LinkedHashSet<>();
		this.onboardRequests = new LinkedHashSet<>();
		this.startTime = startTime;
		updateAccordingToRequests();
		endTime = startTime;
	}

	public VGAVehiclePlan(VGAVehiclePlan vehiclePlan){
		this.vgaVehicle = vehiclePlan.vgaVehicle;
		this.discomfort = vehiclePlan.discomfort;
		this.startTime = vehiclePlan.startTime;
		this.endTime = vehiclePlan.endTime;
		this.travelTimeProvider = vehiclePlan.travelTimeProvider;
		this.actions = new ArrayList<>(vehiclePlan.actions);
		this.requests = new LinkedHashSet<>(vehiclePlan.requests);
		this.onboardRequests = new LinkedHashSet<>(vehiclePlan.onboardRequests);
		this.waitingRequests = new LinkedHashSet<>(vehiclePlan.waitingRequests);
	}

	public void add(PlanRequestAction action) {
		recomputeTime(action.getPosition());
		actions.add(action);
		if(action instanceof PlanActionPickup){
			waitingRequests.remove(action.getRequest());
			onboardRequests.add(action.getRequest());
		} else if (action instanceof PlanActionDropoff) {
//			discomfort += getCurrentTime() - action.request.getOriginTime() -
//					MathUtils.getTravelTimeProvider().getExpectedTravelTime(
//							action.getRequest().getFrom(), action.getRequest().getTo()) / 1000.0;
			discomfort += getCurrentTime() - action.request.getMinSimulationTimeSeconds() - action.request.getMinTravelTime();
			onboardRequests.remove(action.getRequest());
		}
	}
	
	private void recomputeTime(SimulationNode position) {
		if(actions.isEmpty()){
			endTime += travelTimeProvider.getTravelTime(vgaVehicle.getRealVehicle(), position) / 1000.0;
		}
		else{
			endTime += travelTimeProvider.getExpectedTravelTime(getCurrentPosition(), position) / 1000.0;
		}
	}

	SimulationNode getCurrentPosition(){
		if(actions.isEmpty()){
			return vgaVehicle.getPosition();
		}

		return actions.get(actions.size() - 1).getPosition();
	}

	public double getDiscomfort() { return discomfort; }

	public double getCurrentTime() {
		  return endTime;
	}

	public boolean vehicleHasFreeCapacity(){
		return onboardRequests.size() < vgaVehicle.getCapacity();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(PlanRequestAction action : actions){
			sb.append(action.toString());
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof VGAVehiclePlan)) return false;

		return ((VGAVehiclePlan) obj).vgaVehicle == this.vgaVehicle && obj.toString().equals(this.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public IOptimalPlanVehicle getVehicle() { return vgaVehicle; }

	public Set<PlanComputationRequest> getRequests() { return requests; }

	public Set<PlanComputationRequest> getWaitingRequests() { return waitingRequests; }

	public Set<PlanComputationRequest> getOnboardRequests() { return onboardRequests; }

	public List<PlanRequestAction> getActions() { return actions; }

	private void updateAccordingToRequests() {
		for(PlanComputationRequest request: requests){
			if(request.isOnboard()){
				onboardRequests.add(request);
				// mazbe check here if the request match the vehicle?
			}
			else{
				waitingRequests.add(request);
			}
		}
	}

}
