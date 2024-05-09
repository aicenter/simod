package cz.cvut.fel.aic.simod.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.DateTimeParser;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.alite.common.event.EventProcessor;
import cz.cvut.fel.aic.simod.DemandSimulationEntityType;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleState;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicle;
import cz.cvut.fel.aic.simod.entity.agent.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.simod.entity.vehicle.*;
import cz.cvut.fel.aic.simod.event.OnDemandVehicleEvent;
import cz.cvut.fel.aic.simod.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.simod.storage.OnDemandvehicleStationStorage;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

	private final TimeProvider timeProvider;

	private final EventProcessor eventProcessor;

	private final HighwayNetwork highwayNetwork;




	@Inject
	public VehicleInitializer(
		SimodConfig config,
		AgentpolisConfig agentpolisConfig,
		OnDemandVehicleFactorySpec onDemandVehicleFactory,
		OnDemandvehicleStationStorage onDemandVehicleStationStorage,
		OnDemandVehicleStorage onDemandVehicleStorage,
		TimeProvider timeProvider,
		EventProcessor eventProcessor,
		HighwayNetwork highwayNetwork
	) {
		this.config = config;
		this.agentpolisConfig = agentpolisConfig;
		this.onDemandVehicleFactory = onDemandVehicleFactory;
		this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
		this.onDemandVehicleStorage = onDemandVehicleStorage;
		this.timeProvider = timeProvider;
		this.eventProcessor = eventProcessor;
		this.highwayNetwork = highwayNetwork;
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
				String onDemandVehicleId = String.format("%d", vehicleId);
				OnDemandVehicleStation station = null;
				SimulationNode initialPosition = null;
				if (config.stations.on){
					station = onDemandVehicleStationStorage.getEntityById(vehicleNode.get(
						"station_index").asText());
					initialPosition = station.getPosition();
				}
				else {
					initialPosition = highwayNetwork.getNetwork().getNode(vehicleNode.get("node_index").asInt());
				}

				MoDVehicle vehicle = null;

				if (config.reconfigurableVehicles) {
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

					vehicle = new ReconfigurableVehicle(
						String.format("%s - vehicle", onDemandVehicleId),
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						EGraphType.HIGHWAY,
						initialPosition,
						agentpolisConfig.maxVehicleSpeedInMeters,
						slotConfigurations
					);
				}
				else if(vehicleNode.has("slots")){
					// slot parsing
					HashMap<SlotType, Integer> slots = new HashMap<>();
					for (JsonNode slotNode : vehicleNode.get("slots")) {
						slots.put(SlotType.valueOf(slotNode.get("type").asText()), slotNode.get("count").asInt());
					}

					vehicle = new SpecializedTransportVehicle(
						String.format("%s - vehicle", onDemandVehicleId),
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						EGraphType.HIGHWAY,
						initialPosition,
						agentpolisConfig.maxVehicleSpeedInMeters,
						slots
					);
				}
				else {
					vehicle = new SimpleMoDVehicle(
						String.format("%s - vehicle", onDemandVehicleId),
						DemandSimulationEntityType.VEHICLE,
						LENGTH,
						EGraphType.HIGHWAY,
						initialPosition,
						agentpolisConfig.maxVehicleSpeedInMeters,
						vehicleNode.get("capacity").asInt()
					);
				}

				// parse operation times
				ZonedDateTime operationStart = null;
				if(vehicleNode.has("operation_start")) {
					operationStart = DateTimeParser.parseDateTime(vehicleNode.get("operation_start").asText());
				}
				ZonedDateTime operationEnd = null;
				if(vehicleNode.has("operation_end")) {
					operationEnd = DateTimeParser.parseDateTime(vehicleNode.get("operation_end").asText());
				}

				boolean startsImmediately = true;
				if(operationStart != null){
					ZonedDateTime simulationStart = timeProvider.getCurrentSimDateTime();

					if(operationStart.isBefore(simulationStart)) {
						throw new RuntimeException("Operation start is before simulation start");
					}
					startsImmediately = operationStart.isEqual(simulationStart);
				}

				OnDemandVehicleState onDemandVehicleState = startsImmediately ? OnDemandVehicleState.WAITING : OnDemandVehicleState.NON_ACTIVE;

				OnDemandVehicle vehicleAgent = onDemandVehicleFactory.create(
					onDemandVehicleId,
					initialPosition,
					vehicle,
					operationStart,
					operationEnd,
					onDemandVehicleState
				);

				if(config.stations.on) {
					station.parkVehicle(vehicleAgent);
					vehicleAgent.setParkedIn(station);
				}

				onDemandVehicleStorage.addEntity(vehicleAgent);

				// create start operation event if the vehicle does not start immediately
				if(!startsImmediately) {
					long delay = timeProvider.getSimTimeFromDateTime(operationStart);
					eventProcessor.addEvent(OnDemandVehicleEvent.START_OPERATING, vehicleAgent, null, null, delay);
				}

			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
