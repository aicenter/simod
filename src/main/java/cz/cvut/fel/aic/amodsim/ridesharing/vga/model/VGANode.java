package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

public class VGANode {

    private static int currentId = 0;

    private int id;
    private SimulationNode node;
    private TimeWindow window;

    private VGANode(int id, TimeWindow window, SimulationNode node) {
        this.id = id;
        this.node = node;
        this.window = window;
    }

    public static VGANode newInstance(TimeWindow window, SimulationNode node){
        return new VGANode(currentId++, window, node);
    }

    public int getId() { return id; }

    public SimulationNode getSimulationNode() { return node; }

    public TimeWindow getWindow() { return window; }

    public static void resetIds() { currentId = 0; }

}
