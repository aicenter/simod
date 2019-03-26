from init import config

import pandas
import roadmaptools.inout

from pandas import DataFrame

cols = ["id", "length"]
# filename = "{}{}.json".format(config.edges_file_path, '-simplified' if config.simplify_graph else {})


def load_table() -> DataFrame:
	# json = roadmaptools.inout.load_json(filename)
	geojson = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
	data = DataFrame(([int(edge['properties']['id']), int(edge['length'])] for edge in geojson['features']), columns=cols)
	return data


