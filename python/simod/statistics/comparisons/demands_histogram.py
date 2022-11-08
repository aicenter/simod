
import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.dates as dates
from matplotlib.ticker import FuncFormatter

def format_time(miliseconds: int, position) -> str:
	# return str(datetime.timedelta(minutes=minutes))
	return str(int(round(miliseconds / 1000 / 60 / 60))) + ":" + str(int(round(miliseconds / 1000 / 60)))


# bins = np.arange(0, 1000*60*60*24, 1000*60*60)
bins = 24

demands_24k = pd.read_csv('/home/martin/Documents/01_Bakalarka/03_DATA/manhattan_taxi_data_01_12_24k.txt', sep=" ", header=None)
demands_24k.columns = ["time", "y", "x", "y2", "x2", "count", "newline"]
demands_24_times = demands_24k["time"] / (1000*60*60)
x_times = pd.timedelta_range(start='00:00:00', end='23:00:00', freq='1H')
# x_times_floats = x_times.apply(lambda x: x.hours)
print(x_times.values.astype(float))
# print(x_times_floats)

# plt.hist(demands_24_times, bins)
# plt.show()

fig, axes = plt.subplots(1, 1,  figsize=(10, 5))		# subplot_kw={"adjustable": 'box'},

axes.xaxis.set_major_formatter(dates.DateFormatter('%H:%M'))
_n, _bins, patches = axes.hist(demands_24_times, bins)
print(_n)

# axes.bar(x_times.values.astype(float), _n)

plt.show()







# axes.xaxis.set_ticks(np.arange(0, 60*60*24, 60*60))
# axes.xaxis.set_major_formatter(FuncFormatter(format_time))

# axes.set_ylabel("number of demands")
# axes.set_xlabel("passengers per vehicle")
# plt.legend(loc='upper right')