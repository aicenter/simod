/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
