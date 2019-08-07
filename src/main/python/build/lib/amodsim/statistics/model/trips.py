from init import config

import pandas
import roadmaptools.inout

from typing import Union, Dict, List
from pandas import DataFrame
from roadmaptools.printer import print_info

cols = ["start_time", "origin_lat", 'origin_lon', 'destination_lat', 'destination_lon']


def load() -> DataFrame:
	trips_data = pandas.read_csv(config.trips_path, names=cols, delimiter=' ')
	return trips_data





