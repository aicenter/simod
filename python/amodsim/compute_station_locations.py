from amodsim.init import config

import numpy as np
import roadmaptools.inout

from tqdm import tqdm
from gurobipy import *
from roadmaptools.printer import print_info


dm_iter = roadmaptools.inout.load_csv(config.distance_matrix_filepath)

# test = np.zeros((30000,30000))

dm_list = []
for row in tqdm(dm_iter):
	dm_list.append(np.array([int(traveltime) for traveltime in row]))

dm = np.array(dm_list)
# dm = np.recfromcsv(config.distance_matrix_filepath)
del dm_list

max_traveltime_in_ms = config.ridesharing.max_prolongation_in_seconds * 1000

max_traveltime_filter = np.empty(dm.shape)
max_traveltime_filter[:] = max_traveltime_in_ms

rm = np.less(dm, max_traveltime_filter)

# rm = np.zeros((5,5), dtype=int)
# for row_index, row in enumerate(rm):
# 	row[row_index] = 1
#
# rm[1][2] = 1
# rm[2][2] = 1

try:
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
	for row_index, _ in tqdm(enumerate(rm), desc="generating constraints"):
		const = LinExpr()
		for neighbor_index, neigbor in enumerate(rm[row_index]):
			const.addTerms([neigbor], [var_list[neighbor_index]])
		m.addConstr(const, GRB.GREATER_EQUAL, 1, "Row constraint 0")

	# Optimize model
	m.optimize()

	# Print solution
	solution = []
	for index, value in tqdm(enumerate(m.getVars()), desc="preparing solution for export"):
		# print('%s %g' % (v.varName, v.x))
		if value.x == 1:
			solution.append([str(index)])

	print('Obj: %g' % m.objVal)

	roadmaptools.inout.save_csv(solution, config.station_position_filepath)

except GurobiError as e:
	print('Error code ' + str(e.errno) + ": " + str(e))

except AttributeError:
	print('Encountered an attribute error')

