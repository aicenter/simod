
from amodsim.init import config

import time

import roadmaptools.inout
import roadmaptools.osmfilter
import roadmaptools.osmtogeojson
import roadmaptools.clean_geojson
import roadmaptools.sanitize
import roadmaptools.simplify_graph
import roadmaptools.prepare_geojson_to_agentpolisdemo
import roadmaptools.estimate_speed_from_osm
from roadmaptools.printer import print_info

roadmaptools.osmfilter.filter_osm_file()

roadmaptools.osmtogeojson.convert_osm_to_geojson()

roadmaptools.clean_geojson.clean_geojson_files()

roadmaptools.sanitize.sanitize()

# do not simplify for liftago
roadmaptools.simplify_graph.simplify_geojson()

roadmaptools.estimate_speed_from_osm.estimate_posted_speed(roadmaptools_config.simplified_file,
														   roadmaptools_config.simplified_file_with_speed)

roadmaptools.prepare_geojson_to_agentpolisdemo.prepare_graph_to_agentpolisdemo()

print_info('Preparing files for agentpolis-demo... ', end='')
start_time = time.time()

geojson_file = roadmaptools.inout.load_geojson(config.map_dir + 'map-simplified-speed.geojson')
edges, nodes = roadmaptools.prepare_geojson_to_agentpolisdemo.get_nodes_and_edges_for_agentpolisdemo(geojson_file)
roadmaptools.inout.save_geojson(nodes, config.map_dir + 'nodes.geojson')
roadmaptools.inout.save_geojson(edges, config.map_dir + 'edges.geojson')

print_info('done. (%.2f secs)' % (time.time() - start_time))