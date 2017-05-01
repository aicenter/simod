/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import ninja.fido.config.Configuration;
import cz.agents.amodsim.config.Config;
import cz.agents.amodsim.io.TripTransform;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author david
 */
public class PreprocessTrips {
	
	public static void main(String[] args) {
        
            //		Loader loader = new Loader();
//		loader.loadSCKData();
//		List<TimeTrip<GPSLocation>> gpsTrips = loader.getTrips();

        TripTransform tripTransform = new TripTransform();
    
//		List<TimeTrip<Long>> osmNodeTrips = tripTransform.gpsTripsToOsmNodeTrips(gpsTrips, new File(OSM_FILE_PATH), SRID);
//		try {
//			TripTransform.tripsToJson(osmNodeTrips, new File(OUTPUT_FILE_PATH));
//		} catch (IOException ex) {
//			Logger.getLogger(PrepareSCKData.class.getName()).log(Level.SEVERE, null, ex);
//		}
        Config config = Configuration.load(new Config());
        
        try {
            tripTransform.tripsFromTxtToJson(new File(config.agentpolis.tripsPath), new File(config.mapFilePath), 
                    config.srid, new File(config.agentpolis.preprocessedTrips));
        } catch (IOException ex) {
            Logger.getLogger(PreprocessTrips.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.exit(0);
	}
}
