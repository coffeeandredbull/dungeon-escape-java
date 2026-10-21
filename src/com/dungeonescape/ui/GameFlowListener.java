package com.dungeonescape.ui;

/**
 * Callbacks the game view uses to tell the window that a run has ended.
 *
 * <p>Package-private: only the UI layer uses it.</p>
 */
interface GameFlowListener {

    /** Called once when the player runs out of lives. */
    void onGameOver();

    /** Called once when the player completes every level. */
    void onVictory();
}
