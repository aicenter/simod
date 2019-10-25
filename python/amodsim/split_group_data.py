import roadmaptools.inout

from typing import List
from tqdm import tqdm


def export_group_size(group_size: int):
	roadmaptools.inout.save_csv(sorted[group_size], "group_data_size_{}.csv".format(group_size), append=True)
	sorted[group_size] = []


def generate_header(group_size: int) -> List[str]:
	header = []
	header.extend(["feasible", "onboard count", "veh lat", "veh lon"])
	for i in range(1, group_size + 1):
		header.extend(["req {} pickup lat".format(i), "req {} pickup lon".format(i), "req {} pickup max time".format(i),
					   "req {} dropoff lat".format(i), "req {} dropoff lon".format(i), "req {} dropoff max time".format(i)])
	return header


reader = roadmaptools.inout.load_csv("group_data.csv")

sorted = {}

for line in tqdm(reader):
	group_size = int((len(line) - 4) / 6)
	if group_size not in sorted:
		sorted[group_size] = []
		sorted[group_size].append(generate_header(group_size))

	sorted[group_size].append(line)

	if len(sorted[group_size]) >= 10_000:
		export_group_size(group_size)

#  close all remaining files
for size, _ in sorted.items():
	export_group_size(size)
