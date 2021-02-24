package cz.cvut.fel.aic.amodsim.config;

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

  public String discomfortConstraint;

  public Double maximumRelativeDiscomfort;

  public Integer maxProlongationInSeconds;

  public Double weightParameter;

  public Integer vehicleCapacity;

  public Boolean on;

  public String travelTimeProvider;

  public Ridesharing(Map ridesharing) {
    this.vga = new Vga((Map) ridesharing.get("vga"));
    this.batchPeriod = (Integer) ridesharing.get("batch_period");
    this.method = (String) ridesharing.get("method");
    this.insertionHeuristic = new InsertionHeuristic((Map) ridesharing.get("insertion_heuristic"));
    this.discomfortConstraint = (String) ridesharing.get("discomfort_constraint");
    this.maximumRelativeDiscomfort = (Double) ridesharing.get("maximum_relative_discomfort");
    this.maxProlongationInSeconds = (Integer) ridesharing.get("max_prolongation_in_seconds");
    this.weightParameter = (Double) ridesharing.get("weight_parameter");
    this.vehicleCapacity = (Integer) ridesharing.get("vehicle_capacity");
    this.on = (Boolean) ridesharing.get("on");
    this.travelTimeProvider = (String) ridesharing.get("travel_time_provider");
  }
}
