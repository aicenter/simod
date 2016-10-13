/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import com.mycompany.testsim.entity.DemandAgent;
import java.util.ArrayList;

/**
 *
 * @author fido
 */
public class DemandData {
    public ArrayList<Long> locations;
    
    public DemandAgent demandAgent;

    public DemandData(ArrayList<Long> locList, DemandAgent demandAgent) {
        this.locations = locList;
        this.demandAgent = demandAgent;
    }

    
}
