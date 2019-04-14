from amodsim.init import config

import pandas

from pandas import DataFrame

occupancy_cols = ["tick", "vehicle_id", "occupancy"]


def load(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.occupancies_file_name, names=occupancy_cols)
	return occupancy_data


def get_occupancies(data: DataFrame, window_only: bool = False):
	if window_only:
		tick_start = config.analysis.chosen_window_start * 10
		data = data[data.tick > tick_start]

	return data["occupancy"]