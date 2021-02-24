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

import roadmaptools.inout
import roadmaptools.filter

ROAD_TYPES_TO_DISPLAY = {"motorway", "trunk", "primary", "secondary",}
# ROAD_TYPES_TO_DISPLAY = {"motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link", "secondary",
# 						 "secondary_link"}
							# |tertiary|tertiary_link|unclassified|unclassified_link|residential|residential_link|living_street)")


def main_roads_filter(edge: dict) -> bool:
	road_type = edge['properties']['highway']
	return road_type in ROAD_TYPES_TO_DISPLAY


edges = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)

roadmaptools.filter.filter_edges(edges, main_roads_filter)

roadmaptools.inout.save_geojson(edges, config.main_roads_graph_filepath)