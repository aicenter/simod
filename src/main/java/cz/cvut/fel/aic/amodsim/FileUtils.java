/*
 * Copyright (C) 2021 Czech Technical University in Prague.
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Fido
 */
public class FileUtils {
	
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OnDemandVehiclesSimulation.class);
	
	public static void checkFilePathForRead(String path) throws Exception{
		Path filePath = Paths.get(path);
		
		if(!Files.exists(filePath)){
			throw new Exception(String.format("File '%s': does not exist", path));
		}
		
		if(!Files.isReadable(filePath)){
			throw new Exception(String.format("File '%s': you do not have permissons to read the file", path));
		}
	}
	
	public static void checkFilePathForWrite(String path) throws Exception{
		Path filePath = Paths.get(path);
		if(Files.exists(filePath)){
			if(!Files.isWritable(filePath)){
				throw new Exception(String.format("File '%s': you do not have permissons to overwrite the file", path));
//				LOGGER.error("File '%s': you do not have permissons to overwrite the file", path);
//				return false;
			}
		}
		else{
			Path dirPath = filePath.getParent();
			if(!Files.exists(dirPath)){
				throw new Exception(
						String.format("Directory '%s': does not exists", dirPath.toAbsolutePath().toString()));
//				LOGGER.error("Directory '%s': does not exists", dirPath.toAbsolutePath().toString());
//				return false;
			}
			if(!Files.isWritable(dirPath)){
				throw new Exception(String.format("Directory '%s': You do not have write permissons", 
						dirPath.toAbsolutePath().toString()));
//				LOGGER.error("Directory '%s': You do not have write permissons", dirPath.toAbsolutePath().toString());
//				return false;
			}
		}
		
//		return true;
	}
}
