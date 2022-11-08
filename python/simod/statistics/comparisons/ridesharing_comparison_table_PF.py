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
from simod.init import config

import numpy as np
import pandas.errors
import roadmaptools.inout
import simod.statistics.model.traffic_load as traffic_load
import simod.statistics.model.transit as transit
import simod.statistics.model.edges as edges
import simod.statistics.model.ridesharing as ridesharing
import simod.statistics.model.service as service
import simod.statistics.model.occupancy as occupancy

from typing import List, Dict, Iterable
from pandas import DataFrame
from roadmaptools.printer import print_table, print_info
from simod.statistics.traffic_density_histogram import TrafficDensityHistogram
from simod.statistics.model.vehicle_state import VehicleState







def compute_stats_current_state(experiment_dir: str, result: Dict, histogram: TrafficDensityHistogram, load) -> List:

	# distance
	transit_data = transit.load(experiment_dir)
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True,
														   VehicleState.DRIVING_TO_TARGET_LOCATION) / 1000 / 100))

	# average density
	# average_density_list_total_future = histogram.get_average_density_list(load)
	# average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	# congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	dropped_demand_count = 0

	# half congested edges
	# half_congested_count_in_time_window \
	# 	= np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	used_cars_count = result["demandsCount"]

	return [km_total_window, 0, 0, dropped_demand_count , 0, used_cars_count, None, None]


def compute_stats(result: Dict, histogram: TrafficDensityHistogram, load, experiment_dir: str,
				  edge_data: DataFrame) -> List:
	# avg_km_total = result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
	# 			   + result["averageKmRebalancing"]

	# km total
	transit_data = transit.load(experiment_dir)

	# km_total = int(round(avg_km_total * result["numberOfVehicles"]))
	km_total = transit.get_total_distance(transit_data, edge_data) / 1000 / 100
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True) / 1000 / 100))

	# average density
	# average_density_list_total_future = histogram.get_average_density_list(load)
	# average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	# congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	dropped_demand_count = result["numberOfDemandsDropped"]
	dropped_passengers_count = result["numberPassengersDropped"]
	dropped_packages_count = result["numberPackagesDropped"]

	# half congested edges
	# half_congested_count_in_time_window \
	# 	= np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	# used_cars = set()
	# for row in occupancies:
	# 	used_cars.add(row[1])

	occupancies = occupancy.load(experiment_dir)
	occupancies_in_window = occupancy.filter_window(occupancies)
	# used_cars_count = len(occupancies_in_window[occupancies_in_window.occupancy > 0].vehicle_id.unique())
	used_cars_count = len(occupancies_in_window.vehicle_id.unique())

	total_shared_requests = result["totalSharedRequests"]

	# performance
	try:
		performance_data = ridesharing.load(experiment_dir)
		if 'PeopleFreight Heuristic Time' in performance_data:
			avg_time = performance_data['PeopleFreight Heuristic Time'].mean()
		else:
			avg_time = performance_data['Group Generation Time'].mean() + performance_data['Solver Time'].mean()
		# avg_time = int(round(avg_time / 1000))
		avg_time = int(round(avg_time))
	except pandas.errors.EmptyDataError:
		print_info("Empty ridesharing statistic")
		avg_time = "-1"

	# delay
	service_stat = service.load_dataframe(experiment_dir)
	delays_window = service.get_delays(service_stat, True, False)
	mean_delay = int(round(delays_window.mean() / 1000))
	# mean_delay = delays_window.mean()

	return [km_total_window, dropped_passengers_count, dropped_packages_count, total_shared_requests, used_cars_count, avg_time, mean_delay]


# result data load

# edges
loaded_edges = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
edge_data = edges.make_data_frame(loaded_edges)
edge_object_data = edges.load_edges_mapped_by_id(loaded_edges)



exp_dir_8 = config.comparison.experiment_8_dir		# 25k 1h manhattan
exp_dir_9 = config.comparison.experiment_9_dir
exp_dir_10 = config.comparison.experiment_10_dir

exp_dir_11 = config.comparison.experiment_11_dir		# 50k 1h manhattan
exp_dir_12 = config.comparison.experiment_12_dir
exp_dir_13 = config.comparison.experiment_13_dir

exp_dir_14 = config.comparison.experiment_14_dir		# 20k 24h manhattan
exp_dir_15 = config.comparison.experiment_15_dir
exp_dir_16 = config.comparison.experiment_16_dir


# exp_dir_1 = config.comparison.experiment_1_dir		# 20k
# exp_dir_2 = config.comparison.experiment_2_dir
# exp_dir_3 = config.comparison.experiment_3_dir		# 20k 10h
# exp_dir_4 = config.comparison.experiment_4_dir
# exp_dir_5 = config.comparison.experiment_5_dir		# 20k 2h
# exp_dir_6 = config.comparison.experiment_6_dir
# exp_dir_7 = config.comparison.experiment_7_dir		# 20k 1h
# exp_dir_8 = config.comparison.experiment_8_dir
# exp_dir_9 = config.comparison.experiment_9_dir		# 20k Manhattan 1h
# exp_dir_10 = config.comparison.experiment_10_dir



# result json files

results_base_25k \
	= roadmaptools.inout.load_json(exp_dir_8 + config.statistics.result_file_name)
results_multipass_25k \
	= roadmaptools.inout.load_json(exp_dir_9 + config.statistics.result_file_name)
results_insertion_25k\
	= roadmaptools.inout.load_json(exp_dir_10 + config.statistics.result_file_name)

results_base_50k \
	= roadmaptools.inout.load_json(exp_dir_11 + config.statistics.result_file_name)
results_multipass_50k \
	= roadmaptools.inout.load_json(exp_dir_12 + config.statistics.result_file_name)
results_insertion_50k\
	= roadmaptools.inout.load_json(exp_dir_13 + config.statistics.result_file_name)

results_base_24k_24h \
	= roadmaptools.inout.load_json(exp_dir_14 + config.statistics.result_file_name)
results_multipass_24k_24h \
	= roadmaptools.inout.load_json(exp_dir_15 + config.statistics.result_file_name)
results_insertion_24k_24h\
	= roadmaptools.inout.load_json(exp_dir_16 + config.statistics.result_file_name)

"""
results_PF_base \
	= roadmaptools.inout.load_json(exp_dir_1 + config.statistics.result_file_name)
results_PF_insertion\
	= roadmaptools.inout.load_json(exp_dir_2 + config.statistics.result_file_name)

results_PF_base_10h \
	= roadmaptools.inout.load_json(exp_dir_3 + config.statistics.result_file_name)
results_PF_insertion_10h \
	= roadmaptools.inout.load_json(exp_dir_4 + config.statistics.result_file_name)

results_PF_base_2h \
	= roadmaptools.inout.load_json(exp_dir_5 + config.statistics.result_file_name)
results_PF_insertion_2h\
	= roadmaptools.inout.load_json(exp_dir_6 + config.statistics.result_file_name)

results_PF_base_1h \
	= roadmaptools.inout.load_json(exp_dir_7 + config.statistics.result_file_name)
results_PF_insertion_1h\
	= roadmaptools.inout.load_json(exp_dir_8 + config.statistics.result_file_name)

results_PF_base_1h_manhattan \
	= roadmaptools.inout.load_json(exp_dir_9 + config.statistics.result_file_name)
results_PF_insertion_1h_manhattan\
	= roadmaptools.inout.load_json(exp_dir_10 + config.statistics.result_file_name)
"""

# traffic load

loads_base_25k = traffic_load.load_all_edges_load_history(
	exp_dir_8 + config.statistics.all_edges_load_history_file_name)
loads_multipass_25k = traffic_load.load_all_edges_load_history(
	exp_dir_9 + config.statistics.all_edges_load_history_file_name)
loads_insertion_25k = traffic_load.load_all_edges_load_history(
	exp_dir_10 + config.statistics.all_edges_load_history_file_name)

loads_base_50k = traffic_load.load_all_edges_load_history(
	exp_dir_11 + config.statistics.all_edges_load_history_file_name)
loads_multipass_50k = traffic_load.load_all_edges_load_history(
	exp_dir_12 + config.statistics.all_edges_load_history_file_name)
loads_insertion_50k = traffic_load.load_all_edges_load_history(
	exp_dir_13 + config.statistics.all_edges_load_history_file_name)

loads_base_24k_24h = traffic_load.load_all_edges_load_history(
	exp_dir_14 + config.statistics.all_edges_load_history_file_name)
loads_multipass_24k_24h = traffic_load.load_all_edges_load_history(
	exp_dir_15 + config.statistics.all_edges_load_history_file_name)
loads_insertion_24k_24h = traffic_load.load_all_edges_load_history(
	exp_dir_16 + config.statistics.all_edges_load_history_file_name)

"""
loads_PF_base = traffic_load.load_all_edges_load_history(
	exp_dir_1 + config.statistics.all_edges_load_history_file_name)
loads_PF_insertion = traffic_load.load_all_edges_load_history(
	exp_dir_2 + config.statistics.all_edges_load_history_file_name)

loads_PF_base_10h = traffic_load.load_all_edges_load_history(
	exp_dir_3 + config.statistics.all_edges_load_history_file_name)
loads_PF_insertion_10h = traffic_load.load_all_edges_load_history(
	exp_dir_4 + config.statistics.all_edges_load_history_file_name)

loads_PF_base_2h = traffic_load.load_all_edges_load_history(
	exp_dir_5 + config.statistics.all_edges_load_history_file_name)
loads_PF_insertion_2h = traffic_load.load_all_edges_load_history(
	exp_dir_6 + config.statistics.all_edges_load_history_file_name)

loads_PF_base_1h = traffic_load.load_all_edges_load_history(
	exp_dir_7 + config.statistics.all_edges_load_history_file_name)
loads_PF_insertion_1h = traffic_load.load_all_edges_load_history(
	exp_dir_8 + config.statistics.all_edges_load_history_file_name)

loads_PF_base_1h_manhattan = traffic_load.load_all_edges_load_history(
	exp_dir_9 + config.statistics.all_edges_load_history_file_name)
loads_PF_insertion_1h_manhattan = traffic_load.load_all_edges_load_history(
	exp_dir_10 + config.statistics.all_edges_load_history_file_name)
"""



histogram = TrafficDensityHistogram(edge_object_data)

# compute data for output from result

base_data_1h_25k = compute_stats(results_base_25k, histogram, loads_base_25k["ALL"], exp_dir_8, edge_data)
multipass_data_1h_25k = compute_stats(results_multipass_25k, histogram, loads_multipass_25k["ALL"], exp_dir_9, edge_data)
insertion_data_1h_25k = compute_stats(results_insertion_25k, histogram, loads_insertion_25k["ALL"], exp_dir_10, edge_data)

base_data_1h_50k = compute_stats(results_base_50k, histogram, loads_base_50k["ALL"], exp_dir_11, edge_data)
multipass_data_1h_50k = compute_stats(results_multipass_50k, histogram, loads_multipass_50k["ALL"], exp_dir_12, edge_data)
insertion_data_1h_50k = compute_stats(results_insertion_50k, histogram, loads_insertion_50k["ALL"], exp_dir_13, edge_data)

base_data_24h_24k = compute_stats(results_base_24k_24h, histogram, loads_base_24k_24h["ALL"], exp_dir_14, edge_data)
multipass_data_24h_24k = compute_stats(results_multipass_24k_24h, histogram, loads_multipass_24k_24h["ALL"], exp_dir_15, edge_data)
insertion_data_24h_24k = compute_stats(results_insertion_24k_24h, histogram, loads_insertion_24k_24h["ALL"], exp_dir_16, edge_data)

"""
# pf_base_data = compute_stats(results_PF_base, histogram, loads_PF_base["ALL"], exp_dir_1, edge_data)
# pf_insertion_data = compute_stats(results_PF_insertion, histogram, loads_PF_insertion["ALL"],
# 								  exp_dir_2, edge_data)
#
# pf_base_data_10h = compute_stats(results_PF_base_10h, histogram, loads_PF_base_10h["ALL"], exp_dir_3, edge_data)
# pf_insertion_data_10h = compute_stats(results_PF_insertion_10h, histogram, loads_PF_insertion_10h["ALL"],
# 								  exp_dir_4, edge_data)
#
# pf_base_data_2h = compute_stats(results_PF_base_2h, histogram, loads_PF_base_2h["ALL"], exp_dir_5, edge_data)
# pf_insertion_data_2h = compute_stats(results_PF_insertion_2h, histogram, loads_PF_insertion_2h["ALL"],
# 								  exp_dir_6, edge_data)
#
# pf_base_data_1h = compute_stats(results_PF_base_1h, histogram, loads_PF_base_1h["ALL"], exp_dir_7, edge_data)
# pf_insertion_data_1h = compute_stats(results_PF_insertion_1h, histogram, loads_PF_insertion_1h["ALL"],
# 								  exp_dir_8, edge_data)
#
# # Manhattan only
# pf_base_data_1h_manhattan = compute_stats(results_PF_base_1h_manhattan, histogram, loads_PF_base_1h_manhattan["ALL"], exp_dir_9, edge_data)
# pf_insertion_data_1h_manhattan = compute_stats(results_PF_insertion_1h_manhattan, histogram, loads_PF_insertion_1h_manhattan["ALL"],
# 											   exp_dir_10, edge_data)
"""


output_table = np.array([["X", "1h 25k Manhattan BASE", "1h 25k Manhattan MULTIPASS", "1h 25k Manhattan INSERTION"],
						 ["Total veh. dist. traveled (km)", base_data_1h_25k[0], multipass_data_1h_25k[0], insertion_data_1h_25k[0]],
						 ["dropped passengers count", base_data_1h_25k[1], multipass_data_1h_25k[1], insertion_data_1h_25k[1]],
						 ["dropped packages count", base_data_1h_25k[2], multipass_data_1h_25k[2], insertion_data_1h_25k[2]],
						 ["shared demand count", base_data_1h_25k[3], multipass_data_1h_25k[3], insertion_data_1h_25k[3]],
						 ["used car count", base_data_1h_25k[4], multipass_data_1h_25k[4], insertion_data_1h_25k[4]],
						 ["avg. delay", base_data_1h_25k[6], multipass_data_1h_25k[6], insertion_data_1h_25k[6]],
						 ["avg. comp. time", base_data_1h_25k[5], multipass_data_1h_25k[5], insertion_data_1h_25k[5]]])

output_table_2 = np.array([["X", "1h 50k Manhattan BASE", "1h 50k Manhattan MULTIPASS", "1h 50k Manhattan INSERTION"],
						 ["Total veh. dist. traveled (km)", base_data_1h_50k[0], multipass_data_1h_50k[0], insertion_data_1h_50k[0]],
						 ["dropped passengers count", base_data_1h_50k[1], multipass_data_1h_50k[1], insertion_data_1h_50k[1]],
						 ["dropped packages count", base_data_1h_50k[2], multipass_data_1h_50k[2], insertion_data_1h_50k[2]],
						 ["shared demand count", base_data_1h_50k[3], multipass_data_1h_50k[3], insertion_data_1h_50k[3]],
						 ["used car count", base_data_1h_50k[4], multipass_data_1h_50k[4], insertion_data_1h_50k[4]],
						 ["avg. delay", base_data_1h_50k[6], multipass_data_1h_50k[6], insertion_data_1h_50k[6]],
						 ["avg. comp. time", base_data_1h_50k[5], multipass_data_1h_50k[5], insertion_data_1h_50k[5]]])

output_table_3 = np.array([["X", "24h 24k Manhattan BASE", "24h 24k Manhattan MULTIPASS", "24h 24k Manhattan INSERTION"],
						   ["Total veh. dist. traveled (km)", base_data_24h_24k[0], multipass_data_24h_24k[0], insertion_data_24h_24k[0]],
						   ["dropped passengers count", base_data_24h_24k[1], multipass_data_24h_24k[1], insertion_data_24h_24k[1]],
						   ["dropped packages count", base_data_24h_24k[2], multipass_data_24h_24k[2], insertion_data_24h_24k[2]],
						   ["shared demand count", base_data_24h_24k[3], multipass_data_24h_24k[3], insertion_data_24h_24k[3]],
						   ["used car count", base_data_24h_24k[4], multipass_data_24h_24k[4], insertion_data_24h_24k[4]],
						   ["avg. delay", base_data_24h_24k[6], multipass_data_24h_24k[6], insertion_data_24h_24k[6]],
						   ["avg. comp. time", base_data_24h_24k[5], multipass_data_24h_24k[5], insertion_data_24h_24k[5]]])

"""
# output_table = np.array([["X", "BASE", "INSERTION", "10h BASE", "10h INSERTION", "2h BASE", "2h INSERTION"],
# 						 ["Total veh. dist. traveled (km)", pf_base_data[0], pf_insertion_data[0], pf_base_data_10h[0], pf_insertion_data_10h[0], pf_base_data_2h[0], pf_insertion_data_2h[0]],
# 						 ["dropped passengers count", pf_base_data[1], pf_insertion_data[1], pf_base_data_10h[1], pf_insertion_data_10h[1], pf_base_data_2h[1], pf_insertion_data_2h[1]],
# 						 ["dropped packages count", pf_base_data[2], pf_insertion_data[2], pf_base_data_10h[2], pf_insertion_data_10h[2], pf_base_data_2h[2], pf_insertion_data_2h[2]],
# 						 ["shared demand count", pf_base_data[3], pf_insertion_data[3], pf_base_data_10h[3], pf_insertion_data_10h[3], pf_base_data_2h[3], pf_insertion_data_2h[3]],
# 						 ["used car count", pf_base_data[4], pf_insertion_data[4], pf_base_data_10h[4], pf_insertion_data_10h[4], pf_base_data_2h[4], pf_insertion_data_2h[4]],
# 						 ["avg. delay", pf_base_data[6], pf_insertion_data[6], pf_base_data_10h[6], pf_insertion_data_10h[6], pf_base_data_2h[6], pf_insertion_data_2h[6]],
# 						 ["avg. comp. time", pf_base_data[5], pf_insertion_data[5], pf_base_data_10h[5], pf_insertion_data_10h[5], pf_base_data_2h[5], pf_insertion_data_2h[5]]])
#
# output_table_2 = np.array([["X", "1h BASE", "1h INSERTION", "1h Manhattan BASE", "1h Manhattan INSERTION"],
# 						   ["Total veh. dist. traveled (km)", pf_base_data_1h[0], pf_insertion_data_1h[0], pf_base_data_1h_manhattan[0], pf_insertion_data_1h_manhattan[0]],
# 						   ["dropped passengers count", pf_base_data_1h[1], pf_insertion_data_1h[1], pf_base_data_1h_manhattan[1], pf_insertion_data_1h_manhattan[1]],
# 						   ["dropped packages count", pf_base_data_1h[2], pf_insertion_data_1h[2], pf_base_data_1h_manhattan[2], pf_insertion_data_1h_manhattan[2]],
# 						   ["shared demand count", pf_base_data_1h[3], pf_insertion_data_1h[3], pf_base_data_1h_manhattan[3], pf_insertion_data_1h_manhattan[3]],
# 						   ["used car count", pf_base_data_1h[4], pf_base_data_1h[4], pf_base_data_1h_manhattan[4], pf_insertion_data_1h_manhattan[4]],
# 						   ["avg. delay", pf_base_data_1h[6], pf_base_data_1h[6], pf_base_data_1h_manhattan[6], pf_insertion_data_1h_manhattan[6]],
# 						   ["avg. comp. time", pf_base_data_1h[5], pf_base_data_1h[5], pf_base_data_1h_manhattan[5], pf_insertion_data_1h_manhattan[5]]])
"""

# console results
print("COMPARISON:")
print_table(output_table)
print()
print_table(output_table_2)
print()
print_table(output_table_3)

"""
# latex table
print("Latex code:")
print(r"{\renewcommand{\arraystretch}{1.2}%")
print(r"\begin{tabular}{|+l|-r|-r|-r|-r|-r|-r|}")
print(r"\hline")
print(r" &  & \multicolumn{5}{c|}{Mobility-on-Demand}")
print(r"\tabularnewline")
print(r"\cline{3-7}")
print(r" & \thead{Present} & \thead{No Ridesh.} & \thead{IH} & \thead{VGA} & \thead{VGA lim} & \thead{VGA PNAS}")
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
print(r"Optimal & - & - & no & yes & no & no")
print(r"\tabularnewline")
print(r"\hline")
print(r"\rowstyle{\bfseries}")
print("Total veh. dist. (km) & \\num{{{}}} & \\num{{{}}}".format(present_state_data[0], pf_heuristic_data[0]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. delay (s) & {} & \\num{{{}}}".format("-", pf_heuristic_data[7]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
print("Avg. density (veh/km) & \\num{{{}}} & \\num{{{}}}".format(round(present_state_data[1], 4), round(pf_heuristic_data[1], 4)))
print(r"\tabularnewline")
print(r"\hline")
print("Congested seg. & \\num{{{}}} & \\num{{{}}}".format(present_state_data[2], pf_heuristic_data[2]))
print(r"\tabularnewline")
print(r"\hline")
print("Heavily loaded seg.  & \\num{{{}}} & \\num{{{}}}".format(round(present_state_data[4], 1), round(pf_heuristic_data[4], 1)))
print(r"\tabularnewline")
print(r"\hline")
print("Used Vehicles  & \\num{{{}}} & \\num{{{}}}".format(present_state_data[5], pf_heuristic_data[5]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. comp. time (ms)  & {} & \\num{{{}}}".format("-", pf_heuristic_data[6]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\end{tabular}}")
"""

"""
# To text
print("Abstract")

percentage_reduction_compared_to_no_ridesharing \
	= int(round((pf_heuristic_data[0] - vga_data[0]) / pf_heuristic_data[0] * 100))
print("We found that the system that uses optimal ridesharing assignments subject to the maximum travel delay of "
	  "4 minutes reduces the vehicle distance driven by \\SI{{{}}}{{\\percent}}".format(
	percentage_reduction_compared_to_no_ridesharing))

percentage_reduction_compared_to_ih \
	= int(round((insertion_heuristic_data[0] - vga_data[0]) / insertion_heuristic_data[0] * 100))
print("Furthermore, we found that the optimal assignments result in a \\SI{{{}}}{{\\percent}} "
	  " reduction in vehicle distance driven.".format(
	percentage_reduction_compared_to_ih))

delay_reduction \
	= int(round((insertion_heuristic_data[7] - vga_data[7]) / insertion_heuristic_data[7] * 100))
print("and \\SI{{{}}}{{\\percent}} lower average passenger travel delay compared to a system that uses "
	  "insertion heuristic".format(delay_reduction))

print("Our evaluation revealed that optimal ridesharing assignments can reduce the distance driven in the system by"
	  " \\SI{{{}}}{{\\percent}} ".format(percentage_reduction_compared_to_no_ridesharing))

print("Specifically, in the system that uses optimal assignments, the total vehicle distance driven is reduced by"
	  " \\SI{{{}}}{{\\percent}}, "
	  "and simultaneously, average passenger travel delay is reduced by~\\SI{{{}}}{{\\percent}}".format(
	percentage_reduction_compared_to_ih, delay_reduction))

absolute_reduction_compared_to_ih = insertion_heuristic_data[0] - vga_data[0]
print("We can see that when using the \\gls{{vga}} method instead of Insertion Heuristic during the morning peak, we can "
	  "save almost \\SI{{{}}}{{\\km}} of vehicle distance driven, which is more than \\SI{{{}}}{{\\percent}} reduction."
	  .format(absolute_reduction_compared_to_ih, percentage_reduction_compared_to_ih))

absolute_reduction_compared_to_no_ridesharing = pf_heuristic_data[0] - vga_data[0]
absolute_reduction_compared_to_present_state = present_state_data[0] - vga_data[0]
percentage_reduction_compared_to_present_state \
	= int(round((present_state_data[0] - vga_data[0]) / present_state_data[0] * 100))
print("When comparing with the no ridesharing scenario and with the present state, the \\gls{{vga}} method saves over "
	  "\\SI{{{}}}{{\\km}} (\\SI{{{}}}{{\\percent}}) and \\SI{{{}}}{{\\km}} (\\SI{{{}}}{{\\percent}}) respectively."
	  .format(absolute_reduction_compared_to_no_ridesharing, percentage_reduction_compared_to_no_ridesharing,
			  absolute_reduction_compared_to_present_state, percentage_reduction_compared_to_present_state))

vehicles_saved = insertion_heuristic_data[5] - vga_data[5]
vehicles_saved_percent = int(round((insertion_heuristic_data[5] - vga_data[5]) / insertion_heuristic_data[5] * 100))
print("The results confirm that the \\gls{{vga}} mehod indeed makes the \\gls{{mod}} system more efficient, saving "
	  "over \\SI{{{}}}{{vehicles}}~(\\SI{{{}}}{{\percent}}) over the Insertion Heuristic."
	  .format(vehicles_saved, vehicles_saved_percent))

traffic_density_reduction_overih_percent = int(round((insertion_heuristic_data[1] - vga_data[1]) / insertion_heuristic_data[1] * 100))
traffic_density_reduction_no_ridesharing_percent = int(round((pf_heuristic_data[1] - vga_data[1]) / pf_heuristic_data[1] * 100))
traffic_density_reduction_current_state_percent = int(round((present_state_data[1] - vga_data[1]) / present_state_data[1] * 100))
print("in the morning peak, using the VGA method decreases the average traffic density by \\SI{{{}}}{{\\percent}} over the "
	  "Insertion Heuristic ridesharing, and by \\SI{{{}}}{{\\percent}} and \\SI{{{}}}{{\\percent}} over the MoD without "
	  "ridesharing and the current state respectively.".format(traffic_density_reduction_overih_percent,
		   traffic_density_reduction_no_ridesharing_percent, traffic_density_reduction_current_state_percent))

percentage_increase_no_ridesharing_compared_to_present_state \
	= int(round((pf_heuristic_data[0] - present_state_data[0]) / present_state_data[0] * 100))
# print("When using \\gls{{mod}}, the size of the vehicle fleet can be reduced almost four times, at the cost of about "
# 	  "\\SI{{{}}}{{\\percent}} increase in traveled distance due to empty trips between passengers."
# 	  .format(percentage_increase_no_ridesharing_compared_to_present_state))

percentage_reduction_ih_compared_to_present_state \
	= int(round((present_state_data[0] - insertion_heuristic_data[0]) / present_state_data[0] * 100))
# print("We also demonstrated that when using Insertion Heuristic ridesharing, the number of vehicles can be reduced"
# 	  " almost eight times compared to the present state, and the total traveled distance can be reduced "
# 	  "by \\SI{{{}}}{{\\percent}}.".format(percentage_reduction_ih_compared_to_present_state))


print("Finally, the results confirmed that the optimal \\gls{{vga}} method is significantly more efficient than "
	  "the Insertion Heuristic, reducing the traveled distance by \\SI{{{}}}{{\\percent}}:"
	  .format(percentage_reduction_compared_to_present_state))

print("The results confirmed that ridesharing dramatically increases the efficiency of an MoD system: by employing "
	  "the \\gls{{vga}} method, we reduce the total distance driven in the system by more then \\SI{{{}}}{{\\percent}}."
	  .format(percentage_reduction_compared_to_no_ridesharing))

print("Our results show that using the VGA method instead of the Insertion Heuristic, we can reduce the total distance "
	  "traveled by more than \\SI{{{}}}{{\\percent}} while reducing the passenger delays by \\SI{{{}}}{{\\percent}}."
	  .format(percentage_reduction_compared_to_ih, delay_reduction))

percentage_reduction_limited_compared_to_ih \
	= int(round((insertion_heuristic_data[0] - vga_limited_data[0]) / insertion_heuristic_data[0] * 100))
computation_time_reduction_vga_limited_compared_to_vga \
	= int(round((vga_data[6] - vga_limited_data[6]) / vga_data[6] * 100))
print("Finally, our resource-constrained VGA method provides more than \\SI{{{}}}{{\\percent}} travel distance saving"
	  " over IH while reducing the computational time by almost \\SI{{{}}}{{\\percent}}."
	  .format(percentage_reduction_limited_compared_to_ih, computation_time_reduction_vga_limited_compared_to_vga))
"""
