/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.simod.traveltimecomputation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import cz.cvut.fel.aic.agentpolis.siminfrastructure.time.TimeProvider;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.MovingEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.utils.PositionUtil;
import cz.cvut.fel.aic.simod.OnDemandVehiclesSimulation;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

//import io.jhdf.HdfFile;
//import io.jhdf.api.Dataset;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.LoggerFactory;

/**
 *
 * @author fiedlda1
 */
@Singleton
public class DistanceMatrixTravelTimeProvider extends TravelTimeProvider{
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrixTravelTimeProvider.class);
	
	private final PositionUtil positionUtil;
	
	private final SimodConfig config;
	
	private final int[][] distanceMatrix;
	

	
	
	
	
	@Inject
	public DistanceMatrixTravelTimeProvider(TimeProvider timeProvider, PositionUtil positionUtil, SimodConfig config) {
		super(timeProvider);
		this.positionUtil = positionUtil;
		this.config = config;
		if(config.distanceMatrixFilepath.endsWith(".h5") || config.distanceMatrixFilepath.endsWith(".hdf5")){
			if(!OnDemandVehiclesSimulation.hdf5Available()){
				throw new RuntimeException("HDF5 file specified, but HDF5 library not available");
			}
			distanceMatrix = loadDistanceMatrixHDF5(config.distanceMatrixFilepath);
		}
		else {
			distanceMatrix = loadDistanceMatrixFromCsv(config.distanceMatrixFilepath);
		}
	}

	private int[][] loadDistanceMatrixHDF5(String distanceMatrixFilepath) {
		try {
			Class<?> HDFLoaderClass = Class.forName("cz.cvut.fel.aic.simod.traveltimecomputation.HDFLoader");
			Method method = HDFLoaderClass.getMethod("loadDistanceMatrix", String.class);
			return (int[][]) method.invoke(null, distanceMatrixFilepath);
		} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
//		return HDFLoader.loadDistanceMatrix(distanceMatrixFilepath);
	}

	@Override
	public long getTravelTime(MovingEntity entity, SimulationNode positionA, SimulationNode positionB) {
		int durationInMilliseconds = distanceMatrix[positionA.getIndex()][positionB.getIndex()];
		return durationInMilliseconds;
	}

	private int[][] loadDistanceMatrixFromCsv(String distanceMatrixFilepath) {
		LOGGER.info("Loading distance matrix from: {}", distanceMatrixFilepath);
		try {
			Reader reader
					= new BufferedReader(new InputStreamReader(new FileInputStream(distanceMatrixFilepath), "utf-8"));
			
			CsvParserSettings settings = new CsvParserSettings();
			//settings.getFormat().setLineSeparator("\n");
			settings.detectFormatAutomatically(',', ';','|',' ');

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
			LOGGER.info("Loading finished");
			return dm;
		} 
		catch (FileNotFoundException | UnsupportedEncodingException ex) {
			Logger.getLogger(DistanceMatrixTravelTimeProvider.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	@Override
	public void printCalls() {
		LOGGER.info("Distance matrix was queried {} times.", callCount);
	}
	
}
