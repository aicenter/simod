/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.mycompany.testsim.io.TimeTrip;
import com.mycompany.testsim.io.TripTransform;
import cz.agents.basestructures.GPSLocation;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author david
 */
public class PrepareSCKData {
	
	private static final File OSM_FILE = new File("data/Prague/prague-filtered-complete.osm");
	
	private static final File OUTPUT_FILE = new File("data/Prague/trips.json");
    
    private static final File INPUT_FILE = new File("data/Prague/car-trips.txt");
	
//	private static final int SRID = 6635;
	
	private static final int SRID = 2065;
	
	public static void main(String[] args) {
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

            tripTransform.tripsFromTxtToJson(INPUT_FILE, OSM_FILE, SRID, OUTPUT_FILE);
        } catch (IOException ex) {
            Logger.getLogger(PrepareSCKData.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
}
