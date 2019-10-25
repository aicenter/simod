from amodsim.init import config

import pandas

from pandas import DataFrame
# from typing import


def load(filepath: str) -> DataFrame:
	columns = ["demand_time", "from_lat", "from_lon", "to_lat", "to_lon"]
	demand_data \
		= pandas.read_csv(filepath, names=columns, delimiter=" ")
	return demand_data
