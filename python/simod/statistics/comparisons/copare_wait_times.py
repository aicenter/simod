#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of the SiMoD project.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
from python.simod.init import config

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
		= pandas.read_csv(experiment_folder + config.simod.statistics.occupancies_file_name, names=occupancy_cols)
	results = roadmaptools.inout.load_json(experiment_folder + config.simod.statistics.result_file_name)

	# in window
	tick_start = config.analysis.chosen_window_start * 10
	occupancy_data_window = occupancy_data[occupancy_data.tick > tick_start]
	avg_occupancy_demands_window = occupancy_data_window["occupancy"].mean()

	avg_km_driven = results["averageKmWithPassenger"] + results["averageKmToStartLocation"]\
					+ results["averageKmToStation"]

	service_columns = ["demand_time", "demand_id", "vehicle_id", "pickup_time", "dropoff_time", "min_possible_delay"]
	service_data \
		= pandas.read_csv(experiment_folder + config.simod.statistics.service_file_name, names=service_columns)
	delays = service_data["dropoff_time"] - service_data["demand_time"]
	min_delays = service_data["min_possible_delay"]
	prolongations = delays - min_delays
	avg_prolongation = round(Series.mean(prolongations) / 60000)



	return DataFrame([[max_delay, avg_occupancy_demands_window, avg_km_driven, avg_prolongation]], columns=result_cols)


expe_folder = config.simod_data_dir + "experiment/"

experiments = [
	[expe_folder + 'ridesharing_itsc_2018-ridesharing_on-90min-wait_time_7_min/', 7],
	[expe_folder + 'ridesharing_itsc_2018-ridesharing_on-90min-wait_time_10_min/', 10],
	[expe_folder + 'ridesharing_itsc_2018-ridesharing_on-90min-wait_time_12_min/', 12],
	[expe_folder + 'ridesharing_itsc_2018-ridesharing_on-90min-wait_time_15_min/', 15]
]

all_data = DataFrame(columns=result_cols)

for exp in experiments:
	exp_data = load_data_from_experiment(exp[0], exp[1])
	all_data = all_data.append(exp_data)

# occupancy graph
fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(2, 1.5))
# plt.setp(axes, xticks=[int(period) for period in all_data[ExperimentStats.MAX_DELAY.name]])
plt.setp(axes, xticks=range(7, 16, 2))
plt.setp(axes, yticks=[x * 0.1 for x in range(24, 30, 1)])

# axes.set_xlabel("Max delay time [min]")
# axes.set_ylabel("Avg. occupancy")
# axes.tick_params(axis='x')
# axes.set_xticks(all_data[ExperimentStats.MAX_DELAY.name])

axes.plot(all_data[ExperimentStats.MAX_DELAY.name], all_data[ExperimentStats.AVG_OCCUPANCY.name], marker='o')
plt.savefig(config.images.occupancy_comparison, bbox_inches='tight', transparent=True)

# km graph
fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))

axes.set_xlabel("Max delay time [min]")
axes.set_ylabel("Avg. distance traveled [km]")

axes.plot(all_data[ExperimentStats.MAX_DELAY.name], all_data[ExperimentStats.AVG_KM.name])
# plt.savefig(config.images.distance_comparison, bbox_inches='tight', transparent=True)

# service graph
fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))

axes.set_xlabel("Max delay time [min]")
axes.set_ylabel("Avg. delay time [min]")

axes.plot(all_data[ExperimentStats.MAX_DELAY.name], all_data[ExperimentStats.AVG_SERVICE_DELAY.name])
# plt.savefig(config.images.service_comparison, bbox_inches='tight', transparent=True)

plt.show()