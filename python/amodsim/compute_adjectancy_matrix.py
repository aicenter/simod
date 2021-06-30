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
from amodsim.init import config

from typing import Callable
import pandas as pd

import roadmaptools.adjectancy
import roadmaptools.inout
import roadmaptools.estimate_speed_from_osm


class ComputeTravelTimeFromEdge:
	def __init__(self, vehicle_velocity: int, get_speed: Callable[[dict], float]):
		self.vehicle_velocity = vehicle_velocity
		self.get_speed = get_speed

	def __call__(self, edge: dict):
		"""
		Computes edge traveltime in milliseconds.
		:param edge: edge disctionary as loaded from geojson.
		:return: Edge traveltime in milliseconds.
		"""
		distance_cm = edge['properties']['length']
		posted_speed_cm_per_second = self.get_speed(edge)
		vehicle_max_speed_cm_per_second = self.vehicle_velocity * 1E2
		velocity_cm_per_s = min(posted_speed_cm_per_second, vehicle_max_speed_cm_per_second)
		return int(round(distance_cm / velocity_cm_per_s * 1E3))


class ComputeSpeedsFromUberData:
	def __init__(self, uber_speeds: pd.DataFrame):
		self.uber_speeds = uber_speeds
		self.missing_speed_record_count = 0
		self.found_speed_record_count = 0

	def __call__(self, edge: dict):
		from_id = edge["properties"]["from_osm_id"]
		to_id = edge["properties"]["to_osm_id"]
		way_id = edge["id"]
		filtered = speeds[(speeds["from"] == from_id) & (speeds["to"] == to_id) & (speeds["way"] == way_id)]
		if len(filtered.index) == 0:
			# raise Exception("No speed record per edge: from {} to {}".format(from_id, to_id))
			self.missing_speed_record_count += 1
			return roadmaptools.estimate_speed_from_osm.get_speed_per_second_from_edge(edge, 1E2)
		elif len(filtered.index) > 1:
			raise Exception("Multiple speed records per edge: from {} to {}".format(from_id, to_id))

		self.found_speed_record_count += 1
		return roadmaptools.estimate_speed_from_osm.get_speed_per_second(filtered.iloc[0]["speed"], "mph", 1E2)


nodes_path = config.agentpolis.map_nodes_filepath
edges_path = config.agentpolis.map_edges_filepath
out_path = config.data_dir + 'adj.csv'


if config.uber_speeds_file_path:
	speeds = roadmaptools.inout.load_csv_to_pandas(config.uber_speeds_file_path)
	speed_function = ComputeSpeedsFromUberData(speeds)
else:
	speed_function = lambda edge: roadmaptools.estimate_speed_from_osm.get_speed_per_second_from_edge(edge, 1E2, True)

travel_time_functor = ComputeTravelTimeFromEdge(config.vehicle_speed_in_meters_per_second, speed_function)

roadmaptools.adjectancy.create_adj_matrix(nodes_path, edges_path, out_path, travel_time_functor)

if config.uber_speeds_file_path:
	print("Number of missing records: {}".format(speed_function.missing_speed_record_count))
	print("Number of found records: {}".format(speed_function.found_speed_record_count))
