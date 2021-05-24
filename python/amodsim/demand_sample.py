from amodsim.init import config
import math
import pandas as pd
import numpy as np
import psycopg2
import matplotlib
import matplotlib.pyplot as plt
import roadmaptools.inout
import roadmaptools.plotting
import roadmaptools.utm

matplotlib.use('TkAgg')  # or can use 'TkAgg'

# ids = "4465029, 4465030, 4465031, 4465032, 4465033, 4465034"
# ids = "4465026, 4465027, 4465028, 4465047, 4465048, 4465049"
# ids = "4465026, 4465027, 4465028, 4465076, 4465077, 4465078"
# ids = "4465047, 4465048, 4465049, 4465076, 4465077, 4465078"
# ids = "4465047, 4465048, 4465049, 4465504, 4465505, 4465506"
# ids = "4465047, 4465048, 4465049, 4552369, 4552370, 4552371, 4552372"
# ids = "4465047, 4465048, 4465049, 4552405, 4552406, 4552407, 4552408"
# ids = "4465047, 4465048, 4465049, 4492098, 4492099, 4492100, 4492101"
# ids = "4465047, 4465048, 4465049, 4492098, 4492099, 4492100, 4492101"

# ids = "4466656, 4466657, 4466658, 4466659"
# ids = "4467147, 4467148, 4467149, 4467150, 4467862, 4467863, 4467864, 4467865, 4466195, 4466196, 4466197, 4466198, 4467378, 4467379, 4467380, 4467381, 4466473, 4466474, 4466475, 4466476"

# ids = "4467862, 4467863, 4467864, 4467865, 4478770, 4478771, 4478772"
# ids = "4467862, 4467863, 4467864, 4467865, 4789903, 4789904, 4789905"
# ids = "4493897, 4493898, 4493899, 4493900, 4789903, 4789904, 4789905"
ids = "4500942, 4500943, 4500944, 4500945, 4789903, 4789904, 4789905"

connection = psycopg2.connect("dbname=demand_prague user=postgres password=fidofido")

# trips
columns = [
    "trip_id",
    "person_id",
    "from_activity_id",
    "to_activity_id",
    "start_time",
    "end_time",
    "duration",
    "transport_type"
]
cursor = connection.cursor()
cursor.execute("""
SELECT * 
    FROM sck_log.trip_log 
    WHERE trip_id IN ({})
""".format(ids))
data = cursor.fetchall()
cursor.close()
trips = pd.DataFrame(data, columns = columns)


# activities
query = """
SELECT DISTINCT activity_log.*, node_log.* FROM sck_log.activity_log 
	INNER JOIN sck_log.trip_log 
		ON (sck_log.trip_log.from_activity_id = sck_log.activity_log.activity_id OR sck_log.trip_log.to_activity_id = sck_log.activity_log.activity_id)
			AND sck_log.trip_log.person_id = sck_log.activity_log.person_id 
	INNER JOIN sck_log.node_log
		ON sck_log.activity_log.location_id = sck_log.node_log.id 
	WHERE trip_id IN ({})

""".format(ids)


cursor = connection.cursor()
cursor.execute(query)
data = cursor.fetchall()
cursor.close()
columns = [
    "person_id",
    "activity_id",
    "start_time",
    "end_time",
    "duration",
    "previous_trip_duration",
    "activity_type",
    "location_id",
    "attractor_id",
    "mode",
    "id",
    "latitude",
    "longitude",
    "info"
]
activities = pd.DataFrame(data, columns=columns)

fig, axis = plt.subplots(1, 1, figsize=(7, 7))


projection = roadmaptools.utm.TransposedUTM.from_gps(activities["latitude"][0], activities["longitude"][0])
# for edge in edges_iterator:
#     from_coords = roadmaptools.utm.wgs84_to_utm(edge[0][1], edge[0][0], projection)


# latex table: trips
print("Latex code:")
print(r"{\renewcommand{\arraystretch}{1.2}%")
print(r"\begin{tabular}{|r|r|r|r|r|}")
print(r"\hline")
print(r"\thead{Trip} & \thead{Person} & \thead{From} & \thead{To} & \thead{Mode}")
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
for trip in trips.itertuples():
    print("\\num{{{}}} & \\num{{{}}} & \\num{{{}}} & \\num{{{}}} & {} ".format(
        trip.trip_id,
        trip.person_id,
        trip.from_activity_id,
        trip.to_activity_id,
        trip.transport_type))
    print(r"\tabularnewline")
    print(r"\hline")
print(r"\end{tabular}}")
print()


def format_time(milis: int):
    seconds = int(round(milis / 1000)) % (24 * 3600)
    hour = seconds // 3600
    seconds %= 3600
    minutes = seconds // 60
    seconds %= 60

    return "%02d:%02d" % (hour, minutes)

# latex table: activities
print("Activity table Latex code:")
print(r"{\renewcommand{\arraystretch}{1.2}%")
print(r"\begin{tabular}{|r|r|r|r|r|r|r|}")
print(r"\hline")
print(r"\thead{Person} & \thead{Activity} & \thead{Start} & \thead{End} & \thead{Type} & \thead{Lat} & \thead{Lon}")
print(r"\tabularnewline")
print(r"\hline")
print(r"\hline")
for activity in activities.itertuples():
    print("\\num{{{}}} & \\num{{{}}} & {} & {} & {} & \\num{{{}}} & \\num{{{}}}".format(
        activity.person_id,
        activity.activity_id,
        format_time(activity.start_time),
        format_time(activity.end_time),
        activity.activity_type,
        activity.latitude,
        activity.longitude
    ))
    print(r"\tabularnewline")
    print(r"\hline")
print(r"\end{tabular}}")


# Projecting activities
def apply_coords(row: pd.Series):
    coords = roadmaptools.utm.wgs84_to_utm(row["latitude"], row["longitude"], projection)
    row["latitude"] = coords[1]
    row["longitude"] = coords[0]
    return row


activities = activities.apply(apply_coords, axis=1)

# plotting activities
axis.scatter(activities["longitude"], activities["latitude"], color="black")

min_lat = activities["latitude"].min()
max_lat = activities["latitude"].max()
min_lon = activities["longitude"].min()
max_lon = activities["longitude"].max()

arrow_width = 0.4
arrow_length = 0.8


# plotting trips
def comp_length_for_arrow(to_coord, from_coord, diff):
    length = to_coord - from_coord
    length = length - diff if length > 0 else length + diff

    return length


prop = dict(arrowstyle="-|>,head_width={},head_length={}".format(arrow_width, arrow_length),
            shrinkA=0,
            shrinkB=0,
            color="black")


def plot_trip(trip, shift):
    from_act = activities[(activities.person_id == trip.person_id) &
                          (activities.activity_id == trip.from_activity_id)].iloc[0]
    to_act = activities[(activities.person_id == trip.person_id) &
                        (activities.activity_id == trip.to_activity_id)].iloc[0]

    axis.annotate("",
                  xy=(to_act.longitude,to_act.latitude),
                  xytext=(from_act.longitude, from_act.latitude),
                  arrowprops=prop)
    label = "{}".format(trip.transport_type)
    draw_bottom_left = ((to_act.longitude + from_act.longitude) / 2 + shift[0], (to_act.latitude + from_act.latitude) / 2 + shift[1])
    axis.annotate(label,
                  draw_bottom_left,
                  bbox=dict(boxstyle='square,pad=0.01', fc='white', ec='none'))
    # axis.arrow(from_act.latitude,
    #            from_act.longitude,
    #            comp_length_for_arrow(to_act.latitude, from_act.latitude, (max_lat - min_lat) / 100),
    #            comp_length_for_arrow(to_act.longitude, from_act.longitude, (max_lon - min_lon) / 100),
    #            width=0.0001,
    #            head_width=0.002,
    #            head_length=0.005,
    #            length_includes_head=True,
    #            # head_starts_at_zero=True,
    #            fc='k',
    #            ec='k')


shifts_trips = [
    (0, -550),
    (-400, 50),
    (-400, 300),
    (100, -300),
    (-500, 200),
    (-100, 150),
    (50, 0)
]
for index, trip in enumerate(trips.itertuples()):
    plot_trip(trip, shifts_trips[index])

# Activity labels
shifts_raw = [
    (55, -30),
    (180, -20),
    (100, -40),
    (10, 40),
    (55, 40),
    (180, -20),
    (50, 40),
    (200, 30),
    (-25, 25)
]
res = (max_lon - min_lon) / 1000
shifts = []
for shift in shifts_raw:
    shifts.append((shift[0] * res, shift[1] * res))


for index, row in enumerate(activities.itertuples()):
    label = "{}-{}: {}".format(row.person_id, row.activity_id, row.location_id)
    draw_bottom_left = (row.longitude - shifts[index][0], row.latitude - shifts[index][1])
    # draw_bottom_left = (row.longitude, row.latitude)
    axis.annotate(label,
                  draw_bottom_left,
                  bbox=dict(boxstyle='square,pad=0.01', fc='white', ec='none'))

# # # road network
# fc = roadmaptools.inout.load_geojson(config.main_roads_graph_filepath)
# xList, yList = roadmaptools.plotting.export_edges_for_matplotlib(roadmaptools.plotting.geojson_edges_iterator(fc))
# axis.plot(xList, yList, linewidth=0.2, color='black', zorder=1)

# dense road network
fc = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
xList, yList = roadmaptools.plotting.export_edges_for_matplotlib(roadmaptools.plotting.geojson_edges_iterator(fc))
axis.plot(xList, yList, linewidth=0.2, color='gray', zorder=1)

# remove ticks
axis.set_xticklabels([])
axis.set_yticklabels([])
axis.tick_params(
	which='both',  # both major and minor ticks are affected
	bottom=False,  # ticks along the bottom edge are off
	top=False,  # ticks along the top edge are off
	labelbottom=False, right=False, left=False, labelleft=False, labelright=False, labeltop=False)

# zoom to trips
buffer = 1000
axis.set_xlim(min_lon - buffer, max_lon + buffer)
axis.set_ylim(min_lat - buffer, max_lat + buffer)
axis.set_aspect('equal', adjustable='box', anchor='C')
# plt.axis('scaled')

plt.tight_layout(h_pad=1)


plt.savefig(config.images.demand_example, bbox_inches='tight', transparent=True, pad_inches=0.01)

plt.show()