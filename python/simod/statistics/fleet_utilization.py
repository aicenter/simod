
from simod.init import config

from tqdm import tqdm
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib
import roadmaptools.inout
import simod.utils

from matplotlib.ticker import FuncFormatter
from pandas import DataFrame


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


data_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/experiments/test/inactive_vehicles.csv'
data_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/experiments/test/inactive_vehicles.csv'
data_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/experiments/test/inactive_vehicles.csv'

save_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/img/'
save_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/img/'
save_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/img/'

# TODO change dir
df = pd.read_csv(data_dir_3)

# vehicles = df['id'].values

occur = df.groupby(['id']).size()

occur_df = occur.to_frame()
occur_df = occur_df.reset_index()
occur_df.columns=['id','occurance_inactive']
occur_df['time_inactive'] = occur_df['occurance_inactive'] * 30

simulation_duration = 5 * 60 * 60 + 600 # in seconds

occur_df['percentage_active'] = (simulation_duration - occur_df['time_inactive']) / simulation_duration

df_short = occur_df[['id', 'percentage_active']]

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))

plt.gca().xaxis.set_major_formatter(FuncFormatter(to_percent_x))

bins = np.arange(0, 1, 0.1)
axis.set_xticks(bins)
plt.xticks(fontsize=9, rotation=0)

axis.hist(occur_df['percentage_active'], bins, density=False, stacked=False, edgecolor='black', linewidth=0.4)

# plt.title("Vehicle utilization")

# TODO change dir
save_results_to = save_dir_3
plt.savefig(save_results_to + 'vehicle-utilization', bbox_inches='tight', transparent=True)

plt.show()



print('done')