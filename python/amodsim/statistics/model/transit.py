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
