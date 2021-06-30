#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of the SiMoD project.
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
'''
====================
Customized colorbars
====================

This example shows how to build colorbars without an attached mappable.
'''
from statistics.model.traffic_load import TrafficDensityLevel, CRITICAL_DENSITY

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