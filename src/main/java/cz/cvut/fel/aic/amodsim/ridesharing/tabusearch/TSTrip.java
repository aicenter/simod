/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

/**
 *
 * @author olga
 */
public class TSTrip {
    public final int id;
    public final int time;
    public final int start;
    public final int target;
    public final double value;

    public TSTrip(int id, int time, int start, int target, double value) {
        this.id = id;
        this.time = time;
        this.start = start;
        this.target = target;
        this.value = value;
    }
    
    
    
}
