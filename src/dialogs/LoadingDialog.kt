package dialogs

import core.MainApplication
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.BorderPane
import javafx.stage.Modality
import javafx.stage.Stage

/**
 * This class is for making a small temporary dialog that shows an indeterminate progress bar.
 * It is mainly fr user interaction purposes since it allows the user to see that the program is doing something and
 * not just frozen
 *
 * @author Kacper Lubisz
 *
 * @param labelText The text that is to be displayed above the progress bar
 * @param title The title of the window
 */
class LoadingDialog(labelText: String, title: String) {

    private val stage: Stage = Stage()

    init {

        stage.isResizable = false
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = title
        if (MainApplication.icon != null)
            stage.icons.add(MainApplication.icon)

        val root = BorderPane()
        root.padding = Insets(10.0)

        val label = Label(labelText)
        label.alignment = Pos.CENTER
        label.maxWidth = Double.MAX_VALUE
        root.top = label
        root.center = ProgressBar()

        val scene = Scene(root)
        stage.scene = scene
        stage.show()

    }

    /**
     * This method can be called externally an gets rid of the dialog
     */
    fun dispose() {

        stage.close()

    }

}