from amodsim.init import config

import numpy as np
import roadmaptools.inout
import statistics.model.traffic_load as traffic_load
import amodsim.statistics.model.transit as transit
import amodsim.statistics.model.edges as edges

from typing import List, Dict, Iterable
from roadmaptools.printer import print_table
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram


def compute_stats(result: Dict, histogram: TrafficDensityHistogram, load, occuopancies: Iterable, experiment_dir: str) -> List:
	avg_km_total = result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
				   + result["averageKmRebalancing"]

	# km total
	transit_data = transit.load(experiment_dir)
	edge_data = edges.load_table()
	# km_total = int(round(avg_km_total * result["numberOfVehicles"]))
	km_total = transit.get_total_distance(transit_data, edge_data) / 1000
	km_total_window = transit.get_total_distance(transit_data, edge_data, True) / 1000

	# average density
	average_density_list_total_future = histogram.get_average_density_list(load)
	average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	dropped_demand_count = result["numberOfDemandsDropped"]

	# half congested edges
	half_congested_count_in_time_window \
		= np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	used_cars = set()
	for row in occuopancies:
		used_cars.add(row[1])

	return [km_total_window, average_density_in_time_window_non_empty_edges, congested_count_in_time_window, \
		   dropped_demand_count, half_congested_count_in_time_window, len(used_cars)]


# result data load

# edges
edge_object_data = traffic_load.load_edges_mapped_by_id()

# result json files
results_ridesharing_off \
	= roadmaptools.inout.load_json(config.comparison.experiment_1_dir + config.statistics.result_file_name)
results_insertion_heuristic\
	= roadmaptools.inout.load_json(config.comparison.experiment_2_dir + config.statistics.result_file_name)
results_vga \
	= roadmaptools.inout.load_json(config.comparison.experiment_3_dir + config.statistics.result_file_name)

# traffic load
loads_no_ridesharing = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_1_dir + config.statistics.all_edges_load_history_file_name)
loads_insertion_heuristic = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_2_dir + config.statistics.all_edges_load_history_file_name)
loads_vga = traffic_load.load_all_edges_load_history(
	config.comparison.experiment_3_dir + config.statistics.all_edges_load_history_file_name)

histogram = TrafficDensityHistogram(edge_object_data)

# occupancies
occupancies_1 = roadmaptools.inout.load_csv(
	config.comparison.experiment_1_dir + config.statistics.occupancies_file_name, delimiter=',')
occupancies_2 = roadmaptools.inout.load_csv(
	config.comparison.experiment_2_dir + config.statistics.occupancies_file_name, delimiter=',')
occupancies_3 = roadmaptools.inout.load_csv(
	config.comparison.experiment_3_dir + config.statistics.occupancies_file_name, delimiter=',')


# compute data for output from result
no_ridesharing_data = compute_stats(results_ridesharing_off, histogram, loads_no_ridesharing["ALL"], occupancies_1, config.comparison.experiment_1_dir)
insertion_heuristic_data\
	= compute_stats(results_insertion_heuristic, histogram, loads_insertion_heuristic["ALL"], occupancies_2, config.comparison.experiment_2_dir)
vga_data = compute_stats(results_vga, histogram, loads_vga["ALL"], occupancies_3, config.comparison.experiment_3_dir)

output_table = np.array([["X", "NO RIDESHARING", "INSERTION HEURISTIC", "VGA"],
						 ["Total veh. dist. traveled (km)", no_ridesharing_data[0], insertion_heuristic_data[0], vga_data[0]],
						 ["avg. density", no_ridesharing_data[1], insertion_heuristic_data[1], vga_data[1]],
						 ["cong. segments count", no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2]],
						 ["dropped demand count", no_ridesharing_data[3], insertion_heuristic_data[3], vga_data[3]],
						 ["half congested edges", no_ridesharing_data[4], insertion_heuristic_data[4], vga_data[4]],
						 ["used car count", no_ridesharing_data[5], insertion_heuristic_data[5], vga_data[5]]])

# console results
print("COMPARISON:")
print()
print_table(output_table)


# latex table
print("Latex code:")
print(r"\begin{tabular}{|c|c|c|c|}")
print(r"\hline")
print(" & No Ridesharing & Insertion Heuristic & VGA")
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
print("Total veh. dist. traveled (km) & {} & {} & {}".format(round(no_ridesharing_data[0], 1), round(insertion_heuristic_data[0], 1), round(vga_data[0], 1)))
print(r"\tabularnewline")
print(r"\hline")
print("Avg. density (veh/km) & {} & {} & {}".format(round(no_ridesharing_data[1], 4), round(insertion_heuristic_data[1], 4), round(vga_data[1], 4)))
print(r"\tabularnewline")
print(r"\hline")
print("Congested segments & {} & {} & {}".format(no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2]))
print(r"\tabularnewline")
print(r"\hline")
print("Heavily loaded segments & {} & {} & {}".format(round(no_ridesharing_data[4], 1), round(insertion_heuristic_data[4], 1), round(vga_data[4], 1)))
print(r"\tabularnewline")
print(r"\hline")
print("Used Vehicles & {} & {} & {}".format(no_ridesharing_data[5], insertion_heuristic_data[5], vga_data[5]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\end{tabular}")
