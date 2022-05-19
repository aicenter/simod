import geopandas as gpd
import contextily as ctx
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
import shapely
from mpl_toolkits.basemap import Basemap
import rasterio.crs
from ctypes.util import find_library



def add_basemap(ax, zoom, url='http://tile.stamen.com/terrain/tileZ/tileX/tileY.png'):
    xmin, xmax, ymin, ymax = ax.axis()
    basemap, extent = ctx.bounds2img(xmin, ymin, xmax, ymax, zoom=zoom, source=url)
    ax.imshow(basemap, extent=extent, interpolation='bilinear')
    # restore original x/y limits
    ax.axis((xmin, xmax, ymin, ymax))



# find_library('geos_c')

# data = gpd.read_file('/Users/adela/Documents/bakalarka/randomdemand/maps/edges.geojson')
save_dir = '/Users/adela/Documents/bakalarka/vysledky2/'

data = pd.read_csv('/Users/adela/Documents/bakalarka/randomdemand/trips.txt', sep=" ", header=None)
data.columns = ["time", "y", "x", "y2", "x2"]
data_part1 = data[['y', 'x']]
data_part2 = data[['y2', 'x2']]
#
new_data = pd.concat([data_part1, data_part2.rename(columns={'y2': 'y', 'x2': 'x'})], ignore_index=True)
new_data = new_data.reset_index()
#
gdf = gpd.GeoDataFrame(new_data, geometry=gpd.points_from_xy(new_data.y, new_data.x), crs='EPSG:4326')


gdf.geometry.map(lambda polygon: shapely.ops.transform(lambda x, y: (y, x), polygon))

gdf = gdf.set_crs(epsg='4326', allow_override=True)
gdf = gdf['geometry']

df_wm = gdf.to_crs(epsg=3857)
# ax = gdf.plot(figsize=(10, 10), alpha=0.7, c='grey')

ax = gdf.plot(figsize=(10, 10), alpha=0, c='grey')
# ctx.add_basemap(ax, crs='EPSG:3857', source=ctx.providers.Stamen.TonerLite)

sns.kdeplot(data=new_data,
            x='y',
            y='x',
            fill=True,
            cmap='coolwarm',
            alpha=0.6,
            gridsize=300,
            levels=10,
            ax=ax,
            legend=False)

# ax = gdf.plot(figsize=(10, 10), alpha=0.1, c='grey')

# ctx.add_basemap(ax, zoom=18)
# ctx.add_basemap(ax, crs=df_wm.crs.to_string(), zoom=10)
# m.shadedrelief()

# minx, miny, maxx, maxy = data.total_bounds

# print(minx, maxx, miny, maxy)

# ax = gdf.plot(figsize=(10, 10), alpha=0.5, edgecolor='b')

# add_basemap(ax, zoom=10)
# ctx.add_basemap(ax, crs=gdf.crs, zoom=10)

# ctx.add_basemap(ax, zoom=12, source=ctx.providers.OpenStreetMap.Mapnik)
# ctx.add_basemap(ax, crs=gdf.crs, source=ctx.providers.OpenStreetMap.Mapnik)
# ctx.add_basemap(ax, source=ctx.providers.Stamen.TonerLite)
# ctx.add_basemap(ax, source=ctx.providers.Stamen.TonerLite, crs=gdf.crs.to_string())

# ax.get_xaxis().set_ticks([])
# ax.get_yaxis().set_ticks([])
ax.set_ylabel('')
ax.set_xlabel('')

# plt.savefig(save_dir + 'road_graph', bbox_inches='tight', transparent=True)
# plt.savefig(save_dir + 'demand_locations', bbox_inches='tight', transparent=True)
plt.savefig(save_dir + 'demand_heatmap', bbox_inches='tight', transparent=True)

plt.show()

