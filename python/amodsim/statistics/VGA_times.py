
import csv
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib import ticker


def get_max_group_size_row(row: pd.Series) -> int:
    next_level = False

    for size in range(1,12):
        if row["{} Groups Count".format(size)] > 0:
            if size == 11:
                next_level = True
            continue
        else:
            return int((size - 1) / 2)

    if next_level:
        for size in range(1, 11):
            if row["{} Feasible Groups Count".format(size)] > 0:
                continue
            else:
                return int((11 + size - 1) / 2)

    return 11


def get_group_times_per_row(row: pd.Series) -> pd.Series:
    next_level = False
    times = []
    for size in range(1,12):
        if row["{} Groups Time".format(size)] > 0:
            if size == 11:
                next_level = True
            times.append(row["{} Groups Time".format(size)])
        else:
            break

    if next_level:
        for size in range(1, 11):
            if row["{} Feasible Groups Time".format(size)] > 0:
                times.append(row["{} Feasible Groups Time".format(size)])
            else:
                break

    max_group_size = len(times) / 2

    return pd.Series(times[0:max_group_size + 1])


# ridesharing_filepath = r"O:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-weight0/ridesharing.csv"

ridesharing_filepath = r"C:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-weight0/ridesharing.csv"

with open(ridesharing_filepath, 'r', encoding="utf-8")  as ridesharing:
    r = pd.read_csv(ridesharing)

# max_group_sizes = pd.Series()
# for row_index in range(len(r) - 1):
#     max_group_sizes.append(get_max_group_size_row(row_index))

max_group_sizes = r.apply(get_max_group_size_row, axis=1)

# group_times = r.apply(max_group_sizes, axis=1)
for row in r.iter:
    max_group_sizes = get_max_group_size_row(row)
    a = 1

group_column_names = []
for i in range(12):
    group_column_names.append("{} Group Time".format(i))

# Plotting

# FuncFormatter can be used as a decorator
@ticker.FuncFormatter
def minute_formater(x, pos):
    return "{}".format(int(x / 2))


# Simulation Time Plot
fig, axes = plt.subplots(3,1, figsize=(5,6), sharex=True)
ax1 = axes[0]
ax2 = axes[1]
ax3 = axes[2]

# Axis 1
ax1.plot(r["Active Request Count"] / 1000)
ax1.set_xlabel("simulation time [min]")
ax1.set_ylabel("active requests [thousands]")

# Axis 2
ax2.plot(max_group_sizes, color='r')
ax2.set_ylabel("max group size", color='r')
ax2.tick_params('y', colors='r')
ax2.set_ylim(0, 12)

# Axis 3
ax3.plot(r["Group Generation Time"] / 1000)
ax3.plot(r["Solver Time"] / 1000)
ax3.set_ylim(0, 500)
ax3.set_xlim(0, 180)
ax3.set_xlabel("simulation time [min]")
ax3.set_ylabel("computational time [s]")

# Group Size Plot
fig2, axes2 = plt.subplots(2,1, figsize=(5,4), sharex=True)
ax1 = axes[0]
ax2 = axes[1]



# Times
fig, axes = plt.subplots(2,1, figsize=(5,6))
ax1 = axes[0]
ax2 = axes[1]

# Axis 2
ax1.plot(r["Group Generation Time"] / 1000)
ax1.plot(r["Solver Time"] / 1000)
ax1.set_ylim(0, 500)
ax1.set_xlim(0, 180)
ax1.set_xlabel("simulation time [min]")
ax1.set_ylabel("computational time [s]")

# second plot
ax1r = ax1.twinx()
ax1r.plot(r["Active Request Count"] / 1000, color='r')
ax1r.set_ylabel("active requests [thousands]", color='r')
ax1r.tick_params('y', colors='r')
ax1r.set_ylim(0, 25)

ax1.legend(loc=1, prop={'size': 10})

ax1.xaxis.set_major_locator(ticker.MultipleLocator(20))
ax1.xaxis.set_major_formatter(minute_formater)

plt.savefig(r"C:\Users\david\Downloads/vga_simulation_time.png", bbox_inches='tight', transparent=True)

# Bars
ax2.bar


plt.savefig(r"C:\Users\david\Downloads/vga_group_size.png", bbox_inches='tight', transparent=True)

plt.show()



# # Lines
# fig, axes = plt.subplots(2,1, figsize=(5,6))


# # Axis 1
# ax1 = axes[0]
# ax2 = axes[1]
# ax1.plot(r["Active Request Count"] / 1000)
# ax1.set_xlabel("simulation time [min]")
# ax1.set_ylabel("active requests [thousands]")
#
# # second plot
# ax1r = ax1.twinx()         # creates second Axes object with invisible x-Axis but independent y-Axis
# ax1r.plot(max_group_sizes, color='r')
# ax1r.set_ylabel("max group size", color='r')
# ax1r.tick_params('y', colors='r')
# ax1r.set_ylim(0, 12)


# # Axis 2
# ax2.plot(r["Group Generation Time"] / 1000)
# ax2.plot(r["Solver Time"] / 1000)
# ax2.set_ylim(0, 500)
# ax2.set_xlim(0, 180)
# ax2.set_xlabel("simulation time [min]")
# ax2.set_ylabel("computational time [s]")
#
# # second plot
# ax2r = ax2.twinx()
# ax2r.plot(max_group_sizes, color='r')
# ax2r.set_ylabel("max group size", color='r')
# ax2r.tick_params('y', colors='r')
# ax2r.set_ylim(0, 15)
#
# ax2.legend(loc=1, prop={'size': 10})
#
#
# # FuncFormatter can be used as a decorator
# @ticker.FuncFormatter
# def minute_formater(x, pos):
#     return "{}".format(int(x / 2))
#
#
# ax1.xaxis.set_major_locator(ticker.MultipleLocator(20))
# ax1.xaxis.set_major_formatter(minute_formater)
#
# ax2.xaxis.set_major_locator(ticker.MultipleLocator(20))
# ax2.xaxis.set_major_formatter(minute_formater)
#
# plt.savefig(r"C:\Users\Fido\Downloads/vga_times.png", bbox_inches='tight', transparent=True)
#
# plt.show()




