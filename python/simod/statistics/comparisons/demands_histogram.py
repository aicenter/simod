
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as dates
from matplotlib.ticker import FuncFormatter
from matplotlib.ticker import MaxNLocator, FixedLocator

bins = 12
labels = [(str(n) + ":00") for n in range(25)]
# labels = ["0:" + (str(5*n)) for n in range(bins + 1)]
xs = range(bins + 1)

def format_time(miliseconds: int, position) -> str:
	# return str(datetime.timedelta(minutes=minutes))
	return str(int(round(miliseconds / 1000 / 60 / 60))) + ":" + str(int(round(miliseconds / 1000 / 60)))

def format_fn(tick_val, tick_pos):
    if int(tick_val) in xs:
        return labels[int(tick_val)]
    else:
        return ''

def format_hours(tick_value, tick_index):
    return str(tick_value) + ":00"

def format_hours_to_minutes(tick_value, tick_index):
    minutes = np.round(60 * (tick_value - 13)).astype(int)
    if minutes < 10:
        str_minutes = "0:0" + str(minutes)
    elif minutes == 60:
        str_minutes = "1:00"
    else:
        str_minutes = "0:" + str(minutes)
    return str_minutes

# bins = np.arange(0, 1000*60*60*24, 1000*60*60)

# demands_24k = pd.read_csv('/home/martin/Documents/01_Bakalarka/03_DATA/manhattan_taxi_data_01_12_24k.txt', sep=" ", header=None)
# demands_24k.columns = ["time", "y", "x", "y2", "x2", "count", "newline"]
# demands_24_times = demands_24k["time"] / (1000*60*60)
#
# fig, axes = plt.subplots(1, 1,  figsize=(10, 5))
# plt.xticks(np.arange(0, 25, 2))
# # plt.xticks(rotation=-45)
# axes.xaxis.set_major_formatter(format_hours)
# _n, _bins, patches = axes.hist(demands_24_times, bins)


# demands_25k = pd.read_csv('/home/martin/Documents/01_Bakalarka/03_DATA/manhattan_trips_0_to_1.txt', sep=" ", header=None)
# demands_25k.columns = ["time", "y", "x", "y2", "x2", "count", "newline"]
# demands_25_times = demands_25k["time"] / (1000*60*60)
#
# fig, axes = plt.subplots(1, 1,  figsize=(10, 5))
# plt.xticks(np.arange(0, 1.01, (1/12)))
# axes.xaxis.set_major_formatter(format_hours_to_minutes)
# _n, _bins, patches = axes.hist(demands_25_times, bins)


demands_50k = pd.read_csv('/home/martin/Documents/01_Bakalarka/03_DATA/manhattan_trips_13_to_14_50k.txt', sep=" ", header=None)
demands_50k.columns = ["time", "y", "x", "y2", "x2", "count", "newline"]
demands_50_times = demands_50k["time"] / (1000*60*60)

fig, axes = plt.subplots(1, 1,  figsize=(10, 5))
plt.xticks(np.arange(13, 14.01, (1/12)))
axes.xaxis.set_major_formatter(format_hours_to_minutes)
_n, _bins, patches = axes.hist(demands_50_times, bins)





plt.show()




# demands_24k = pd.read_csv('/home/martin/Documents/01_Bakalarka/03_DATA/manhattan_taxi_data_01_12_24k.txt', sep=" ", header=None)
# demands_24k.columns = ["time", "y", "x", "y2", "x2", "count", "newline"]
# demands_24_times = demands_24k["time"] / (1000*60*60)
#
# fig, axes = plt.subplots(1, 1,  figsize=(10, 5))
# axes.xaxis.set_major_formatter(format_fn)
# axes.xaxis.set_major_locator(MaxNLocator(integer=True))
# _n, _bins, patches = axes.hist(demands_24_times, bins)








# fig, ax = plt.subplots()
# xs = range(26)
# ys = range(26)
# labels = list('abcdefghijklmnopqrstuvwxyz')
#
# def format_fn(tick_val, tick_pos):
#     if int(tick_val) in xs:
#         return labels[int(tick_val)]
#     else:
#         return ''
#
# # A FuncFormatter is created automatically.
# ax.xaxis.set_major_formatter(format_fn)
# ax.xaxis.set_major_locator(MaxNLocator(integer=True))
# ax.plot(xs, ys)
#
#
# plt.show()

# x_times = pd.timedelta_range(start='00:00:00', end='23:00:00', freq='1H')
# x_times_floats = x_times.apply(lambda x: x.hours)
# print(x_times.values.astype(float))
# print(x_times_floats)
# plt.hist(demands_24_times, bins)
# plt.show()

# axes.xaxis.set_ticks(np.arange(0, 60*60*24, 60*60))
# axes.xaxis.set_major_formatter(FuncFormatter(format_time))

# axes.set_ylabel("number of demands")
# axes.set_xlabel("passengers per vehicle")
# plt.legend(loc='upper right')