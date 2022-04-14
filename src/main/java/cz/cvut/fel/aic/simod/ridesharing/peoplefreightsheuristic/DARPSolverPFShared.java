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
package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;

import cz.cvut.fel.aic.simod.ridesharing.DARPSolver;
import cz.cvut.fel.aic.simod.ridesharing.PlanCostProvider;
import cz.cvut.fel.aic.simod.ridesharing.RidesharingDispatcher;
//import cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic.RidesharingPFDispatcher;
import cz.cvut.fel.aic.simod.ridesharing.insertionheuristic.DriverPlan;
import cz.cvut.fel.aic.simod.ridesharing.model.DefaultPlanComputationRequest;  //.DefaultPlanComputationRequestFactory;
import cz.cvut.fel.aic.simod.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.simod.statistics.content.RidesharingBatchStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public abstract class DARPSolverPFShared extends DARPSolver {

    protected final OnDemandVehicleStorage vehicleStorage;

    protected final TravelTimeProvider travelTimeProvider;

    protected final PlanCostProvider planCostProvider;

    protected final DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory;

    protected final List<RidesharingBatchStats> ridesharingStats;


    protected RidesharingPFdispatcher ridesharingDispatcher;



    public List<RidesharingBatchStats> getRidesharingStats() {
        return ridesharingStats;
    }


    public void setDispatcher(RidesharingPFdispatcher ridesharingDispatcher){
        this.ridesharingDispatcher = ridesharingDispatcher;
    }





    public DARPSolverPFShared(OnDemandVehicleStorage vehicleStorage, TravelTimeProvider travelTimeProvider,
                              PlanCostProvider travelCostProvider, DefaultPFPlanCompRequest.DefaultPFPlanComputationRequestFactory requestFactory) {
        super(vehicleStorage, travelTimeProvider, travelCostProvider, null);
        this.vehicleStorage = vehicleStorage;
        this.travelTimeProvider = travelTimeProvider;
        this.planCostProvider = travelCostProvider;
        this.requestFactory = requestFactory;
        ridesharingStats = new ArrayList<>();
    }




    public abstract Map<PeopleFreightVehicle, DriverPlan> solve(List<PlanComputationRequest> newPeopleRequests,
                                                                List<PlanComputationRequest> waitingPeopleRequests,
                                                                List<PlanComputationRequest> newFreightRequests,
                                                                List<PlanComputationRequest> waitingFreightRequests);
}

