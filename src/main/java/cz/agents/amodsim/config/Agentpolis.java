package cz.agents.amodsim.config;

import java.lang.String;

public final class Agentpolis {
  public final String preprocessedTrips;

  public final String tripsPath;

  public final String preprocessorPath;

  public Agentpolis(String preprocessedTrips, String tripsPath, String preprocessorPath) {
    this.preprocessedTrips = preprocessedTrips;
    this.tripsPath = tripsPath;
    this.preprocessorPath = preprocessorPath;
  }
}
