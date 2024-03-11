package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Ridesharing {
  public Vga vga;

  public Integer batchPeriod;

  public String method;

  public InsertionHeuristic insertionHeuristic;

  public Double weightParameter;

  public Integer vehicleCapacity;

  public Boolean on;

  public Ridesharing(Map ridesharing) {
    this.vga = new Vga((Map) ridesharing.get("vga"));
    this.batchPeriod = (Integer) ridesharing.get("batch_period");
    this.method = (String) ridesharing.get("method");
    this.insertionHeuristic = new InsertionHeuristic((Map) ridesharing.get("insertion_heuristic"));
    this.weightParameter = (Double) ridesharing.get("weight_parameter");
    this.vehicleCapacity = (Integer) ridesharing.get("vehicle_capacity");
    this.on = (Boolean) ridesharing.get("on");
  }
}
