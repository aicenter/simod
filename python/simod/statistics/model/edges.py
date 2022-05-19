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
import roadmaptools.inout
import geojson
from typing import Union, Dict, List
from pandas import DataFrame
from roadmaptools.printer import print_info

cols = ["id", "length"]
# dtypes = ['int', 'int', 'int']
# filename = "{}{}.json".format(config.edges_file_path, '-simplified' if config.simplify_graph else {})


# def load_edges() -> Union[Dict, List]:
# 	print_info("loading edges")
# 	modifier = "-simplified" if config.simplify_graph else ""
# 	# json_file = open(config.amodsim.edges_file_path + modifier + ".json", 'r')
# 	# return json.loads(json_file.read())
# 	return roadmaptools.inout.load_json(config.edges_file_path + modifier + ".json")


# def load_edges_mapped_by_id():
# 	geojson = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
# 	edge_object_data = {}
# 	for edge in geojson['features']:
# 		edge_object_data[edge['properties']['id']] = edge['properties']


def load_edges_mapped_by_id(geojson_data: geojson.feature.FeatureCollection = None):
	if not geojson_data:
		geojson_data = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
	edge_object_data = {}
	for edge in geojson_data['features']:
		edge_object_data[edge['properties']['id']] = edge['properties']
	return edge_object_data


def load_edge_pairs() -> Union[Dict, List]:
	print_info("loading edge pairs")
	modifier = "-simplified" if config.simplify_graph else ""
	# jsonFile = open(config.amodsim.edge_pairs_file_path + modifier + ".json", 'r')
	# return json.loads(jsonFile.read())
	return roadmaptools.inout.load_json(config.edge_pairs_file_path + modifier + ".json")


def make_data_frame(geojson: geojson.feature.FeatureCollection) -> DataFrame:
	return DataFrame(([str(edge['properties']['id']), int(edge['properties']['length'])] for edge in geojson['features']), columns=cols)


def load_table() -> DataFrame:
	# json = roadmaptools.inout.load_json(filename)
	geojson = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
	return make_data_frame(geojson)





