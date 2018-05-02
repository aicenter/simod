from amodsim.init import config, roadmaptools_config

import json
import numpy as np
import roadmaptools.inout
import amodsim.traffic_load as traffic_load

from typing import List, Tuple
from amodsim.utils import col_to_percent, to_percetnt
from scripts.printer import print_table
from amodsim.traffic_load import VehiclePhase
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram, CONGESTION_INDEX


def compute_stats(result: List, histogram: TrafficDensityHistogram, load) -> List:
	avg_km_total = result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
				   + result["averageKmRebalancing"]
	dropped_demand_count = result["numberOfDemandsDropped"]

	# average density
	average_density_list_total_future = histogram.get_average_density_list(load)
	average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)

	# congested count
	congested_count_in_time_window = np.where(average_density_list_total_future > config.critical_density)[0].size

	# km total
	km_total = int(round(avg_km_total * result["numberOfVehicles"]))

	# half congested edges
	half_congested_count_in_time_window = np.where(average_density_list_total_future > (config.critical_density / 2))[0].size

	return [avg_km_total, average_density_in_time_window_non_empty_edges, congested_count_in_time_window, \
		   dropped_demand_count, km_total, half_congested_count_in_time_window]


results_ridesharing_off = roadmaptools.inout.load_json(config.analysis.results_ridesharing_off_filepath)
results_ridesharing_on = roadmaptools.inout.load_json(config.analysis.results_ridesharing_on_filepath)


edges = traffic_load.load_edges_mapped_by_id()

loads_capacity_1 = traffic_load.load_all_edges_load_history(config.analysis.edge_load_ridesharing_off_filepath)
loads_capacity_5 = traffic_load.load_all_edges_load_history(config.analysis.edge_load_ridesharing_on_filepath)

histogram = TrafficDensityHistogram(edges)

present_data = compute_stats(results_ridesharing_off, histogram, loads_capacity_1[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name])
mod_data = compute_stats(results_ridesharing_off, histogram, loads_capacity_1["ALL"])
ridesharing_data = compute_stats(results_ridesharing_on, histogram, loads_capacity_5["ALL"])

# correct present data
present_data[0] = results_ridesharing_off["averageKmWithPassenger"]
present_data[4] = int(round(results_ridesharing_off["averageKmWithPassenger"] * results_ridesharing_off["numberOfVehicles"]))
# present_data[3] = 0

output_table = np.array([["X", "PRESENT", "MOD", "RIDESHARING"],
				["avg. km per vehicle", present_data[0], mod_data[0], ridesharing_data[0]],
				["avg. density", present_data[1], mod_data[1], ridesharing_data[1]],
				["cong. segments count", present_data[2], mod_data[2], ridesharing_data[2]],
				["dropped demand count", present_data[3], mod_data[3], ridesharing_data[3]],
				["km total", present_data[4], mod_data[4], ridesharing_data[4]],
				["half congested edges", present_data[5], mod_data[5], ridesharing_data[5]]])

print("COMPARISON:")
print()
print_table(output_table)

print("Latex code:")

print(r"\begin{tabular}{|c|c|c|c|}")
print("\hline")
print("Stats & Present & MoD & MoD with ridesharing")
print(r"\tabularnewline")
print("\hline")
print("\hline")
print("Km traveled & {} & {} & {}".format(round(present_data[4], 1), round(mod_data[4], 1), round(ridesharing_data[4], 1)))
print(r"\tabularnewline")
print("\hline")
print("Avg. km/vehicle & {} & {} & {}".format(round(present_data[0], 1), round(mod_data[0], 1), round(ridesharing_data[0], 1)))
print(r"\tabularnewline")
print("\hline")
print("Avg. density & {} & {} & {}".format(round(present_data[1], 4),round(mod_data[1], 4), round(ridesharing_data[1], 4)))
print(r"\tabularnewline")
print("\hline")
print("Congested segments & {} & {} & {}".format(present_data[2], mod_data[2], ridesharing_data[2]))
print(r"\tabularnewline")
print("\hline")
print("Heavily loaded segments & {} & {} & {}".format(round(present_data[5], 1), round(mod_data[5], 1), round(ridesharing_data[5], 1)))
print(r"\tabularnewline")
print("\hline")
# print("Dropped demands & {} & {} & {}".format(present_data[3], mod_data[3], ridesharing_data[3]))
# print(r"\tabularnewline")
# print("\hline")
print("\end{tabular}")


