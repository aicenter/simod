/*
 */
package com.mycompany.testsim.pathPlanner;

import cz.agents.planningalgorithms.SearchNode;

/**
 *
 * @author F-I-D-O
 */
public class SimpleSearchNode extends SearchNode<SimpleSearchNode>{
	
	private SimpleSearchNode predecessor;
	
	private int arrivalTime;

	
	
	
	public int getArrivalTime() {
		return arrivalTime;
	}

	public SimpleSearchNode getPredecessor() {
		return predecessor;
	}



	
	
	public SimpleSearchNode(int nodeId, SimpleSearchNode predecessor, int arrivalTime) {
		super(nodeId);
		this.predecessor = predecessor;
		this.arrivalTime = arrivalTime;
	}
	
	

	@Override
	public void replaceWithBetter(SimpleSearchNode node) {
		this.predecessor = node.predecessor;
		this.arrivalTime = node.arrivalTime;
	}
	
}
