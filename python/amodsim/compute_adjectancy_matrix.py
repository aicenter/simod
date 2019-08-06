from amodsim.init import config

import roadmaptools.adjectancy


def compute_traveltime_from_edge(edge: dict):
	"""
	Computes edge traveltime in miliseconds.
	:param edge: edge disctionary as loaded from geojson.
	:return: Edge traveltime in miliseconds.
	"""
	distance = edge['properties']['length']
	velocity = min(compute_traveltime_from_edge.vehicle_velocity, edge['properties']['maxspeed'] / 3.6)
	return int(round(distance / velocity * 1000))


compute_traveltime_from_edge.vehicle_velocity = config.vehicle_speed_in_meters_per_second


nodes_path = config.agentpolis.map_nodes_filepath
edges_path = config.agentpolis.map_edges_filepath
out_path = r'C:\AIC data\Shared\amod-data\VGA Evaluation\maps/adj.csv'

roadmaptools.adjectancy.create_adj_matrix(nodes_path, edges_path, out_path, compute_traveltime_from_edge)