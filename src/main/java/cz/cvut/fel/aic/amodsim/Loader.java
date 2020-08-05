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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import cz.cvut.fel.aic.agentpolis.simmodel.IdGenerator;
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
	
	private IdGenerator tripIdGenerator;
	
	private ArrayList<TimeTrip<GPSLocation>> trips;

	public ArrayList<TimeTrip<GPSLocation>> getTrips() {
		return trips;
	}
	
	@Inject
	public Loader(IdGenerator tripIdGenerator) {
		this.tripIdGenerator = tripIdGenerator;
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
                                                LOGGER.info("count " + count);

						while (resultSet.next()) {
							trips.add(new TimeTrip(tripIdGenerator.getId(),resultSet.getLong("start_time"), resultSet.getLong("end_time"), 
									getLocationsFromJson(resultSet.getString("path")).toArray()));
						}
					}
				}
			} catch (SQLException ex) {
				LOGGER.error(null,ex);
			}

		} 
		catch (Exception e) {
			LOGGER.error(null,e);
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
