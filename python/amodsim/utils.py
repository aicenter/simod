
def to_percetnt(float_number, decimals=1):
    pattern = "{0:." + str(decimals) + "f}%"
    return pattern.format(float(float_number) * 100)


def col_to_percent(collection):
    i = 0
    for float_number in collection:
        collection[i] = to_percetnt(float_number)
        i += 1
    return collection