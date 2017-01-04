
from __future__ import print_function, division

import matplotlib
import matplotlib.pyplot as plt

from scripts import trip_loader

trips = trip_loader.trips

HISTOGRAM_SAMPLES = 40

plt.hist(trips[:,0] / 60, HISTOGRAM_SAMPLES)

plt.show()