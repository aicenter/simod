#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of the SiMoD project.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
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