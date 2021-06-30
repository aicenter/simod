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
from pandas.io.formats.format import return_docstring

from amodsim.init import config

import json
import matplotlib
import roadmaptools.inout

from enum import Enum
from matplotlib import cm

from roadmaptools.printer import print_info
from amodsim.json_cache import load_json_file

CRITICAL_DENSITY = config.critical_density



WINDOW_LENGTH = config.analysis.chosen_window_end - config.analysis.chosen_window_start + 1
WINDOW_START = config.analysis.chosen_window_start
WINDOW_END = config.analysis.chosen_window_end

# COLOR_LIST = [NORMAL_COLOR, COLOR_1, COLOR_2, COLOR_3, COLOR_4, COLOR_5, CONGESTED_COLOR]


color_map = cm.get_cmap('gist_heat')


def load_all_edges_load_history(filepath: str):
	print_info("Loading edge load history from: " + filepath)

	loads = json.load(open(filepath, 'r'))

	# for type_name in loads:
	# 	type = loads[type_name]
	# 	for timestep in type:
	# 		for edge_name in timestep:
	# 			timestep[edge_name] *= config.trips_multiplier
	return loads


def get_total_load_sum(filepath):
	loads = load_all_edges_load_history(filepath)
	load = loads["ALL"]

	print_info("counting total load from: {0} ({1} timesteps)".format(filepath, len(load)))
	total_load = 0
	total_load_in_window = 0
	for type_name in loads:
		type = loads[type_name]
		for index, timestep in enumerate(type):
			for edge_name in timestep:
				total_load += timestep[edge_name]
				if index >= WINDOW_START and index <= WINDOW_END:
					total_load_in_window += timestep[edge_name]

	return total_load, total_load_in_window


# def load_all_edges_load_history():
#     print_info("loading edge load history")
#     json_file = open(config.agentpolis.statistics.all_edges_load_history_file_path, 'r')
#     loads = json.loads(json_file.read())
#     for type_name in loads:
#         type = loads[type_name]
#         for timestep in type:
#             for edge_name in timestep:
#                 timestep[edge_name] *= config.analysis.trips_multiplier
#     return loads


def get_normalized_load(load, length, lane_count):
	return load / (length / 100) / lane_count


def get_color_from_load(load, length, lane_count):
	normalized_load = get_normalized_load(load, length, lane_count)
	return get_color_from_normalized_load(normalized_load)


def get_color_from_normalized_load(load):
	return TrafficDensityLevel.get_by_density(load).color
	# if load > CRITICAL_DENSITY:
	#     return CONGESTED_COLOR
	# elif load > CRITICAL_DENSITY * 0.75:
	#     return COLOR_5
	# elif load > CRITICAL_DENSITY * 0.5:
	#     return COLOR_4
	# elif load > CRITICAL_DENSITY * 0.25:
	#     return COLOR_3
	# elif load > CRITICAL_DENSITY * 0.1:
	#     return COLOR_2
	# elif load > CRITICAL_DENSITY * 0.05:
	#     return COLOR_1
	# else:
	#     return NORMAL_COLOR


def color_from_map(value):
	color_np_float = color_map(value)
	return tuple([float(np_value) for np_value in color_np_float])


class TrafficDensityLevel(Enum):
	# CONGESTED = (100, "red")
	# ALMOST_CONGESTED = (1, "orangered")
	# HALF_CONGESTED = (0.75, "darkorange")
	# FREQUENT = (0.5, "lightsalmon")
	# MILD = (0.25, "navajowhite")
	# INFREQUENT = (0.1, "lemonchiffon")
	# FREE = (0.05, "lightgrey")
	CONGESTED = (100000, color_from_map(0.0))
	ALMOST_CONGESTED = (1, color_from_map(0.17))
	HALF_CONGESTED = (0.75, color_from_map(0.34))
	FREQUENT = (0.5, color_from_map(0.51))
	MILD = (0.25, color_from_map(0.68))
	INFREQUENT = (0.1, color_from_map(0.85))
	FREE = (0.05, matplotlib.colors.colorConverter.to_rgba("gainsboro"))


	def __init__(self, max_level, color):
		self.color = color
		self.max_level = max_level



	@staticmethod
	def get_by_density(density):
		level = density / CRITICAL_DENSITY
		for traffic_level in reversed(TrafficDensityLevel):
			if traffic_level.max_level > level:
				return traffic_level

	def get_max_density(self):
		return self.max_level * CRITICAL_DENSITY