import csv
import os
from tqdm import tqdm

input_file_path = r"O:\AIC data\data\speeds/movement-speeds-quarterly-by-hod-new-york-2018-Q2.csv"
output_file_path = r"O:\AIC data\data\speeds/new_york-2018-Q2-19h.csv"

hour = 19

with open(input_file_path, 'r') as input_file, open(output_file_path, 'w') as output_file:
    reader = csv.reader(input_file)
    print("Loading csv file from: {}".format(os.path.realpath(input_file_path)))

    # header
    header = next(reader)
    output_file.write(",".join(header) + "\n")

    for row in tqdm(reader):
        if int(row[2]) == hour:
            output_file.write(",".join(row) + "\n")