from amodsim.init import config

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


def load_edges_mapped_by_id():
	geojson = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
	edge_object_data = {}
	for edge in geojson['features']:
		edge_object_data[edge['properties']['id']] = edge['properties']


def load_edges_mapped_by_id(geojson: geojson.feature.FeatureCollection):
	edge_object_data = {}
	for edge in geojson['features']:
		edge_object_data[edge['properties']['id']] = edge['properties']


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





