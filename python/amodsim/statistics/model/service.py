from amodsim.init import config

import pandas

from pandas import DataFrame
# from typing import

MILLISECONDS_IN_DENSITY_PERIOD = 600000


def load_dataframe(experiment_dir: str) -> DataFrame:
	service_columns = ["demand_time", "demand_id", "vehicle_id", "pickup_time", "dropoff_time", "min_possible_delay"]
	service_data \
		= pandas.read_csv(experiment_dir + config.statistics.service_file_name, names=service_columns)
	return service_data


def get_delays(service: DataFrame, window_only: bool = False, in_minutes: int = True, bugfix = True) -> DataFrame:
	if window_only:
		start_demand_time = config.analysis.chosen_window_start * MILLISECONDS_IN_DENSITY_PERIOD
		service = service[service["demand_time"] >= start_demand_time]
	if bugfix:
		delays = service["dropoff_time"] - service["demand_time"] - service["min_possible_delay"] / 1000 # / 1000 is a temp bugfix, remove in new experiments!
	else:
		delays = service["dropoff_time"] - service["demand_time"] - service["min_possible_delay"]
	if in_minutes:
		delays = delays / 60000
	return delays
