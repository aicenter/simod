package cz.cvut.fel.aic.simod.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.agent.DemandAgent;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.*;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleInitializer {

	private static final int LENGTH = 4;


	private final SimodConfig config;

	private final AgentpolisConfig agentpolisConfig;

	private final OnDemandVehicleFactorySpec onDemandVehicleFactory;

	private final OnDemandvehicleStationStorage onDemandVehicleStationStorage;

	private final OnDemandVehicleStorage onDemandVehicleStorage;

	@Inject
	public VehicleInitializer(
		SimodConfig config,
		AgentpolisConfig agentpolisConfig,
		OnDemandVehicleFactorySpec onDemandVehicleFactory,
		OnDemandvehicleStationStorage onDemandVehicleStationStorage,
		OnDemandVehicleStorage onDemandVehicleStorage
	) {
		this.config = config;
		this.agentpolisConfig = agentpolisConfig;
		this.onDemandVehicleFactory = onDemandVehicleFactory;
		this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
		this.onDemandVehicleStorage = onDemandVehicleStorage;
	}

	public void run() {
		if(!config.heterogeneousVehicles && !config.reconfigurableVehicles) {
			throw new RuntimeException("Homogeneous vehicles are not supported for vehicle file yet");
		}

		ObjectMapper objectMapper = new ObjectMapper();

		File vehicleFile = new File(config.vehiclesFilePath);

		try {
			JsonNode vehicles = objectMapper.readTree(vehicleFile);
			for (JsonNode vehicleNode : vehicles) {
				int vehicleId = vehicleNode.get("id").asInt();
				String onDemandVehicelId = String.format("%d", vehicleId);
				OnDemandVehicleStation station = onDemandVehicleStationStorage.getEntityById(vehicleNode.get(
					"station_index").asText());

				MoDVehicle vehicle = null;
				List<SlotConfiguration> slotConfigurations = new ArrayList<>();
				for (JsonNode slotConfigurationNode: vehicleNode.get("configurations")) {
					HashMap<SlotType, Integer> slots = new HashMap<>();
					var it = slotConfigurationNode.fields();
					int configurationId = slotConfigurationNode.get("configuration_id").asInt();
					while (it.hasNext()) {
						Map.Entry<String, JsonNode> slotTypeEntry = it.next();
						if (slotTypeEntry.getKey().equals("configuration_id")) {
							continue;
						}
						slots.put(SlotType.valueOf(slotTypeEntry.getKey()), slotTypeEntry.getValue().asInt());
					}
					slotConfigurations.add(new SlotConfiguration(Integer.toString(configurationId), slots));
				}

				if (config.reconfigurableVehicles) {
					vehicle = new ReconfigurableVehicle(
						String.format("%s - vehicle", onDemandVehicelId),
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						EGraphType.HIGHWAY,
						station.getPosition(),
						agentpolisConfig.maxVehicleSpeedInMeters,
						slotConfigurations
					);
				}
				else {
					// slot parsing
					HashMap<SlotType, Integer> slots = new HashMap<>();
					for (JsonNode slotNode : vehicleNode.get("slots")) {
						slots.put(SlotType.valueOf(slotNode.get("type").asText()), slotNode.get("count").asInt());
					}

					vehicle = new SpecializedTransportVehicle(
						String.format("%s - vehicle", onDemandVehicelId),
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						EGraphType.HIGHWAY,
						station.getPosition(),
						agentpolisConfig.maxVehicleSpeedInMeters,
						slots
					);
				}

				OnDemandVehicle vehicleAgent = onDemandVehicleFactory.create(
					onDemandVehicelId,
					station.getPosition(),
					vehicle
				);
				station.parkVehicle(vehicleAgent);
				vehicleAgent.setParkedIn(station);
				onDemandVehicleStorage.addEntity(vehicleAgent);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
