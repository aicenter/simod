#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of Amodsim project.
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
from amodsim.init import config

import pandas

from typing import Union
from pandas import DataFrame
from amodsim.statistics.model.vehicle_state import VehicleState

cols = ["time", "edge_id", "vehicle_state"]

MILLISECONDS_IN_DENSITY_PERIOD = 600000


def load(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + config.statistics.transit_file_name, names=cols, dtype={"edge_id": 'str'})
	return occupancy_data


def get_total_distance(transit: DataFrame, edges: DataFrame, window_only: bool = False,
					   vehicle_state: Union[VehicleState,None] = None) -> int:
	if window_only:
		tick_start = config.analysis.chosen_window_start * MILLISECONDS_IN_DENSITY_PERIOD
		transit = transit[transit.time > tick_start]

	if vehicle_state:
		transit = transit[transit.vehicle_state == vehicle_state.index]

	data = transit.set_index("edge_id").join(edges.set_index("id"))
	return data["length"].sum()
