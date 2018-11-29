/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.cvut.fel.aic.amodsim.ridesharing.taxify.groupgeneration;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author F.I.D.O.
 */
public class Plan {
	
	final LinkedList<Action> actions;
	
	final long startTime;
	
	final long endTime;

	
	
	
	public Plan(LinkedList<Action> actions, long startTime, long endTime) {
		this.actions = actions;
		this.startTime = startTime;
		this.endTime = endTime;
	}

}
