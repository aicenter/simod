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


def compute_stats(result: Dict, histogram: TrafficDensityHistogram, load, experiment_dir: str,
                  edge_data: DataFrame) -> List:
	# km total
	transit_data = transit.load(experiment_dir)
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True) / 1000 / 100))
	km_per_served_demand = round(km_total_window / (result["demandsCount"] - result["numberOfDemandsDropped"]), 3)

	dropped_demand_count = result["numberOfDemandsDropped"]
	demand_count_served = result["demandsCount"] - result["numberOfDemandsDropped"]

	occupancies = occupancy.load(experiment_dir)
	occupancies_in_window = occupancy.filter_window(occupancies)
	used_cars_count = len(occupancies_in_window.vehicle_id.unique())

	# delay
	service_stat = service.load_dataframe(experiment_dir)
	delays_window = service.get_delays(service_stat, True, False)
	mean_delay = int(round(delays_window.mean() / 1000))

	transfers_count = result['transfersDone']

	return [km_total_window, dropped_demand_count, used_cars_count, mean_delay, demand_count_served,
	        km_per_served_demand, transfers_count]

exp_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/experiments/test/'
# exp_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/experiments/test/'
exp_dir_2 = '/Users/adela/Documents/bakalarka/randomdemand/experiments/test/'

exp_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/experiments/test/'

edges_path = '/Users/adela/Documents/bakalarka/randomdemand/maps/edges.geojson'
loaded_edges = roadmaptools.inout.load_geojson(edges_path)
edge_data = edges.make_data_frame(loaded_edges)
edge_object_data = edges.load_edges_mapped_by_id(loaded_edges)

results_insertion_heuristic \
	= roadmaptools.inout.load_json(exp_dir_1 + config.statistics.result_file_name)
results_insertion_heuristic_transfer \
	= roadmaptools.inout.load_json(exp_dir_2 + config.statistics.result_file_name)
results_taset \
	= roadmaptools.inout.load_json(exp_dir_3 + config.statistics.result_file_name)

loads_insertion_heuristic = traffic_load.load_all_edges_load_history(
	exp_dir_1 + config.statistics.all_edges_load_history_file_name)
loads_insertion_heuristic_transfer = traffic_load.load_all_edges_load_history(
	exp_dir_2 + config.statistics.all_edges_load_history_file_name)
loads_taset = traffic_load.load_all_edges_load_history(
	exp_dir_3 + config.statistics.all_edges_load_history_file_name)

histogram = TrafficDensityHistogram(edge_object_data)

insertion_heuristic_data = compute_stats(results_insertion_heuristic, histogram, loads_insertion_heuristic["ALL"],
                                         exp_dir_1, edge_data)
insertion_heuristic_transfer_data = compute_stats(results_insertion_heuristic_transfer, histogram,
                                                  loads_insertion_heuristic_transfer["ALL"], exp_dir_2,
                                                  edge_data)
taset_data = compute_stats(results_taset, histogram, loads_taset["ALL"],
                           exp_dir_3, edge_data)

output_table = np.array([[" ", "INSERTION HEURISTIC", "INSERTION HEURISTIC TRANSFER", "GREEDY HEURISTIC TRANSFER"],
                         ["Dropped demands", insertion_heuristic_data[1], insertion_heuristic_transfer_data[1],
                          taset_data[1]],
                         ["Demands served", insertion_heuristic_data[4], insertion_heuristic_transfer_data[4],
                          taset_data[4]],
                         ["Total transfers", insertion_heuristic_data[6], insertion_heuristic_transfer_data[6],
                          taset_data[6]],
                         ["Total veh. dist. traveled (km)", insertion_heuristic_data[0],
                          insertion_heuristic_transfer_data[0], taset_data[0]],
                         ["Total veh. dist. traveled per demand served (km)", insertion_heuristic_data[5],
                          insertion_heuristic_transfer_data[5], taset_data[5]],
                         ["Used car count", insertion_heuristic_data[2], insertion_heuristic_transfer_data[2],
                          taset_data[2]],
                         ["Average delay (s)", insertion_heuristic_data[3], insertion_heuristic_transfer_data[3],
                          taset_data[3]],

                         ])

# console results
print("COMPARISON:")
print()
print_table(output_table)