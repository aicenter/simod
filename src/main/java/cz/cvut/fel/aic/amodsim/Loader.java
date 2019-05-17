/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author david
 */
public class Loader {
	
		private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Loader.class);
	
//	private Connection connection;
	
	private ArrayList<TimeTrip<GPSLocation>> trips;

	public ArrayList<TimeTrip<GPSLocation>> getTrips() {
		return trips;
	}
	
	
	
	
	public void loadSCKData(){
		try {
			
			String SQL = 
					"SELECT trip_id, start_time, end_time, type, ST_AsGeoJSON(ST_GeomFromEWKT(path)) AS path "
						+ "FROM leg_log WHERE type = 'CAR' AND ST_GeomFromEWKT(path) "
							+ "&& ST_MakeEnvelope(14.2714931, 49.9868519, 14.5966197, 50.1519053, 4326)";
 
			trips = new ArrayList<>();
			
			try (Connection connection = 
						DriverManager.getConnection("jdbc:postgresql://localhost/postgis_test", "postgres", "fido7382");
					){
				try(Statement statement = connection.createStatement();){
					statement.executeUpdate("SET search_path TO public, topology, tiger, sck_200k_20151208");
					
					try(ResultSet resultSet = statement.executeQuery(SQL)) {
						resultSet.next();
						int count = resultSet.getInt(1);
						System.out.println("count" + count);

						while (resultSet.next()) {
							trips.add(new TimeTrip(getLocationsFromJson(resultSet.getString("path")), 
									resultSet.getLong("start_time"), resultSet.getLong("end_time")));
						}
					}
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
				System.out.println(ex.getMessage());
			}

		} 
		catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName()+": "+e.getMessage());
			System.exit(0);
		}
		
	}
	
	private ArrayList<GPSLocation> getLocationsFromJson(String locationsString){
		ArrayList<GPSLocation> locations = new ArrayList<>();
		try {
			Map<String, Object> jsonContent  = new ObjectMapper().readValue(locationsString, Map.class);
			ArrayList<ArrayList<Double>> locationsParsed = (ArrayList<ArrayList<Double>>) jsonContent.get("coordinates");
			for (ArrayList<Double> location : locationsParsed) {
				locations.add(new GPSLocation(location.get(1), location.get(0), 0, 0));
			}
		} catch (IOException ex) {
						LOGGER.error(null, ex);
		}

		return locations;
	}
}
