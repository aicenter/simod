import csv
import os
from tqdm import tqdm

input_file_path = r"O:\AIC data\data\speeds/movement-speeds-hourly-new-york-2018-5.csv"
output_file_path = r"O:\AIC data\data\speeds/manhattan-2018-05-saturdays.csv"

days = {4, 11, 18, 25}

with open(input_file_path, 'r') as input_file, open(output_file_path, 'w') as output_file:
    reader = csv.reader(input_file)
    print("Loading csv file from: {}".format(os.path.realpath(input_file_path)))

    # header
    header = next(reader)
    output_file.write(",".join(header) + "\n")

    for row in tqdm(reader):
        if int(row[2]) in days:
            output_file.write(",".join(row) + "\n")