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
package cz.cvut.fel.aic.simod.init;

import com.google.inject.Inject;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.networks.NodesMappedByIndex;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.simod.entity.OnDemandVehicleStation.OnDemandVehicleStationFactory;
import cz.cvut.fel.aic.simod.traveltimecomputation.DistanceMatrixTravelTimeProvider;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

/**
 * @author david
 */
public class StationsInitializer {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrixTravelTimeProvider.class);

	private final OnDemandVehicleStation.OnDemandVehicleStationFactory onDemandVehicleStationFactory;

	private final NodesMappedByIndex nodesMappedByIndex;

	private final SimodConfig config;


	@Inject
	public StationsInitializer(OnDemandVehicleStationFactory onDemandVehicleStationFactory, NodesMappedByIndex
			nodesMappedByIndex, SimodConfig config) {
		this.onDemandVehicleStationFactory = onDemandVehicleStationFactory;
		this.nodesMappedByIndex = nodesMappedByIndex;
		this.config = config;
	}


	public void loadStations() {
		List<String[]> stationRows = loadStationRows(config.stationPositionFilepath);
		LOGGER.info("{} Stations indexes loaded from {}", stationRows.size(), config.stationPositionFilepath);

		int counter = 0;
		int discarded = 0;
		int i = 0;        // temp for-cycle iteration variable
		for (String[] row : stationRows) {
			int index = Integer.parseInt(row[0]);
			SimulationNode node = nodesMappedByIndex.getNodeByIndex(index);

			int cars_limit = 4;		// TODO toto je docasne nastaveni pro debugging
			int stations_limit = 10;

			if (node == null) {
				LOGGER.info("Station at node with index {} discarded as it is not in the Agentpolis road graph", index);
				discarded++;
			}

			else {
//				int initCount = Integer.parseInt(row[1]) + 100;        // TODO: odkomentovat puvodni kod
//				if (initCount < 500) {
//					initCount += 100;
//				}
				int initCount = cars_limit;
				createStation(node, initCount, counter++);
			}

			if (++i >= stations_limit) {
				break;
			}
		}
		LOGGER.info("{} Stations Discarded", discarded);
	}

	private List<String[]> loadStationRows(String filepath) {
		LOGGER.info("Loading station positions from: {}", filepath);

		try {
			Reader reader
					= new BufferedReader(new InputStreamReader(new FileInputStream(filepath), "utf-8"));
			CsvParserSettings settings = new CsvParserSettings();

			settings.getFormat().setLineSeparator("\r\n");

			//turning off features enabled by default
			settings.setIgnoreLeadingWhitespaces(false);
			settings.setIgnoreTrailingWhitespaces(false);
			settings.setSkipEmptyLines(false);
			settings.setColumnReorderingEnabled(false);

			CsvParser parser = new CsvParser(settings);

			Iterator<String[]> it = parser.iterate(reader).iterator();

			String[] row;

			// first row processing
			List<String[]> stationRows = new ArrayList<>();
			while (it.hasNext()) {
				row = it.next();
				stationRows.add(row);
			}
			parser.stopParsing();
			return stationRows;
		}
		catch (FileNotFoundException | UnsupportedEncodingException ex) {
			Logger.getLogger(DistanceMatrixTravelTimeProvider.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private void createStation(SimulationNode position, int vehicleCount, int id) {
		onDemandVehicleStationFactory.create(Integer.toString(id), position, vehicleCount);
	}
}
