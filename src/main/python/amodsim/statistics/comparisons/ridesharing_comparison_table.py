from amodsim.init import config

import numpy as np
import roadmaptools.inout
import amodsim.statistics.model.traffic_load as traffic_load
import amodsim.statistics.model.transit as transit
import amodsim.statistics.model.edges as edges
import amodsim.statistics.model.ridesharing as ridesharing
import amodsim.statistics.model.service as service
import amodsim.statistics.model.occupancy as occupancy

from typing import List, Dict, Iterable
from pandas import DataFrame
from roadmaptools.printer import print_table
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram
from amodsim.statistics.model.vehicle_state import VehicleState


def compute_stats_current_state(experiment_dir: str, result: Dict, histogram: TrafficDensityHistogram, load) -> List:

	# distance
	transit_data = transit.load(experiment_dir)
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True,
														   VehicleState.DRIVING_TO_TARGET_LOCATION) / 1000))

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
	km_total = transit.get_total_distance(transit_data, edge_data) / 1000
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True) / 1000))

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
	used_cars_count = len(occupancies_in_window[occupancies_in_window.occupancy > 0].vehicle_id.unique())

	# performance
	performance_data = ridesharing.load(experiment_dir)
	if 'Insertion Heuristic Time' in performance_data:
		avg_time = performance_data['Insertion Heuristic Time'].mean()
	else:
		avg_time = performance_data['Group Generation Time'].mean() + performance_data['Solver Time'].mean()
	avg_time = int(round(avg_time / 1000))

	# delay
	service_stat = service.load_dataframe(experiment_dir)
	delays_window = service.get_delays(service_stat, True, False)
	mean_delay = int(round(delays_window.mean() / 1000))

	return [km_total_window, average_density_in_time_window_non_empty_edges, congested_count_in_time_window,
		   dropped_demand_count, half_congested_count_in_time_window, used_cars_count, avg_time, mean_delay]


# result data load

# edges
edge_data = edges.load_table()
edge_object_data = edges.load_edges_mapped_by_id()

# result json files
results_ridesharing_off \
	= roadmaptools.inout.load_json(config.comparison.experiment_5_dir + config.statistics.result_file_name)
results_insertion_heuristic\
	= roadmaptools.inout.load_json(config.comparison.experiment_6_dir + config.statistics.result_file_name)
results_vga \
	= roadmaptools.inout.load_json(config.comparison.experiment_7_dir + config.statistics.result_file_name)

results_vga_group_limit \
	= roadmaptools.inout.load_json(config.comparison.experiment_8_dir + config.statistics.result_file_name)

# traffic load
loads_no_ridesharing = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_5_dir + config.statistics.all_edges_load_history_file_name)
loads_insertion_heuristic = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_6_dir + config.statistics.all_edges_load_history_file_name)
loads_vga = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_7_dir + config.statistics.all_edges_load_history_file_name)
loads_vga_group_limit = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_8_dir + config.statistics.all_edges_load_history_file_name)

histogram = TrafficDensityHistogram(edge_object_data)

# compute data for output from result
present_state_data = compute_stats_current_state(config.comparison.experiment_5_dir, results_ridesharing_off, histogram,
								   loads_no_ridesharing[VehicleState.DRIVING_TO_TARGET_LOCATION.name])
no_ridesharing_data = compute_stats(results_ridesharing_off, histogram, loads_no_ridesharing["ALL"],
									config.comparison.experiment_5_dir, edge_data)
insertion_heuristic_data = compute_stats(results_insertion_heuristic, histogram, loads_insertion_heuristic["ALL"],
					config.comparison.experiment_6_dir, edge_data)
vga_data = compute_stats(results_vga, histogram, loads_vga["ALL"], config.comparison.experiment_7_dir,
						 edge_data)
vga_limited_data = compute_stats(results_vga_group_limit, histogram, loads_vga_group_limit["ALL"],
								 config.comparison.experiment_8_dir, edge_data)

output_table = np.array([["X", "PRESENT STATE", "NO RIDESHARING", "INSERTION HEURISTIC", "VGA", "VGA limited"],
						 ["Total veh. dist. traveled (km)", present_state_data[0], no_ridesharing_data[0], insertion_heuristic_data[0], vga_data[0], vga_limited_data[0]],
						 ["avg. density", present_state_data[1], no_ridesharing_data[1], insertion_heuristic_data[1], vga_data[1], vga_limited_data[1]],
						 ["cong. segments count", present_state_data[2], no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2], vga_limited_data[2]],
						 ["dropped demand count", present_state_data[3], no_ridesharing_data[3], insertion_heuristic_data[3], vga_data[3], vga_limited_data[3]],
						 ["half congested edges", present_state_data[4], no_ridesharing_data[4], insertion_heuristic_data[4], vga_data[4], vga_limited_data[4]],
						 ["used car count", present_state_data[5], no_ridesharing_data[5], insertion_heuristic_data[5], vga_data[5], vga_limited_data[5]],
						 ["avg. delay", present_state_data[7], no_ridesharing_data[7], insertion_heuristic_data[7], vga_data[7], vga_limited_data[7]],
						 ["avg. comp. time", present_state_data[6], no_ridesharing_data[6], insertion_heuristic_data[6], vga_data[6], vga_limited_data[6]]])

# console results
print("COMPARISON:")
print()
print_table(output_table)


# latex table
print("Latex code:")
print(r"\begin{tabular}{|l|r|r|r|r|r|}")
print(r"\hline")
print(r" & \thead{Present} & \thead{No Ridesh.} & \thead{IH} & \thead{VGA} & \thead{VGA lim}")
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
print("Total veh. dist. (km) & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format(present_state_data[0], no_ridesharing_data[0],
	insertion_heuristic_data[0], vga_data[0], vga_limited_data[0]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. density (veh/km) & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}& \\num{{{}}}".format(round(present_state_data[1], 4), round(no_ridesharing_data[1], 4),
	 round(insertion_heuristic_data[1], 4), round(vga_data[1], 4), round(vga_limited_data[1], 4)))
print(r"\tabularnewline")
print(r"\hline")
print("Congested seg. & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}& \\num{{{}}}".format(present_state_data[2], no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2],
	vga_limited_data[2] ))
print(r"\tabularnewline")
print(r"\hline")
print("Heavily loaded seg.  & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}& \\num{{{}}}".format(round(present_state_data[4], 1), round(no_ridesharing_data[4], 1),
	round(insertion_heuristic_data[4], 1), round(vga_data[4], 1), round(vga_limited_data[4], 1)))
print(r"\tabularnewline")
print(r"\hline")
print("Used Vehicles  & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}& \\num{{{}}}".format(present_state_data[5], no_ridesharing_data[5], insertion_heuristic_data[5], vga_data[5],
	vga_limited_data[5]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. delay (s) & {} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}& \\num{{{}}}".format("-", no_ridesharing_data[7], insertion_heuristic_data[7], vga_data[7],
	vga_limited_data[7]))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. comp. time (s)  & {} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}}".format("-", no_ridesharing_data[6], insertion_heuristic_data[6], vga_data[6],
	vga_limited_data[6]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\end{tabular}")
