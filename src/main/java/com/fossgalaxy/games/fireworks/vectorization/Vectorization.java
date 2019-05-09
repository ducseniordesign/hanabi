package com.fossgalaxy.games.fireworks.vectorization;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Vectorizing state/action pairs to binary bits
 */
public class Vectorization {
	
    private static void getCardsPlayed(int[] dataPoint, int begin, int end, CardColour colour, GameState state) {
    	int cardsPlayed = state.getTableValue(colour);
    	for (int i = begin; i <= end; i++) {
    		// Not necessary since this should all be 0 by default, but just to be safe
    		dataPoint[i] = 0; 
    	}
    	dataPoint[begin + cardsPlayed] = 1;
    }
    
    private static void vectorizedDiscards (int[] dataPoint, int begin, int[] discarded) {
    	// This step isn't really necessary, but just to be safe
    	for (int i = begin; i < begin + 10; i++) {
    		dataPoint[i] = 0;
    	}
    	
    	// The value 1 of this color
    	if (discarded[1] >= 1) dataPoint[begin] = 1;
    	if (discarded[1] >= 2) dataPoint[begin + 1] = 1;
    	if (discarded[1] >= 3) dataPoint[begin + 2] = 1;
    	
    	// The value 2 of this color
    	if (discarded[2] >= 1) dataPoint[begin + 3] = 1;
    	if (discarded[2] >= 2) dataPoint[begin + 4] = 1;
    	
    	// The value 3 of this color
    	if (discarded[3] >= 1) dataPoint[begin + 5] = 1;
    	if (discarded[3] >= 2) dataPoint[begin + 6] = 1;
    	
    	// The value 4 of this color 
    	if (discarded[4] >= 1) dataPoint[begin + 7] = 1;
    	if (discarded[4] >= 2) dataPoint[begin + 8] = 1;
    	
    	// The value 5 of this color
    	if (discarded[5] >= 1) dataPoint[begin + 9] = 1;
    }
    
    private static void getDiscardedCards(int[] dataPoint, GameState state) {
    	Collection<Card> discarded = state.getDiscards();
    	// Storing the values of discarded
    	int[] redDiscards = new int[6];
    	int[] orangeDiscards = new int[6];
    	int[] greenDiscards = new int[6];
    	int[] whiteDiscards = new int[6];
    	int[] blueDiscards = new int[6];
    	// Count the discards of each color
    	for (Card card : discarded) {
    		int val = card.value;
    		if (card.colour == CardColour.RED) {
    			redDiscards[val]++;
    		}
    		if (card.colour == CardColour.ORANGE) {
    			orangeDiscards[val]++;
    		}
    		if (card.colour == CardColour.GREEN) {
    			greenDiscards[val]++;
    		}
    		if (card.colour == CardColour.WHITE) {
    			whiteDiscards[val]++;
    		}
    		if (card.colour == CardColour.BLUE) {
    			blueDiscards[val]++;
    		}
    	}
    	// Now that we have the discard count, we can put this info in the data point vector
    	vectorizedDiscards(dataPoint, 84, redDiscards);
    	vectorizedDiscards(dataPoint, 94, orangeDiscards);
    	vectorizedDiscards(dataPoint, 104, greenDiscards);
    	vectorizedDiscards(dataPoint, 114, whiteDiscards);
    	vectorizedDiscards(dataPoint, 124, blueDiscards);
    }
    
    private static void vectorizeObservedCard (Hand hand, int cardPos, int begin, int[] dataPoint) {
    	Card card = hand.getCard(cardPos);
    	if (card == null) return;
    	if (card.colour == CardColour.RED) {
			dataPoint[begin + card.value - 1] = 1;
		}
		if (card.colour == CardColour.ORANGE) {
			dataPoint[begin + 5 + card.value - 1] = 1;
		}
		if (card.colour == CardColour.GREEN) {
			dataPoint[begin + 10 + card.value - 1] = 1;
		}
		if (card.colour == CardColour.WHITE) {
			dataPoint[begin + 15 + card.value - 1] = 1;
		}
		if (card.colour == CardColour.BLUE) {
			dataPoint[begin + 20 + card.value - 1] = 1;
		}
    }
    
    public static void getObservedCard (int playerID, int begin, int[] dataPoint, GameState state) {
    	Hand hand = state.getHand(playerID);
    	// Zero out data point vector
    	for (int i = begin; i < begin + 125; i++) {
    		dataPoint[i] = 0;
    	}
    	// Vectorize the cards
    	vectorizeObservedCard(hand, 0, begin, dataPoint);
    	vectorizeObservedCard(hand, 1, begin + 25, dataPoint);
    	vectorizeObservedCard(hand, 2, begin + 50, dataPoint);
    	vectorizeObservedCard(hand, 3, begin + 75, dataPoint);
    	vectorizeObservedCard(hand, 4, begin + 100, dataPoint);
    }
    
    
    
    /**
     * Separate function to vectorize the basic game state.
     * <p>
     * We don't use writeState() as to maintain the usability of that function for other
     * functions in the framework
     *
     */
    public static void basicGameState(GameState state, int[] dataPoint, 
    		int[][][][] possibleCards, int nextPlayer, int playerLength) {
    	// Lives left - Bits 0 to 3
    	int lives = state.getLives();
    	for (int i = 0; i < 4; i++) {
    		if (i <= lives) {
    			dataPoint[i] = 1;
    		} else {
    			dataPoint[i] = 0;
    		}
    	}
    	// Hints left - Bits 4 to 12
    	int hints = state.getInfomation();
    	for (int i = 0; i < 9; i++) {
    		if (i <= hints) {
    			dataPoint[4 + i] = 1;
    		} else {
    			dataPoint[4 + i] = 0;
    		}
    	}
    	// Deck size remaining - Bits 13 to 53
    	int deckSize = state.getDeck().getCardsLeft();
    	for (int i = 0; i < 41; i++) {
    		if (i <= deckSize) {
    			dataPoint[13 + i] = 1;
    		} else {
    			dataPoint[13 + i] = 0;
    		}
    	}
    	
    	// Red cards played on table - Bits 55 to 60
    	getCardsPlayed(dataPoint, 54, 59, CardColour.RED, state);
    	// Orange cards played on table - Bits 55 to 60
    	getCardsPlayed(dataPoint, 60, 65, CardColour.ORANGE, state);
    	// Green cards played on table - Bits 61 to 66
    	getCardsPlayed(dataPoint, 66, 71, CardColour.GREEN, state);
    	// White cards played on table - Bits 67 to 72
    	getCardsPlayed(dataPoint, 72, 77, CardColour.WHITE, state);
    	// Blue cards played on table - Bits 73 to 78
    	getCardsPlayed(dataPoint, 78, 83, CardColour.BLUE, state);
    	
    	// Get discarded cards
    	getDiscardedCards(dataPoint, state);
    	
    	// Get observed cards of other players
    	getObservedCard((nextPlayer + 1) % playerLength, 134, dataPoint, state);
    	
    	// Push the possible cards vector onto the data point as well
    	int curr = 259;
    	// Possible cards for self
    	for (int card = 0; card < 5; card++) {
			for (int colour = 0; colour < 5; colour++) {
				for (int value = 1; value < 6; value++) {
					dataPoint[curr] = possibleCards[nextPlayer][card][colour][value];
    				curr++;
				}
			}
		}
    	// Possible cards for other player
    	for (int card = 0; card < 5; card++) {
			for (int colour = 0; colour < 5; colour++) {
				for (int value = 1; value < 6; value++) {
					dataPoint[curr] = possibleCards[(nextPlayer + 1) % playerLength][card][colour][value];
    				curr++;
				}
			}
		}
    	// Turns left
    	int turnsLeft = state.getMovesLeft();
    	for (int i = 0; i < 2; i++) {
    		if (i <= turnsLeft) {
    			dataPoint[1731 + i] = 1;
    		}
    	}
    	// Player bit
    	dataPoint[1734] = nextPlayer;
    	
    	// Done with the game state representation, the rest will be action representation
    	return;
    	
    }
    
    public static void getDeckInfo (int begin, int[] dataPoint, GameState state) {
    	// Fill with 0s
    	for (int i = 0; i < 1025; i++) {
    		dataPoint[begin + i] = 0;
    	}
    	List<Card> deck = state.getDeck().toList();
    	Collections.reverse(deck);
    	for (Card card : deck) {
        	if (card == null) return;
        	if (card.colour == CardColour.RED) {
    			dataPoint[begin + card.value - 1] = 1;
    		}
    		if (card.colour == CardColour.ORANGE) {
    			dataPoint[begin + 5 + card.value - 1] = 1;
    		}
    		if (card.colour == CardColour.GREEN) {
    			dataPoint[begin + 10 + card.value - 1] = 1;
    		}
    		if (card.colour == CardColour.WHITE) {
    			dataPoint[begin + 15 + card.value - 1] = 1;
    		}
    		if (card.colour == CardColour.BLUE) {
    			dataPoint[begin + 20 + card.value - 1] = 1;
    		}
    		begin += 25; // Move to next card
    	}
    }
    
    private static int colourToNumber(CardColour colour) {
    	// Convert a colour into a number, for ease of calculation later on
    	if (colour == CardColour.RED) return 0; 
    	if (colour == CardColour.ORANGE) return 1; 
    	if (colour == CardColour.GREEN) return 2;
    	if (colour == CardColour.WHITE) return 3; 
    	return 4;
    }
    
    private static void processPlayAction(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin, Action lastAction,
    		GameState lastState, int lastPlayer, GameState state) {
    	for (int i = begin; i < begin + 20; i++) {
    		dataPoints[i] = 0;
    	}
    	int playSlot = lastAction.hashCode(); // This returns the card slot played
    	dataPoints[begin + playSlot] = 1; // This bit on to know which card was played
    	// Because we drew a new card, we need to update the possible card field
    	if (updatePossibleCards) {
    		for (int colour = 0; colour < 5; colour++) {
        		for (int value = 1; value < 6; value++) {
        			possibleCards[lastPlayer][playSlot][colour][value] = 1;
        		}
        	}
    	}
    	// Now process the outcome bits
    	if (!generateOutcome) {
    		return;
    	}
    	begin += 20;
    	for (int i = begin; i < begin + 32; i++) {
    		dataPoints[i] = 0;
    	}
    	Card playedCard = lastState.getCardAt(lastPlayer, playSlot);
    	if (playedCard != null) {
    		int cardColour = colourToNumber(playedCard.colour);
    		int cardValue = playedCard.value;
    		// We played this card
    		dataPoints[begin + 5 + cardColour * 5 + cardValue - 1] = 1;
    		dataPoints[begin + 30] = 1; // All our agents for now only make legal moves
    		if (lastState.getInfomation() + 1 == state.getInfomation()) {
    			// We gained a hint through the last play action
    			dataPoints[851 + 31] = 1;
    		}
    	}
    }
    
    private static void processDiscardAction(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin, Action lastAction,
    		GameState lastState, int lastPlayer) {
    	for (int i = begin; i < 20; i++) {
    		dataPoints[i] = 0;
    	}
    	int playSlot = lastAction.hashCode(); // This returns the card slot discarded
    	dataPoints[begin + 5 + playSlot] = 1; // This bit on to know which card was discarded
    	// Because we drew a new card, we need to update the possible card field
    	if (updatePossibleCards) {
    		for (int colour = 0; colour < 5; colour++) {
        		for (int value = 1; value < 6; value++) {
        			possibleCards[lastPlayer][playSlot][colour][value] = 1;
        		}
        	}
    	}
    	// Now process the outcome bits
    	if (!generateOutcome) {
    		return;
    	}
    	begin += 20;
    	for (int i = begin; i < begin + 32; i++) {
    		dataPoints[i] = 0;
    	}
    	Card discardedCard = lastState.getCardAt(lastPlayer, playSlot);
    	if (discardedCard != null) {
    		int cardColour = colourToNumber(discardedCard.colour);
    		int cardValue = discardedCard.value;
    		// We discarded this card
    		dataPoints[begin + 5 + cardColour * 5 + cardValue - 1] = 1;
    		dataPoints[begin + 30] = 1; // All our agents for now only make legal moves
    	}
    }
    
    private static void processTellColour(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin,
    		Action lastAction, GameState lastState, int lastPlayer) {
    	for (int i = begin; i < 20; i++) {
    		dataPoints[i] = 0;
    	}
    	
    	TellColour act = (TellColour)lastAction; // Should not fail because we did the checking

    	CardColour colour = act.getColour();
    	int colourNum = colourToNumber(colour);
    	dataPoints[begin + 5 + 5 + colourNum] = 1;
    	
    	if (generateOutcome) {
    		begin += 20;
    		for (int i = begin; i < begin + 32; i++) {
        		dataPoints[i] = 0;
        	}
    	}
    	
    	
    	// Update the possible cards for the player receiving the hint
    	Hand hand = lastState.getHand((lastPlayer + 1) % 2);
    	for (int i = 0; i < 5; i++) {
    		// For each of the card
    		Card card = hand.getCard(i);
    		if (card == null) continue;
    		int cardColour = colourToNumber(card.colour);
    		if (cardColour == colourNum) {
    			// This card was hinted, no color but this one is possible
    			if (generateOutcome) {
    				// Card was hinted
        			dataPoints[begin + i] = 1;
    			}
    			if (!updatePossibleCards) {
    				continue;
    			}
    			for (int possibleColour = 0; possibleColour < 5; possibleColour++) {
    				if (possibleColour != cardColour) {
    					for (int value = 1; value < 6; value++) {
    						possibleCards[(lastPlayer + 1) % 2][i][possibleColour][value] = 0;
    					}
    				}
    			}
    		} else if (updatePossibleCards) {
    			// This card was not hinted, eleminate this color
    			for (int value = 1; value < 6; value++) {
    				possibleCards[(lastPlayer + 1) % 2][i][colourNum][value] = 0;
    			}
    		}
    	}
    	
    	// Our agents only play legal actions
    	if (generateOutcome) {
        	dataPoints[begin + 30] = 1;
    	}
    }
    
    private static void processTellValue(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin,
    		Action lastAction, GameState lastState, int lastPlayer) {
    	for (int i = begin; i < 20; i++) {
    		dataPoints[i] = 0;
    	}
    	
    	TellValue act = (TellValue)lastAction; // Should not fail because we did the checking
    	
    	int value = act.getValue();
    	dataPoints[begin + 5 + 5 + 5 + value - 1] = 1;
    	
    	if (generateOutcome) {
    		begin += 20;
    		for (int i = begin; i < begin + 32; i++) {
        		dataPoints[i] = 0;
        	}
    	}
    	
    	
    	// Update the possible cards for the player receiving the hint
    	Hand hand = lastState.getHand((lastPlayer + 1) % 2);
    	for (int i = 0; i < 5; i++) {
    		// For each of the card
    		Card card = hand.getCard(i);
    		if (card == null) continue;
    		int cardValue = card.value;
    		if (cardValue == value) {
    			// This card was hinted, no value but this one is possible
    			if (generateOutcome) {
    				// Card was hinted
        			dataPoints[begin + i] = 1;
    			}
    			if (!updatePossibleCards) {
    				continue;
    			}
    			for (int colour = 0; colour < 5; colour++) {
    				for (int possibleValue = 1; possibleValue < 6; possibleValue++) {
    					if (possibleValue != value) {
    						possibleCards[(lastPlayer + 1) % 2][i][colour][possibleValue] = 0;
    					}
    				}
    			}
    		} else if (updatePossibleCards) {
    			// This card was not hinted, eleminate this color
    			for (int colour = 0; colour < 5; colour++) {
    				possibleCards[(lastPlayer + 1) % 2][i][colour][value] = 0;
    			}
    		}
    	}
    	
    	// Our agents only play legal actions
    	if (generateOutcome) {
        	dataPoints[begin + 30] = 1;
    	}
    }
    
    /**
     * Process the last action made and vectorize the data. 
     * Also change the possible cards for each player
     */
    public static void processAction(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin,
    		Action lastAction, GameState lastState, int lastPlayer, GameState state) {
    	if (lastAction instanceof PlayCard) {
    		// If the last action was playing a card
    		processPlayAction(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin, lastAction, lastState, lastPlayer, state);
    	}
    	if (lastAction instanceof DiscardCard) {
    		// If the last action was discarding a card
    		processDiscardAction(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin, lastAction, lastState, lastPlayer);
    	}
    	if (lastAction instanceof TellColour) {
    		// If the last action was hinting a colour
    		processTellColour(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin, lastAction, lastState, lastPlayer);
    	}
    	if (lastAction instanceof TellValue) {
    		// If the last action was hinting a value
    		processTellValue(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin, lastAction, lastState, lastPlayer);
    	}
    }

}
