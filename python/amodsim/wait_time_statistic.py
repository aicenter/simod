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



