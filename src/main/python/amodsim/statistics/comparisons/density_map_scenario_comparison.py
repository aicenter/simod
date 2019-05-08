
from amodsim.init import config #, roadmaptools_config

import matplotlib.pyplot as plt
import numpy as np
import amodsim.traffic_load as traffic_load
import amodsim.statistics.trafic_density_map as trafic_density_map

from roadmaptools.printer import print_info


edgePairs = traffic_load.load_edge_pairs()

#retrieve results of both experiments
loads_capacity_1 = traffic_load.load_all_edges_load_history(config.analysis.edge_load_ridesharing_off_filepath)

loads_capacity_5 = traffic_load.load_all_edges_load_history(config.analysis.edge_load_ridesharing_on_filepath)

fig, axis = \
		plt.subplots(1, 3, sharex=True, sharey=True, subplot_kw={"adjustable": 'box', "aspect": 1.0}, figsize=(120, 30))

np.vectorize(trafic_density_map.set_axis_params)(axis)

axis[0].set_xlabel("a) Present situation")
axis[1].set_xlabel("b) Superblock adaptation")

#print_info("Plotting present sitution load")
#trafic_density_map.plot_edges_optimized(edgePairs, axis[0], loads_capacity_1["DRIVING_TO_TARGET_LOCATION"])

print_info("Plotting MoD load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[0], loads_capacity_1["ALL"])

print_info("Plotting MoD with ridesharing load")
trafic_density_map.plot_edges_optimized(edgePairs, axis[1], loads_capacity_5["ALL"])

# zoom
plt.axis([14308000, 14578000, 49970000, 50186000])

# plt.colorbar()

plt.savefig(config.images.traffic_density_map_comparison, bbox_inches='tight', transparent=True)

plt.show()
