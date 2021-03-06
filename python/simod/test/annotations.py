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
import numpy as np
import matplotlib.pyplot as plt

fig, ax = plt.subplots()
x = np.linspace(0,1)
ax.plot(x, x, 'o')

for xi in x:
	ax.annotate('default line', xy=(xi,xi), xytext=(60,0),
				textcoords="offset pixels",
            arrowprops={'arrowstyle': '-', 'shrinkB': 50}, va='center')

plt.show()
