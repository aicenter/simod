/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import java.util.LinkedHashSet;

/**
 *
 * @author matal
 */
public class TestVehicle implements IOptimalPlanVehicle{

    @Override
    public LinkedHashSet<PlanComputationRequest> getRequestsOnBoard() {
        return new LinkedHashSet<>();
    }

    @Override
    public SimulationNode getPosition() {
        return new TestSimulationNode(50027242,14493074);
    }

    @Override
    public int getCapacity() {
        return 5;
    }

    @Override
    public String getId() {
        return "-1"; 
    }
    
}
