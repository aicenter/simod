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

import roadmaptools.adjectancy


def compute_traveltime_from_edge(edge: dict):
	"""
	Computes edge traveltime in milliseconds.
	:param edge: edge disctionary as loaded from geojson.
	:return: Edge traveltime in milliseconds.
	"""
	distance_cm = edge['properties']['length']
	posted_speed_cm_per_second = int(round(edge['properties']['maxspeed'] / 3.6 * 1E2))
	vehicle_max_speed_cm_per_second = compute_traveltime_from_edge.vehicle_velocity * 1E2
	velocity_cm_per_s = min(posted_speed_cm_per_second, vehicle_max_speed_cm_per_second)
	return int(round(distance_cm / velocity_cm_per_s * 1E3))


compute_traveltime_from_edge.vehicle_velocity = config.vehicle_speed_in_meters_per_second


nodes_path = config.agentpolis.map_nodes_filepath
edges_path = config.agentpolis.map_edges_filepath
out_path = out_path = config.data_dir + 'adj.csv'

roadmaptools.adjectancy.create_adj_matrix(nodes_path, edges_path, out_path, compute_traveltime_from_edge)
