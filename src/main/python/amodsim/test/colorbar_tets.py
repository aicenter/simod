'''
====================
Customized colorbars
====================

This example shows how to build colorbars without an attached mappable.
'''
from amodsim.traffic_load import TrafficDensityLevel, CRITICAL_DENSITY

import matplotlib.pyplot as plt
import matplotlib as mpl
import matplotlib.colorbar

# Make a figure and axes with dimensions as desired.
fig, axes = plt.subplots(1,1, figsize=(4, 3))

x = range(0,10)
y = [0.5 * x for x in x]

axes.plot(x, y)
# The third example illustrates the use of custom length colorbar
# extensions, used on a colorbar with discrete intervals.

ax3 = fig.add_axes([0.9, 0.10, 0.05, 0.8])
cmap = mpl.colors.ListedColormap([level.color for level in reversed(TrafficDensityLevel)])
# ticks = [level.max_level for level in TrafficDensityLevel]
bounds = [level.max_level for level in TrafficDensityLevel]
bounds[0] = 5
bounds.append(0)
bounds = list(reversed(bounds))
bounds = [bound * CRITICAL_DENSITY for bound in bounds]

ticks = [str(bound) for bound in bounds]
ticks[len(ticks) - 1] = 'inf'

norm = mpl.colors.BoundaryNorm(bounds, cmap.N)



cb3 = mpl.colorbar.ColorbarBase(ax3, cmap=cmap,
                                # ticks=ticks,
								# values=ticks,
								norm=norm,
								boundaries=bounds,
                                orientation='vertical')
cb3.set_label('vehicle per meter')

ax3.set_yticklabels(ticks)

plt.show()