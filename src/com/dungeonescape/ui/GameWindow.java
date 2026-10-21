package com.dungeonescape.ui;

import com.dungeonescape.asset.AssetLoader;
import com.dungeonescape.asset.AudioManager;
import com.dungeonescape.game.GameSession;
import com.dungeonescape.game.HighScoreBoard;
import com.dungeonescape.game.HighScoreStorage;
import com.dungeonescape.game.Level;
import com.dungeonescape.game.Levels;
import com.dungeonescape.game.ScoreHistory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The application's main window.
 *
 * <p>Owns the title → game → result flow with a {@link CardLayout} and the
 * {@link AudioManager}, which is created when a run starts and released when
 * the window closes. It also keeps the {@link HighScoreBoard} leaderboard and
 * adds one entry per finished run, recorded from the result screen; the
 * complete run history ({@link ScoreHistory}, including runs outside the
 * top 10) backs the result screen's player-name search box. Both persist
 * to a CSV file in the user's home directory (see {@link
 * HighScoreStorage}): the history is loaded once at startup, the top-10
 * board is rebuilt from it, and the history is saved once per finished
 * run, never during gameplay.</p>
 */
public class GameWindow extends JFrame {

    private static final String TITLE_CARD = "title";
    private static final String GAME_CARD = "game";
    private static final String RESULT_CARD = "result";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final HudPanel hud = new HudPanel();
    private final HighScoreBoard highScores = new HighScoreBoard();
    private final TitlePanel titlePanel = new TitlePanel(this::startGame);
    private final AssetLoader loader = new AssetLoader();
    private final Path scoreFile = HighScoreStorage.defaultFile();
    private final ScoreHistory scoreHistory = new ScoreHistory();
    private final ResultPanel resultPanel =
            new ResultPanel(this::startGame, scoreHistory);

    private AudioManager audio;
    private GameSession session;
    private JPanel gamePanel;

    /** Creates the window. Callers should invoke this on the Swing EDT. */
    public GameWindow() {
        super("Dungeon Escape");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(content, BorderLayout.CENTER);
        content.add(titlePanel, TITLE_CARD);
        content.add(resultPanel, RESULT_CARD);
        cards.show(content, TITLE_CARD);

        // Restore the complete score history from the CSV first, then
        // rebuild the visible top-10 leaderboard from it, so the board's
        // insertion sort reproduces the ranking across restarts. A
        // missing or unreadable file simply leaves everything empty.
        HighScoreStorage.load(scoreHistory, scoreFile);

        for (HighScoreBoard.ScoreEntry entry : scoreHistory.entries()) {
            highScores.record(entry.player(), entry.score());
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disposeAudio();
            }
        });

        installUndoKeyBinding();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /** Starts a fresh run: builds the session, view, and audio. */
    private void startGame() {
        if (audio == null) {
            audio = new AudioManager();
            loadSounds(audio);
        }

        session = new GameSession(Levels.create(), audio::play);
        DungeonPanel dungeonPanel = new DungeonPanel(loader, hud, session);
        dungeonPanel.setGameFlowListener(new GameFlowListener() {
            @Override
            public void onGameOver() {
                showResult(false);
            }

            @Override
            public void onVictory() {
                showResult(true);
            }
        });

        if (gamePanel != null) {
            content.remove(gamePanel);
        }
        gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(hud, BorderLayout.NORTH);
        gamePanel.add(dungeonPanel, BorderLayout.CENTER);
        content.add(gamePanel, GAME_CARD);
        cards.show(content, GAME_CARD);
        dungeonPanel.requestFocusInWindow();
    }

    /**
     * Switches to the result card with the appropriate message.
     *
     * <p>Called exactly once per run (the game view notifies the flow
     * listener only once), so each finished run adds exactly one row to the
     * high-score board before the result is displayed.</p>
     */
    private void showResult(boolean victory) {
        if (session == null) {
            return;
        }

        // The history keeps every completed run (even runs outside the
        // top 10); the board keeps displaying the qualifying top 10.
        scoreHistory.append(titlePanel.playerName(), session.score());
        highScores.record(titlePanel.playerName(), session.score());
        HighScoreStorage.save(scoreHistory, scoreFile);
        resultPanel.setScoreLines(describeHighScores());

        if (victory) {
            resultPanel.configure("ESCAPED!",
                    "You escaped the dungeon with a score of " + session.score() + ".", false);
        } else {
            resultPanel.configure("GAME OVER",
                    "The monster caught you. Final score: " + session.score() + ".", true);
        }
        cards.show(content, RESULT_CARD);
    }

    /**
     * Formats the board's top rows for display on the result screen.
     */
    private List<String> describeHighScores() {
        List<String> lines = new ArrayList<>();
        List<HighScoreBoard.ScoreEntry> entries = highScores.entries();

        for (int i = 0; i < entries.size() && i < 5; i++) {
            HighScoreBoard.ScoreEntry entry = entries.get(i);

            lines.add((i + 1) + ". " + entry.player()
                    + " - " + entry.score());
        }

        return lines;
    }

    /**
     * Binds the Z key to undoing the player's last recorded move.
     *
     * <p>Installed on the window's root pane so the dungeon panel's own
     * key bindings (whose count InputBindingTest pins at exactly 16) are
     * left untouched. WHEN_IN_FOCUSED_WINDOW wording keeps the binding
     * active whenever this window is focused, on any card. Holding Z
     * repeats the binding, so a long press undoes several steps.</p>
     */
    private void installUndoKeyBinding() {
        InputMap inputMap = getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0, false),
                "undo-last-move");

        actionMap.put("undo-last-move", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (session != null) {
                    session.undoLastMove();
                }
            }
        });
    }

    private void loadSounds(AudioManager audio) {
        audio.load("door", loader, "audio/doorOpen_2.wav");
        audio.load("chest", loader, "audio/gold_sack.wav");
        audio.load("key", loader, "audio/key.wav");
        audio.load("potion", loader, "audio/drink.wav");
        audio.load("monster", loader, "audio/zombie-2.wav");
        audio.load("gameover", loader, "audio/GameOver.wav");
        audio.load("victory", loader, "audio/Victory!.wav");
        audio.load("footstep", loader, "audio/footstep06.wav");
    }

    private void disposeAudio() {
        if (audio != null) {
            audio.dispose();
        }
    }
}
