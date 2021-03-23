from amodsim.init import config

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
from amodsim.statistics.model import demand_trips

trips_data = demand_trips.load()

# trip start histogram
fig, axis = plt.subplots(figsize=(6, 4))

trip_data_minutes = trips_data / 60

print("Max trip length: {}".format(trip_data_minutes.max()))

bins = np.arange(0.49, 25.51, 1)

_counts, _bins, patches = axis.hist(trip_data_minutes, bins)

# tick_interval = int(HISTOGRAM_SAMPLES / 10)

# axis.set_xticks(bins[0::tick_interval])
# axis.xaxis.set_major_formatter(FuncFormatter(format_timestamps))
labels = axis.get_xticklabels()
plt.subplots_adjust(bottom=0.2)
axis.get_xaxis().set_tick_params(direction='out')

axis.set_xlabel("Trip duration [min]")

plt.savefig(config.images.demand_trip_duration_histogram, bbox_inches='tight', transparent=True, pad_inches=0)

plt.show()