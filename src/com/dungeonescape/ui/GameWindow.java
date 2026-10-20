package com.dungeonescape.ui;

import com.dungeonescape.asset.AssetLoader;

import javax.swing.JFrame;
import java.awt.BorderLayout;

/**
 * The application's main window.
 *
 * <p>Stage 2 scope: the window is a fixed-size composition of a compact HUD
 * above the dungeon view. All gameplay remains out of scope.</p>
 */
public class GameWindow extends JFrame {

    /** Creates the window. Callers should invoke this on the Swing EDT. */
    public GameWindow() {
        super("Dungeon Escape V2 - Stage 2");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        AssetLoader loader = new AssetLoader();
        add(new HudPanel(), BorderLayout.NORTH);
        add(new DungeonPanel(loader), BorderLayout.CENTER);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }
}
