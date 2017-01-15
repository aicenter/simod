package cz.agents.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;

public final class Rebalancing {
  public final Integer maxWaitInQueue;

  public final String loadShapes;

  public final String filePath;

  public final Double vehCoef;

  public final String useSmoothedDemand;

  public final String policyFilePath;

  public final String method;

  public final Integer timestep;

  public final Integer vehicleLimit;

  public final String type;

  public Rebalancing(Integer maxWaitInQueue, String loadShapes, String filePath, Double vehCoef,
      String useSmoothedDemand, String policyFilePath, String method, Integer timestep,
      Integer vehicleLimit, String type) {
    this.maxWaitInQueue = maxWaitInQueue;
    this.loadShapes = loadShapes;
    this.filePath = filePath;
    this.vehCoef = vehCoef;
    this.useSmoothedDemand = useSmoothedDemand;
    this.policyFilePath = policyFilePath;
    this.method = method;
    this.timestep = timestep;
    this.vehicleLimit = vehicleLimit;
    this.type = type;
  }
}
