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
import roadmaptools.inout

from typing import List
from tqdm import tqdm


def export_group_size(group_size: int):
	roadmaptools.inout.save_csv(sorted[group_size], "group_data_size_{}.csv".format(group_size), append=True)
	sorted[group_size] = []


def generate_header(group_size: int) -> List[str]:
	header = []
	header.extend(["feasible", "onboard count", "veh lat", "veh lon"])
	for i in range(1, group_size + 1):
		header.extend(["req {} pickup lat".format(i), "req {} pickup lon".format(i), "req {} pickup max time".format(i),
					   "req {} dropoff lat".format(i), "req {} dropoff lon".format(i), "req {} dropoff max time".format(i)])
	return header


reader = roadmaptools.inout.load_csv("group_data.csv")

sorted = {}

for line in tqdm(reader):
	group_size = int((len(line) - 4) / 6)
	if group_size not in sorted:
		sorted[group_size] = []
		sorted[group_size].append(generate_header(group_size))

	sorted[group_size].append(line)

	if len(sorted[group_size]) >= 10_000:
		export_group_size(group_size)

#  close all remaining files
for size, _ in sorted.items():
	export_group_size(size)
