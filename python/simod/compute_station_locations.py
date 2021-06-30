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
import scipy.spatial
import roadmaptools.inout
import roadmaptools.graph
import roadmaptools.utm

from typing import Dict, Tuple, Iterable
from tqdm import tqdm
from gurobipy import *
from geojson import FeatureCollection
from roadmaptools.printer import print_info


def _get_data_for_kdtree(fc: FeatureCollection, projection: roadmaptools.utm.TransposedUTM)\
		-> Tuple[Dict[Tuple[float,float],Dict], np.ndarray]:
	node_map = {}
	coord_array = np.zeros((len(fc['features']), 2))

	for i, point in enumerate(fc['features']):
		coords = point['geometry']['coordinates']
		coords_projected = roadmaptools.utm.wgs84_to_utm(coords[1], coords[0], projection)

		coord_array[i][0] = coords_projected[0]
		coord_array[i][1] = coords_projected[1]
		node_map[coords_projected] = point

	return node_map, coord_array

# MAX TRAVEL TIME COMPUTATION
# max prolongation
max_traveltime_in_seconds = config.ridesharing.max_prolongation_in_seconds

print("Maximum configured travel time: {} s", max_traveltime_in_seconds)

# we have to take the batch length into account
max_traveltime_in_seconds -= 30

# max delta between distance matrix and astar
max_traveltime_in_seconds -= 1

print("Maximum effective travel time: {} s", max_traveltime_in_seconds)

max_traveltime_in_ms = max_traveltime_in_seconds * 1000

# COMPUTE NODES WHERE DEMAND CAN APPEAR
trips = roadmaptools.inout.load_csv(config.trips_path, delimiter=' ')
nodes = roadmaptools.inout.load_geojson(config.agentpolis.map_nodes_filepath)
first_coords = nodes['features'][0]['geometry']['coordinates']
projection = roadmaptools.utm.TransposedUTM.from_gps(first_coords[1], first_coords[0])
node_map, coords = _get_data_for_kdtree(nodes, projection)
kdtree = scipy.spatial.cKDTree(coords)
nearest_nodes = set()
nearest_nodes_counts = {}

counter = 0
for trip in tqdm(trips, desc="creating the union of nearest nodes for all demands"):
	from_coords = (trip[2], trip[1])
	from_projected = roadmaptools.utm.wgs84_to_utm(float(from_coords[1]), float(from_coords[0]), projection)
	nearest_coords = coords[kdtree.query(from_projected)[1]]
	nearest_node_index = node_map[(nearest_coords[0], nearest_coords[1])]['properties']['index']
	nearest_nodes.add(nearest_node_index)
	nearest_nodes_counts[nearest_node_index] \
		= 1 if nearest_node_index not in nearest_nodes_counts else nearest_nodes_counts[nearest_node_index] + 1
	counter += 1
	# if counter > 100:
	# 	break


dm_iter = roadmaptools.inout.load_csv(config.distance_matrix_filepath)

# test = np.zeros((30000,30000))

dm_list = []
for row in tqdm(dm_iter, desc="loading distance matrix"):
	dm_list.append(np.array([int(traveltime) for traveltime in row]))

dm = np.array(dm_list)
# dm = np.recfromcsv(config.distance_matrix_filepath)
del dm_list



max_traveltime_filter = np.empty(dm.shape)
max_traveltime_filter[:] = max_traveltime_in_ms

rm = np.less(dm, max_traveltime_filter)

# rm = np.zeros((5,5), dtype=int)
# for row_index, row in enumerate(rm):
# 	row[row_index] = 1
#
# rm[1][2] = 1
# rm[2][2] = 1

# try:
# locations = [index for index, _ in enumerate(rm)]

# Create a new model
m = Model("Station Location Model")

# # vars
# stations = m.addVars(locations, obj=1, name="loc")
#
# # constr
# m.addConstrs((rm[l].sum() >= 1 for l in locations), "availability")

objective = LinExpr()
var_list = []

for row_index, _ in tqdm(enumerate(rm), desc="generating variables"):
		# Create variables
		var = m.addVar(vtype=GRB.BINARY, name="Station at position {}".format(row_index))
		var_list.append(var)

		# Set objective
		objective += var

m.setObjective(objective, GRB.MINIMIZE)

# Add constraint:
for column_index, _ in tqdm(enumerate(rm.T), desc="generating constraints"):
		if column_index in nearest_nodes:
			const = LinExpr()
			for row_index, from_location in enumerate(rm.T[column_index]):
				const.addTerms([from_location], [var_list[row_index]])
			m.addConstr(const, GRB.GREATER_EQUAL, 1, "Row constraint 0")

# Optimize model
m.optimize()

print('Obj: %g' % m.objVal)

# Print solution
stations = []
for index, value in tqdm(enumerate(m.getVars()), desc="preparing solution for export"):
		if value.x == 1:
			stations.append(index)

station_counts = {}

for index, count in nearest_nodes_counts.items():
		best_traveltime = 1_000_000
		nearest_station = 99999

		for station in stations:
			traveltime = dm[station][index]
			if traveltime < best_traveltime:
				best_traveltime = traveltime
				nearest_station = station

		station_counts[nearest_station] = count if nearest_station not in station_counts \
			else station_counts[nearest_station] + count

ratio = 10
# solution generation
solution = []
for index, count in station_counts.items():
		final_count = max(int(round(count / ratio)), 1)
		final_count += 100
		solution.append([str(index), str(final_count)])

roadmaptools.inout.save_csv(solution, config.station_position_filepath)

# except GurobiError as e:
# 	print('Error code ' + str(e.errno) + ": " + str(e))
#
# except AttributeError:
# 	print('Encountered an attribute error')

