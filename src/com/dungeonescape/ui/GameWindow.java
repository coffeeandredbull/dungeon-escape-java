package com.dungeonescape.ui;

import com.dungeonescape.asset.AssetLoader;
import com.dungeonescape.asset.AudioManager;
import com.dungeonescape.game.GameSession;
import com.dungeonescape.game.Level;
import com.dungeonescape.game.Levels;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * The application's main window.
 *
 * <p>Owns the title → game → result flow with a {@link CardLayout} and the
 * {@link AudioManager}, which is created when a run starts and released when
 * the window closes.</p>
 */
public class GameWindow extends JFrame {

    private static final String TITLE_CARD = "title";
    private static final String GAME_CARD = "game";
    private static final String RESULT_CARD = "result";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final HudPanel hud = new HudPanel();
    private final TitlePanel titlePanel = new TitlePanel(this::startGame);
    private final ResultPanel resultPanel = new ResultPanel(this::startGame);
    private final AssetLoader loader = new AssetLoader();

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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disposeAudio();
            }
        });

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

    /** Switches to the result card with the appropriate message. */
    private void showResult(boolean victory) {
        if (session == null) {
            return;
        }
        if (victory) {
            resultPanel.configure("ESCAPED!",
                    "You escaped the dungeon with a score of " + session.score() + ".", false);
        } else {
            resultPanel.configure("GAME OVER",
                    "The monster caught you. Final score: " + session.score() + ".", true);
        }
        cards.show(content, RESULT_CARD);
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
