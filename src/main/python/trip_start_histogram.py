
from __future__ import print_function, division

import matplotlib
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

from scripts import trip_loader
from scripts.config_loader import cfg as config

HOURS_IN_DAY = 24

MINUTES_IN_HOUR = 60

HISTOGRAM_SAMPLES = 96


def format_timestamps(tick, tick_index):
    hours, rem = divmod(tick_index * tick_interval, HISTOGRAM_SAMPLES / HOURS_IN_DAY)
    minutes = rem * MINUTES_IN_HOUR / (HISTOGRAM_SAMPLES / HOURS_IN_DAY)
    return "{:0>2}:{:0>2}".format(int(hours), int(minutes))

trips = trip_loader.trips


fig, axis = plt.subplots(figsize=(6, 4))

counts, bins, patches = axis.hist(trips[:,0], HISTOGRAM_SAMPLES)

tick_interval = int(HISTOGRAM_SAMPLES / 16)

axis.set_xticks(bins[0::tick_interval])
axis.xaxis.set_major_formatter(FuncFormatter(format_timestamps))
labels = axis.get_xticklabels()
plt.setp(labels, rotation=90)
plt.subplots_adjust(bottom=0.2)
axis.get_xaxis().set_tick_params(direction='out')


plt.savefig(config.images.trip_start_histogram, bbox_inches='tight', transparent=True, pad_inches=0)

plt.show()