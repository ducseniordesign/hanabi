package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.rule.logic.HandUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Created by piers on 12/12/16.
 */
public class TellAnyoneAboutUselessCard extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayer = (playerID + i) % state.getPlayerCount();
            Hand hand = state.getHand(nextPlayer);
            if (nextPlayer == playerID) {
                continue;
            }

            for (int slot = 0; slot < state.getHandSize(); slot++) {
                if (!hand.hasCard(slot)) {
                    continue;
                }
                if (HandUtils.isSafeToDiscard(state, nextPlayer, slot)) {
                    Card card = hand.getCard(slot);
                    return (hand.getKnownValue(slot) == null) ? new TellValue(nextPlayer, card.value) : new TellColour(nextPlayer, card.colour);
                }
            }
        }
        return null;
    }
}