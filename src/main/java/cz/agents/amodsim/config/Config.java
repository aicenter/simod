package cz.agents.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;

public final class Config {
  public final Double vehicleSpeedInMeters;

  public final String tripsFilename;

  public final Agentpolis agentpolis;

  public final Shapefiles shapefiles;

  public final Stations stations;

  public final Integer srid;

  public final String mapFilePath;

  public final String amodsimDataDir;

  public final Double tripsMultiplier;

  public final String pythonDataDir;

  public final String tripsFilePath;

  public final Integer tripsLimit;

  public final Db db;

  public final TutmProjectionCentre tutmProjectionCentre;

  public final Rebalancing rebalancing;

  public Config(Double vehicleSpeedInMeters, String tripsFilename, Agentpolis agentpolis,
      Shapefiles shapefiles, Stations stations, Integer srid, String mapFilePath,
      String amodsimDataDir, Double tripsMultiplier, String pythonDataDir, String tripsFilePath,
      Integer tripsLimit, Db db, TutmProjectionCentre tutmProjectionCentre,
      Rebalancing rebalancing) {
    this.vehicleSpeedInMeters = vehicleSpeedInMeters;
    this.tripsFilename = tripsFilename;
    this.agentpolis = agentpolis;
    this.shapefiles = shapefiles;
    this.stations = stations;
    this.srid = srid;
    this.mapFilePath = mapFilePath;
    this.amodsimDataDir = amodsimDataDir;
    this.tripsMultiplier = tripsMultiplier;
    this.pythonDataDir = pythonDataDir;
    this.tripsFilePath = tripsFilePath;
    this.tripsLimit = tripsLimit;
    this.db = db;
    this.tutmProjectionCentre = tutmProjectionCentre;
    this.rebalancing = rebalancing;
  }
}
