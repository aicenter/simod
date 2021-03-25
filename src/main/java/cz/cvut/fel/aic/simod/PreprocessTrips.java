/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.simod;

import org.slf4j.LoggerFactory;

/**
 *
 * @author david
 */
public class PreprocessTrips {
	
		private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PreprocessTrips.class);
	
	public static void main(String[] args) {
		
			//		Loader loader = new Loader();
//		loader.loadSCKData();
//		List<TimeTrip<GPSLocation>> gpsTrips = loader.getTrips();

//		TripTransform tripTransform = new TripTransform();
	
//		List<TimeTrip<Long>> osmNodeTrips = tripTransform.gpsTripsToOsmNodeTrips(gpsTrips, new File(OSM_FILE_PATH), SRID);
//		try {
//			TripTransform.tripsToJson(osmNodeTrips, new File(OUTPUT_FILE_PATH));
//		} catch (IOException ex) {
//					  LOGGER.error(null, ex);
//		}
//		SimodConfig config = Configuration.load(new SimodConfig());
		
//		try {
//			tripTransform.tripsFromTxtToJson(new File(config.tripsPath), new File(config.mapFilePath), 
//					config.srid, new File(config.preprocessedTrips));
//		} catch (IOException ex) {
//			LOGGER.error(null, ex);
//		}
		
		System.exit(0);
	}
}
