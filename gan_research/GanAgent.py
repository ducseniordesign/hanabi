#!/usr/bin/env python3
import keras
import numpy as np

# receive 571 '1's or '0's on stdin
# print 20 '1's or '0's on stdout

data = np.array([ord(bit)-48 for bit in input()]).reshape(1,571)
model = keras.models.load_model("GanAgentModel.h5")
action = model.predict(data)
quantized = [0] * action.shape[1]
quantized[max(enumerate(action[0]), key=lambda x: x[1])[0]] = 1
print("".join([chr(bit+48) for bit in quantized]))
