package core

import components.RecordingsListPane
import javafx.application.Application
import javafx.application.Application.launch
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

import java.util.*

class MainApplication : Application() {

    private val activityStack: Stack<Activity> = Stack()
    private val sceneStack: Stack<Scene> = Stack()
    private lateinit var stage: Stage

    override fun start(primaryStage: Stage) {

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

    abstract class Activity(protected val application: MainApplication) {
        abstract fun onCreate(): Scene
        abstract fun onPause()
        abstract fun onResume()
        abstract fun onDestroy()
        abstract fun onClose()
    }

    companion object {

        const val TITLE: String = "NoteWize"
        val icon = try {
            Image("res/icon.png")
        } catch (e: Exception) {
            null
        }

    }

    fun setTitle(title: String) {
        stage.title = title
    }

}

fun main(args: Array<String>) {
    launch(MainApplication::class.java, *args)
}
