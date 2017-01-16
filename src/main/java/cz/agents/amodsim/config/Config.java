package cz.agents.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;

public final class Config {
  public final Double vehicleSpeedInMeters = 8.3;

  public final String tripsFilename = "car-trips-loader-test";

  public final Agentpolis agentpolis = new Agentpolis();

  public final Shapefiles shapefiles = new Shapefiles();

  public final Stations stations = new Stations();

  public final Integer srid = 2065;

  public final String mapFilePath = "/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/Prague/prague-latest.osm";

  public final String amodsimDataDir = "/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/Prague/";

  public final Double tripsMultiplier = 3.433;

  public final String pythonDataDir = "/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/prague/";

  public final String tripsFilePath = "/home/fido/AIC data/Shared/amod-data/agentpolis-experiment/prague/car-trips-loader-test";

  public final Integer tripsLimit = 1000;

  public final Db db = new Db();

  public final TutmProjectionCentre tutmProjectionCentre = new TutmProjectionCentre();

  public final Rebalancing rebalancing = new Rebalancing();

  public Config() {
  }
}
