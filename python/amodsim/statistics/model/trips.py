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
import roadmaptools.inout

from typing import Union, Dict, List
from pandas import DataFrame
from roadmaptools.printer import print_info

cols = ["start_time", "origin_lat", 'origin_lon', 'destination_lat', 'destination_lon']


def load() -> DataFrame:
	trips_data = pandas.read_csv(config.trips_path, names=cols, delimiter=' ')
	return trips_data





