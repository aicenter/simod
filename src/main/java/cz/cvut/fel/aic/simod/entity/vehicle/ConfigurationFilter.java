package cz.cvut.fel.aic.simod.entity.vehicle;

import cz.cvut.fel.aic.simod.DefaultPlanComputationRequest;
import cz.cvut.fel.aic.simod.PlanComputationRequest;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationFilter {
	private final List<SlotConfiguration> validConfigurations;

	private final boolean[] remainingConfigurations;

	private final HashMap<SlotType, Integer> usedSlots;

	public ConfigurationFilter(ReconfigurableVehicle vehicle) {
		this.validConfigurations = vehicle.getValidConfigurations();

		this.remainingConfigurations = new boolean[validConfigurations.size()];
		Arrays.fill(remainingConfigurations, true);

		usedSlots = new HashMap<>();
		for(SlotType slotType: SlotType.values()){
			usedSlots.put(slotType, 0);
		}

		// Count onboard requests towards used slots
		for(DemandAgent demandAgent: vehicle.getTransportedEntities()){
			pickUp(demandAgent.getRequest());
		}
	}

	public boolean canTransport(DefaultPlanComputationRequest request){
		for(var configuration: validConfigurations){
			if(configuration.canTransport(request)){
				return true;
			}
		}
		return false;
	}

	public boolean hasCapacityFor(PlanComputationRequest request){
		for(int i = 0; i < validConfigurations.size(); i++){
			if(remainingConfigurations[i]){
				var configuration = validConfigurations.get(i);
				if(configuration.canTransport(request)){
					var slotCount = configuration.getSlotCount(request.getRequiredSlotType());
					if(slotCount > usedSlots.get(request.getRequiredSlotType())){
						return true;
					}
				}
			}
		}
		return false;
	}

	public void pickUp(PlanComputationRequest request){
		if(!hasCapacityFor(request)){
			throw new IllegalArgumentException("Cannot transport entity");
		}
		var slotType = request.getRequiredSlotType();
		usedSlots.put(slotType, usedSlots.get(slotType) + 1);
		for(int i = 0; i < validConfigurations.size(); i++){
			if(remainingConfigurations[i]){
				var configuration = validConfigurations.get(i);
				if(configuration.canTransport(request)){
					var maxSlotCount = configuration.getSlotCount(slotType);
					if(maxSlotCount < usedSlots.get(slotType)){
						remainingConfigurations[i] = false;
					}
				}
			}
		}
	}

	public void dropOff(PlanComputationRequest request){
		var slotType = request.getRequiredSlotType();
		usedSlots.put(slotType, usedSlots.get(slotType) - 1);
		for(int i = 0; i < validConfigurations.size(); i++){
			if(!remainingConfigurations[i]){
				var configuration = validConfigurations.get(i);
				if(configuration.canTransport(request)){
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
