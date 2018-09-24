package cz.cvut.fel.aic.amodsim.config;

import java.lang.String;
import java.util.Map;

public class Roadmaptools {
  public String osmMapFilename;

  public String osmFilterParams;

  public String mapDir;

  public Roadmaptools(Map roadmaptools) {
    this.osmMapFilename = (String) roadmaptools.get("osm_map_filename");
    this.osmFilterParams = (String) roadmaptools.get("osm_filter_params");
    this.mapDir = (String) roadmaptools.get("map_dir");
  }
}
