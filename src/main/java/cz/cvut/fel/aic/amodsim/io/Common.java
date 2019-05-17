/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
