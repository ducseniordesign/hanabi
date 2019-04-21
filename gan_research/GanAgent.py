#!/usr/bin/env python3
import keras
import math
import numpy as np
import pandas as pd
from keras.models import Sequential
from keras.layers import Dense, Activation
import sys

for line in sys.stdin:
    data = list(map(int, line[1:-1].split(",")))
    data = np.array((data)).reshape(1,571)

modelfile = "GanAgentModel.h5"
model = keras.models.load_model(modelfile)
action = model.predict(data)
#print(action)
for val in np.nditer(action):
    print(val)


