from __future__ import print_function, division

import numpy as np
import pandas as pd
from datetime import time

from scripts.config_loader import cfg as config
from scripts import trip_loader
from scripts.printer import print_info, print_table


def milis_to_time(millis):
    seconds = int(round(millis / 1000))
    return seconds_to_time(seconds)
    # seconds = (millis / 1000) % 60
    # minutes = (millis / (1000 * 60)) % 60
    # hours = (millis / (1000 * 60 * 60)) % 24
    # return seconds, minutes, hours


def seconds_to_time(seconds_total):

    # if we have seconds in float
    seconds_total = int(round(seconds_total))

    seconds = seconds_total % 60
    seconds_total -= seconds

    minutes = int((seconds_total / 60) % 60)
    seconds_total -= minutes * 60

    hours = int((seconds_total / 60 * 60) % 24)
    return seconds, minutes, hours


def milis_to_time_format(millis):
    seconds, minutes, hours = milis_to_time(millis)
    return time(hours, minutes, seconds).strftime("%H:%M:%S")


def seconds_to_time_format(seconds):
    seconds, minutes, hours = seconds_to_time(seconds)
    return time(hours, minutes, seconds).strftime("%H:%M:%S")


# simulation statistics

departures \
    = pd.read_csv(config.agentpolis.statistics.on_demand_vehicle_statistic.pickup_file_path,
                  names=['pickup_time', 'demand_id'], header=None)

arrivals \
    = pd.read_csv(config.agentpolis.statistics.on_demand_vehicle_statistic.drop_off_file_path,
                  names=['drop_off_time', 'demand_id'], header=None)

table = pd.merge(departures, arrivals, how='inner', on="demand_id")

table["demand_trip_length"] = table["drop_off_time"] - table["pickup_time"]

average_demand_trip_length = table["demand_trip_length"].mean()
median_demand_trip_length = table["demand_trip_length"].median()

trip_lenghts = np.genfromtxt(config.agentpolis.statistics.trip_distances_file_path, delimiter=',')

average_demand_trip_distance = np.average(trip_lenghts)
median_demand_trip_distance = np.median(trip_lenghts)

print("Simulation statistics")
print("Average demand trip duration: {0}".format(milis_to_time_format(average_demand_trip_length)))
print("Median demand trip duration: {0}".format(milis_to_time_format(median_demand_trip_length)))
print("Average demand trip distance: {0} km".format(average_demand_trip_distance / 1000))
print("Median demand trip distance: {0} km".format(median_demand_trip_distance / 1000))


# demand model statistics - time in seconds!

print("")

trips = trip_loader.trips
intervals = trips[:, 1] - trips[:, 0]
trips = np.append(trips, np.reshape(intervals, (len(intervals), 1)), 1)
# trips[:,:-1] = trips[:,1] - trips[:,0]
average_demand_trip_length = np.average(trips[:,6])
median_demand_trip_length = np.median(trips[:,6])

print("Demand model statistics")
print("Average demand trip length: {0}".format(seconds_to_time_format(average_demand_trip_length)))
print("Median demand trip length: {0}".format(seconds_to_time_format(median_demand_trip_length)))

