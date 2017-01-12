/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.amodsim.configLoader.Config;
import cz.agents.amodsim.configLoader.ConfigParser;
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
    
    private static final File CONFIG_FILE 
            = new File("/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/Prague/default.cfg");

//    private static File EXPERIMENT_DIR = new File("data/Prague");
//
////    private static final String OSM_FILE = "prague-filtered-complete.osm";
//    
//    private static final String OSM_FILE = "prague-latest.osm";  
//
//    private static final String OUTPUT_FILE = "trips.json";
//
////    private static final String INPUT_FILE = "car-trips-valid.txt";
//    private static final String INPUT_FILE = "car-trips-mixed-trips-completed.txt";
//
//    //	private static final int SRID = 6635;
//    private static final int SRID = 2065;
	
	public static void main(String[] args) {
//        if (args.length >= 1) {
//            EXPERIMENT_DIR = new File(args[0]);
//        }
        try {
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
        Config config = new ConfigParser().parseConfigFile(CONFIG_FILE);

        tripTransform.tripsFromTxtToJson(new File(config.<String>get("agentpolis", "trips_path")), 
                    new File(config.<String>get("map_file_path")), config.get("srid"), 
                    new File(config.<String>get("agentpolis", "preprocessed_trips")));
        } catch (IOException ex) {
            Logger.getLogger(PreprocessTrips.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
}
