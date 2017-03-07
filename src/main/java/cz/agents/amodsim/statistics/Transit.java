/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.statistics;

/**
 *
 * @author fido
 */
public class Transit {
    private final long time;
    
    private final String id;

    public long getTime() {
        return time;
    }

    public String getId() {
        return id;
    }

    
    
    
    public Transit(long time, String id) {
        this.time = time;
        this.id = id;
    }
    
    
}
