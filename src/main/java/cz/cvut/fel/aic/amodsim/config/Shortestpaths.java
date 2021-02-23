package cz.cvut.fel.aic.amodsim.config;

import java.util.Map;

public class Shortestpaths {
  public String shortestpathsDataDir;

  public String chFileName;

  public String mappingFileName;

  public String tnrFileName;

  public String tnrFilePath;

  public String tnrafFileName;

  public String mappingFilePath;

  public String chFilePath;

  public String tnrafFilePath;

  public Shortestpaths(Map shortestpaths) {
    this.shortestpathsDataDir = (String) shortestpaths.get("shortestpaths_data_dir");
    this.chFileName = (String) shortestpaths.get("ch_file_name");
    this.mappingFileName = (String) shortestpaths.get("mapping_file_name");
    this.tnrFileName = (String) shortestpaths.get("tnr_file_name");
    this.tnrFilePath = (String) shortestpaths.get("tnr_file_path");
    this.tnrafFileName = (String) shortestpaths.get("tnraf_file_name");
    this.mappingFilePath = (String) shortestpaths.get("mapping_file_path");
    this.chFilePath = (String) shortestpaths.get("ch_file_path");
    this.tnrafFilePath = (String) shortestpaths.get("tnraf_file_path");
  }
}
