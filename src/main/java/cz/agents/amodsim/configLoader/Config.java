/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.configLoader;

import java.util.HashMap;

/**
 *
 * @author fido
 */
public class Config {
    private final HashMap<String,Object> config;

	public HashMap<String, Object> getConfig() {
		return config;
	}
	
	

    public Config(HashMap<String, Object> config) {
        this.config = config;
    }
    
    public <T> T get(String... path){
        HashMap<String,Object> currentObject = config;
        for(int i = 0; i < path.length; i++){
            if(i == path.length - 1){
                return (T) currentObject.get(path[i]);
            }
            else{
                currentObject = (HashMap<String, Object>) currentObject.get(path[i]);
            }
        }
        return null;
    }
}
