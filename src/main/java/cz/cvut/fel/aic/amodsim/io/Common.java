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
package cz.cvut.fel.aic.amodsim.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author fido
 */
public class Common {
	public static FileWriter getFileWriter(String path, boolean append) throws IOException{
		File file = new File(path);
		file.getParentFile().mkdirs();
		return new FileWriter(file, append);
	}
	
	public static FileWriter getFileWriter(String path) throws IOException{
		return getFileWriter(path, false);
	}
}
