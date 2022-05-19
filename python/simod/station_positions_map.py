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

import numpy as np
import matplotlib
import matplotlib.pyplot as plt
import roadmaptools.inout
import roadmaptools.plotting
import roadmaptools.utm
import matplotlib
import matplotlib.font_manager as fm
import simod.demand

from mpl_toolkits.axes_grid1.anchored_artists import AnchoredSizeBar
from matplotlib.colors import LogNorm
from roadmaptools.printer import print_info
from roadmaptools.graph import RoadGraph

HEATMAP_RESOLUTION = 200

# Prague
# MIN_LAT = 5_530_000
# MAX_LAT = 5_560_000
# MIN_LON = 445_000
# MAX_LON = 480_000

# Manhtattan
MIN_LAT = 4_505_000
MAX_LAT = 4_527_000
MIN_LON = 582_000
MAX_LON = 593_000

matplotlib.rcParams.update({'font.size': 18})


# DEMAND
fig, map_axis = plt.subplots(1, 1, figsize=(7, 7))
fig.subplots_adjust(wspace=0.01)
# map_axis = axes[0]
# colorbar_axis = axes[1]

# road network
fc = roadmaptools.inout.load_geojson(config.main_roads_graph_filepath)
xList, yList = roadmaptools.plotting.export_edges_for_matplotlib(roadmaptools.plotting.geojson_edges_iterator(fc))
map_axis.plot(xList, yList, linewidth=0.2, color='black', zorder=1)

# demand
#  - generate empty heatmap
lon_range = np.arange(MIN_LON, MAX_LON, HEATMAP_RESOLUTION)
lat_range = np.arange(MIN_LAT, MAX_LAT, HEATMAP_RESOLUTION)
map = np.zeros((len(lat_range), len(lon_range)))
# map = np.random.rand(len(lon_range), len(lat_range))

# - fill heatmap
demand_data = simod.demand.load(config.trips_path)
xlist, ylist = roadmaptools.plotting.export_nodes_for_matplotlib(demand_data[["from_lat", "from_lon"]].to_numpy())
for i, x in enumerate(xlist):
	y = ylist[i]
	x_index = round((x - MIN_LON) / HEATMAP_RESOLUTION)
	y_index = round((y - MIN_LAT) / HEATMAP_RESOLUTION)
	map[y_index][x_index] += config.trips_multiplier

# - mask empty cells
map = np.ma.masked_where(map == 0, map)
cmap = plt.cm.Reds
cmap.set_bad(color='white')

heatmap = map_axis.matshow(map, extent=(MIN_LON, MAX_LON, MIN_LAT, MAX_LAT), origin='lower', cmap=cmap,
			 norm=LogNorm(vmin=1, vmax=map.max()), alpha=0.8)


plt.colorbar(heatmap, orientation='horizontal', fraction=0.048, pad=0.01)

# scale bar
scalebar = AnchoredSizeBar(map_axis.transData,
                           5000, '5 km', 'lower right',
                           pad=0.3,
                           color='black',
                           frameon=True,
                           size_vertical=1,
							borderpad=0.5,
                           fontproperties=fm.FontProperties(size=18))
map_axis.add_artist(scalebar)

# remove ticks
map_axis.set_xticklabels([])
map_axis.set_yticklabels([])
map_axis.tick_params(
	which='both',  # both major and minor ticks are affected
	bottom=False,  # ticks along the bottom edge are off
	top=False,  # ticks along the top edge are off
	labelbottom=False, right=False, left=False, labelleft=False, labelright=False, labeltop=False)

map_axis.set_xlim(MIN_LON, MAX_LON)
map_axis.set_ylim(MIN_LAT, MAX_LAT)

plt.tight_layout(h_pad=1)

#matplotlib.rcParams['lines.linewidth'] = 0.1
plt.savefig(config.images.demand_heatmap, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)


# STATION POSITIONS
fig, axis = plt.subplots(1, 1, figsize=(7, 6))
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
xList, yList = roadmaptools.plotting.export_edges_for_matplotlib(roadmaptools.plotting.geojson_edges_iterator(fc))
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
# axis.set_xlim(min(x_list), max(x_list))
# axis.set_ylim(min(y_list), max(y_list))

# scale bar
scalebar = AnchoredSizeBar(axis.transData,
                           5000, '5 km', 'lower right',
                           pad=0.3,
                           color='black',
                           frameon=True,
                           size_vertical=1,
							borderpad=0.5,
                           fontproperties=fm.FontProperties(size=18))
axis.add_artist(scalebar)

axis.set_xlim(MIN_LON, MAX_LON)
axis.set_ylim(MIN_LAT, MAX_LAT)

plt.tight_layout(h_pad=1)

plt.savefig(config.images.stations, bbox_inches='tight', transparent=True, pad_inches=0.0)

plt.show()

