/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.entity.DemandAgent;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanActionPickup;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;

/**
 *
 * @author matal
 */
public class TestRequest implements PlanComputationRequest{
    private int maxPickupTime;
    private int maxDropoffTime;
    private SimulationNode from;
    private SimulationNode to;

    public TestRequest(SimulationNode from, int maxPickupTime, SimulationNode to , int maxDropoffTime) {
        this.maxPickupTime = maxPickupTime;
        this.maxDropoffTime = maxDropoffTime;
        this.from = from;
        this.to = to;
    }
    
    @Override
    public int getMaxPickupTime() {
        return maxPickupTime;
    }

    @Override
    public int getMaxDropoffTime() {
        return maxDropoffTime;
    }

    @Override
    public int getOriginTime() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getMinTravelTime() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SimulationNode getFrom() {
        return from;
    }

    @Override
    public SimulationNode getTo() {
        return to;
    }

    @Override
    public boolean isOnboard() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PlanActionPickup getPickUpAction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PlanActionDropoff getDropOffAction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DemandAgent getDemandAgent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
