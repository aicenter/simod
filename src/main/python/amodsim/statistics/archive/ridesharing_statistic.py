from amodsim.init import config, roadmaptools_config

import numpy as np
import json

from scripts import trip_loader
from scripts.config_loader import cfg as config

milis_start = config.analysis.chosen_window_start / 144 * 24 * 60 * 60 * 1000
milis_end = config.analysis.chosen_window_end / 144 * 24 * 60 * 60 * 1000

json_file = open(config.amodsim.statistics.result_file_path, 'r')
result = json.loads(json_file.read())

trips = trip_loader.trips
departures = np.genfromtxt(config.amodsim.statistics.car_left_station_to_serve_demand_times_file_path, delimiter=',')

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
