from amodsim.init import config

import pandas

from typing import Union
from pandas import DataFrame
from amodsim.statistics.model.vehicle_state import VehicleState

cols = ["false_count", "true_count", "nn_time", "rest_time"]

MILLISECONDS_IN_DENSITY_PERIOD = 600000


def load(experiment_dir: str) -> DataFrame:
	occupancy_data \
		= pandas.read_csv(experiment_dir + "NN_logs.csv", names=cols)
	return occupancy_data


def get_value_count(NN: DataFrame, column: str) -> int:
	return NN[column].sum()
def percentage_value_time(NN: DataFrame, column: str) -> int:
	val = NN["nn_time"].sum() + NN["rest_time"].sum()
	return NN[column].sum() * 100 / val
def percentage_value_feasible(NN: DataFrame, column: str) -> int:
	val = NN["false_count"].sum() + NN["true_count"].sum()
	return NN[column].sum() * 100 / val