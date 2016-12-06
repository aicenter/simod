from __future__ import print_function, division

# import sys

# sys.path.insert(0,'../../../../amod/python/')



from amod.common import *
from amod.shapes import load_shapes, plot_borders, plot_motorways, plot_roads, plot_trips

PRAGUE_DATA = '../../../data/prague/'
SCK_SCALING_FACTOR = 2
DIFF_THRESHOLD = 100


# def draw_connections():
#     for departure_index in departures_tod.argmax(axis=2):
#         if departures_tod[0]departure_index

# Project to Euclidean plane with origin in the center of Prague such that the units are meters.
projection = TransposedUTM(50.0877506,14.4209293)

# load the data
trips = load_trips(PRAGUE_DATA + "car-trips-new.npz", projection)
regions = load_shapes(PRAGUE_DATA + "shapefiles/prague-border", projection)
motorways = load_shapes(PRAGUE_DATA + "shapefiles/motorways", projection)
roads = load_shapes(PRAGUE_DATA + "shapefiles/roads", projection)

timeslices = 1
xslices = 100
yslices = 100

# depatrure matrix
grid, departures_tod = generate_tod_matrix_on_grid(trips, timeslices, xslices, yslices, SCK_SCALING_FACTOR)

# we do't need travel times here
traveltimes = np.zeros((xslices * yslices, xslices * yslices))

# arrival matrix
# arrivals_tod = arr(departures_tod, traveltimes, wrap=True)

departures = np.sum(departures_tod, axis=2)
arrivals = np.sum(departures_tod, axis=1)
diff = arrivals - departures
extreme = max(-np.min(diff), np.max(diff))

with np.errstate(divide='ignore', invalid="ignore"):
    diffRatio = diff / (departures + arrivals)
    diffRatio = np.nan_to_num(diffRatio)

extremeRatio = max(-np.min(diffRatio), np.max(diffRatio))


fig, axes = plt.subplots(1, 2, squeeze=True, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim', "aspect": 1.0})

axis = axes[0]
axis.axis('equal')
plot_borders(regions, axis)
plot_motorways(motorways, axis)
plot_roads(roads, axis)
trips_jitter = trips + 20 * np.random.standard_normal(trips.shape)
plot_trips(trips_jitter, axis, 1000000)
mesh = axis.pcolormesh(grid.edges_x / 1000, grid.edges_y / 1000, diff[0].reshape(grid.yslices, grid.xslices),
                          alpha=1, cmap='RdBu', vmin=-extreme, vmax=extreme)
cbar_ax = fig.add_axes([0.45, 0.15, 0.05, 0.7])
plt.colorbar(mesh, cax=cbar_ax)
mesh.set_array(diff[0])

axis = axes[1]
axis.axis('equal')
plot_borders(regions, axis)
plot_motorways(motorways, axis)
plot_roads(roads, axis)
trips_jitter = trips + 20 * np.random.standard_normal(trips.shape)
plot_trips(trips_jitter, axis, 1000000)
mesh = axis.pcolormesh(grid.edges_x / 1000, grid.edges_y / 1000, diffRatio[0].reshape(grid.yslices, grid.xslices),
                          alpha=1, cmap='RdBu', vmin=-extremeRatio, vmax=extremeRatio)
cbar_ax = fig.add_axes([0.875, 0.15, 0.05, 0.7])
plt.colorbar(mesh, cax=cbar_ax)
mesh.set_array(diffRatio[0])


plt.show()

