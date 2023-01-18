import geopandas as gpd
import contextily as ctx
import matplotlib.pyplot as plt
import numpy as np

data = gpd.read_file('/home/martin/Documents/01_Bakalarka/01_simod/data/maps/edges.geojson')
nodes = gpd.read_file('/home/martin/Documents/01_Bakalarka/01_simod/data/maps/nodes.geojson')
save_dir = '/home/martin/Documents/01_Bakalarka/02_PLOTS_RESULTS/'

gdf = data['geometry']

nodes_X = nodes['geometry'].x
nodes_Y = nodes['geometry'].y
min_X = np.min(nodes_X)
max_X = np.max(nodes_X)
min_Y = np.min(nodes_Y)
max_Y = np.max(nodes_Y)
print(min_X, max_X, min_Y, max_Y)

gdf = gdf.to_crs(epsg=3857)

ax = gdf.plot(figsize=(10, 10), alpha=0.5, edgecolor='b')
ctx.add_basemap(ax, zoom=12, source=ctx.providers.OpenStreetMap.Mapnik)
# ctx.add_basemap(ax, zoom=12, url='http://b.tile.openstreetmap.org/tileZ/tileX/tileY.png')

ax.set_ylabel('')
ax.set_xlabel('')

# plt.savefig(save_dir + 'road_graph', bbox_inches='tight', transparent=True)
plt.show()
