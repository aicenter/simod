/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

/**
 *
 * @author matal
 */
public class TestSimulationNode extends SimulationNode {
    
    public TestSimulationNode(int latE6, int lonE6) {
        super(-1, -1, latE6, lonE6, -1, -1, -1);
    }
    
}
