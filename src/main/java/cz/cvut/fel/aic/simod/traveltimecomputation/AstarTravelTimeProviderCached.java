package cz.cvut.fel.aic.simod.traveltimecomputation;

import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;

import java.util.HashMap;

public class AstarTravelTimeProviderCached extends TravelTimeProvider{

	private record Key(long from, long to){};

	private final AstarTravelTimeProvider astarTravelTimeProvider;

	private final HashMap<Key, Long> cache = new HashMap<>();


	@Inject
	public AstarTravelTimeProviderCached(TimeProvider timeProvider, AstarTravelTimeProvider astarTravelTimeProvider) {
		super(timeProvider);
		this.astarTravelTimeProvider = astarTravelTimeProvider;
	}

	@Override
	public synchronized long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		Key key = new Key(positionA.getId(), positionB.getId());
		if(cache.containsKey(key)){
			return cache.get(key);
		}
		long travelTime = astarTravelTimeProvider.getTravelTime(entity, positionA, positionB);
		cache.put(key, travelTime);
		return travelTime;
	}
}
