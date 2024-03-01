package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationFilter {
	private final List<SlotConfiguration> validConfigurations;

	private final boolean[] remainingConfigurations;

	private final HashMap<SlotType, Integer> usedSlots;

	public ConfigurationFilter(List<SlotConfiguration> validConfigurations) {
		this.validConfigurations = validConfigurations;

		this.remainingConfigurations = new boolean[validConfigurations.size()];
		Arrays.fill(remainingConfigurations, true);

		usedSlots = new HashMap<>();
		for(SlotType slotType: SlotType.values()){
			usedSlots.put(slotType, 0);
		}
	}

	public boolean canTransport(TransportableEntity entity){
		for(var configuration: validConfigurations){
			if(configuration.canTransport(entity)){
				return true;
			}
		}
		return false;
	}

	public boolean hasCapacityFor(TransportableEntity entity){
		for(int i = 0; i < validConfigurations.size(); i++){
			if(remainingConfigurations[i]){
				var configuration = validConfigurations.get(i);
				if(configuration.canTransport(entity)){
					var slotCount = configuration.getSlotCount(SlotType.getRequiredSlotType(entity));
					if(slotCount > usedSlots.get(SlotType.getRequiredSlotType(entity))){
						return true;
					}
				}
			}
		}
		return false;
	}

	public void pickUp(TransportableEntity entity){
		if(!hasCapacityFor(entity)){
			throw new IllegalArgumentException("Cannot transport entity");
		}
		var slotType = SlotType.getRequiredSlotType(entity);
		usedSlots.put(slotType, usedSlots.get(slotType) + 1);
		for(int i = 0; i < validConfigurations.size(); i++){
			if(remainingConfigurations[i]){
				var configuration = validConfigurations.get(i);
				if(configuration.canTransport(entity)){
					var maxSlotCount = configuration.getSlotCount(slotType);
					if(maxSlotCount < usedSlots.get(slotType)){
						remainingConfigurations[i] = false;
					}
				}
			}
		}
	}

	public void dropOff(TransportableEntity entity){
		var slotType = SlotType.getRequiredSlotType(entity);
		usedSlots.put(slotType, usedSlots.get(slotType) - 1);
		for(int i = 0; i < validConfigurations.size(); i++){
			if(!remainingConfigurations[i]){
				var configuration = validConfigurations.get(i);
				if(configuration.canTransport(entity)){
					boolean unblocked = true;
					for(Map.Entry<SlotType, Integer> entry: configuration.getSlots().entrySet()){
						if(usedSlots.get(entry.getKey()) > entry.getValue()){
							unblocked = false;
							break;
						}
					}

					if(unblocked){
						remainingConfigurations[i] = true;
					}
				}
			}
		}
	}


}
