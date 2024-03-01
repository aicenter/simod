package cz.cvut.fel.aic.simod.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.TransportableEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.vehicle.SlotType;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.entity.vehicle.SpecializedTransportVehicle;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
		ObjectMapper objectMapper = new ObjectMapper();

		File vehicleFile = new File(config.vehiclesFilePath);

		try {
			JsonNode vehicles = objectMapper.readTree(vehicleFile);
			for (JsonNode vehicleNode : vehicles) {
				if (config.heterogeneousVehicles) {
					int vehicleId = vehicleNode.get("id").asInt();
					String onDemandVehicelId = String.format("%d", vehicleId);
					OnDemandVehicleStation station = onDemandVehicleStationStorage.getEntityById(vehicleNode.get(
						"station_index").asText());

					// slot parsing
					HashMap<SlotType, Integer> slots = new HashMap<>();
					for (JsonNode slotNode : vehicleNode.get("slots")) {
						slots.put(SlotType.valueOf(slotNode.get("type").asText()), slotNode.get("count").asInt());
					}

					SpecializedTransportVehicle<TransportableEntity> vehicle = new SpecializedTransportVehicle<>(
						String.format("%s - vehicle", onDemandVehicelId),
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						EGraphType.HIGHWAY,
						station.getPosition(),
						agentpolisConfig.maxVehicleSpeedInMeters,
						slots
					);

					OnDemandVehicle vehicleAgent = onDemandVehicleFactory.create(
						onDemandVehicelId,
						station.getPosition(),
						vehicle
					);
					station.parkVehicle(vehicleAgent);
					vehicleAgent.setParkedIn(station);
					onDemandVehicleStorage.addEntity(vehicleAgent);
				} else {
					throw new RuntimeException("Homogeneous vehicles are not supported for vehicle file yet");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
