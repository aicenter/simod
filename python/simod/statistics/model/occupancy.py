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
from simod.init import config

import pandas

from pandas import DataFrame

occupancy_cols = ["tick", "vehicle_id", "occupancy"]
combined_occupancy_cols = ["tick", "vehicle_id", "people_occupancy", "package_occupancy"]


def load(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.occupancies_file_name, names=occupancy_cols)
	return occupancy_data

def load_packages(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.packages_occupancies_file_name, names=occupancy_cols)
	return occupancy_data

def load_combined_occupancy(experiment_dir: str) -> DataFrame:
	occupancy_data = pandas.read_csv(experiment_dir + config.statistics.combined_occupancies_file_name, names=combined_occupancy_cols)
	return occupancy_data

def load_no_people(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.no_people_occupancies_file_name, names=occupancy_cols)
	return occupancy_data

def load_people_onboard(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.people_onboard_occupancies_file_name, names=occupancy_cols)
	return occupancy_data

def filter_window(data: DataFrame) -> DataFrame:
	tick_start = config.analysis.chosen_window_start * 10
	return data[data.tick > tick_start]


def get_occupancies(data: DataFrame, window_only: bool = False):
	if window_only:
		tick_start = config.analysis.chosen_window_start * 10
		data = data[data.tick > tick_start]

	return data["occupancy"]


def get_combined_occupancies(data: DataFrame, window_only: bool = False):
	if window_only:
		tick_start = config.analysis.chosen_window_start * 10
		data = data[data.tick > tick_start]

	return [data["people_occupancy"], data["package_occupancy"]]