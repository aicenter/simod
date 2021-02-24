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

public class Vga {
  public Boolean logPlanComputationalTime;

  public Integer maxGroupSize;

  public Integer solverTimeLimit;

  public Integer groupGenerationTimeLimit;

  public Boolean exportGroupData;

  public String groupGeneratorLogFilepath;

  public Vga(Map vga) {
    this.logPlanComputationalTime = (Boolean) vga.get("log_plan_computational_time");
    this.maxGroupSize = (Integer) vga.get("max_group_size");
    this.solverTimeLimit = (Integer) vga.get("solver_time_limit");
    this.groupGenerationTimeLimit = (Integer) vga.get("group_generation_time_limit");
    this.exportGroupData = (Boolean) vga.get("export_group_data");
    this.groupGeneratorLogFilepath = (String) vga.get("group_generator_log_filepath");
  }
}
