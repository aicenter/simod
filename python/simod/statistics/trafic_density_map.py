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
from amodsim.init import config

import matplotlib.pyplot as plt
import itertools
import numpy as np
import amodsim.statistics.model.traffic_load as traffic_load

from roadmaptools.printer import print_info
from amodsim.statistics.model.traffic_load import TrafficDensityLevel, WINDOW_END as CHOSEN_WINDOW_END, WINDOW_START as CHOSEN_WINDOW_START



SHIFT_DISTANCE = 30


# edges = traffic_load.load_edges()

if __name__ == "__main__":
	edgePairs = traffic_load.load_edge_pairs()
	loads = traffic_load.load_all_edges_load_history()


colorTypes = {}

CRITICAL_DENSITY = config.critical_density


def plot_edges_optimized(pairs, axis, loads, color_func=None):
	if color_func == None:
		color_func = get_level

	for level in TrafficDensityLevel:
		colorType = {}
		colorType["xPairs"] = []
		colorType["yPairs"] = []
		colorType["width"] = 0.5 if level == TrafficDensityLevel.FREE else 1.0
		colorType["opacity"] = 1.0 if level == TrafficDensityLevel.FREE else 1.0
		colorTypes[level] = colorType

	for pair in itertools.islice(pairs, 0, 100000000):
		edge1 = pair["edge1"]
		id1 = str(edge1["id"])
		color1 = color_func(loads, id=id1, length=edge1["length"], lane_count=edge1["laneCount"])

		if not pair["edge2"]:
			add_line([edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]],
					  color1)
		else:
			edge2 = pair["edge2"]
			id2 = str(edge2["id"])
			color2 = color_func(loads, id=id2, length=edge2["length"], lane_count=edge2["laneCount"])
			line1 = compute_shift(
				[[edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]]],
				SHIFT_DISTANCE, 1)
			line2 = compute_shift(
				[[edge2["from"]["lonE6"], edge2["from"]["latE6"]], [edge2["to"]["lonE6"], edge2["to"]["latE6"]]],
				SHIFT_DISTANCE, 1)
			add_line(line1[0], line1[1], color1)
			add_line(line2[0], line2[1], color2)

	for level in reversed(TrafficDensityLevel):
		colorType = colorTypes[level]
		xList, yList = lines_to_list(colorType["xPairs"], colorType["yPairs"])
		axis.plot(xList, yList, linewidth=colorType["width"], color=level.color, alpha=colorType["opacity"])


def add_line(a, b, color):
	colorTypes[color]["xPairs"].append([a[0], b[0]])
	colorTypes[color]["yPairs"].append([a[1], b[1]])


def lines_to_list(xpairs, ypairs):
	xlist = []
	ylist = []
	for xends, yends in zip(xpairs, ypairs):
		xlist.extend(xends)
		xlist.append(None)
		ylist.extend(yends)
		ylist.append(None)

	return xlist, ylist


# def get_color(edge, loads, scalarMap):
#     if str(edge["id"]) in loads[50]:
#         return scalarMap.to_rgba(loads[50][str(edge["id"])])
#     else:
#         return 'black'


def new_congestion_level(loads_all, id, length, lane_count):
	if length == 0:
		return TrafficDensityLevel.FREE
	loads_passanger_trip = loads["DRIVING_TO_TARGET_LOCATION"]
	load_total_passanger_trip = 0
	load_total_all = 0
	current_frame = CHOSEN_WINDOW_START
	while current_frame <= CHOSEN_WINDOW_END:
		if id in loads_passanger_trip[current_frame]:
			load_total_all += loads_all[current_frame][id]
			load_total_passanger_trip += loads_passanger_trip[current_frame][id]
		current_frame += 1

	average_load_all = load_total_all / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)
	average_load_passanger_trip = load_total_passanger_trip / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)
	if traffic_load.get_normalized_load(average_load_all, length, lane_count) > CRITICAL_DENSITY \
		and traffic_load.get_normalized_load(average_load_passanger_trip, length, lane_count) <= CRITICAL_DENSITY:
			return TrafficDensityLevel.CONGESTED
	else:
		return TrafficDensityLevel.FREE


def get_level(loads, id, length, lane_count):
	if length == 0:
		return TrafficDensityLevel.FREE
	load_total = 0
	current_frame = CHOSEN_WINDOW_START
	while current_frame <= CHOSEN_WINDOW_END:
		if id in loads[current_frame]:
			load_total += loads[current_frame][id]
		current_frame += 1

	average_load = load_total / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)

	# return traffic_load.get_color_from_load(load=average_load, length=length, lane_count=lane_count)
	level = TrafficDensityLevel.get_by_density(traffic_load.get_normalized_load(average_load, length, lane_count))
	return level


def compute_shift(line, distance, direction):
	normal_vector = np.array([-(line[0][1] - line[1][1]), line[0][0] - line[1][0]])
	length = np.linalg.norm(normal_vector)
	final_vector = normal_vector / length * distance * direction
	return np.array([line[0] + final_vector, line[1] + final_vector])


def make_edge_pairs(edges):
	# for edge in edges:
	pairs = []
	while edges:
		edge1 = edges.pop(0)
		index = -1
		for index, edge in enumerate(edges):
			if edge["from"] == edge1["to"] and edge["to"] == edge1["from"]:
				break

		if index == -1:
			pairs.append([edge1])
		else:
			pairs.append([edge1, edges.pop(index)])

	return pairs


def location_quals(loc1, loc2):
	return loc1["lonE6"] == loc2["lonE6"] and loc1["latE6"] == loc2["latE6"]


def set_axis_params(axis):
	axis.set_xticklabels([])
	axis.set_yticklabels([])
	axis.tick_params(
		which='both',  # both major and minor ticks are affected
		bottom=False,  # ticks along the bottom edge are off
		top=False,  # ticks along the top edge are off
		labelbottom=False, right=False, left=False, labelleft=False, labelright=False, labeltop=False)


def plot_main_map():
	# "adjustable": 'datalim', "aspect": 1.0 - naprosto nevim proc to takhle funguje - dokumentace == NULL
	fig, axis = \
		plt.subplots(2, 3, sharex=True, sharey=True, subplot_kw={"adjustable": 'box', "aspect": 1.0},
					 figsize=(12, 6))

	np.vectorize(set_axis_params)(axis)

	# axis[0][0].set_xlabel("All")
	# axis[0][1].set_xlabel("To passenger")
	# axis[0][2].set_xlabel("Demanded trip")
	# axis[1][0].set_xlabel("To station")
	# axis[1][1].set_xlabel("Rebalancing")
	# axis[1][2].set_xlabel("New congestion")

	axis[0][0].set_xlabel("a) All")
	axis[0][1].set_xlabel("b) Pickup")
	axis[0][2].set_xlabel("c) Demand")
	axis[1][0].set_xlabel("d) Drop off")
	axis[1][1].set_xlabel("e) Rebalancing")
	axis[1][2].set_xlabel("f) New congestion")

	print_info("plotting all load")
	plot_edges_optimized(pairs, axis[0][0], loads["ALL"])

	print_info("plotting to start location load")
	plot_edges_optimized(pairs, axis[0][1], loads["DRIVING_TO_START_LOCATION"])

	print_info("plotting to target location load")
	plot_edges_optimized(pairs, axis[0][2], loads["DRIVING_TO_TARGET_LOCATION"])

	print_info("plotting to station load")
	plot_edges_optimized(pairs, axis[1][0], loads["DRIVING_TO_STATION"])

	print_info("plotting rebalancing load")
	plot_edges_optimized(pairs, axis[1][1], loads["REBALANCING"])

	print_info("plotting new congestion")
	plot_edges_optimized(pairs, axis[1][2], loads["ALL"], color_func=new_congestion_level)

	# zoom
	plt.axis([14308000, 14578000, 49970000, 50186000])

	plt.savefig(config.images.main_map, bbox_inches='tight', transparent=True)

	plt.show()


def plot_map_in_detail():
	# "adjustable": 'datalim', "aspect": 1.0 - naprosto nevim proc to takhle funguje - dokumentace == NULL
	fig, axis = \
		plt.subplots(1, 1, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim', "aspect": 1.0},
					 figsize=(12, 6))

	plt.tick_params(
		which='both',  # both major and minor ticks are affected
		bottom='off',  # ticks along the bottom edge are off
		top='off',  # ticks along the top edge are off
		labelbottom='off', right='off', left='off', labelleft='off')

	np.vectorize(set_axis_params)(axis)

	axis.set_xlabel("All")

	print_info("plotting all load")
	plot_edges_optimized(pairs, axis, loads["ALL"])

	# zoom
	plt.axis([14308000, 14578000, 49970000, 50186000])

	# plt.savefig(config.images.main_map, bbox_inches='tight', transparent=True)

	plt.show()


if __name__ == "__main__":
	pairs = edgePairs


	# plot_map_in_detail()

	plot_main_map()
