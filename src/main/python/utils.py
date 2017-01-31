
def to_percetnt(float_number):
    return "{0:.1f}%".format(float(float_number) * 100)


def col_to_percent(collection):
    i = 0
    for float_number in collection:
        collection[i] = to_percetnt(float_number)
        i += 1
    return collection