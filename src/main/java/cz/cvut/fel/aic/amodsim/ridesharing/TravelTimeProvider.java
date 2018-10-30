/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.geographtools.GPSLocation;

/**
 *
 * @author fiedlda1
 */
public interface TravelTimeProvider {
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB);
    public double getTravelTime(SimulationNode positionA, SimulationNode positionB);
    public double getTravelTime(Integer startId, Integer targetId);
   // public double computeBestLength(TimeTripWithValue<GPSLocation> start, SimulationNode target);
    //public double computeBestLength(SimulationNode start, TimeTripWithValue<GPSLocation> target) ;
    //public double computeBestLength(SimulationNode start, SimulationNode target) ;
    public double computeBestLength(TimeTripWithValue<GPSLocation> trip);
    //public double computeBestLength(TimeTripWithValue<GPSLocation> start, TimeTripWithValue<GPSLocation> target); 
    public double computeBestLength(int[] startNodes, int[] endNodes);
}
