import roadmaptools.inout

from tqdm import tqdm


def export_group_size(group_size: int):
	roadmaptools.inout.save_csv(sorted[group_size], "group_data_size_{}".format(group_size), append=True)
	sorted[group_size] = []


reader = roadmaptools.inout.load_csv("group_data.csv")

sorted = {}

for line in tqdm(reader):
	group_size = int((len(line) - 4) / 6)
	if group_size not in sorted:
		sorted[group_size] = []

	sorted[group_size].append(line)

	if len(sorted[group_size]) >= 1_000:
		export_group_size(group_size)

#  close all remaining files
for size, _ in sorted.items():
	export_group_size(size)
