/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.DateTimeParser;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.EGraphType;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.HighwayNetwork;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.WKTPrintableCoord;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.SlotType;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author F-I-D-O
 */
@Singleton
public class TripTransform {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TripTransform.class);

//	private PathPlanner pathPlanner;

	private final SimodConfig config;

	private int zeroLenghtTripsCount = 0;

	private int sameStartAndTargetInDataCount = 0;

	private final Graph<SimulationNode, SimulationEdge> highwayGraph;

	private final NearestElementUtils nearestElementUtils;

	private IdGenerator tripIdGenerator;


	@Inject
	public TripTransform(
		SimodConfig config,
		HighwayNetwork highwayNetwork,
		NearestElementUtils nearestElementUtils,
		IdGenerator tripIdGenerator
	) {
		this.highwayGraph = highwayNetwork.getNetwork();
		this.nearestElementUtils = nearestElementUtils;
		this.tripIdGenerator = tripIdGenerator;
		this.config = config;
	}


	public static <T extends WKTPrintableCoord> void tripsToJson(
		List<TimeTrip<T>> trips,
		File outputFile
	) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		mapper.writeValue(outputFile, trips);
	}

	public static <T extends WKTPrintableCoord> List<TimeTrip<T>> jsonToTrips(
		File inputFile,
		Class<T> locationType
	) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();

		return mapper.readValue(inputFile, typeFactory.constructCollectionType(
			List.class, typeFactory.constructParametricType(TimeTrip.class, locationType)));
	}

	public List<TimeTrip<SimulationNode>> loadTripsFromTxt(File inputFile) {
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();
		LOGGER.info("Loading trips from: {}", inputFile);
		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(",");
				GPSLocation startLocation = new GPSLocation(
					Double.parseDouble(parts[1]),
					Double.parseDouble(parts[2]),
					0,
					0
				);
				GPSLocation targetLocation = new GPSLocation(
					Double.parseDouble(parts[3]),
					Double.parseDouble(parts[4]),
					0,
					0
				);

				if (startLocation.equals(targetLocation)) {
					sameStartAndTargetInDataCount++;
				} else {
					SimulationNode startNode = nearestElementUtils.getNearestElement(startLocation, EGraphType.HIGHWAY);
					SimulationNode targetNode
						= nearestElementUtils.getNearestElement(targetLocation, EGraphType.HIGHWAY);

					if (startNode == targetNode) {
						zeroLenghtTripsCount++;
					}
					else {
						if (config.heterogeneousVehicles) {
							SlotType requiredSlotType = SlotType.valueOf(parts[5]);
							trips.add(new TimeTripWithRequirements<>(
								tripIdGenerator.getId(),
								DateTimeParser.parseDateTimeFromUnknownFormat(parts[0]),
								requiredSlotType,
								startNode,
								targetNode
							));
						}
						else {
							trips.add(new TimeTrip<>(
								tripIdGenerator.getId(),
								DateTimeParser.parseDateTimeFromUnknownFormat(parts[0]),
								startNode,
								targetNode
							));
						}
					}
				}
			}
		} catch (IOException ex) {
			LOGGER.error(null, ex);
		}

		LOGGER.info("Number of trips with same source and destination: {}", sameStartAndTargetInDataCount);
		LOGGER.info("{} trips with zero lenght discarded", zeroLenghtTripsCount);

		return trips;
	}
}
