import csv
import os
from tqdm import tqdm

input_file_path = r"O:\AIC data\data\speeds/manhattan-2018-05-saturdays.csv"
output_file_path = r"O:\AIC data\data\speeds/manhattan-2018-05-saturdays-aggregated.csv"

class Record:
    def __init__(self):
        self.sum = 0
        self.count = 0

with open(input_file_path, 'r') as input_file, open(output_file_path, 'w') as output_file:
    reader = csv.reader(input_file)
    print("Loading csv file from: {}".format(os.path.realpath(input_file_path)))

    # header
    header = next(reader)
    output_file.write("from,to,way,speed\n")

    records = dict()
    # count = 0
    for row in tqdm(reader):
        key = (int(row[9]), int(row[10]), int(row[8]))
        value = float(row[11])

        if key not in records:
            records[key] = Record()

        records[key].sum += value
        records[key].count += 1
        # count += 1
        # if count > 9:
        #     break

    print("Writing avg speeds to: {}".format(os.path.realpath(output_file_path)))
    for key, record in tqdm(records.items()):
        avg = record.sum / record.count
        output_file.write("{},{},{},{}\n".format(key[0], key[1], key[2], avg))