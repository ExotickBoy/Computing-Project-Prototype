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


class LoadingDialog(labelText: String, title: String) {

    private val stage: Stage

    init {

        stage = Stage()
        stage.isResizable = false
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = title
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


//        val content = JPanel(BorderLayout())
//        content.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
//
//        val label = JLabel(labelText)
//        label.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

//        val progressBar = JProgressBar()
//        progressBar.isIndeterminate = true
//
//        content.add(label, BorderLayout.NORTH)
//        content.add(progressBar, BorderLayout.CENTER)
//
//        contentPane = content
//        pack()
//        setLocationRelativeTo(owner)
//        defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
//
//        thread(name = "Loading Worker") {
//            action.invoke()
////            dispose()
//        }
//        isVisible = true

    }

    fun dispose() {

        stage.close()

    }

}