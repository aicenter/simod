from init import config

import matplotlib.pyplot as plt
import amodsim.statistics.model.ridesharing as ridesharing


ih_capacity1_stats = ridesharing.load(config.comparison.experiment_1_dir)
ih_capacity5_stats = ridesharing.load(config.comparison.experiment_2_dir)
vga_stats = ridesharing.load(config.comparison.experiment_3_dir)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))

axes.plot(ih_capacity1_stats['Batch'], ih_capacity1_stats['Insertion Heuristic Time'] / 1000)
axes.plot(ih_capacity5_stats['Batch'], ih_capacity5_stats['Insertion Heuristic Time'] / 1000)
axes.plot(vga_stats['Batch'], (vga_stats['Group Generation Time'] + vga_stats['Solver Time']) / 1000)

axes.legend(['No Ridesharing', 'Insertion Heuristic', 'VGA'])

plt.savefig(config.images.ridesharing_performance_comparison, bbox_inches='tight', transparent=True)

plt.show()