/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify.search;

/**
 *
 * @author olga
 */
public interface PathFinder {
    public int[] search(int origin, int destination);
    public int[] search(int origin);
    
    
}
