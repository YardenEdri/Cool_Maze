package org.example;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Entry point: creates the single JFrame and shows the settings screen. */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Maze Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(520, 400);
            frame.setLocationRelativeTo(null);
            frame.add(new SettingsPanel(frame));
            frame.setVisible(true);
        });
    }
}
