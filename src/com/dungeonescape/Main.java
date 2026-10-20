package com.dungeonescape;

import com.dungeonescape.asset.AssetDiagnostic;
import com.dungeonescape.asset.AssetLoadException;
import com.dungeonescape.asset.AssetLoader;
import com.dungeonescape.ui.GameWindow;

import javax.swing.SwingUtilities;

/**
 * Application entry point for Dungeon Escape V2.
 *
 * <p>Stage 1 scope: this only reports the asset situation and shows a minimal
 * Swing window. No game mechanics are implemented yet.</p>
 *
 * <p>The GUI is created on the Swing Event Dispatch Thread (EDT), because all
 * Swing component creation and updates must happen on that thread.</p>
 */
public class Main {

    /** Prevent instantiation; this class only holds the entry point. */
    private Main() {
    }

    /**
     * Starts the application.
     *
     * <p>Asset diagnostics run first so problems are reported to the console.
     * A missing or invalid asset never prevents the window from opening and is
     * never silently ignored.</p>
     *
     * @param args command-line arguments (unused in Stage 1)
     */
    public static void main(String[] args) {
        try {
            AssetLoader loader = new AssetLoader();
            AssetDiagnostic.run(loader, System.out);
        } catch (AssetLoadException e) {
            System.err.println("[asset diagnostics] " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
