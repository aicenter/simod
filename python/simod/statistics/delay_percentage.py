import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import pandas.errors
import roadmaptools.inout
import simod.statistics.model.traffic_load as traffic_load
import simod.statistics.model.transit as transit
import simod.statistics.model.edges as edges
import simod.statistics.model.ridesharing as ridesharing
import simod.statistics.model.service as service
import simod.statistics.model.occupancy as occupancy


exp_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/experiments/test/'
exp_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/experiments/test/'
exp_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/experiments/test/'

save_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/img/'
save_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/img/'
save_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/img/'


# delays dataframe
# TODO change dir
service_stat = service.load_dataframe(exp_dir_3)
# TODO change dir
save_results_to = save_dir_3

delays = service.get_delays(service_stat, True, False).to_frame()
service_stat['percentage_delay'] = (service_stat['dropoff_time'] - service_stat['demand_time']) / service_stat['min_possible_delay'] - 1
maximum = service_stat['percentage_delay'].max()
service_stat['percentage_delay_norm'] = service_stat['percentage_delay'] / maximum

new_df = service_stat.where(service_stat['percentage_delay'] > 0.0)
new_df = new_df.dropna()

bins = [0.0, 0.1, 0.2, 0.3, 0.4, 0.6, 0.8, 1 * maximum]


bins_df = pd.cut(new_df['percentage_delay'], bins)
print(bins_df.value_counts())

t = bins_df.value_counts(sort=False)
labels = ['< 10 %', '< 20 %', '< 30 %', '< 40 %', '< 60 %', '< 80 %', '81 % +']


counts = t
plt.axis('equal')
explode = bins
colors = ['#191970','#0038E2','#0071C6','#329A82', '#46C3A6', '#93DCCB', '#E0F5F0']
colors2 = ['#E6F7FF', '#BAE7FF', '#91D5FF', '#69C0FF', '#1890FF', '#096DD9', '#0050B3', '#002766']
cc = list(reversed(colors))
counts.plot(kind='pie', fontsize=15, colors=cc, labels=t,
            wedgeprops={"edgecolor": "black",
                        'linewidth': 0.2,
                        'antialiased': True}
            )
plt.legend(labels=labels, loc="best")
plt.ylabel('')


plt.savefig(save_results_to + 'delay_percentages', bbox_inches='tight', transparent=True)


plt.show()