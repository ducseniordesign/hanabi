package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.TimedHand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Tell any other player about a card in their hand if it is useful in this situation.
 */
public class TellAnyoneAboutOldestUsefulCard extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {

        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayer = (playerID + i) % state.getPlayerCount();

            //gard against trying to tell ourselves things
            if (nextPlayer == playerID) {
                continue;
            }


            int oldestAge = Integer.MAX_VALUE;
            Action bestAction = null;
            TimedHand hand = (TimedHand)state.getHand(nextPlayer);

            for (int slot = 0; slot < state.getHandSize(); slot++) {

                Card card = hand.getCard(slot);
                if (card == null) {
                    continue;
                }

                if (hand.getAge(slot) > oldestAge) {
                    continue;
                }

                int currTable = state.getTableValue(card.colour);
                if (card.value != currTable + 1) {
                    continue;
                }

                if (hand.getKnownValue(slot) == null) {
                    bestAction = new TellValue(nextPlayer, card.value);
                } else if (hand.getKnownColour(slot) == null) {
                    bestAction = new TellColour(nextPlayer, card.colour);
                }
            }

            if (bestAction != null) {
                return bestAction;
            }
        }

        return null;
    }
}