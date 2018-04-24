from __future__ import print_function, division

import numpy as np
import pandas as pd

df = pd.DataFrame({'A' : ['foo', 'bar', 'foo', 'bar', 'foo', 'bar', 'foo', 'foo'],
                   'B' : ['one', 'one', 'two', 'three', 'two', 'two', 'one', 'three'],
                    'C' : np.random.randn(8), 'D' : np.random.randn(8)})