package com.dungeonescape.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * The application's main window.
 *
 * <p>Stage 1 scope: a minimal titled frame with a short status label. The
 * dungeon view, HUD, and all gameplay are added in later stages.</p>
 */
public class GameWindow extends JFrame {

    /** Creates the window. Callers should invoke this on the Swing EDT. */
    public GameWindow() {
        super("Dungeon Escape V2 - Stage 1");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 480);
        setLocationRelativeTo(null);

        JLabel status = new JLabel(
                "Dungeon Escape V2 - Stage 1 foundation (assets verified, no gameplay yet)",
                SwingConstants.CENTER);
        add(status, BorderLayout.CENTER);
    }
}
