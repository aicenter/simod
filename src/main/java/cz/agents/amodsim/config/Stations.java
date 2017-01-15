package cz.agents.amodsim.config;

import java.lang.Integer;
import java.lang.String;

public final class Stations {
  public final String demandFilePath;

  public final String distanceMatrixPath;

  public final Integer regions;

  public final Integer timestamps;

  public final String distanceCalculatorPath;

  public final String dir;

  public final String centroidsFilePath;

  public final String stationsFilePath;

  public final String distanceMatrixOutputPath;

  public final String smoothedDemandFilePath;

  public Stations(String demandFilePath, String distanceMatrixPath, Integer regions,
      Integer timestamps, String distanceCalculatorPath, String dir, String centroidsFilePath,
      String stationsFilePath, String distanceMatrixOutputPath, String smoothedDemandFilePath) {
    this.demandFilePath = demandFilePath;
    this.distanceMatrixPath = distanceMatrixPath;
    this.regions = regions;
    this.timestamps = timestamps;
    this.distanceCalculatorPath = distanceCalculatorPath;
    this.dir = dir;
    this.centroidsFilePath = centroidsFilePath;
    this.stationsFilePath = stationsFilePath;
    this.distanceMatrixOutputPath = distanceMatrixOutputPath;
    this.smoothedDemandFilePath = smoothedDemandFilePath;
  }
}
