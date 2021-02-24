#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of Amodsim project.
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

from amodsim.init import config

import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import amodsim.statistics.model.traffic_load as traffic_load
import amodsim.statistics.model.edges as edges
import amodsim.statistics.trafic_density_map as trafic_density_map

from roadmaptools.printer import print_info
from amodsim.statistics.model.vehicle_state import VehicleState

FONT_SIZE = 14

matplotlib.rcParams.update({'font.size': FONT_SIZE})

edgePairs = edges.load_edge_pairs()

loads_1 = traffic_load.load_all_edges_load_history(config.comparison.experiment_1_dir + config.statistics.all_edges_load_history_file_name)
loads_2 = traffic_load.load_all_edges_load_history(config.comparison.experiment_2_dir + config.statistics.all_edges_load_history_file_name)
loads_3 = traffic_load.load_all_edges_load_history(config.comparison.experiment_3_dir + config.statistics.all_edges_load_history_file_name)
loads_4 = traffic_load.load_all_edges_load_history(config.comparison.experiment_4_dir + config.statistics.all_edges_load_history_file_name)

# loads_1 = traffic_load.load_all_edges_load_history(config.comparison.experiment_5_dir + config.statistics.all_edges_load_history_file_name)
# loads_2 = traffic_load.load_all_edges_load_history(config.comparison.experiment_6_dir + config.statistics.all_edges_load_history_file_name)
# loads_3 = traffic_load.load_all_edges_load_history(config.comparison.experiment_7_dir + config.statistics.all_edges_load_history_file_name)
# loads_4 = traffic_load.load_all_edges_load_history(config.comparison.experiment_8_dir + config.statistics.all_edges_load_history_file_name)


# subplot_kw={"adjustable": 'datalim', "aspect": 1.0}
fig, axis = \
		plt.subplots(1, 5, sharex=True, sharey=True, figsize=(15, 3))

fig.subplots_adjust(wspace=0.01)

np.vectorize(trafic_density_map.set_axis_params)(axis)

axis[0].set_xlabel("a) Present State")
axis[1].set_xlabel("b) No Ridesharing")
axis[2].set_xlabel("c) Insertion Heuristic")
axis[3].set_xlabel("d) VGA (optimal)")
axis[4].set_xlabel("e) VGA (limited)")

print_info("Plotting Present State load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[0], loads_1[VehicleState.DRIVING_TO_TARGET_LOCATION.name])

print_info("Plotting No Ridesharing load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[1], loads_1["ALL"])

print_info("Plotting Insertion Heuristic load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[2], loads_2["ALL"])

print_info("Plotting VGA load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[3], loads_3["ALL"])

print_info("Plotting VGA limited load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[4], loads_4["ALL"])

# zoom
plt.axis([14308000, 14578000, 49970000, 50186000])

# plt.colorbar()

plt.savefig(config.images.traffic_density_map_comparison, bbox_inches='tight', transparent=True, pad_inches=0.0)

plt.show()