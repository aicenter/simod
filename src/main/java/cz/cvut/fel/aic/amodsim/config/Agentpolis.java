package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Agentpolis {
  public Boolean showStackedEntities;

  public String mapNodesFilepath;

  public Boolean showVisio;

  public Integer simulationDurationInMillis;

  public String mapEdgesFilepath;

  public Agentpolis(Map agentpolis) {
    this.showStackedEntities = (Boolean) agentpolis.get("show_stacked_entities");
    this.mapNodesFilepath = (String) agentpolis.get("map_nodes_filepath");
    this.showVisio = (Boolean) agentpolis.get("show_visio");
    this.simulationDurationInMillis = (Integer) agentpolis.get("simulation_duration_in_millis");
    this.mapEdgesFilepath = (String) agentpolis.get("map_edges_filepath");
  }
}
