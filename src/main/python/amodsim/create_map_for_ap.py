
from amodsim.init import config, roadmaptools_config

import roadmaptools.inout
import roadmaptools.osmfilter
import roadmaptools.osmtogeojson
import roadmaptools.clean_geojson
import roadmaptools.sanitize
import roadmaptools.simplify_graph
import roadmaptools.prepare_geojson_to_agentpolisdemo
import roadmaptools.estimate_speed_from_osm

roadmaptools.osmfilter.filter_osm_file()

roadmaptools.osmtogeojson.convert_osm_to_geojson()

roadmaptools.clean_geojson.clean_geojson_files()

roadmaptools.sanitize.sanitize()

# do not simplify for liftago
roadmaptools.simplify_graph.simplify_geojson()

roadmaptools.estimate_speed_from_osm.estimate_posted_speed(roadmaptools_config.simplified_file,
														   roadmaptools_config.simplified_file_with_speed)

roadmaptools.prepare_geojson_to_agentpolisdemo.prepare_graph_to_agentpolisdemo()