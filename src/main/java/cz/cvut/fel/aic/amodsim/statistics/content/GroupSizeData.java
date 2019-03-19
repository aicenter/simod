/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics.content;

/**
 *
 * @author david
 */
public class GroupSizeData{

	public GroupSizeData(int totalTime, int groupCount) {
		this.totalTime = totalTime;
		this.groupCount = groupCount;
	}


	public final int totalTime;

	public final int groupCount;
}
