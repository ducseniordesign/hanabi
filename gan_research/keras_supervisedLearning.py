import keras
import math
import numpy as np
import pandas as pd
from keras.models import Sequential
from keras.layers import Dense, Activation

datafile = open("iggi.txt", mode = 'r', newline = '\n')

model = Sequential()
model.add(Dense(512, input_shape=(829,), activation = 'relu', kernel_initializer = keras.initializers.VarianceScaling(scale = 1.0 / np.sqrt(3.0), mode = 'fan_in', distribution='normal')))
model.add(Dense(512, activation = 'relu', kernel_initializer = keras.initializers.VarianceScaling(scale = 1.0 / np.sqrt(3.0), mode = 'fan_in', distribution='normal')))
model.add(Dense(22, activation = 'sigmoid', kernel_initializer = keras.initializers.VarianceScaling(scale = 1.0 / np.sqrt(3.0), mode = 'fan_in', distribution='normal')))
opt = keras.optimizers.SGD(lr= 0.001)
model.compile(optimizer = opt , loss = keras.losses.mean_squared_logarithmic_error, metrics = ['accuracy'])

inputs = []
outputs = []
samples = 0
for data in datafile:
    line = list(map(int, data[:892]))
    inputs.append(np.array(line[:829]))
    outputs.append(np.array(line[829:851]))

training = 0.95
training_samples = math.floor(len(outputs) * training)
x_train = np.array(inputs[:training_samples])
y_train = np.array(outputs[:training_samples])
x_test = np.array(inputs[training_samples:])
y_test = np.array(outputs[training_samples:])

model.fit(x_train, y_train, epochs = 10, batch_size= 32)
score = model.evaluate(x_test, y_test, batch_size= 32)
print(score)