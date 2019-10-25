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