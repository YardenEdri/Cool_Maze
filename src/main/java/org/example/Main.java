package org.example;

import kong.unirest.Unirest;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

/** Entry point: creates the single JFrame and shows the settings screen. */
public class Main {

    public static void main(String[] args) throws Exception {
        // The main thread stays parked here until the window is closed. Without
        // that, main() returns immediately and AWT can race to shut itself down
        // before (or just after) the frame is shown, so the app never appears.
        CountDownLatch windowClosed = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame("Maze Game");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    windowClosed.countDown();
                }
            });
            frame.setSize(520, 400);
            frame.setLocationRelativeTo(null);
            frame.add(new SettingsPanel(frame));
            frame.setVisible(true);
        });

        windowClosed.await();

        // Unirest keeps non-daemon HTTP threads alive after its first request, so
        // once the window is gone main() returns but the JVM (java.exe) never
        // exits on its own. Without this, every run leaves an invisible process
        // behind and they pile up. Shut Unirest down, then force the exit.
        Unirest.shutDown();
        System.exit(0);
    }
}
