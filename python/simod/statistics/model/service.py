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
# from typing import

MILLISECONDS_IN_DENSITY_PERIOD = 600000


def load_dataframe(experiment_dir: str) -> DataFrame:
	service_columns = ["demand_time", "demand_id", "vehicle_id", "pickup_time", "dropoff_time", "min_possible_delay"]
	service_data \
		= pandas.read_csv(experiment_dir + config.statistics.service_file_name, names=service_columns)
	return service_data


def get_delays(service: DataFrame, window_only: bool = False, in_minutes: int = True) -> DataFrame:
	if window_only:
		start_demand_time = config.analysis.chosen_window_start * MILLISECONDS_IN_DENSITY_PERIOD
		service = service[service["demand_time"] >= start_demand_time]
	delays = service["dropoff_time"] - service["demand_time"] - service["min_possible_delay"]
	if in_minutes:
		delays = delays / 60000
	return delays
