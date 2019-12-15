from __future__ import print_function, division

import matplotlib
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter

from scripts import trip_loader

HISTOGRAM_SAMPLES = 40


def to_percent(y, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = str(100 * y)

    # The percent symbol needs escaping in latex
    if matplotlib.rcParams['text.usetex'] is True:
        return s + r'$\%$'
    else:
        return s + '%'



trips = trip_loader.trips

intervals = (trips[:,1] - trips[:,0]) / 60

plt.hist(intervals, HISTOGRAM_SAMPLES, normed=True)

# Create the formatter using the function to_percent. This multiplies all the
# default labels by 100, making them all percentages
formatter = FuncFormatter(to_percent)

# Set the formatter
plt.gca().yaxis.set_major_formatter(formatter)


plt.xlabel('Duration in minutes')
plt.ylabel('% of trips')
plt.grid(True)

plt.show()