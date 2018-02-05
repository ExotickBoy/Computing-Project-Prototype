package core

import components.RecordingsListPane
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.image.Image
import javafx.stage.Stage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.system.exitProcess

/**
 * This is the class that represents the instance of the running program
 *
 * @author Kacper Lubisz
 *
 */
class MainApplication : Application() {

    private val activityStack: Stack<Activity> = Stack()
    private val sceneStack: Stack<Scene> = Stack()
    private lateinit var stage: Stage

    /**
     * This method is called by javafx and is where the program starts
     */
    override fun start(primaryStage: Stage) {

        // in javafx the stage is window and the scene is it's contents

        this.stage = primaryStage
        stage.isResizable = false

        stage.title = TITLE
        stage.setOnCloseRequest {
            activityStack.peek().onClose()
            it.consume()
        }
        if (icon != null)
            stage.icons.add(icon)
        push(RecordingsListPane(this))

        stage.show()

    }

    /**
     * Adds an activity to the stack (onCreate, onResume)
     * @param activity The activity that will be added
     */
    fun push(activity: Activity) {

        if (activityStack.isNotEmpty())
            activityStack.peek().onPause()

        activityStack.push(activity)
        val scene = activity.onCreate()
        sceneStack.push(scene)

        stage.scene = scene
        stage.centerOnScreen()

        activity.onResume()

    }

    /**
     * Removes the very top activity from the stack and resumes the one underneath it
     */
    fun pop() {

        activityStack.peek().onPause()
        activityStack.pop().onDestroy()
        sceneStack.pop()

        if (activityStack.isEmpty()) {

            stage.close()
            exitProcess(0)

        } else {

            stage.scene = sceneStack.peek()
            stage.centerOnScreen()

            activityStack.peek().onResume()

        }

    }

    /**
     * Pops all of th activities off the stack and ends the program
     */
    internal fun popAll() {

        activityStack.peek().onPause()
        activityStack.pop().onDestroy()
        sceneStack.pop()

        while (activityStack.isNotEmpty()) {
            activityStack.pop().onDestroy()
            sceneStack.pop()
        }

        stage.close()
        System.exit(0)

    }

    /**
     * This class represents each scene that the user can see.
     * These scenes are organised in activities that have their life cycles on the activity stack.
     */
    abstract class Activity(protected val application: MainApplication) {
        /**
         * When the activity is first created and added to the stack
         * @return The scene that will be shown in the main stage when this activity is on top of the stack
         */
        abstract fun onCreate(): Scene

        /**
         * When the activity becomes the shown activity
         */
        abstract fun onPause()

        /**
         * When the activity is left in the background (lower on the stack)
         */
        abstract fun onResume()

        /**
         * When the activity is removed from the stack and will never be needed again
         */
        abstract fun onDestroy()

        /**
         * When the user requests to close the window
         */
        abstract fun onClose()
    }

    /**
     * Sets the title of the window
     */
    fun setTitle(title: String) {
        stage.title = title
    }

    companion object {

        const val TITLE: String = "NoteWize"
        val icon = try {
            Image(MainApplication::class.java.getResourceAsStream("/icon.png"))
        } catch (e: Exception) {
            null
        }
    }

}

private const val TEMP_DIRECTORY_PREFIX = "NOTE_WIZE"
private const val FAILED_TO_LOAD_MESSAGE = "Failed to load"
private const val ERROR_DIALOG_HEADER = "A fatal error has occurred"
private const val ERROR_DIALOG_TITLE = "Error"

/**
 * The entry point
 */
fun main(args: Array<String>) {

    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        // this will handel any error that is thrown and not handed
        // this shouldn't really be called in any case except in the
        // case of the code just below (load and temp files)

        exception.printStackTrace()
        // this is just for debugging

        if (Platform.isFxApplicationThread()) {
            showErrorDialog(thread, exception)
        } else {
            Platform.runLater {
                showErrorDialog(thread, exception)
            }
        }
    }

    try {
        System.load(copyFilesToTemp(Model.TENSOR_FLOW_NATIVES))
        // this unwraps the native files from the jar and then loads them
        // this is because the natives can't be read from inside the jar
        Model.MODEL_FILES.forEach { copyFilesToTemp(it) }
        // this is for the model files, similarly TensorFlow doesn't allow for reading from inside the jar#
        Model.load(getTempDir(Model.MODEL_DIR))
    } catch (e: Exception) {
        // this can fail in several ways which shouldn't ever happen.
        // I rethrow this generic error which will act as an error message
        throw Exception(FAILED_TO_LOAD_MESSAGE)
    }

    launch(MainApplication::class.java, *args)
    // launches the javafx window

}

/**
 * This function displays an error dialog based on a thread and an exception
 */
private fun showErrorDialog(thread: Thread, exception: Throwable) {

    val alert = Alert(Alert.AlertType.ERROR)
    if (MainApplication.icon != null)
        (alert.dialogPane.scene.window as Stage).icons.add(MainApplication.icon)

    alert.title = ERROR_DIALOG_TITLE
    alert.headerText = ERROR_DIALOG_HEADER
    alert.contentText = exception.message

    alert.showAndWait()

    System.exit(1)

}

/**
 * This function moves a file from inside of the jar file into the temporary directory
 */
fun copyFilesToTemp(file: String): String {

    val sections = file.split("/")
    val path = sections.subList(0, sections.size - 1).reduce { a, b -> "$a/$b" }
    val fileName = sections.last()

    val tempDir = getTempDir(path)

    val temp = File(tempDir, fileName).absoluteFile

    if (!temp.exists())
        MainApplication::class.java.getResourceAsStream(file).use { Files.copy(it, temp.toPath(), StandardCopyOption.REPLACE_EXISTING) }

    return temp.toPath().toString()

}

/**
 * This creates a directory in the temporary folder
 * @param path the relative path inside the temp folder
 * @return the file that points to the newly created folder
 */
private fun getTempDir(path: String): File {

    val tempDir = File(System.getProperty("java.io.tmpdir") + "/" + TEMP_DIRECTORY_PREFIX + path)
    // the temporary folder that will store the natives
    tempDir.mkdirs()

    return tempDir

}