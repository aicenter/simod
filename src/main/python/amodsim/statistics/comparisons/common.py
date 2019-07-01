
from enum import Enum


class Experiment(Enum):
	PRESENT_STATE = (0, "grey", "ooo", "Present State")
	NO_RIDESHARING = (1, "red", "+++", "No Ridesharing")
	IH = (2, "green", "///", "Insertion Heuristic")
	VGA = (3, "blue", "\\\\\\", "VGA")
	VGA_LIMITED = (4, "orange", "***", "VGA Limited")

	def __init__(self, index, color, pattern, label):
		self.color = color
		self.index = index
		self.pattern = pattern
		self.label = label

labels = [exp.label for exp in Experiment]
colors = [exp.color for exp in Experiment]
hatches = [exp.pattern for exp in Experiment]