
from amodsim.init import config

import matplotlib.pyplot as plt
import numpy as np
import amodsim.statistics.model.traffic_load as traffic_load
import amodsim.statistics.model.edges as edges
import amodsim.statistics.trafic_density_map as trafic_density_map

from roadmaptools.printer import print_info


edgePairs = edges.load_edge_pairs()

loads_1 = traffic_load.load_all_edges_load_history(config.comparison.experiment_1_dir + config.statistics.all_edges_load_history_file_name)
loads_2 = traffic_load.load_all_edges_load_history(config.comparison.experiment_2_dir + config.statistics.all_edges_load_history_file_name)
loads_3 = traffic_load.load_all_edges_load_history(config.comparison.experiment_3_dir + config.statistics.all_edges_load_history_file_name)
loads_4 = traffic_load.load_all_edges_load_history(config.comparison.experiment_4_dir + config.statistics.all_edges_load_history_file_name)

fig, axis = \
		plt.subplots(1, 4, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim', "aspect": 1.0}, figsize=(16, 3))

np.vectorize(trafic_density_map.set_axis_params)(axis)

axis[0].set_xlabel("a) No Ridesharing")
axis[1].set_xlabel("b) Insertion Heuristic")
axis[2].set_xlabel("c) VGA")
axis[3].set_xlabel("c) VGA limited")

print_info("Plotting No Ridesharing load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[0], loads_1["ALL"])

print_info("Plotting Insertion Heuristic load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[1], loads_2["ALL"])

print_info("Plotting VGA load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[2], loads_3["ALL"])

print_info("Plotting VGA limited load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[3], loads_4["ALL"])

# zoom
plt.axis([14308000, 14578000, 49970000, 50186000])

# plt.colorbar()

plt.savefig(config.images.traffic_density_map_comparison, bbox_inches='tight', transparent=True)

plt.show()