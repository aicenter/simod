from enum import Enum


class VehicleState(Enum):
	WAITING = (0, "grey", "ooo", "waiting")
	DRIVING_TO_START_LOCATION = (1, "green", "+++", "pickup_trips")
	DRIVING_TO_TARGET_LOCATION = (2, "blue", "///", "demand_trips")
	DRIVING_TO_STATION = (3, "black", "\\\\\\", "drop off trips")
	REBALANCING = (4, "red", "***", "rebalancing trips")

	def __init__(self, index, color, pattern, label):
		self.color = color
		self.index = index
		self.pattern = pattern
		self.label = label