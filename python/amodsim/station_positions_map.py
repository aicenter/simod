from amodsim.init import config

import matplotlib.pyplot as plt
import roadmaptools.inout
import roadmaptools.plotting

from roadmaptools.printer import print_info
from roadmaptools.graph import RoadGraph


fig, axis = plt.subplots(1, 1, figsize=(4, 3))
fig.subplots_adjust(wspace=0.01)

axis.set_xticklabels([])
axis.set_yticklabels([])
axis.tick_params(
	which='both',  # both major and minor ticks are affected
	bottom=False,  # ticks along the bottom edge are off
	top=False,  # ticks along the top edge are off
	labelbottom=False, right=False, left=False, labelleft=False, labelright=False, labeltop=False)

# road network
fc = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
xList, yList = roadmaptools.plotting.export_for_matplotlib(roadmaptools.plotting.geojson_iterator(fc))
axis.plot(xList, yList, linewidth=0.2, color='gray', zorder=1)

# stations
stations = roadmaptools.inout.load_csv(config.station_position_filepath)
nodes = roadmaptools.inout.load_geojson(config.agentpolis.map_nodes_filepath)
node_dict = {}
for item in nodes['features']:
	node_dict[item['properties']['index']] = item
x_list = []
y_list = []
for station in stations:
	index = int(station[0])
	x_list.append(node_dict[index]['geometry']['coordinates'][0])
	y_list.append(node_dict[index]['geometry']['coordinates'][1])
axis.scatter(x_list, y_list, edgecolors='red', facecolors='white', marker='o', s=20, zorder=2)
axis.set_xlim(min(x_list), max(x_list))
axis.set_ylim(min(y_list), max(y_list))

plt.savefig(config.images.stations, bbox_inches='tight', transparent=True, pad_inches=0.0)

plt.show()

