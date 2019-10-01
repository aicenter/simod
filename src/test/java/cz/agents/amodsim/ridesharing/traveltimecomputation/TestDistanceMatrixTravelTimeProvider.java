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
package cz.agents.amodsim.ridesharing.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

/**
 *
 * @author matal
 */
@Singleton
public class TestDistanceMatrixTravelTimeProvider extends TravelTimeProvider{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TestDistanceMatrixTravelTimeProvider.class);

	private final int[][] distanceMatrix;
	
	private long callCount = 0;

	public long getCallCount() {
		return callCount;
	}
	
	
	
	
	@Inject
	public TestDistanceMatrixTravelTimeProvider(TimeProvider timeProvider) {
		super(timeProvider);
                String package_path = "cz/agents/amodsim/ridesharing/traveltimecomputation/";
                String dmPath = getClass().getClassLoader().getResource(package_path + "dm.csv").getPath();
		distanceMatrix = loadDistanceMatrix(dmPath);
	}
	
	

	@Override
	public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		callCount++;		
		int durationInMilliseconds = distanceMatrix[positionA.getIndex()][positionB.getIndex()];
		return durationInMilliseconds;
	}

	private int[][] loadDistanceMatrix(String distanceMatrixFilepath) {
		LOGGER.info("Loading distance matrix from: {}", distanceMatrixFilepath);
		try {
			Reader reader
					= new BufferedReader(new InputStreamReader(new FileInputStream(distanceMatrixFilepath), "utf-8"));
			
			CsvParserSettings settings = new CsvParserSettings();
			settings.setLineSeparatorDetectionEnabled(true);

			//turning off features enabled by default
			settings.setIgnoreLeadingWhitespaces(false);
			settings.setIgnoreTrailingWhitespaces(false);
			settings.setSkipEmptyLines(false);
			settings.setColumnReorderingEnabled(false);
			settings.setMaxColumns(100_000);
			
		
			CsvParser parser = new CsvParser(settings);
			
			Iterator<String[]> it = parser.iterate(reader).iterator();
			
			String[] row;
			
			// first row processing
			row = it.next();
			int size = row.length;
			int[][] dm = new int[size][size];
			ProgressBar pb = new ProgressBar("Loading Distance Matrix", size);
			for(int j = 0; j < size; j++){
				dm[0][j] = Integer.parseInt(row[j]);
			}
			pb.step();
			
			int i = 1;
			while (it.hasNext()) {
				row = it.next();
				for(int j = 0; j < size; j++){
					dm[i][j] = Integer.parseInt(row[j]);
				}
				i++;
				pb.step();
			}
			pb.close();
			parser.stopParsing();
			return dm;
		} 
		catch (FileNotFoundException | UnsupportedEncodingException ex) {
			Logger.getLogger(TestDistanceMatrixTravelTimeProvider.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}
	
}