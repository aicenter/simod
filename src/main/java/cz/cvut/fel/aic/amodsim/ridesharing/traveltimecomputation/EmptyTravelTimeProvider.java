/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

/**
 * used by class MapVisualizer - no computations are done in order to run faster
 * @author travnja5
 */
public class EmptyTravelTimeProvider extends TravelTimeProvider{
    
    @Inject
    public EmptyTravelTimeProvider(TimeProvider timeProvider) {
        super(timeProvider);
    }  
    
    @Override
    public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
        return 0;
    }
    
    
    
}
