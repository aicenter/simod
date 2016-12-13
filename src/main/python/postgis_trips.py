
import psycopg2
import numpy as np
import json

db_name = 'sck_log' # 'postgres'
user = 'postgres' # 'martin'
password = "fidofido"

LINESTRING = "14.5324125 50.177223, 14.530652 50.166475, 14.5343915 50.1617465, 14.5506445 50.166111, " \
             "14.561158 50.1615335, 14.5604855 50.155236, 14.563052 50.1501795, 14.578664 50.150234, " \
             "14.5893995 50.15444, 14.5962215 50.1538715, 14.593079 50.148537, 14.588623 50.147205, " \
             "14.587994 50.1442465, 14.6011295 50.1291565, 14.608626 50.127149, 14.632183 50.129896, " \
             "14.6350225 50.123983, 14.6589595 50.122349, 14.657637 50.106499, 14.6659325 50.102681, " \
             "14.6911885 50.0999505, 14.688568 50.0987555, 14.6892235 50.096168, 14.7036545 50.0920105, " \
             "14.706855 50.0870425, 14.700559 50.072406, 14.6893445 50.071058, 14.670059 50.06578, 14.640119 50.0568635, " \
             "14.6401265 50.0483645, 14.643978 50.042293, 14.6541035 50.049341, 14.6672025 50.0383605, " \
             "14.6572335 50.0378145, 14.656326 50.0310415, 14.6693535 50.016293, 14.657943 50.0043975, " \
             "14.6500375 50.0088255, 14.638792 50.005941, 14.647167 49.9987595, 14.6400105 49.9942865," \
             " 14.612182 49.998967, 14.608112 50.002369, 14.603484 50.0017495, 14.601994 50.009465, " \
             "14.5891425 50.0087145, 14.581365 50.011025, 14.5818635 50.015115, 14.568318 50.007504, " \
             "14.557651 50.0119045, 14.5538065 50.0121105, 14.550897 50.008107, 14.5357255 50.01197, " \
             "14.521158 50.0077045, 14.5300735 50.0006755, 14.5283495 49.9997695, 14.5134385 49.9941295, " \
             "14.509312 49.9939015, 14.507311 49.9974395, 14.484229 49.992454, 14.4840995 49.9876445, " \
             "14.4869405 49.9858925, 14.4763645 49.9842245, 14.4665925 49.980984, 14.465133 49.975256, " \
             "14.461463 49.970848, 14.4444785 49.973805, 14.4390895 49.968181, 14.421655 49.963686, 14.40191  " \
             "49.9709325, 14.394896 49.955333, 14.3961955 49.947311, 14.3953835 49.9416465, 14.388907 49.9452755, " \
             "14.3874745 49.949942, 14.373211 49.9466245, 14.368904 49.9516005, 14.360329 49.9473955, " \
             "14.3383055 49.9483545, 14.326421 49.956505, 14.3257975 49.959575, 14.332818 49.966746, " \
             "14.3435565 49.9668905, 14.3472335 49.9727055, 14.326764 49.971491, 14.3389175 49.981167, " \
             "14.342394 49.990646, 14.33564  49.993913, 14.318033 49.988983, 14.307296 49.9961175, 14.306503 49.9969995, " \
             "14.295416 50.002188, 14.3008055 50.011461, 14.3110275 50.0073595, 14.315295 50.016516, 14.315528 50.02388," \
             " 14.296502 50.0243005, 14.2945545 50.0256255, 14.29019  50.0269395, 14.2892795 50.0274095, " \
             "14.270526 50.040381, 14.271348 50.0542935, 14.2641225 50.05233, 14.2573445 50.054311, " \
             "14.2479785 50.0582845, 14.2475345 50.0622795, 14.2580335 50.0714725, 14.2903805 50.0747155, " \
             "14.283675 50.0817475, 14.2713535 50.086199, 14.2611625 50.0877955, 14.260965 50.096378, " \
             "14.2583025 50.099413, 14.2526175 50.1021415, 14.244847 50.1028525, 14.226279 50.1004925, " \
             "14.224306 50.102919, 14.240733 50.1112535, 14.249797 50.110651, 14.273692 50.1174805, " \
             "14.278362 50.11896, 14.2849015 50.1151035, 14.297468 50.120873, 14.2951095 50.124559, " \
             "14.301255 50.129522, 14.3104105 50.12802, 14.315712 50.1230495, 14.320514 50.115188, " \
             "14.361376 50.1163075, 14.3561415 50.1255345, 14.3540915 50.137475, 14.3560445 50.1402565, " \
             "14.3717645 50.148095, 14.384434 50.147251, 14.3929355 50.1413685, 14.398479 50.1434345, " \
             "14.3995535 50.1479155, 14.39971022 50.14789667, 14.39971926 50.14792943, 14.4222775 50.1498245, " \
             "14.4232875 50.152728, 14.428311 50.157558, 14.430109 50.158026, 14.4640855 50.1597865, " \
             "14.4669115 50.1695615, 14.4978825 50.17204, 14.507307 50.17127, 14.50944 50.1741785, " \
             "14.5268895 50.1774435, 14.5324125 50.177223"


def connect():
    try:
        conn = psycopg2.connect("host=localhost dbname=" + db_name + " user=" + user + " password=" + password)
    except Exception, e:
        print("I am unable to connect to the database - " + str(e))

    return conn


def disconnect(connection):
    connection.commit()
    connection.close();


def get_legs(cursor, connection, mode="CAR", limit=50):
    sqlfilter = "";

    if limit > 0:
        LIMIT = " LIMIT " + str(limit)
    else:
        LIMIT = ""

    # sqlfilter = "WHERE ST_Within(ST_GeomFromEWKT(path), ST_MakeEnvelope(14.2714931, 49.9868519, 14.5966197, 50.1519053, 4326))"
    # sqlfilter = "WHERE ST_Within(ST_GeomFromEWKT(path), ST_MakePolygon(ST_GeomFromText('LINESTRING(" + LINESTRING + ")', 4326)))"



    if mode != "":
        sqlfilter += " AND legs.type='""" + mode + "' "

    # sqlfilter += " AND ST_Within(ST_Startpoint(ST_GeomFromEWKT(legs.path)), ST_MakeEnvelope(14.376, 49.9910, 14.379, 49.994, 4326))"

    # sqlfilter += " AND legs.trip_id = 4864398"

    cursor.execute("SET search_path TO \"$user\", public, topology, sck_log")

    query = """SELECT legs.trip_id, legs.start_time, legs.end_time, legs.type, ST_AsGeoJSON(ST_GeomFromEWKT(legs.path))
                  FROM leg_log AS legs LEFT JOIN leg_log AS outer_legs
                    ON NOT ST_Within(ST_GeomFromEWKT(outer_legs.path), ST_MakePolygon(ST_GeomFromText('LINESTRING(""" + LINESTRING + """)', 4326)))
			          AND outer_legs.trip_id = legs.trip_id
                  WHERE outer_legs.trip_id IS NULL """ + sqlfilter + LIMIT + " ;"

    # query = "SELECT COUNT(trip_id) from leg_log WHERE " \
    #         "ST_GeomFromEWKT(path) && ST_MakeEnvelope(14.2714931, 49.9868519, 14.5966197, 50.1519053, 4326) AND type = 'CAR'"

    cursor.execute(query)
    trips = cursor.fetchall()
    return trips


def parse_leg(leg):
    trip_id = leg[0]
    starttime = leg[1]
    endtime = leg[2]
    mode = leg[3]
    geometrystr = leg[4]

    geometry = json.loads(geometrystr)
    path = np.array(geometry['coordinates'])

    return (trip_id, starttime, endtime, mode, path)


def legs2od(legs):
    odmatrix = np.zeros((len(legs), 6), dtype=np.float64)
    for i in range(len(legs)):
        leg = parse_leg(legs[i])

        odmatrix[i, 0] = leg[1]
        odmatrix[i, 1] = leg[2]

        odmatrix[i, 2] = leg[4][0][1]
        odmatrix[i, 3] = leg[4][0][0]

        odmatrix[i, 4] = leg[4][-1][1]
        odmatrix[i, 5] = leg[4][-1][0]

    return odmatrix


def load_trips(limit):
    connection = connect()
    cursor = connection.cursor()
    print("Getting legs from the database...")
    legs = get_legs(cursor, connection, "CAR", limit)
    print(str(len(legs)) + " legs successfuly loaded.")
    disconnect(connection)
    print("Parsing...")
    trips = legs2od(legs)
    return trips




