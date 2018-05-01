from amodsim.init import config, roadmaptools_config

import pandas
import matplotlib.pyplot as plt
import roadmaptools.inout

from enum import Enum
from pandas import DataFrame, Series


class ExperimentStats(Enum):
	MAX_DELAY = (0)
	AVG_OCCUPANCY = (1)
	AVG_KM = (2)
	AVG_SERVICE_DELAY = (3)

	def __init__(self, index):
		self.index = index


result_cols = [ExperimentStats.MAX_DELAY.name,
	ExperimentStats.AVG_OCCUPANCY.name,
			ExperimentStats.AVG_KM.name,
			ExperimentStats.AVG_SERVICE_DELAY.name]


def load_data_from_experiment(experiment_folder: str, max_delay: int):
	occupancy_cols = ["tick", "vehicle_id", "occupancy"]
	occupancy_data\
		= pandas.read_csv(experiment_folder + config.amodsim.statistics.occupancies_file_name, names=occupancy_cols)
	results = roadmaptools.inout.load_json(experiment_folder + config.amodsim.statistics.result_file_name)

	# in window
	tick_start = config.analysis.chosen_window_start * 10
	occupancy_data_window = occupancy_data[occupancy_data.tick > tick_start]
	avg_occupancy_demands_window = occupancy_data_window["occupancy"].mean() / occupancy_data_window.shape[0]

	avg_km_driven = results["averageKmWithPassenger"] + results["averageKmToStartLocation"]\
					+ results["averageKmToStation"]

	service_columns = ["demand_time", "demand_id", "vehicle_id", "pickup_time", "dropoff_time", "min_possible_delay"]
	service_data \
		= pandas.read_csv(experiment_folder + config.amodsim.statistics.service_file_name, names=service_columns)
	delays = service_data["dropoff_time"] - service_data["demand_time"]
	min_delays = service_data["min_possible_delay"]
	prolongations = delays - min_delays
	avg_prolongation = Series.mean(prolongations)

	return DataFrame([[max_delay, avg_occupancy_demands_window, avg_km_driven, avg_prolongation]], columns=result_cols)


expe_folder = config.amodsim_data_dir + "experiment/"

experiments = [
	[expe_folder + 'ridesharing_itsc_2018-ridesharing_on-90min-wait_time_10_min/', 10]
]

all_data = DataFrame(columns=result_cols)

for exp in experiments:
	exp_data = load_data_from_experiment(exp[0], exp[1])
	all_data = all_data.append(exp_data)

# occupancy graph
fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.plot(all_data[ExperimentStats.MAX_DELAY.name], all_data[ExperimentStats.AVG_OCCUPANCY.name])
plt.savefig(config.images.occupancy_comparison, bbox_inches='tight', transparent=True)

# km graph

# service graph

plt.show()