package com.fossgalaxy.games.fireworks.utils;

import com.fossgalaxy.games.fireworks.GameRunner;
import com.fossgalaxy.games.fireworks.GameRunnerCheat;
import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.players.Player;

/**
 * This contains tools to help run games in a consistent, reproducible way.
 */
public final class GameUtils {

    private GameUtils() {

    }

    /**
     * Play a standard game of Hanabi
     *
     * @param gameID the Id of the game to play
     * @param seed the seed used for deck ordering
     * @param players the players that will play the game
     * @return the result of the game
     */
    public static GameStats runGame(String gameID, Long seed, Player... players) {
        assert gameID != null;
        assert players != null;

        GameRunner runner = new GameRunner(gameID, players.length);

        for (Player player : players) {
            runner.addPlayer(player);
        }

        return runner.playGame(seed);
    }


    /**
     * Play a complete information (cheated) version of Hanabi
     *
     * @param gameID The gameID to log this game under
     * @param name the names of the agents that are playing
     * @param seed the seed to use for deck ordering
     * @param players the players to use for the game
     * @return the outcome of the game
     */
    public static GameStats runCheatGame(String gameID, String[] name, Long seed, Player ... players) {
        assert gameID != null;
        assert players != null;

        GameRunner runner = new GameRunnerCheat(gameID, players.length);

        for (Player player : players) {
            runner.addPlayer(player);
        }

        return runner.playGame(seed);
    }

}
