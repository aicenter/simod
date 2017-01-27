
from __future__ import print_function, division

import matplotlib
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

from scripts import trip_loader
from scripts.config_loader import cfg as config

HOURS_IN_DAY = 24

HISTOGRAM_SAMPLES = 24


def format_timestamps(tick, sample_index):
    hours, rem = divmod(sample_index, HISTOGRAM_SAMPLES / HOURS_IN_DAY)
    minutes = rem * 60
    return "{:0>2}:{:0>2}".format(int(hours), int(minutes))

trips = trip_loader.trips


fig, axis = plt.subplots(figsize=(25, 14))

counts, bins, patches = axis.hist(trips[:,0], HISTOGRAM_SAMPLES)

axis.set_xticks(bins)
axis.xaxis.set_major_formatter(FuncFormatter(format_timestamps))


plt.savefig(config.images.trip_start_histogram, bbox_inches='tight', transparent=True, pad_inches=0)

plt.show()