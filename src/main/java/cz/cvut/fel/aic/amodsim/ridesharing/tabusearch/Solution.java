/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.tabusearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author olga
 */
public class Solution {
    private static int counter = 0;
    private static int maxCars = 10000;
    
    private final int id;
    private double totalValue;
    private double numOfCars;
   
    private List<Route> routes;
    
    private Map<Integer, List<Integer>> carMap;

    public Solution() {
        this.id = counter++;
        totalValue = 0;
        numOfCars = 0;
        routes = new LinkedList<>();
    }
    
    public void addNewRoute(int carId, SearchNode startNode){
        Route newRoute = new Route(carId, startNode);
        routes.add(newRoute);
//        if(carMap)
//        carMap.put(carId, newRoute.id);
    }
    
    public boolean isFeasible(){
        return routes.stream().allMatch((route) -> (route.isFeasible()));
    }
    
    public double getTotalValue(){
        return routes.stream().map(route->route.getTotalValue())
            .mapToDouble(Double::doubleValue).sum();
    }
    
    
    
    
}
