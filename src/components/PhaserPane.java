package components;

import core.Recording;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class PhaserPane extends JPanel {

    private static final long serialVersionUID = 1L;
    private Recording recording;

    private static final double SCALE = 100;

    PhaserPane(Recording recording) {

        this.recording = recording;
        setPreferredSize(new Dimension(500, 150));

    }

    @Override
    protected void paintComponent(Graphics g2) {

        super.paintComponent(g2);

        if (recording.getTimeSteps().size() > 0) {

            Graphics2D g = (Graphics2D) g2;
            RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            rh.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHints(rh);

            Double[] dePhased = recording.getTimeSteps().get(recording.getTimeSteps().size() - 1).getDePhased();
            Dimension size = getSize();

            Path2D.Double graph = new Path2D.Double();

            int resolution = 2;
            for (int i = 0; i < dePhased.length; i += resolution) {

                double x = size.getWidth() * i / dePhased.length;
                if (i == 0) {

                    graph.moveTo(x, size.height / 2 + dePhased[i] * SCALE);

                } else {

                    graph.lineTo(x, size.height / 2 + dePhased[i] * SCALE);

                }

            }
            g.setStroke(new BasicStroke(.2f));
            g.draw(graph);

        }

    }

}
