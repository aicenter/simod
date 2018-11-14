/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.MoveUtil;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.TransportNetworks;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TimeTripWithValue;
import cz.cvut.fel.aic.amodsim.ridesharing.taxify.TripTaxify;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;


/**
 *
 * @author fiedlda1
 */
@Singleton
public class EuclideanTravelTimeProvider implements TravelTimeProvider{
	private final PositionUtil positionUtil;
	private final AmodsimConfig config;
    private final double travelSpeedEstimatePerSecond;
	private long callCount = 0;
    private final Graph<SimulationNode, SimulationEdge> graph;
    
    
	public long getCallCount() {
		return callCount;
	}
	

	@Inject
	public EuclideanTravelTimeProvider(PositionUtil positionUtil, AmodsimConfig config, TransportNetworks transportNetworks) {
		this.positionUtil = positionUtil;
		this.config = config;
		travelSpeedEstimatePerSecond = config.amodsim.ridesharing.maxSpeedEstimation / 3.6;
        this.graph = transportNetworks.getGraph(EGraphType.HIGHWAY);
	}
	
	
	@Override
	public double getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		callCount++;
		double distance = positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB));
		long traveltime = MoveUtil.computeDuration(travelSpeedEstimatePerSecond, distance);
		return traveltime;
	}
    
    @Override
	public double getTravelTime(SimulationNode positionA, SimulationNode positionB) {
		callCount++;
		double distance = positionUtil.getPosition(positionA).distance(positionUtil.getPosition(positionB));
		long traveltime = MoveUtil.computeDuration(travelSpeedEstimatePerSecond, distance);
		return traveltime;
	}

    @Override
    public int getTravelTimeInMillis(int[] startNodes, int[] endNodes) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getTravelTimeInMillis(TripTaxify<GPSLocation> trip) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getTravelTimeInMillis(Integer startId, Integer targetId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



}
