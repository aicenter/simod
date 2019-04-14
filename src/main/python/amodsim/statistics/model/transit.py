from amodsim.init import config

import pandas

from pandas import DataFrame

cols = ["time", "edge_id", "trip_id"]

MILLISECONDS_IN_DENSITY_PERIOD = 600000


def load(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.transit_file_name, names=cols)
	return occupancy_data


def get_total_distance(transit: DataFrame, edges: DataFrame, window_only: bool = False) -> int:
	if window_only:
		tick_start = config.analysis.chosen_window_start * MILLISECONDS_IN_DENSITY_PERIOD
		transit = transit[transit.time > tick_start]

	data = transit.set_index("edge_id").join(edges.set_index("id"))
	return data["length"].sum()
