from amodsim.init import config

import matplotlib.pyplot as plt
import roadmaptools.inout
import roadmaptools.plotting
import roadmaptools.utm
import matplotlib.font_manager as fm

from mpl_toolkits.axes_grid1.anchored_artists import AnchoredSizeBar
from roadmaptools.printer import print_info
from roadmaptools.graph import RoadGraph


fig, axis = plt.subplots(1, 1, figsize=(8, 6))
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
projection = None
for station in stations:
	index = int(station[0])

	if not projection:
		projection = roadmaptools.utm.TransposedUTM.from_gps(node_dict[index]['geometry']['coordinates'][1],
															 node_dict[index]['geometry']['coordinates'][0])

	coords = roadmaptools.utm.wgs84_to_utm(node_dict[index]['geometry']['coordinates'][1],
										   node_dict[index]['geometry']['coordinates'][0])

	x_list.append(coords[0])
	y_list.append(coords[1])
axis.scatter(x_list, y_list, edgecolors='red', facecolors='white', marker='o', s=40, zorder=2)
axis.set_xlim(min(x_list), max(x_list))
axis.set_ylim(min(y_list), max(y_list))

# scale bar
scalebar = AnchoredSizeBar(axis.transData,
                           5000, '5 km', 'lower right',
                           pad=0.3,
                           color='black',
                           frameon=True,
                           size_vertical=1,
							borderpad=0.5,
                           fontproperties=fm.FontProperties(size=10))

axis.add_artist(scalebar)

plt.savefig(config.images.stations, bbox_inches='tight', transparent=True, pad_inches=0.0)

plt.show()

