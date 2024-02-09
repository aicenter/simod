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
package cz.cvut.fel.aic.simod.ridesharing.vga.pnas;

///*
// * Copyright (C) 2021 Czech Technical University in Prague.
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 2.1 of the License, or (at your option) any later version.
// *
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this library; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
// * MA 02110-1301  USA
// */
//package cz.cvut.fel.aic.amodsim.ridesharing.vga.pnas;
//
//import com.google.inject.Provider;
//import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
//import cz.cvut.fel.aic.alite.common.event.typed.TypedSimulation;
//import cz.cvut.fel.aic.amodsim.config.SimodConfig;
//import cz.cvut.fel.aic.amodsim.ridesharing.PlanCostProvider;
//import cz.cvut.fel.aic.amodsim.ridesharing.model.DefaultPlanComputationRequest;
//import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.VehicleGroupAssignmentSolver;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;
//import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
//import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
//import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
//
///**
// *
// * @author Fido
// */
//public class PNASVehicleGroupAssignmentSolver extends VehicleGroupAssignmentSolver<PNASGroupGenerator>{
//	
//	public PNASVehicleGroupAssignmentSolver(
//			TravelTimeProvider travelTimeProvider, 
//			PlanCostProvider travelCostProvider, 
//			OnDemandVehicleStorage vehicleStorage, 
//			SimodConfig config, 
//			TimeProvider timeProvider, 
//			DefaultPlanComputationRequest.DefaultPlanComputationRequestFactory requestFactory, 
//			TypedSimulation eventProcessor, 
//			GurobiSolver gurobiSolver, 
//			Provider<PNASGroupGenerator> groupGeneratorProvider, 
//			OnDemandvehicleStationStorage onDemandvehicleStationStorage) {
//		super(
//				travelTimeProvider, 
//				travelCostProvider, 
//				vehicleStorage, 
//				config, 
//				timeProvider, 
//				requestFactory, 
//				eventProcessor, 
//				gurobiSolver, 
//				groupGeneratorProvider, 
//				onDemandvehicleStationStorage);
//	}
//	
//}
