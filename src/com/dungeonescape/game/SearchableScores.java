package com.dungeonescape.game;

import java.util.List;

/**
 * A score container that the result screen can search by player name.
 *
 * <p>Implemented by both {@link HighScoreBoard} (the displayed top-10
 * leaderboard) and {@link ScoreHistory} (the complete, unbounded record of
 * every completed run). The result screen depends on this interface only,
 * so its search box can be pointed at either source without UI changes:</p>
 *
 * <ul>
 *   <li>{@link #findBestScoreByName(String)} — the single highest score for
 *       a player, matched case-insensitively and ignoring surrounding
 *       whitespace.</li>
 *   <li>{@link #findScoresByName(String)} — every matching run, best score
 *       first (duplicate names are legitimate).</li>
 * </ul>
 *
 * <p>Both operations are linear scans over the underlying entries.</p>
 */
public interface SearchableScores {

    /**
     * Returns the best score recorded for a player, or null when no
     * entry matches.
     */
    HighScoreBoard.ScoreEntry findBestScoreByName(String player);

    /**
     * Returns every run recorded for a player, best score first. The
     * returned list cannot modify the source.
     */
    List<HighScoreBoard.ScoreEntry> findScoresByName(String player);
}
