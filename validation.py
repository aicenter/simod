import pandas as pd
import numpy as np
import os
import sys

import requests
import logging

import time

pd.options.mode.chained_assignment = None  # default='warn'

timestr = time.strftime("%Y%m%d-%H%M%S")

logger = None
error_logger = None
scores_logger = None


''' from https://stackoverflow.com/questions/11232230/logging-to-two-files-with-different-settings '''
formatter = logging.Formatter('%(asctime)s %(funcName)20s \n %(message)s')
handlers = []
def setup_logger(name, log_file, level=logging.DEBUG):
    """Function setup as many loggers as you want"""

    handler = logging.FileHandler(log_file)
    handler.setFormatter(formatter)

    logger = logging.getLogger(name)
    logger.setLevel(level)
    logger.addHandler(handler)
    handlers.append(handler)
    return logger

def close_loggers():
    for handler in handlers:
        handler.close()


#   IMPORTANT: Change names here.
#   TODO: Make them commandline arguments
def read_data_frames(csv_to_test):
    df = pd.read_csv(csv_to_test+'.csv')
    actual_order_df = pd.read_csv('robotex2.csv', engine='python')
    actual_order_df['passenger_id'] = np.arange(len(actual_order_df))
    merged_df = pd.merge(left=df, right=actual_order_df, left_on='passenger_id', right_on='passenger_id')
    merged_df['flagged'] = 0 # add a column for flagging rows
    return actual_order_df, df, merged_df

#   Returns distance in m between two coordinates - NEEDS OSRM
def distance_between_lat_lng(start_lat, start_lng, dest_lat, dest_lng):
    source_coordinates = str(start_lng)+','+str(start_lat)+';'
    dest_coordinates = str(dest_lng)+','+str(dest_lat)
    url =  'http://127.0.0.1:5000/route/v1/driving/'+source_coordinates+dest_coordinates

    payload = {"overview":"simplified","steps":"false","geometries":"geojson"}
    response = requests.get(url,params=payload)
    data = response.json()
    return (data['routes'][0]['distance'])


# TESTS

#---ORDER TESTS---#
#   Every order used only once (every passenger_id once)
def unique_orders(test_df, ao_df, merged_df):
    print("--Test unique_orders--")
    passenger_ids = test_df['passenger_id']
    if (passenger_ids.is_unique):
        logger.debug('PASS')
    else:
        logger.debug('FAIL')
        duplicates = test_df[test_df.duplicated(['passenger_id'], keep=False)]

        error_logger.debug('The following orders were not unique'.upper())
        error_logger.debug(duplicates.to_string() + '\n')
        duplicates['flagged'] = 1
        merged_df.update(duplicates)

#   Every order in output log actually exists in actual orders
def real_orders(test_df, ao_df, merged_df):
    print("--Test real_orders--")
    passenger_ids = test_df['passenger_id']
    total_number_of_rows = ao_df.shape[0] # actual number of rows
    if (passenger_ids.max() <= (total_number_of_rows-1)
        and passenger_ids.min() >= 0):
        logger.debug('PASS')
    else:
        logger.debug('FAIL')
        fake_orders = test_df.loc[(test_df['passenger_id'] > (total_number_of_rows -1)) |
                                  (test_df['passenger_id'] < 0)]

        error_logger.debug('The following passenger_ids do not exist'.upper())
        error_logger.debug(fake_orders.to_string() + '\n')
        fake_orders['flagged'] = 1
        merged_df.update(fake_orders)

#   Every order has right price
def right_price(test_df, ao_df, merged_df):
    print("--Test right_price--")
    wrong_price = merged_df.loc[(merged_df['ride_value_x'] != merged_df['ride_value_y'])]
    if (wrong_price.shape[0] == 0):
        logger.debug('PASS')
    else:
        logger.debug('FAIL')
        error_logger.debug('For the given ride, the prices do not match with given data'.upper())
        error_logger.debug(wrong_price.to_string()+'\n')
        wrong_price['flagged'] = 1
        merged_df.update(wrong_price)


def rider_decay_check(row):
    order_start_time = pd.to_datetime(row.start_time)
    ride_start_time = pd.to_datetime(row.ride_start_time)
    return int((ride_start_time - order_start_time).seconds <= 180)

#   Every order picked up within 3min
def pickup_time(test_df, ao_df, merged_df):
    print("--Test pickup_time within 3min--")
    merged_df['valid_waittime'] = merged_df.apply(rider_decay_check, axis = 1)
    invalid_waittime = merged_df.loc[merged_df['valid_waittime'] == 0]
    if (invalid_waittime.shape[0] == 0):
        logger.debug('PASS')
    else:
        logger.debug('FAIL')
        error_logger.debug('For the given passengers, the waittime was too long'.upper())
        error_logger.debug(invalid_waittime.to_string()+'\n')
        invalid_waittime['flagged'] = 1
        merged_df.update(invalid_waittime)
        print('INVALID COUNT {}'.format(invalid_waittime.shape[0]))

#   Every order takes "approx" right amount of time to reach to destination - not too fast
#   TODO: Check how to get actual shortest distance!!!

def dif_with_osrm_distance(row):
    return distance_between_lat_lng(row['pickup_lat'], row['pickup_lng'], row['dropoff_lat'], row['dropoff_lng'])-row['distance']

def dif_with_direct_distance(row):
    return ((abs(row["pickup_lat"] - row["pickup_lat"]) ** 2 + (abs(row["dropoff_lng"] - row["dropoff_lng"]) / 2) ** 2) ** 0.5 * 111000)-row['distance']

CAR_SPEED = 50 #km/h
def actual_distance(row):
    return ((pd.to_datetime(row['ride_end_time']) - pd.to_datetime(row['ride_start_time'])).seconds)/3600*CAR_SPEED*1000


ERROR_ALLOWED = 0.5 #km
def not_flying_approx(test_df, ao_df, merged_df):
    print("--Test not_flying_approx--")
    merged_df["distance"] = merged_df.apply(actual_distance, axis=1)
    merged_df['direct_distance_greater'] = merged_df.apply(dif_with_direct_distance, axis=1)
    merged_df['osrm_distance_greater'] = merged_df.apply(dif_with_osrm_distance, axis=1)

    less_than_direct = merged_df.loc[merged_df['direct_distance_greater'] > 0]


    if (less_than_direct.shape[0] == 0):
        logger.debug('PASS')
    else:
        logger.debug('FAIL')
        error_logger.debug('For these rides, it appears car travelled faster than straight line'.upper())
        error_logger.debug(less_than_direct.to_string()+'\n')
        less_than_direct['flagged'] = 1
        merged_df.update(less_than_direct)

    #   TODO: Check how use this (OSRM gives fastest not shortest route, might have to change profile settings)
    less_than_osrm = merged_df.loc[merged_df['osrm_distance_greater'] > 0]
    if (less_than_osrm.shape[0] != 0):
        logger.debug('WARNING')
    else:
        logger.debug('WARNING')
        error_logger.debug('For these rides, it appears car travelled faster than OSRM'.upper())
        error_logger.debug(less_than_osrm.to_string()+'\n')
        less_than_osrm['flagged'] = 1
        merged_df.update(less_than_osrm)

#   Every order takes max 30min (start to finish)

#---CAR TESTS---#
#   Every car can possibly reach to the next order within timeframe


#   Every car has max 4 ppl on at the same time
#   TODO: check if correct
def max_pool_count(test_df, ao_df, merged_df):
    print("--Test max_pool_count--")
    test_passed = True

    unique_car_id = merged_df['car_id'].unique()
    for c_id in unique_car_id:
        car_series = merged_df.loc[merged_df['car_id'] == c_id]
        car_series['ride_start_time'] = pd.to_datetime(car_series['ride_start_time'])
        car_series = car_series.sort_values(by=['ride_start_time'])
        people_in_car = 0
        end_times = []
        for row in car_series.itertuples():
            given_start_time = pd.to_datetime(row[3])
            given_end_time = pd.to_datetime(row[4])
            end_times.append(given_end_time)
            for time in end_times:
                if time <= given_start_time:
                    end_times.remove(time)
                    people_in_car = people_in_car - 1
            people_in_car += 1
            if (people_in_car > 4):
                test_passed = False
                logger.debug('FAIL')
                error_logger.debug('This car has more than 4 people in the car at the same time'.upper() + '\n car_id: {} at time: {} \n'.format(c_id, given_start_time))
                too_many_ppl = merged_df.loc[(merged_df['car_id'] == c_id) & (merged_df['passenger_id'] == row[2])]
                too_many_ppl['flagged'] = 1
                merged_df.update(too_many_ppl)

    if test_passed:
        logger.debug('PASS')

#   Every car charges after driving for 200km



# SCORE

#   Money earned
def calc_money_earned(test_df, ao_df, merged_df):
    valid_rides = merged_df.loc[merged_df['flagged'] == 0]
    print(valid_rides.shape)
    earned_ride_value = valid_rides['ride_value_x'].sum()
    total_ride_value = ao_df['ride_value'].sum()
    percentage = earned_ride_value/float(total_ride_value)*100
    scores_logger.debug("Earned a total of {} out of {} ({}%)".format(earned_ride_value, total_ride_value, percentage))
##    print("Earned a total of {} out of {} ({}%)".format(earned_ride_value, total_ride_value, percentage))

#   Cars used
MAX_CARS_ALLOWED = 10000
def calc_cars_used(test_df, ao_df, merged_df):
    valid_rides = merged_df.loc[merged_df['flagged'] == 0]
    unique_car_id = valid_rides['car_id'].unique()
    unique_count = len(unique_car_id)
    percentage = unique_count/float(MAX_CARS_ALLOWED)*100
    scores_logger.debug("Used a total of {} cars out of {} ({}%)".format(unique_count, MAX_CARS_ALLOWED, percentage))

#   END

def calculate_scores(test_df, ao_df, merged_df):
    calc_money_earned(test_df, ao_df, merged_df)
    calc_cars_used(test_df, ao_df, merged_df)

def do_tests(test_df, ao_df, merged_df):
    unique_orders(test_df, ao_df, merged_df)
    real_orders(test_df, ao_df, merged_df)

    right_price(test_df, ao_df, merged_df)
    pickup_time(test_df, ao_df, merged_df)
##    not_flying_approx(test_df, ao_df, merged_df)
    max_pool_count(test_df, ao_df, merged_df)

if __name__ == "__main__":
    csv_to_test = ""
    if (len(sys.argv) > 1):
        csv_to_test = sys.argv[1]
    if (csv_to_test == ""):
        print("Please pass a csv file as an argument or hardcode it in as csv_to_test (for example your csv file name is 'results_x.csv' you run ./validation.py results_x)")
    else:
        if not os.path.isdir('outputs'):
            os.mkdir('outputs')
        # first file logger
        logger = setup_logger('first_logger', 'outputs/'+csv_to_test+'_TEST_OUTPUT_'+timestr+'.log')
        # second file logger
        error_logger = setup_logger('second_logger', 'outputs/'+csv_to_test+'_FAILED_CASES_'+timestr+'.log')
        #scores logger
        scores_logger = setup_logger('scores_logger', 'outputs/'+csv_to_test+'_SCORES_'+timestr+'.log')

        ao_df, test_df, merged_df = read_data_frames(csv_to_test)

        do_tests(test_df, ao_df, merged_df)
        calculate_scores(test_df, ao_df, merged_df)

        close_loggers()
