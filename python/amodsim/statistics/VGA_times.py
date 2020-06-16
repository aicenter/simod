
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


ridesharing_filepath = r"O:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-weight0/ridesharing.csv"

with open(ridesharing_filepath, 'r', encoding="utf-8")  as ridesharing:
    r = pd.read_csv(ridesharing)

# max_group_sizes = pd.Series()
# for row_index in range(len(r) - 1):
#     max_group_sizes.append(get_max_group_size_row(row_index))

max_group_sizes = r.apply(get_max_group_size_row, axis=1)

# Plotting
fig, axes = plt.subplots(2,1, figsize=(5,6))


# Axis 1
ax1 = axes[0]
ax2 = axes[1]
ax1.plot(r["Active Request Count"] / 1000)
ax1.set_xlabel("simulation time [min]")
ax1.set_ylabel("active requests [thousands]")

# second plot
ax1r = ax1.twinx()         # creates second Axes object with invisible x-Axis but independent y-Axis
ax1r.plot(max_group_sizes, color='r')
ax1r.set_ylabel("max group size", color='r')
ax1r.tick_params('y', colors='r')
ax1r.set_ylim(0, 12)


# Axis 2
ax2.plot(r["Group Generation Time"] / 1000)
ax2.plot(r["Solver Time"] / 1000)
ax2.set_ylim(0, 500)
ax2.set_xlim(0, 180)
ax2.set_xlabel("simulation time [min]")
ax2.set_ylabel("computational time [s]")

# second plot
ax2r = ax2.twinx()
ax2r.plot(max_group_sizes, color='r')
ax2r.set_ylabel("max group size", color='r')
ax2r.tick_params('y', colors='r')
ax2r.set_ylim(0, 15)

ax2.legend(loc=1, prop={'size': 10})


# FuncFormatter can be used as a decorator
@ticker.FuncFormatter
def minute_formater(x, pos):
    return "{}".format(int(x / 2))


ax1.xaxis.set_major_locator(ticker.MultipleLocator(20))
ax1.xaxis.set_major_formatter(minute_formater)

ax2.xaxis.set_major_locator(ticker.MultipleLocator(20))
ax2.xaxis.set_major_formatter(minute_formater)

plt.savefig(r"C:\Users\Fido\Downloads/vga_times.png", bbox_inches='tight', transparent=True)

plt.show()




