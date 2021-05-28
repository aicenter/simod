package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Ridesharing {
  public String method;

  public Double maximumRelativeDiscomfort;

  public Integer vehicleCapacity;

  public Vga vga;

  public Integer batchPeriod;

  public InsertionHeuristic insertionHeuristic;

  public String discomfortConstraint;

  public Integer parcelMaxProlongationInSeconds;

  public Integer maxProlongationInSeconds;

  public Double weightParameter;

  public Double parcelMaximumRelativeDiscomfort;

  public Boolean on;

  public Integer trunkCapacity;

  public Ridesharing(Map ridesharing) {
    this.method = (String) ridesharing.get("method");
    this.maximumRelativeDiscomfort = (Double) ridesharing.get("maximum_relative_discomfort");
    this.vehicleCapacity = (Integer) ridesharing.get("vehicle_capacity");
    this.vga = new Vga((Map) ridesharing.get("vga"));
    this.batchPeriod = (Integer) ridesharing.get("batch_period");
    this.insertionHeuristic = new InsertionHeuristic((Map) ridesharing.get("insertion_heuristic"));
    this.discomfortConstraint = (String) ridesharing.get("discomfort_constraint");
    this.parcelMaxProlongationInSeconds = (Integer) ridesharing.get("parcel_max_prolongation_in_seconds");
    this.maxProlongationInSeconds = (Integer) ridesharing.get("max_prolongation_in_seconds");
    this.weightParameter = (Double) ridesharing.get("weight_parameter");
    this.parcelMaximumRelativeDiscomfort = (Double) ridesharing.get("parcel_maximum_relative_discomfort");
    this.on = (Boolean) ridesharing.get("on");
    this.trunkCapacity = (Integer) ridesharing.get("trunk_capacity");
  }
}
