import tensorflow as tf

import numpy as np
import scipy
from scipy.io import wavfile
import time
import threading
from queue import Queue
import os
import re
import math
from random import *
import urllib.request
import matplotlib.pyplot as plt

from tensorflow.contrib.signal import hamming_window
from tensorflow.contrib.rnn import MultiRNNCell, GRUCell, DropoutWrapper, LSTMCell


def generate_mel_transform(start_mel_frequency, end_mel_frequency, mel_filters, frame_length):
    mel_points = np.linspace(start_mel_frequency, end_mel_frequency,
                             mel_filters + 2)  # Equally spaced in Mel scale
    hz_points = (700 * (10 ** (mel_points / 2595) - 1))  # Convert Mel to Hz
    f_bin = np.floor((frame_length + 1) * hz_points / DataGenerator.sample_frequency)

    f_bank = np.zeros((mel_filters, int(np.floor(frame_length / 2 + 1))))
    for m in range(1, mel_filters + 1):
        f_m_minus = int(f_bin[m - 1])  # left
        f_m = int(f_bin[m])  # center
        f_m_plus = int(f_bin[m + 1])  # right

        for k in range(f_m_minus, f_m):
            f_bank[m - 1, k] = (k - f_bin[m - 1]) / (f_bin[m] - f_bin[m - 1])
        for k in range(f_m, f_m_plus):
            f_bank[m - 1, k] = (f_bin[m + 1] - k) / (f_bin[m + 1] - f_bin[m])
        f_bank[m - 1, f_m] = 1

    return f_bank.T


class DataGenerator:
    data_mean = -10.5589236
    data_var = 8.025949716970832656 ** .5

    sample_frequency = 44100

    guitars_folder = "sounds/guitars"
    noise_folder = "sounds/noise"
    instruments_folder = "sounds/instruments"

    data_start_pitch = 24
    data_end_pitch = 84

    # data constants

    min_overall_volume = math.log10(1 / 4)
    max_overall_volume = math.log10(4)
    initial_pause = .1
    pause_duration = .2
    lstm_delay = 0

    bpm_mean = 200
    bpm_sd = 15
    min_bpm = 10

    layers_of_noise = 3
    noise_volume_sd = math.log(.9, 10)
    noise_volume = .25

    note_volume_sd = math.log(.9, 10)

    note_duration_max = 2.4  # in seconds
    note_duration_min = .1  # in seconds
    resonance_volume_mean = 0.05
    resonance_volume_sd = 0.02
    resonance_amount = 4

    chord_delay_sd = .0002
    chord_delay_factor = .002

    roll = 3  #

    def __init__(self):

        self.guitars = self.load_sound_from(DataGenerator.guitars_folder)
        self.noise = self.load_sound_from(DataGenerator.noise_folder)
        self.instruments = self.load_sound_from(DataGenerator.instruments_folder)

        self.chords = DataGenerator.load_chords()

        self.queue = Queue()
        self.thread = GeneratorThread(self)
        self.thread.start()

    @property
    def generate_batch(self):

        num_frames = int(np.ceil(float(np.abs(Trainer.example_length - Model.frame_length)) / Model.frame_step))

        data_in = np.zeros((Trainer.batch_size, num_frames, Model.mel_filters), np.float32)
        data_out = np.zeros((Trainer.batch_size, num_frames, Model.end_pitch - Model.start_pitch, 2), np.float32)
        data_out[:, :, :] = [1, 0]

        for example in range(0, Trainer.batch_size):

            volume = 10 ** uniform(DataGenerator.min_overall_volume, DataGenerator.max_overall_volume)

            guitar = choice(self.guitars)
            notes = []
            noises = []

            for noise_type in sample(self.noise, DataGenerator.layers_of_noise):
                current_volume = DataGenerator.noise_volume * (10 ** gauss(0, DataGenerator.noise_volume_sd)) / np.sqrt(
                    DataGenerator.layers_of_noise)
                start_offset = randrange(0, noise_type.length)

                noises.append(Noise(start_offset, noise_type, current_volume))

            # out = open("D:/test.csv", "a")

            current_time = DataGenerator.sample_frequency * DataGenerator.initial_pause
            last = []

            while current_time < Trainer.example_length - Model.frame_step:

                action = DataGenerator.weighted_choice(
                    {"note": 8, "chord": 6, "repeat": 0 if len(last) == 0 else 1, "guitar_swap": 1, "pause": 4})

                if action == "note":  # note

                    for note in last:
                        note.duration = int(
                            max(min(current_time - note.start_time - uniform(0, 500), note.duration), 0))

                    last = []

                    duration = DataGenerator.sample_frequency * (.5 * random() * (
                            DataGenerator.note_duration_max - DataGenerator.note_duration_min) + DataGenerator.note_duration_min)
                    pitch = randrange(guitar.range.start, guitar.range.stop)
                    current_volume = 10 ** gauss(0, DataGenerator.note_volume_sd)
                    roll = randrange(-DataGenerator.roll, DataGenerator.roll)

                    current = Note(current_time, min(duration, Trainer.example_length - current_time), pitch, roll,
                                   guitar, current_volume,
                                   DataGenerator.lstm_delay * Model.samples_per_dft, "note")
                    notes.append(current)
                    for res in self.make_resonances(current):
                        notes.append(res)

                    current_time += duration * uniform(.4, .6)

                elif action == "chord":  # chord

                    for note in last:
                        note.duration = int(
                            max(min(current_time - note.start_time - uniform(0, 500), note.duration), 0))

                    last = []

                    chord_type = DataGenerator.weighted_choice(dict(map(lambda x: (x, x.weight), self.chords)))
                    chord_notes = list(filter(lambda x: x in guitar.range, chord_type.notes))
                    current_volume = 10 ** gauss(0, DataGenerator.note_volume_sd)

                    duration = DataGenerator.sample_frequency * (.25 + .25 * random() * (
                            DataGenerator.note_duration_max - DataGenerator.note_duration_min) + DataGenerator.note_duration_min)

                    for index, note in enumerate(chord_notes):
                        roll = randrange(-DataGenerator.roll, DataGenerator.roll)
                        start_sample = current_time + gauss(index * DataGenerator.chord_delay_factor,
                                                            DataGenerator.chord_delay_sd) * DataGenerator.sample_frequency
                        current = Note(start_sample,
                                       min(duration, Trainer.example_length - start_sample), note, roll,
                                       guitar, current_volume,
                                       DataGenerator.lstm_delay * Model.samples_per_dft,
                                       "chord(" + chord_type.name + ")")
                        last.append(current)
                        for res in self.make_resonances(current):
                            notes.append(res)
                        notes.append(current)

                    current_time += duration * uniform(.4, .9)

                elif action == "repeat":  # repeat

                    for note in last:
                        note.duration = int(
                            max(min(current_time - note.start_time - uniform(0, 500), note.duration), 0))

                    new_last = []
                    for note in last:
                        current = Note(current_time, note.duration, note.pitch,
                                       note.roll, note.guitar, note.volume,
                                       note.output_delay, "re" + note.origin, note.outs)

                        notes.append(current)
                        new_last.append(current)
                        for res in self.make_resonances(current):
                            notes.append(res)

                    current_time += last[0].duration * uniform(.4, .9)
                    last = new_last

                elif action == "guitar_swap":  # switch guitar
                    guitar = choice(self.guitars)

                elif action == "pause":  # skip
                    current_time += DataGenerator.sample_frequency * DataGenerator.pause_duration * uniform(1, 1.5)

            signal = np.zeros([Trainer.example_length])

            for noise in noises:
                signal += noise.volume * np.roll(
                    np.tile(noise.noise_data.data, int(math.ceil(len(signal) / noise.noise_data.length))),
                    noise.start_offset)[0:signal.size]

            for note in notes:
                if not note.outs:
                    continue

                data_start_sample = int(
                    (note.pitch - Model.start_pitch) * DataGenerator.note_duration_max * DataGenerator.sample_frequency)

                start_frame = int(
                    (note.start_time + note.output_delay) * Model.dft_rate / DataGenerator.sample_frequency)
                end_frame = min(int((note.start_time + note.duration + note.output_delay) * Model.dft_rate /
                                    DataGenerator.sample_frequency), num_frames)

                if end_frame >= num_frames:
                    continue

                signal[note.start_time:note.start_time + note.duration] += \
                    note.volume * np.roll(
                        note.guitar.data[data_start_sample:data_start_sample + note.duration],
                        note.roll)

                data_out[example, start_frame + 1:end_frame, note.pitch - Model.start_pitch] = [0, 1]
                data_out[example, start_frame, note.pitch - Model.start_pitch] = [1, 0]

            signal *= volume

            emphasized_signal = np.append(signal[0], signal[1:] - Model.pre_emphasis * signal[:-1])

            pad_signal_length = num_frames * Model.frame_step + Model.frame_length
            z = np.zeros((pad_signal_length - Model.frame_length))
            pad_signal = np.append(emphasized_signal, z)

            indices = np.tile(np.arange(0, Model.frame_length), (num_frames, 1)) + np.tile(
                np.arange(0, num_frames * Model.frame_step, Model.frame_step), (Model.frame_length, 1)).T
            frames = pad_signal[indices.astype(np.int32, copy=False)]

            frames *= np.hamming(Model.frame_length)

            mag_frames = np.absolute(np.fft.rfft(frames, Model.frame_length))  # Magnitude of the FFT
            pow_frames = (mag_frames ** 2) / Model.frame_length  # Power Spectrum

            filter_banks = np.dot(pow_frames, Model.f_bank)
            filter_banks = np.where(filter_banks == 0, np.finfo(float).eps, filter_banks)
            filter_banks = np.log(filter_banks)

            data_in[example] = filter_banks

            # out.write(str(np.mean(filter_banks)) + "," + str(np.std(filter_banks)) + "\n")
            # out.flush()

            if example < 10 and False:
                print(example)
                scipy.io.wavfile.write("trash/test" + str(example)
                                       + ".wav", 44100, signal)
                with open("trash/test" + str(example) + "log.txt", "w") as file:
                    file.write("DATA\n")
                    file.write("NOTES\n")
                    for note in notes:
                        if note.origin[0:4] == "res-":
                            continue
                        file.write(str(note) + "\n")
                    file.write("NOISE\n")
                    for note in noises:
                        file.write(str(note) + "\n")

                plt.imshow(np.transpose(data_in[example]), cmap='nipy_spectral', interpolation='nearest')
                plt.colorbar()
                plt.show()

                plt.imshow(np.transpose(data_out[example, :, :, 1]), cmap='nipy_spectral', interpolation='nearest')
                plt.colorbar()
                plt.show()

        return np.reshape(data_in, [Trainer.batch_size, num_frames, Model.mel_filters, 1]), data_out

    @staticmethod
    def make_resonances(note):
        notes = []
        for i in range(DataGenerator.resonance_amount):
            new = Note(note.start_time,
                       min(DataGenerator.note_duration_max * DataGenerator.sample_frequency,
                           Trainer.example_length - note.start_time),
                       note.pitch + choice([-12, -4, -3, 3, 4, 12]),
                       note.roll,
                       note.guitar,
                       note.volume * gauss(DataGenerator.resonance_volume_mean,
                                           DataGenerator.resonance_volume_sd),
                       note.output_delay,
                       "res-" + note.origin,
                       outs=False)
            if note.pitch in range(Model.start_pitch, Model.end_pitch):
                notes.append(new)
        return []

    def get_batch(self):
        while self.queue.empty():
            time.sleep(.1)
        return self.queue.get()

    def __del__(self):
        self.thread.stop()
        self.thread.join()

    @staticmethod
    def load_sound_from(folder):
        sounds = tuple(
            (SoundData(folder + "/" + file_name) for file_name in os.listdir(folder) if file_name.endswith(".wav")))

        average_rms = sum(map(lambda sound: sound.rms, sounds)) / len(sounds)

        for sound in sounds:
            sound.data *= average_rms / sound.rms

        return sounds

    @staticmethod
    def weighted_choice(choices):
        weight_sum = sum(w for c, w in choices.items())

        value = uniform(0, weight_sum)

        counter = 0
        for c, w in choices.items():
            if counter + w >= value:
                return c
            counter += w

    @staticmethod
    def hz_to_mel(freq):
        return 2595 * math.log10(1 + freq / 700)

    @staticmethod
    def mel_to_hz(mel):
        return 700 * (10 ** (mel / 2595) - 1)

    @staticmethod
    def pitch_to_frequency(pitch):
        return 27.5 * (20 ** (1 / 12)) ** (pitch - 9)

    @staticmethod
    def load_chords():

        # triad chords
        chord_types = [["maj", [0, 4, 7], 30],
                       ["min", [0, 3, 7], 30],
                       ["dim", [0, 3, 6], 5],
                       ["aug", [0, 4, 8], 5],
                       ["+7", [0, 7], 15]]  # aka power chords

        roots = list(range(0, 12))
        root_letters = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B", ]
        octaves = list(range(2, 5))

        chords = []

        for i, root in enumerate(roots):
            for octave in octaves:
                for chord_type in chord_types:
                    notes = list(map(lambda x: x + root + octave * 12, chord_type[1]))
                    chords.append(Chord(root_letters[i] + chord_type[0] + str(octave), notes, chord_type[2]))

        # guitar chords
        raw_chord_data = urllib.request.urlopen("http://www.chordie.com/chords.php").read()
        chord_matches = re.findall(r'title="[^".]*"', str(raw_chord_data))
        chord_data = map(lambda raw_chord: raw_chord[7:-1].split("="), chord_matches)
        standard_tuning = [28, 33, 38, 43, 47, 52]

        for chord in chord_data:
            name = chord[0]
            chord_notes = []
            for i, n in enumerate(chord[1][:-2]):
                if n != "N":
                    chord_notes.append(standard_tuning[i] + int(n))
            chords.append(Chord("@" + name, chord_notes, 1))
            # print(len(chord_notes), "\t", chord, "\t", chord_notes)

        return chords


class GeneratorThread(threading.Thread):
    def __init__(self, data):
        super(GeneratorThread, self).__init__(name="Generator")
        self.data = data
        self.should_stop = False

    def run(self):
        while not self.should_stop:
            while (not self.data.queue.empty()) and (not self.should_stop):
                time.sleep(.1)
            self.data.queue.put(self.data.generate_batch)

    def stop(self):
        self.should_stop = True


class SoundData:
    def __init__(self, file_path):
        self.file_path = file_path
        _, self.data = scipy.io.wavfile.read(file_path)  # sample frequency is disregarded since it will be constant
        if len(self.data.shape) > 1:
            self.data = self.data[:, 0]  # select only one chanel of sound
        self.length = self.data.shape[0]  # in samples

        self.rms = np.sqrt(np.mean(self.data ** 2))

        range_matches = re.findall(r"\[[\d]*-[\d]*\]", file_path)
        if len(range_matches) == 1:
            split = range_matches[0].split("-")
            start = int(split[0][1:])
            end = int(split[1][:-1])

            self.range = range(max(Model.start_pitch, start),
                               Model.end_pitch if end == 0 else min(Model.end_pitch, end))
        else:
            self.range = None

    def __str__(self):
        return "SoundData{" + \
               "file_path='" + self.file_path.split("/")[-1] + "'" + \
               ", length=" + str(round(self.length / DataGenerator.sample_frequency, 2)) + \
               "}"


class Chord:
    def __init__(self, name, notes, weight):
        self.name = name
        self.notes = notes
        self.weight = weight

    def __str__(self):
        return "Chord{" + \
               "name=" + self.name + \
               ", notes=" + str(self.notes) + \
               ", weight=" + str(self.weight) + \
               "}"


class Note:
    def __init__(self, start_time, duration, pitch, roll, guitar, volume, output_delay, origin, outs=True):
        self.start_time = int(start_time)
        self.duration = int(duration)
        self.pitch = pitch
        self.roll = roll
        self.guitar = guitar
        self.volume = volume
        self.output_delay = output_delay
        self.origin = origin
        self.outs = outs

    def __str__(self):
        return "Note{" + \
               ", start_time=" + str(round(self.start_time / DataGenerator.sample_frequency, 2)) + \
               ", duration=" + str(round(self.duration / DataGenerator.sample_frequency, 2)) + \
               ", pitch=" + str(self.pitch) + \
               ", guitar=" + str(self.guitar) + \
               ", volume=" + str(self.volume) + \
               ", origin=" + str(self.origin) + \
               ", output_delay=" + str(self.output_delay) + \
               "}"


class Noise:
    def __init__(self, start_offset, noise_data, volume):
        self.start_offset = start_offset
        self.noise_data = noise_data
        self.volume = volume

    def __str__(self):
        return "Noise{" + \
               "noise_data=" + str(self.noise_data) + \
               ", volume=" + str(self.volume) + \
               "}"


class Trainer:
    keep_prob = .95
    example_length = 20 * DataGenerator.sample_frequency
    batch_size = 10  # 10
    learning_rate = 0.000005

    def __init__(self, data_generator, model):
        self.data_generator = data_generator
        self.model = model
        self.model = model

        self.epochs = 50000
        self.iterations = 70

    def train(self, session, continue_training, log_file=""):
        print("Starting Training")

        console_form = "Epoch: {0:" + str(len(str(self.epochs))) + "d}/" + str(self.epochs) + " Iteration: {1:" + str(
            len(str(self.iterations))) + "d}/" + str(self.iterations) + " Cost: {2:0.15f}"
        log_form = "{0}, {1}, {2}, {3}\n"

        if continue_training:
            try:
                self.model.load_from_save(session)
            except:
                print("Failed to load previous model")
                continue_training = False

        if len(log_file) != 0:
            log_file = open(log_file, "a" if continue_training else "w")
        else:
            log_file = None

        for epoch in range(self.epochs):

            x, y = self.data_generator.get_batch()
            session.run(self.model.init_data_x, feed_dict={self.model.in_data_x: x})
            session.run(self.model.init_data_y, feed_dict={self.model.in_data_y: y})
            # feed_dict = {start_state: zero_state.eval()}  # keep_prob_placeholder: keep_prob

            for iteration in range(self.iterations):
                session.run(self.model.train_step)
                current_cost = session.run(self.model.cost)

                print(console_form.format((epoch + 1), (iteration + 1), current_cost))
                if log_file is not None:
                    log_file.write(log_form.format(epoch, iteration, current_cost, time.time()))

            if log_file is not None:
                log_file.flush()
            self.model.save(session)

        if log_file is not None:
            log_file.close()
        print("Finished training")


class Model:
    pre_emphasis = .95
    start_mel_frequency = DataGenerator.hz_to_mel(60)
    end_mel_frequency = DataGenerator.hz_to_mel(1500)

    mel_filters = 124

    end_pitch = 60
    start_pitch = 24

    frame_length = 2 ** 12
    dft_rate = 30
    frame_step = int(round(DataGenerator.sample_frequency / dft_rate))
    samples_per_dft = DataGenerator.sample_frequency / dft_rate

    export_path = "models/model"
    save_path = "saves/tf_save"

    f_bank = generate_mel_transform(start_mel_frequency, end_mel_frequency, mel_filters, frame_length)

    def __init__(self, is_export_version):
        # the export version has more organisation to aid the use of the exported model

        self.is_export_version = is_export_version

        self.y = None
        self.cost = None
        self.train_step = None
        self.in_data_x = None
        self.in_data_y = None
        self.init_data_x = None
        self.init_data_y = None

        self.define()

    def define(self):

        batch_size = 1 if self.is_export_version else Trainer.batch_size
        example_length = 9 if self.is_export_version else int(
            np.ceil(float(np.abs(Trainer.example_length - Model.frame_length)) / Model.frame_step))

        if self.is_export_version:
            x = tf.placeholder(tf.float32, [Model.frame_length], "inputs")

            x = tf.concat([[x[0]], x[1:] - Model.pre_emphasis * x[:-1]], 0)

            x = x * hamming_window(Model.frame_length)

            tf_f_bank = tf.constant(Model.f_bank, tf.float32, shape=[Model.frame_length // 2 + 1, Model.mel_filters],
                                    verify_shape=True)

            fft = tf.spectral.rfft(x)
            de_phased = tf.spectral.irfft(tf.complex(tf.sqrt(tf.real(fft) ** 2 + tf.imag(fft) ** 2), 0.0),
                                          name="de_phased_reconstruction")
            de_phased_power = tf.sqrt(tf.reduce_mean(tf.square(de_phased)), "de_phased_rms")

            fft_reshape = tf.abs(tf.reshape(fft, [1, -1]))

            tf_pow_frames = (tf.pow(fft_reshape, 2)) / Model.frame_length  # Power Spectrum

            tf_filter_banks = tf.matmul(tf_pow_frames, tf_f_bank)
            tf_filter_banks = tf.maximum(tf_filter_banks, 1e-10)
            tf_filter_banks = tf.log(tf_filter_banks, "mel_bins")

            x_current = tf.reshape(tf_filter_banks, [1, Model.mel_filters, 1])  # batch, time, height, pixel_depth

            input_queue = tf.FIFOQueue(1, tf.float32, shapes=[example_length - 1, Model.mel_filters, 1])
            input_queue.enqueue(tf.tile(x_current, [example_length - 1, 1, 1]), "enqueue_start_inputs")
            input_queue.dequeue_up_to(1, "deque_inputs")

            x_previous = input_queue.dequeue()

            x = tf.concat([x_previous, x_current], 0)
            input_queue.enqueue(x[1:example_length], "enqueue_new_inputs")
            x = tf.reshape(x, [1, example_length, Model.mel_filters, 1])

        else:

            data_x = tf.Variable(tf.zeros((batch_size, example_length, Model.mel_filters, 1), tf.float32), False,
                                 collections=[tf.GraphKeys.LOCAL_VARIABLES])
            data_y = tf.Variable(
                tf.zeros((batch_size, example_length, Model.end_pitch - Model.start_pitch, 2), tf.float32),
                False,
                collections=[tf.GraphKeys.LOCAL_VARIABLES])
            self.in_data_x = tf.placeholder(tf.float32, data_x.get_shape())
            self.in_data_y = tf.placeholder(tf.float32, data_y.get_shape())
            self.init_data_x = data_x.assign(self.in_data_x, True)
            self.init_data_y = data_y.assign(self.in_data_y, True)

            x = data_x
            y_hat = tf.identity(data_y, "targets")

        x_normal = (x - DataGenerator.data_mean) / DataGenerator.data_var

        layers = [
            ("valid", 5, 48, 1, 2),
            # ("same", 5, 48, 1, 2),
            # ("same", 5, 48, 1, 2),
            # ("same", 5, 48, 1, 1),
            ("valid", 5, 48, 1, 1)
        ]

        last_layer = x_normal

        for padding, kernel_size, filters, pool_size, strides in layers:

            conv = tf.layers.conv2d(
                inputs=last_layer,
                filters=filters,
                kernel_size=[kernel_size, kernel_size],
                padding=padding,
                activation=tf.nn.relu)

            conv = tf.layers.max_pooling2d(inputs=conv, pool_size=[1, pool_size], strides=[1, strides])
            if not self.is_export_version:
                conv = tf.nn.dropout(conv, Trainer.keep_prob)
            last_layer = self.normalise(conv)

        conv_out = tf.reshape(last_layer, [batch_size, example_length - 8, -1])

        dense_size = 192

        output_w1 = tf.Variable(
            tf.random_normal([conv_out.get_shape()[-1].value, dense_size]),
            name="output_w1")
        output_b1 = tf.Variable(tf.zeros(dense_size), name="output_b1")

        dense_1 = tf.nn.relu(
            tf.matmul(tf.reshape(conv_out, [-1, conv_out.get_shape()[-1].value]), output_w1) + output_b1)
        if not self.is_export_version:
            dense_1 = tf.nn.dropout(dense_1, Trainer.keep_prob)
        dense_1 = self.normalise(dense_1)

        output_w2 = tf.Variable(
            tf.random_normal([dense_size, 2 * (Model.end_pitch - Model.start_pitch)]),
            name="output_w1")
        output_b2 = tf.Variable(tf.zeros([2 * (Model.end_pitch - Model.start_pitch)]), name="output_b1")

        self.y = tf.reshape(tf.nn.softmax(tf.reshape(tf.matmul(dense_1, output_w2) + output_b2, [-1, 2])),
                            [batch_size, example_length - 8, Model.end_pitch - Model.start_pitch, 2], "y")

        if self.is_export_version:
            predictions = tf.identity(self.y[:, :, :, 1], name="predictions")
        else:
            y_hat = y_hat[:, 4:-4, :, :]
            self.cost = -tf.reduce_mean(
                tf.reduce_sum(
                    y_hat * tf.log(tf.clip_by_value(self.y, 1e-10, 1.0)),
                    axis=[1]))  # cross entropy

            self.train_step = tf.train.AdamOptimizer(Trainer.learning_rate).minimize(self.cost)

    @staticmethod
    def normalise(tensor):
        # mean, var = tf.nn.moments(tensor, [0])
        # return (tensor - mean) / tf.maximum(var, 1e-4)
        return tf.nn.l2_normalize(tensor, [1])

    def init(self, session):
        session.run(tf.global_variables_initializer())

    def load_from_save(self, session):
        print("Loading save from " + Model.save_path)

        saver = tf.train.Saver()
        saver.restore(session, Model.save_path)
        del saver

    def save(self, session):
        print("Saving to " + Model.save_path)

        saver = tf.train.Saver()
        saver.save(session, Model.save_path)
        del saver

    def export(self, session, name):
        print("Exporting model to " + Model.export_path + " as " + name)

        builder = tf.saved_model.builder.SavedModelBuilder(Model.export_path + name)
        builder.add_meta_graph_and_variables(session, [tf.saved_model.tag_constants.SERVING])
        builder.save(True)
        del builder


def main():
    selected = input("Train/Export the model ? (ENTER for train otherwise save name)")

    if len(selected) == 0:  # train

        continue_training = len(
            input("Do you want to continue training the last saved model? (ENTER for no/any for yes)")) != 0
        # I would have called this continue but it's a keyword

        data_generator = DataGenerator()
        model = Model(False)
        trainer = Trainer(data_generator, model)

        with tf.Session() as session:
            model.init(session)
            trainer.train(session, continue_training, "log.csv")

    else:  # save

        model = Model(True)
        with tf.Session() as session:
            model.init(session)
            model.load_from_save(session)
            model.export(session, selected)


if __name__ == '__main__':
    main()
