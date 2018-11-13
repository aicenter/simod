/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

import cz.cvut.fel.aic.amodsim.ridesharing.taxify.TravelTimeProviderTaxify;
import java.io.Serializable;
import org.slf4j.LoggerFactory;

/**
 *
 * @author olga
 */
public class DistanceMatrix implements Serializable {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DistanceMatrix.class); 
    int n;
    int[][] matrix;

    public DistanceMatrix(int n) {
        this.n = n;
        matrix = new int[n][n];
    }
    
    public void update(int node, int[] dist){
        matrix[node] = dist;
    }
    
    public int get(int n1, int n2){
        return matrix[n1][n2];
    }
    
    
    
}
