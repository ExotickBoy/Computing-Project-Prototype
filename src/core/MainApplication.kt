package core

import components.RecordingsListPane
import javafx.application.Application
import javafx.application.Application.launch
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

import java.util.*

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
            System.exit(0)

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
            Image("res/icon.png")
        } catch (e: Exception) {
            null
        }

    }

}

/**
 * The entry point
 */
fun main(args: Array<String>) {
    launch(MainApplication::class.java, *args)
}
