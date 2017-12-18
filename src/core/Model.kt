package core

import org.tensorflow.SavedModelBundle
import org.tensorflow.Tensor
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
    private const val ENQUEUE_START_INPUTS: String = "enqueue_start_inputs"
    private const val ENQUEUE_NEW_STATE: String = "enqueue_new_state"
    private const val ENQUEUE_NEW_INPUT: String = "enqueue_new_inputs"
    private const val CLEAR_STATE: String = "clear_state_queue"
    private const val MEL_BINS_TENSOR: String = "mel_bins"
    private const val DEPHASED_SAMPLES: String = "no_phase_reconstruction"

    private const val MODEL_LOCATION = "res/model55" // the location of the model

    private val tensorFlowSession = SavedModelBundle.load(MODEL_LOCATION, "serve").session()
    // The TensorFlow session which is an instance of the execution of the TensorFlow computation

    private val samplesInputBuffer: FloatBuffer // The FloatBuffer used for writing the samples and feeding them into the Model
    private val noteOutputBuffer: FloatBuffer // The buffer used for reading predictions from the Model
    private val spectrumOutputBuffer: FloatBuffer // The buffer used for reading the spectrum output from the Model
    private val dephasedBuffer: FloatBuffer // The buffer used for reading the depahsed samples from the Model

    init {

        // create the buffers
        samplesInputBuffer = FloatBuffer.allocate(FFT_SIZE)
        noteOutputBuffer = FloatBuffer.allocate(PITCH_RANGE)
        spectrumOutputBuffer = FloatBuffer.allocate(MEL_BINS_AMOUNT)
        dephasedBuffer = FloatBuffer.allocate(FFT_SIZE)

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

        synchronized(tensorFlowSession) {
            // locks session to prevent concurrent modification

            val results = tensorFlowSession.runner()
//                    .addTarget(ENQUEUE_NEW_STATE)
                    .addTarget(ENQUEUE_NEW_INPUT)
                    .fetch(OUTPUT_TENSOR_NAME)
                    .fetch(MEL_BINS_TENSOR)
                    .fetch(DEPHASED_SAMPLES)
                    .feed(INPUT_TENSOR_NAME,
                            Tensor.create(longArrayOf(1, 1, Model.FFT_SIZE.toLong()), samplesInputBuffer))
                    .run()
            // to understand fetches and feeds, see Model in train.py

            noteOutputBuffer.rewind()
            spectrumOutputBuffer.rewind()
            dephasedBuffer.rewind()
            results[0].writeTo(noteOutputBuffer) // result[0] = OUTPUT_TENSOR_NAME
            results[1].writeTo(spectrumOutputBuffer) // result[1] = MEL_BINS_TENSOR
            results[2].writeTo(dephasedBuffer) // result[1] = DEPHASED_SAMPLES

        }

        return StepOutput(noteOutputBuffer.array(), spectrumOutputBuffer.array(), dephasedBuffer.array())

    }

    /**
     * Instantiates the sessions state queue with the start state
     */
    private fun setState() {
        synchronized(tensorFlowSession) {
            // locks session to prevent concurrent modification

            tensorFlowSession.runner()
//                    .addTarget(ENQUEUE_ZERO_STATE)
                    .addTarget(ENQUEUE_START_INPUTS)
                    .run()

        }
    }

    /**
     * Removes the current state stored in the session and replaces it with the start state
     */
    fun resetState() {
        synchronized(tensorFlowSession) {
            // locks session to prevent concurrent modification

//            tensorFlowSession.runner().addTarget(CLEAR_STATE).run()
//            tensorFlowSession.runner().addTarget(ENQUEUE_ZERO_STATE).run()

        }
    }

}