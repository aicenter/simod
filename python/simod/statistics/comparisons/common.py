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

from enum import Enum


class Experiment(Enum):
	PRESENT_STATE = (0, "grey", "ooo", "Present State", 'o')
	NO_RIDESHARING = (1, "red", "+++", "No Ridesharing", '+')
	IH = (2, "green", "///", "Insertion Heuristic", '/')
	VGA = (3, "blue", "\\\\\\", "VGA (optimal)", 'v')
	VGA_LIMITED = (4, "orange", "***", "VGA (limited)", '*')
	VGA_PNAS = (4, "darkcyan", "---", "VGA (PNAS)", 's')

	def __init__(self, index, color, pattern, label, marker):
		self.color = color
		self.index = index
		self.pattern = pattern
		self.label = label
		self.marker = marker


labels = [exp.label for exp in Experiment]
colors = [exp.color for exp in Experiment]
hatches = [exp.pattern for exp in Experiment]