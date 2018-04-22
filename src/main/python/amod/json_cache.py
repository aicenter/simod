import os
import json
import cPickle as pickle

from scripts.printer import print_info


def load_json_file(filepath):
    pickle_filepath = os.path.splitext(filepath)[0]+'.pickle'
    if(os.path.isfile(pickle_filepath)):
        print_info("loading file from cache: " + pickle_filepath)
        data = pickle.load(open(pickle_filepath, 'r'))
    else:
        print_info("loading json file: " + filepath)
        data = json.load(open(filepath, 'r'))

        print_info("saving cache to: " + pickle_filepath)
        pickle.dump(data, open(pickle_filepath, 'w'), protocol=2)
    return data