/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import cz.agents.amodsim.io.TripTransform;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author david
 */
public class PreprocesTrips {

    private static File EXPERIMENT_DIR = new File("data/Prague");

//    private static final String OSM_FILE = "prague-filtered-complete.osm";
    
    private static final String OSM_FILE = "prague-latest.osm";  

    private static final String OUTPUT_FILE = "trips.json";

//    private static final String INPUT_FILE = "car-trips-valid.txt";
    private static final String INPUT_FILE = "car-trips-mixed-trips-completed.txt";

    //	private static final int SRID = 6635;
    private static final int SRID = 2065;
	
	public static void main(String[] args) {
        if (args.length >= 1) {
            EXPERIMENT_DIR = new File(args[0]);
        }
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

            tripTransform.tripsFromTxtToJson(new File(EXPERIMENT_DIR, INPUT_FILE), new File(EXPERIMENT_DIR, OSM_FILE), SRID, new File(EXPERIMENT_DIR, OUTPUT_FILE));
        } catch (IOException ex) {
            Logger.getLogger(PreprocesTrips.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
}
