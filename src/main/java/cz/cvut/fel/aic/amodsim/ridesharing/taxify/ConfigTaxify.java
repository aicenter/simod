/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.taxify;

/**
 *
 * @author olga
 */
public class ConfigTaxify {
    public final int SRID = 32633;
    
    public int maxCar = 10000; // -1 for infinite number of cars
    public int maxStations = 30;
    public int maxPassengers = 4;
    
    //distances, m
    public int pickupRadius = 50; 
    public int maxRideDistance = 25*1000; //25 km (30 min @ 50 km/h)
        
    // times,  milliseconds
    public int maxWaitTime = 3*60*1000; // 3 minutes
    public int maxRideTime = 30*60*1000; // 30 minutes 
    public int maxChargeMs = 4*60*60*1000; //4 hours
    public int chargingTimeMs = 2*60*60*1000; //2 hours
    public int hkSigma = 15*60*1000; // 15 minutes
    public int timeBuffer = 2*1000; //2 seconds
    public int totalDuration = 48*60*60*1000; // 48 hours
    
    // speed in m/s
    public double speed = 50/3.6; //50km/h = 13.89 m/s
    public String startTime = "2022-03-01 00:00:00";
    public String startYM = startTime.substring(0, 8);
    // paths
    //TODO CHANGE THIS FOR YOUR MACHINE
    public String dir = "../data/";
    public String tripFileName = dir+ "robotex2.csv";
    public String depoFileName = dir + "robotex-depos.csv";
    public String matrixFileName = dir + "tallin_dist.bin";

//    public int optimizeDepoLocations = 0; //default locations
    public int optimizeDepoLocations = 1; //by demand
//    public int optimizeDepoLocations = 2; //by nodes
}
