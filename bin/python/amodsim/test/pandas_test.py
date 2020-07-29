import numpy as np
import matplotlib.pyplot as plt

from pandas import DataFrame, Series

df = DataFrame([[0, 'r', 14],
				[1, 'r', 10],
				[0, 'b', 5],
				[2, 'l', 6],
				[2, 'k', 3]], columns=["first", "second", "third"])

# g = df.groupby("first")
#
# fig, axes = plt.subplots(1,1)
#
# axes.plot(g["third"].agg(np.mean))

a = df["first"] - df["third"]

b = 1

# plt.show()