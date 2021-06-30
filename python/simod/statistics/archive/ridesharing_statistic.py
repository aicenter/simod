#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of the SiMoD project.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
from simod.init import config, roadmaptools_config

import numpy as np
import json

from scripts import trip_loader
from scripts.config_loader import cfg as config

milis_start = config.analysis.chosen_window_start / 144 * 24 * 60 * 60 * 1000
milis_end = config.analysis.chosen_window_end / 144 * 24 * 60 * 60 * 1000

json_file = open(config.simod.statistics.result_file_path, 'r')
result = json.loads(json_file.read())

trips = trip_loader.trips
departures = np.genfromtxt(config.simod.statistics.car_left_station_to_serve_demand_times_file_path, delimiter=',')

departures_in_window = np.where((departures >= milis_start) & (departures <=milis_end))

trips_in_window = np.where((trips[:,0] >= milis_start) & (trips[:,0] <=milis_end))

# trip_count = len(trips_in_window) * config.trips_multiplier
trip_count = len(trips_in_window)

departure_count = len(departures_in_window)

utilization_in_window = trip_count / departure_count

# utilization = len(trips) * config.trips_multiplier / len(departures)
utilization = len(trips) / len(departures)

print("Average utilization in window: {0}".format(utilization_in_window))

print("Overall average utilization: {0}".format(utilization))
