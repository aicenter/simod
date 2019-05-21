/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim;

import ninja.fido.config.Configuration;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.io.TripTransform;
import java.io.File;
import java.io.IOException;
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
//		AmodsimConfig config = Configuration.load(new AmodsimConfig());
		
//		try {
//			tripTransform.tripsFromTxtToJson(new File(config.tripsPath), new File(config.mapFilePath), 
//					config.srid, new File(config.preprocessedTrips));
//		} catch (IOException ex) {
//			LOGGER.error(null, ex);
//		}
		
		System.exit(0);
	}
}
