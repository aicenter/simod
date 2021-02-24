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
from __future__ import print_function, division

import numpy as np
import datetime

from scripts.config_loader import cfg as config

milis_start = config.analysis.chosen_window_start / 144 * 24 * 60 * 60 * 1000
milis_end = config.analysis.chosen_window_end / 144 * 24 * 60 * 60 * 1000

time_table = np.genfromtxt(config.agentpolis.statistics.demand_service_statistic_file_path, delimiter=',')

intervals = time_table[:,1] - time_table[:,0]

time_table = np.append(time_table, intervals[:,None], 1)

time_table_window = time_table[(time_table[:,0] > milis_start) & (time_table[:,0] < milis_end)]

average_interval = np.average(time_table[:,2])

average_interval_window = np.average(time_table_window[:,2])

print("Average wait time in window: {0}".format(datetime.datetime.fromtimestamp(average_interval_window / 1000).strftime("%M:%S")))

print("Overall average wait time: {0}".format(datetime.datetime.fromtimestamp(average_interval / 1000).strftime("%M:%S")))



