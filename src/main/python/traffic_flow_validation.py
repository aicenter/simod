from __future__ import print_function, division

import json
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import time
import subprocess
import os
from sys import getsizeof

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

sum_ap = result['counts'].sum()

sum_tsk = result['join_VSE2015'].sum()

print("Total transit TSK: ")

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



