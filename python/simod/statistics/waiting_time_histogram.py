import matplotlib.pyplot as plt
import pandas.errors
import pandas as pd
import numpy as np
import matplotlib
import roadmaptools.inout
from matplotlib.ticker import FuncFormatter

import simod.statistics.model.traffic_load as traffic_load
import simod.statistics.model.transit as transit
import simod.statistics.model.edges as edges
import simod.statistics.model.ridesharing as ridesharing
import simod.statistics.model.service as service
import simod.statistics.model.occupancy as occupancy

def to_percent_x(x, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = str(int(round(100 * x)))

    # The percent symbol needs escaping in latex
    if matplotlib.rcParams['text.usetex'] is True:
        return s + r'$\%$'
    else:
        return s + '%'

def to_percent_y(y, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = str(int(round(10 * y)))

    # The percent symbol needs escaping in latex
    if matplotlib.rcParams['text.usetex'] is True:
        return s + r'$\%$'
    else:
        return s + '%'


exp_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/experiments/test/waiting_times.csv'
save_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/img/'
exp_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/experiments/test/waiting_times.csv'
save_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/img/'


# delays dataframe
# TODO change dir
df = pd.read_csv(exp_dir_2)
# TODO change dir
save_results_to = save_dir_2

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))

maximum = df['waiting_time'].max()

bins = np.arange(0, maximum+20, 10)
labels = bins[0::2]
axis.set_xticks(labels)
plt.xticks(fontsize=8, rotation=0)

axis.hist(df['waiting_time'], bins, density=False, stacked=False, edgecolor='black', linewidth=0.4)

# plt.title("Waiting times")

plt.savefig(save_results_to + 'waiting_times', bbox_inches='tight', transparent=True)

plt.show()