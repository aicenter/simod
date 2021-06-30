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
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np


data = pd.Series(1, index=np.arange(20))

fig, axes = plt.subplots(1, 2, figsize=(6, 3), sharex=True, sharey=True)

fig.subplots_adjust(wspace=0.01)

plt.savefig(r'C:/Users/david/Downloads/test',  bbox_inches='tight', pad_inches=0.0)

plt.show()