from amodsim.init import config

import numpy as np
import roadmaptools.inout
import amodsim.traffic_load as traffic_load

from typing import List
from roadmaptools.printer import print_table
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram


def compute_stats(result: List, histogram: TrafficDensityHistogram, load) -> List:
	avg_km_total = result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
				   + result["averageKmRebalancing"]

	# km total
	km_total = int(round(avg_km_total * result["numberOfVehicles"]))

	# average density
	average_density_list_total_future = histogram.get_average_density_list(load)
	average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	dropped_demand_count = result["numberOfDemandsDropped"]

	# half congested edges
	half_congested_count_in_time_window \
		= np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	return [km_total, average_density_in_time_window_non_empty_edges, congested_count_in_time_window, \
		   dropped_demand_count, half_congested_count_in_time_window]


# result data load

# edges
edges = traffic_load.load_edges_mapped_by_id()

# result json files
results_ridesharing_off = roadmaptools.inout.load_json(r'C:\AIC data\Shared\amod-data\VGA Evaluation\experiments\IH-constraint4min-capacity1\result.json')
results_insertion_heuristic = roadmaptools.inout.load_json(r'C:\AIC data\Shared\amod-data\VGA Evaluation\experiments\IH-constraint4min\result.json')
results_vga = roadmaptools.inout.load_json(r'C:\AIC data\Shared\amod-data\VGA Evaluation\experiments\vga-constraint4min\result.json')

# traffic load
loads_no_ridesharing = traffic_load.load_all_edges_load_history(r'C:\AIC data\Shared\amod-data\VGA Evaluation\experiments\IH-constraint4min-capacity1\allEdgesLoadHistory.json')
loads_insertion_heuristic = traffic_load.load_all_edges_load_history(r'C:\AIC data\Shared\amod-data\VGA Evaluation\experiments\IH-constraint4min\allEdgesLoadHistory.json')
loads_vga = traffic_load.load_all_edges_load_history(r'C:\AIC data\Shared\amod-data\VGA Evaluation\experiments\vga-constraint4min\allEdgesLoadHistory.json')

histogram = TrafficDensityHistogram(edges)


# compute data for output from result
no_ridesharing_data = compute_stats(results_ridesharing_off, histogram, loads_no_ridesharing["ALL"])
insertion_heuristic_data = compute_stats(results_insertion_heuristic, histogram, loads_insertion_heuristic["ALL"])
vga_data = compute_stats(results_vga, histogram, loads_vga["ALL"])

output_table = np.array([["X", "NO RIDESHARING", "INSERTION HEURISTIC", "VGA"],
						 ["Total veh. dist. traveled (km)", no_ridesharing_data[0], insertion_heuristic_data[0], vga_data[0]],
						 ["avg. density", no_ridesharing_data[1], insertion_heuristic_data[1], vga_data[1]],
						 ["cong. segments count", no_ridesharing_data[2], insertion_heuristic_data[2], vga_data[2]],
						 ["dropped demand count", no_ridesharing_data[3], insertion_heuristic_data[3], vga_data[3]],
						 ["half congested edges", no_ridesharing_data[4], insertion_heuristic_data[4], vga_data[4]]])

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
print("Used Vehicles & {} & {} & {}".format(no_ridesharing_data[3], insertion_heuristic_data[3], vga_data[3]))
print(r"\tabularnewline")
print(r"\hline")
print(r"\end{tabular}")
