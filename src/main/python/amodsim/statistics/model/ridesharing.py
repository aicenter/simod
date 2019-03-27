from init import config

import pandas

from pandas import DataFrame


def load(experiment_dir: str) -> DataFrame:
	data = pandas.read_csv(experiment_dir + config.statistics.ridesharing_stats_file_name)
	return data

