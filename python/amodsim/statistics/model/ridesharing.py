from amodsim.init import config

import pandas
import roadmaptools.inout

from pandas import DataFrame


def load(experiment_dir: str) -> DataFrame:
	data = roadmaptools.inout.load_csv_to_pandas(experiment_dir + config.statistics.ridesharing_stats_file_name)
	return data

