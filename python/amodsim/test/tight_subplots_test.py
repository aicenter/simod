import matplotlib.pyplot as plt
import pandas as pd
import numpy as np


data = pd.Series(1, index=np.arange(20))

fig, axes = plt.subplots(1, 2, figsize=(6, 3), sharex=True, sharey=True)

fig.subplots_adjust(wspace=0.01)

plt.savefig(r'C:/Users/david/Downloads/test',  bbox_inches='tight', pad_inches=0.0)

plt.show()