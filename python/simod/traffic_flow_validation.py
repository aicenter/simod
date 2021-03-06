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
from __future__ import print_function, division

import json
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import time
import subprocess
import os
from sys import getsizeof
import matplotlib.pyplot as plt

from scripts.config_loader import cfg as config
from scripts.printer import print_info, print_table


def wccount(filename):
    out = subprocess.Popen(['wc', '-l', filename], stdout=subprocess.PIPE, stderr=subprocess.STDOUT ).communicate()[0]
    return int(out.partition(b' ')[0])

if os.path.isfile(config.analysis.transit_tsk_validation_cache):
    print_info("loading from cache")
    result = pd.read_pickle(config.analysis.transit_tsk_validation_cache)
else:
    print_info("loading tsk transit")
    tsk_transit = pd.read_csv(config.analysis.map_tsk_matching_file_path)
    # tsk_transit = np.genfromtxt(config.analysis.map_tsk_matching_file_path, delimiter=',')

    print_info("loading agentpolis transit")
    ap_transit = pd.read_csv(config.agentpolis.statistics.transit_statistic_file_path,
                             names=['time', 'osm_id', 'trip_id'], header=None)

    print_info("agentpolis transit - deleting timings")
    ap_transit.drop('time', axis=1, inplace=True)

    print_info("joining results")
    tsk_ap_table = pd.merge(tsk_transit.ix[:,['join_NO', 'osm_id']], ap_transit, how='inner', on="osm_id")
    # del tsk_transit
    del ap_transit

    print_info("aggregating results")
    tsk_ap_table_grouped = tsk_ap_table.groupby(['join_NO'])['trip_id'].nunique().reset_index(name='counts')

    tsk_all_cols_grouped \
        = tsk_transit.ix[:,['join_NO', 'join_VSE2015', 'join_POM2015', 'join_BUSMHD2015', 'join_TRAM2015']]\
        .groupby('join_NO').last().reset_index()

    result = pd.merge(tsk_all_cols_grouped, tsk_ap_table_grouped, how='inner', on="join_NO")

    result['counts'].apply(np.vectorize(lambda x: int(round(x * config.analysis.analysis_multiplier))))


    # print_info("grouping by osm_id")
    # # ap_daily_counts = ap_transit.groupby('osm_id').size()
    # # ap_daily_counts = pd.DataFrame(ap_transit.groupby('osm_id').size().rename('counts'))
    # # ap_daily_counts = ap_transit.groupby('osm_id')[['osm_id']].agg('count')
    # ap_daily_counts = ap_transit.groupby('osm_id').size().reset_index(name='counts')
    # del ap_transit
    #
    # print_info("joining results")
    # result = pd.merge(tsk_transit, ap_daily_counts, how='inner', on="osm_id")
    # del tsk_transit
    # del ap_daily_counts

    print_info("saving to cache")
    result.to_pickle(config.analysis.transit_tsk_validation_cache)

# result = result.groupby(['join_NO', 'join_VSE2015', 'join_POM2015', 'join_BUSMHD2015', 'join_TRAM2015'])\
#     .agg({'counts': np.sum}).reset_index()

# a = 1

tsk_counts = result['join_VSE2015'] - result['join_POM2015'] - result['join_BUSMHD2015'] - result['join_TRAM2015']

ap_counts = result['counts']

sum_ap = ap_counts.sum()

sum_tsk = tsk_counts.sum()

# fig, axis = \
#     plt.subplots(1, 3, figsize=(24, 12))
#
# differences = tsk_counts - ap_counts
# axis[0].hist(differences, 100)
#
# relative_diff = differences / tsk_counts
# axis[1].hist(relative_diff, 100)
#
# ap_tsk_ratio = ap_counts / tsk_counts
# axis[2].hist(ap_tsk_ratio, 100)
#
# plt.show()

# fig, axis = \
#     plt.subplots(2, 1, figsize=(24, 12), sharex=True, sharey=True)
#
# axis[0].hist(tsk_counts, 100)
#
# axis[1].hist(ap_counts, 100)
#
#
# plt.show()

# fig, axis = \
#     plt.subplots(1, 1, figsize=(24, 12), sharex=True, sharey=True)
#
# axis.plot(sorted(tsk_counts))
#
# axis.plot(sorted(ap_counts))
#
#
# plt.show()

# fig, axis = \
#     plt.subplots(1, 1, figsize=(24, 12), sharex=True, sharey=True)
#
# axis.plot(tsk_counts[ap_counts>1000])
#
# axis.plot(ap_counts[ap_counts>1000])
#
#
# plt.show()

fig, axis = \
    plt.subplots(1, 1, figsize=(24, 12), sharex=True, sharey=True)

axis.scatter(tsk_counts[ap_counts>1000], ap_counts[ap_counts>1000], alpha=0.3
             )

# axis.plot(ap_counts[ap_counts>1000])


plt.show()



# print("Total transit TSK: ")

# if os.path.isfile(config.analysis.transit_statistic_filtered_file_path + ".npy"):
#     print_info("loading filtered transit from cache")
#
#     filtered_list = np.load(config.analysis.transit_statistic_filtered_file_path + ".npy")
# else:
#     print_info("filtering transit")
#     osm_ids = set(tsk_transit[1:, 0].astype('int'))
#
#     limit = 10000000
#     filtered_list = np.zeros((limit, 2), 'int32')
#     counter = 0
#     with open(config.agentpolis.statistics.transit_statistic_file_path) as sim_data_file:
#         for line in sim_data_file:
#             line_splitted=line.split(',')
#             line_numbers = list(map(int, line_splitted))
#             if line_numbers[1] in osm_ids:
#                 filtered_list[counter][0] = line_numbers[0]
#                 filtered_list[counter][1] = line_numbers[1]
#                 counter += 1
#             if counter >= len(filtered_list):
#                 filtered_list = np.concatenate((filtered_list, np.zeros((limit,2), 'int32')))
#                 print_info("enlarging matrix to {0} rows".format(len(filtered_list)))
#
#     filtered_list = filtered_list[~(filtered_list == 0).all(1)]
#
#     print_info("saving filtered transit")
#     np.save(config.analysis.transit_statistic_filtered_file_path, filtered_list)
#     print_info("{0} rows saved".format(len(filtered_list)))
#
#
# # sim_transit = np.genfromtxt(config.agentpolis.statistics.transit_statistic_file_path, delimiter=',')
#
# # sum
# daily_counts = np.column_stack(np.unique(filtered_list[:,1], return_counts=True))
#
# comparision = 1



