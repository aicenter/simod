/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.mycompany.testsim.io.Trip;
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
	
	private static final String OSM_FILE_PATH = "C:\\AIC data\\prague\\prague-filtered.osm";
	
	private static final String OUTPUT_FILE_PATH = "C:\\AIC data\\Prague\\trips.json";
	
//	private static final int SRID = 6635;
	
	private static final int SRID = 2065;
	
	public static void main(String[] args) {
		Loader loader = new Loader();
		loader.loadSCKData();
		List<Trip<GPSLocation>> gpsTrips = loader.getTrips();
		TripTransform tripTransform = new TripTransform();
		List<Trip<Long>> osmNodeTrips = tripTransform.gpsTripsToOsmNodeTrips(gpsTrips, new File(OSM_FILE_PATH), SRID);
		try {
			TripTransform.tripsToJson(osmNodeTrips, new File(OUTPUT_FILE_PATH));
		} catch (IOException ex) {
			Logger.getLogger(PrepareSCKData.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
