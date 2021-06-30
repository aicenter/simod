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
import os
import json
import pickle

from roadmaptools.printer import print_info


def load_json_file(filepath):
    pickle_filepath = os.path.splitext(filepath)[0]+'.pickle'
    if os.path.isfile(pickle_filepath):
        print_info("loading file from cache: " + pickle_filepath)
        data = pickle.load(open(pickle_filepath, 'r'))
    else:
        print_info("loading json file: " + filepath)
        data = json.load(open(filepath, 'r'))

        print_info("saving cache to: " + pickle_filepath)
        pickle.dump(data, open(pickle_filepath, 'w'), protocol=2)
    return data