from pandas.io.formats.format import return_docstring

from amodsim.init import config, roadmaptools_config

import json
import matplotlib
import roadmaptools.inout

from enum import Enum
from matplotlib import cm

from scripts.printer import print_info
from amodsim.json_cache import load_json_file

CRITICAL_DENSITY = config.critical_density



WINDOW_LENGTH = config.analysis.chosen_window_end - config.analysis.chosen_window_start + 1
WINDOW_START = config.analysis.chosen_window_start
WINDOW_END = config.analysis.chosen_window_end

# COLOR_LIST = [NORMAL_COLOR, COLOR_1, COLOR_2, COLOR_3, COLOR_4, COLOR_5, CONGESTED_COLOR]


color_map = cm.get_cmap('gist_heat')


def load_all_edges_load_history(filepath = config.amodsim.statistics.all_edges_load_history_file_path):
	print_info("loading edge load history from: " + filepath)

	# json_file = open(filepath, 'r')
	# loads = json.loads(json_file.read())
	loads = json.load(open(filepath, 'r'))

	# loads = load_json_file(filepath)

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


def load_edges():
	print_info("loading edges")
	modifier = "-simplified" if config.amodsim.simplify_graph else ""
	# json_file = open(config.amodsim.edges_file_path + modifier + ".json", 'r')
	# return json.loads(json_file.read())
	return roadmaptools.inout.load_json(config.amodsim.edges_file_path + modifier + ".json")

def load_edge_pairs():
	print_info("loading edge pairs")
	modifier = "-simplified" if config.amodsim.simplify_graph else ""
	# jsonFile = open(config.amodsim.edge_pairs_file_path + modifier + ".json", 'r')
	# return json.loads(jsonFile.read())
	return roadmaptools.inout.load_json(config.amodsim.edge_pairs_file_path + modifier + ".json")


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


def load_edges_mapped_by_id():
	edges = load_edges()
	edges_mapped_by_id = {}
	for edge in edges:
		edges_mapped_by_id[edge["id"]] = edge

	return edges_mapped_by_id


def get_normalized_load(load, length, lane_count):
	return load / length / lane_count


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


class VehiclePhase(Enum):
	DRIVING_TO_TARGET_LOCATION = (0, "blue", "///", "demand_trips")
	DRIVING_TO_START_LOCATION = (1, "green", "+++", "pickup_trips")
	DRIVING_TO_STATION = (2, "black", "\\\\\\", "drop off trips")
	REBALANCING = (3, "red", "***", "rebalancing trips")

	def __init__(self, index, color, pattern, label):
		self.color = color
		self.index = index
		self.pattern = pattern
		self.label = label


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