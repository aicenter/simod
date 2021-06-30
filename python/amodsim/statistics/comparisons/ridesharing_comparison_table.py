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

import numpy as np
import pandas.errors
import roadmaptools.inout
import amodsim.statistics.model.traffic_load as traffic_load
import amodsim.statistics.model.transit as transit
import amodsim.statistics.model.edges as edges
import amodsim.statistics.model.ridesharing as ridesharing
import amodsim.statistics.model.service as service
import amodsim.statistics.model.occupancy as occupancy

from typing import List, Dict, Iterable
from pandas import DataFrame
from roadmaptools.printer import print_table, print_info
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram
from amodsim.statistics.model.vehicle_state import VehicleState


def compute_stats_current_state(experiment_dir: str, result: Dict, histogram: TrafficDensityHistogram, load) -> List:

	# distance
	transit_data = transit.load(experiment_dir)
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True,
														   VehicleState.DRIVING_TO_TARGET_LOCATION) / 1000 / 100))

	# average density
	average_density_list_total_future = histogram.get_average_density_list(load)
	average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	dropped_demand_count = 0

	# half congested edges
	half_congested_count_in_time_window \
		= np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	used_cars_count = result["demandsCount"]

	return [km_total_window, average_density_in_time_window_non_empty_edges, congested_count_in_time_window,
		   dropped_demand_count, half_congested_count_in_time_window, used_cars_count, None, None]


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
	average_density_list_total_future = histogram.get_average_density_list(load)
	average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	dropped_demand_count = result["numberOfDemandsDropped"]

	# half congested edges
	half_congested_count_in_time_window \
		= np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	# used_cars = set()
	# for row in occuopancies:
	# 	used_cars.add(row[1])

	occuopancies = occupancy.load(experiment_dir)
	occupancies_in_window = occupancy.filter_window(occuopancies)
	# used_cars_count = len(occupancies_in_window[occupancies_in_window.occupancy > 0].vehicle_id.unique())
	used_cars_count = len(occupancies_in_window.vehicle_id.unique())

	# performance
	try:
		performance_data = ridesharing.load(experiment_dir)
		if 'Insertion Heuristic Time' in performance_data:
			avg_time = performance_data['Insertion Heuristic Time'].mean()
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

	return [km_total_window, average_density_in_time_window_non_empty_edges, congested_count_in_time_window,
		   dropped_demand_count, half_congested_count_in_time_window, used_cars_count, avg_time, mean_delay]


# result data load

# edges

loaded_edges = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
edge_data = edges.make_data_frame(loaded_edges)
edge_object_data = edges.load_edges_mapped_by_id(loaded_edges)


exp_dir_1 = config.comparison.experiment_1_dir
exp_dir_2 = config.comparison.experiment_2_dir
exp_dir_3 = config.comparison.experiment_3_dir
exp_dir_4 = config.comparison.experiment_4_dir
exp_dir_5 = config.comparison.experiment_9_dir
# exp_dir_1 = config.comparison.experiment_5_dir
# exp_dir_2 = config.comparison.experiment_6_dir
# exp_dir_3 = config.comparison.experiment_7_dir
# exp_dir_4 = config.comparison.experiment_8_dir
# exp_dir_5 = config.comparison.experiment_10_dir

# result json files
results_ridesharing_off \
	= roadmaptools.inout.load_json(exp_dir_1 + config.statistics.result_file_name)
results_insertion_heuristic\
	= roadmaptools.inout.load_json(exp_dir_2 + config.statistics.result_file_name)
results_vga \
	= roadmaptools.inout.load_json(exp_dir_3 + config.statistics.result_file_name)

results_vga_group_limit \
	= roadmaptools.inout.load_json(exp_dir_4 + config.statistics.result_file_name)
results_vga_pnas \
	= roadmaptools.inout.load_json(exp_dir_5 + config.statistics.result_file_name)

# traffic load
loads_no_ridesharing = traffic_load.load_all_edges_load_history(
	exp_dir_1 + config.statistics.all_edges_load_history_file_name)
loads_insertion_heuristic = traffic_load.load_all_edges_load_history(
	exp_dir_2 + config.statistics.all_edges_load_history_file_name)
loads_vga = traffic_load.load_all_edges_load_history(
	exp_dir_3 + config.statistics.all_edges_load_history_file_name)
loads_vga_group_limit = traffic_load.load_all_edges_load_history(
	exp_dir_4 + config.statistics.all_edges_load_history_file_name)
loads_vga_pnas = traffic_load.load_all_edges_load_history(
	exp_dir_5 + config.statistics.all_edges_load_history_file_name)

histogram = TrafficDensityHistogram(edge_object_data)

# compute data for output from result
present_state_data = compute_stats_current_state(exp_dir_1, results_ridesharing_off, histogram,
								   loads_no_ridesharing[VehicleState.DRIVING_TO_TARGET_LOCATION.name])
no_ridesharing_data = compute_stats(results_ridesharing_off, histogram, loads_no_ridesharing["ALL"],
									exp_dir_1, edge_data)
insertion_heuristic_data = compute_stats(results_insertion_heuristic, histogram, loads_insertion_heuristic["ALL"],
					exp_dir_2, edge_data)
vga_data = compute_stats(results_vga, histogram, loads_vga["ALL"], exp_dir_3,
						 edge_data)
vga_limited_data = compute_stats(results_vga_group_limit, histogram, loads_vga_group_limit["ALL"],
								 exp_dir_4, edge_data)
vga_pnas_data = compute_stats(results_vga_pnas, histogram, loads_vga_pnas["ALL"],
								 exp_dir_5, edge_data)

output_table = np.array([["X", "PRESENT STATE", "NO RIDESHARING", "INSERTION HEURISTIC", "VGA", "VGA limited", "VGA PNAS"],
						 ["Total veh. dist. traveled (km)", present_state_data[0], no_ridesharing_data[0], insertion_heuristic_data[0], vga_data[0], vga_limited_data[0], vga_pnas_data[0]],
						 ["avg. density", present_state_data[1], no_ridesharing_data[1], insertion_heuristic_data[1], vga_data[1], vga_limited_data[1], vga_pnas_data[1]],
						 ["cong. segments count", present_state_data[2], no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2], vga_limited_data[2], vga_pnas_data[2]],
						 ["dropped demand count", present_state_data[3], no_ridesharing_data[3], insertion_heuristic_data[3], vga_data[3], vga_limited_data[3], vga_pnas_data[3]],
						 ["half congested edges", present_state_data[4], no_ridesharing_data[4], insertion_heuristic_data[4], vga_data[4], vga_limited_data[4], vga_pnas_data[4]],
						 ["used car count", present_state_data[5], no_ridesharing_data[5], insertion_heuristic_data[5], vga_data[5], vga_limited_data[5], vga_pnas_data[5]],
						 ["avg. delay", present_state_data[7], no_ridesharing_data[7], insertion_heuristic_data[7], vga_data[7], vga_limited_data[7], vga_pnas_data[7]],
						 ["avg. comp. time", present_state_data[6], no_ridesharing_data[6], insertion_heuristic_data[6], vga_data[6], vga_limited_data[6], vga_pnas_data[6]]])

# console results
print("COMPARISON:")
print()
print_table(output_table)


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
print("Total veh. dist. (km) & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}& \\num{{{}}}".format(present_state_data[0], no_ridesharing_data[0],
	insertion_heuristic_data[0], vga_data[0], vga_limited_data[0], vga_pnas_data[0]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. delay (s) & {} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format("-", no_ridesharing_data[7], insertion_heuristic_data[7], vga_data[7],
	vga_limited_data[7], vga_pnas_data[7]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
print("Avg. density (veh/km) & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format(round(present_state_data[1], 4), round(no_ridesharing_data[1], 4),
	 round(insertion_heuristic_data[1], 4), round(vga_data[1], 4), round(vga_limited_data[1], 4), round(vga_pnas_data[1], 4)))
print(r"\tabularnewline")
print(r"\hline")
print("Congested seg. & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format(present_state_data[2], no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2],
	vga_limited_data[2], vga_pnas_data[2] ))
print(r"\tabularnewline")
print(r"\hline")
print("Heavily loaded seg.  & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format(round(present_state_data[4], 1), round(no_ridesharing_data[4], 1),
	round(insertion_heuristic_data[4], 1), round(vga_data[4], 1), round(vga_limited_data[4], 1), round(vga_pnas_data[4], 1)))
print(r"\tabularnewline")
print(r"\hline")
print("Used Vehicles  & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format(present_state_data[5], no_ridesharing_data[5], insertion_heuristic_data[5], vga_data[5],
	vga_limited_data[5], vga_pnas_data[5]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. comp. time (ms)  & {} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format("-", no_ridesharing_data[6], insertion_heuristic_data[6], vga_data[6],
	vga_limited_data[6], vga_pnas_data[6]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\end{tabular}}")

# To text
print("Abstract")
percentage_reduction_compared_to_no_ridesharing \
	= int(round((no_ridesharing_data[0] - vga_data[0]) / no_ridesharing_data[0] * 100))
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

absolute_reduction_compared_to_no_ridesharing = no_ridesharing_data[0] - vga_data[0]
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
traffic_density_reduction_no_ridesharing_percent = int(round((no_ridesharing_data[1] - vga_data[1]) / no_ridesharing_data[1] * 100))
traffic_density_reduction_current_state_percent = int(round((present_state_data[1] - vga_data[1]) / present_state_data[1] * 100))
print("in the morning peak, using the VGA method decreases the average traffic density by \\SI{{{}}}{{\\percent}} over the "
	  "Insertion Heuristic ridesharing, and by \\SI{{{}}}{{\\percent}} and \\SI{{{}}}{{\\percent}} over the MoD without "
	  "ridesharing and the current state respectively.".format(traffic_density_reduction_overih_percent,
		   traffic_density_reduction_no_ridesharing_percent, traffic_density_reduction_current_state_percent))

percentage_increase_no_ridesharing_compared_to_present_state \
	= int(round((no_ridesharing_data[0] - present_state_data[0]) / present_state_data[0] * 100))
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