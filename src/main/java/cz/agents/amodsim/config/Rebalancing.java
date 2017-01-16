package cz.agents.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;

public final class Rebalancing {
  public final Integer maxWaitInQueue = 0;

  public final String loadShapes = "true";

  public final String filePath = "/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/prague/stations_40/stations";

  public final Double vehCoef = 1.0;

  public final String useSmoothedDemand = "true";

  public final String policyFilePath = "/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/prague/stations_40/policy.json";

  public final String method = "emd-s";

  public final Integer timestep = 600;

  public final Integer vehicleLimit = 0;

  public final String type = "stations";

  public Rebalancing() {
  }
}
