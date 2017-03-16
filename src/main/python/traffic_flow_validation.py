from __future__ import print_function, division

import json
import matplotlib.pyplot as plt
import numpy as np
import time
import subprocess
import os
from sys import getsizeof

from scripts.config_loader import cfg as config
from scripts.printer import print_info, print_table


def wccount(filename):
    out = subprocess.Popen(['wc', '-l', filename], stdout=subprocess.PIPE, stderr=subprocess.STDOUT ).communicate()[0]
    return int(out.partition(b' ')[0])

print_info("loading tsk transit")
tsk_transit = np.genfromtxt(config.analysis.map_tsk_matching_file_path, delimiter=',')

osm_ids = set(tsk_transit[1:,0].astype('int'))

# filtered_list = []
# counter = 0
# with open(config.agentpolis.statistics.transit_statistic_file_path) as sim_data_file:
#     for line in sim_data_file:
#         counter += 1
#         line_splitted=line.split(',')
#         line_numbers = list(map(int, line_splitted))
#         if(line_numbers[1] in tsk_transit[:,0]):
#             filtered_list.append(line_numbers)
#         if counter > 1000000:
#             break

if os.path.isfile(config.analysis.transit_statistic_filtered_file_path + ".npy"):
    print_info("loading filtered transit from cache")
    # filtered_list = np.genfromtxt(config.analysis.transit_statistic_filtered_file_path, delimiter=',')

    # limit = 10000000
    # filtered_list = np.zeros((limit, 2), int)
    # counter = 0
    # with open(config.analysis.transit_statistic_filtered_file_path) as sim_data_file:
    #     for line in sim_data_file:
    #         line_splitted = line.split(',')
    #         line_numbers = list(map(int, line_splitted))
    #         filtered_list[counter][0] = line_numbers[0]
    #         filtered_list[counter][1] = line_numbers[1]
    #         counter += 1
    #         if counter >= len(filtered_list):
    #             filtered_list = np.concatenate((filtered_list, np.zeros((limit, 2), int)))
    #             print_info("enlarging matrix to {0} rows".format(len(filtered_list)))
    #
    # filtered_list = filtered_list[~(filtered_list == 0).all(1)]

    filtered_list = np.load(config.analysis.transit_statistic_filtered_file_path + ".npy")
else:
    print_info("filtering transit")
    # start_time = time.time()

    limit = 10000000
    filtered_list = np.zeros((limit, 2), 'int32')
    counter = 0
    with open(config.agentpolis.statistics.transit_statistic_file_path) as sim_data_file:
        for line in sim_data_file:
            line_splitted=line.split(',')
            line_numbers = list(map(int, line_splitted))
            if line_numbers[1] in osm_ids:
                filtered_list[counter][0] = line_numbers[0]
                filtered_list[counter][1] = line_numbers[1]
                counter += 1
            # if counter >= limit - 1:
            #     break
            if counter >= len(filtered_list):
                filtered_list = np.concatenate((filtered_list, np.zeros((limit,2), 'int32')))
                print_info("enlarging matrix to {0} rows".format(len(filtered_list)))

    filtered_list = filtered_list[~(filtered_list == 0).all(1)]

    # print("--- %s seconds ---" % (time.time() - start_time))
    print_info("saving filtered transit")
    # np.savetxt(config.analysis.transit_statistic_filtered_file_path, filtered_list, delimiter=',', fmt='%i')
    np.save(config.analysis.transit_statistic_filtered_file_path, filtered_list)
    print_info("{0} rows saved".format(len(filtered_list)))


# sim_transit = np.genfromtxt(config.agentpolis.statistics.transit_statistic_file_path, delimiter=',')

# sum
daily_counts = np.column_stack(np.unique(filtered_list[:,1], return_counts=True))

comparision = 1



