package cz.agents.amodsim.config;

import java.lang.String;

public final class Shapefiles {
  public final String border;

  public final String rivers;

  public final String towns;

  public final String motorways;

  public final String roads;

  public final String dir;

  public Shapefiles(String border, String rivers, String towns, String motorways, String roads,
      String dir) {
    this.border = border;
    this.rivers = rivers;
    this.towns = towns;
    this.motorways = motorways;
    this.roads = roads;
    this.dir = dir;
  }
}
