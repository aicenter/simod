/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

//        TripTransform tripTransform = new TripTransform();
    
//		List<TimeTrip<Long>> osmNodeTrips = tripTransform.gpsTripsToOsmNodeTrips(gpsTrips, new File(OSM_FILE_PATH), SRID);
//		try {
//			TripTransform.tripsToJson(osmNodeTrips, new File(OUTPUT_FILE_PATH));
//		} catch (IOException ex) {
//                      LOGGER.error(null, ex);
//		}
//        AmodsimConfig config = Configuration.load(new AmodsimConfig());
        
//        try {
//            tripTransform.tripsFromTxtToJson(new File(config.amodsim.tripsPath), new File(config.mapFilePath), 
//                    config.srid, new File(config.amodsim.preprocessedTrips));
//        } catch (IOException ex) {
//            LOGGER.error(null, ex);
//        }
        
        System.exit(0);
	}
}
