import numpy as np
import matplotlib.pyplot as plt

fig, ax = plt.subplots()
x = np.linspace(0,1)
ax.plot(x, x, 'o')

for xi in x:
	ax.annotate('default line', xy=(xi,xi), xytext=(60,0),
				textcoords="offset pixels",
            arrowprops={'arrowstyle': '-', 'shrinkB': 50}, va='center')

plt.show()
