#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of Amodsim project.
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

from pandas import DataFrame, Series

df = DataFrame([[0, 'r', 14],
				[1, 'r', 10],
				[0, 'b', 5],
				[2, 'l', 6],
				[2, 'k', 3]], columns=["first", "second", "third"])

# g = df.groupby("first")
#
# fig, axes = plt.subplots(1,1)
#
# axes.plot(g["third"].agg(np.mean))

a = df["first"] - df["third"]

b = 1

# plt.show()