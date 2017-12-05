package core

import java.nio.FloatBuffer

/**
 * The object that interfaces with the model created in TensorFlow
 * @see org.tensorflow.Session
 * @see org.tensorflow.Tensor
 * @author Kacper Lubisz
 */
object Model {

    const val FFT_SIZE: Int = 4096 // the size of the fft used, equivalent to the input size

    const val START_PITCH: Int = 28 // the lowest pitch the model outputs
    const val END_PITCH: Int = 84
    const val PITCH_RANGE: Int = END_PITCH - START_PITCH

    const val CONFIDENCE_CUT_OFF = .5 // the confidence below which predictions will be discarded
    const val MEL_BINS_AMOUNT: Int = 124 // the size of the output spectrum


    // The names of tensors in the model
    private const val INPUT_TENSOR_NAME: String = "inputs"
    private const val OUTPUT_TENSOR_NAME: String = "predictions"
    private const val ENQUEUE_ZERO_STATE: String = "enqueue_zero_state"
    private const val ENQUEUE_NEW_STATE: String = "enqueue_new_state"
    private const val CLEAR_STATE: String = "clear_queue"
    private const val MEL_BINS_TENSOR: String = "mel_bins"

    private const val MODEL_LOCATION = "E:\\Project/model46" // the location of the model

    private val session = SavedModelBundle.load(MODEL_LOCATION, "serve").session()
    // The TensorFlow session which is an instance of the execution of the TensorFlow computation

    private val samplesInputBuffer: FloatBuffer // The FloatBuffer used for writing the samples and feeding them into the Model
    private val noteOutputBuffer: FloatBuffer // The buffer used for reading predictions from the Model
    private val spectrumOutputBuffer: FloatBuffer // The buffer used for reading the spectrum output from the Model

    init {

        // create the buffers
        samplesInputBuffer = FloatBuffer.allocate(FFT_SIZE)
        noteOutputBuffer = FloatBuffer.allocate(PITCH_RANGE)
        spectrumOutputBuffer = FloatBuffer.allocate(MEL_BINS_AMOUNT)

        // initialise the state of the LSTM to the start state
        setState()

    }

    /**
     * This method forwards the samples through the TensorFlow model of a new TimeStep
     * @param samples The sound samples for this TimeStep
     * @return The object that represents the output of the model at this TimeStep
     * @see Analyser
     * @see TimeStep
     * @see StepOutput
     */
    fun feedForward(samples: FloatArray): StepOutput {

        samplesInputBuffer.rewind()
        samplesInputBuffer.put(samples)
        samplesInputBuffer.rewind()

        synchronized(session) {
            // locks session to prevent concurrent modification

            val results = session.runner()
                    .addTarget(ENQUEUE_NEW_STATE)
                    .fetch(OUTPUT_TENSOR_NAME)
                    .fetch(MEL_BINS_TENSOR)
                    .feed(INPUT_TENSOR_NAME,
                            Tensor.create(longArrayOf(1, 1, Model.FFT_SIZE.toLong()), samplesInputBuffer))
                    .run()
            // to understand fetches and feeds, see Model in train.py

            noteOutputBuffer.rewind()
            spectrumOutputBuffer.rewind()
            results[0].writeTo(noteOutputBuffer) // result[0] = OUTPUT_TENSOR_NAME
            results[1].writeTo(spectrumOutputBuffer) // result[1] = MEL_BINS_TENSOR

        }

        return StepOutput(noteOutputBuffer.array(), spectrumOutputBuffer.array());

    }

    /**
     * Instantiates the sessions state queue with the start state
     */
    private fun setState() {
        synchronized(session) {
            // locks session to prevent concurrent modification

            session.runner().addTarget(ENQUEUE_ZERO_STATE).run()

        }
    }

    /**
     * Removes the current state stored in the session and replaces it with the start state
     */
    fun resetState() {
        synchronized(session) {
            // locks session to prevent concurrent modification

            session.runner().addTarget(CLEAR_STATE).run()
            session.runner().addTarget(ENQUEUE_ZERO_STATE).run()

        }
    }

}