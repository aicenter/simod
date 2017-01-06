/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import org.python.util.PythonInterpreter;

/**
 *
 * @author fido
 */
public class ConfigLoader {
    
    private static File CONFIG_FILE = new File("data/Prague/default.json");
    
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.setProperty("python.path", "../config-0.3.9");
        PythonInterpreter.initialize(System.getProperties(), props, new String[] {""});
        
        PythonInterpreter interpreter = new PythonInterpreter();
        
//        String realpath = new File("./src/main/python/config_loader.py").getAbsolutePath();
        interpreter.execfile("./src/main/python/config_loader.py");
        interpreter.eval("serialize()");
        
        ObjectMapper mapper = new ObjectMapper();
        
        TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

		
		Object config = mapper.readValue(CONFIG_FILE, typeRef);
        return;
    }
}
