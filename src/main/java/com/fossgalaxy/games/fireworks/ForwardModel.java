package com.fossgalaxy.games.fireworks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import com.fossgalaxy.games.fireworks.vectorization.Vectorization;

public class ForwardModel {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String inputStr = reader.readLine();
        // Input here should be have 1735 bits
        if (inputStr.length() != 1735) {
        	System.err.println("Expecting 1735 bits as input");
        	System.exit(1);
        }
        int[] input = new int[1735];
        for (int i = 0; i < 1735; i++) {
        	input[i] = (inputStr.charAt(i) == '0') ? 0 : 1;
        }
        // Set up the game state - Hand size 5 - 2 players
        BasicState state = new BasicState(5, 2);
        // Read from the input to the game state
        // Lives left
        for (int i = 3; i >= 0; i--) {
        	if (input[i] == 1) {
        		state.setLives(i);
        		break;
        	}
        }
        // Hints left 
        for (int i = 12; i >= 4; i--) {
        	if (input[i] == 1) {
        		state.setInformation(i - 4);
        		break;
        	}
        }
        // Moves left
        for (int i = 1733; i >= 1731; i--) {
        	if (input[i] == 1) {
        		state.setMovesLeft(i - 1731);
        		break;
        	}
        }
        // Which player are we
        int playerBit = input[1734];
        int otherPlayer = 1 - playerBit;
        // Cards played
        setCardsPlayed(input, state, 54, CardColour.RED);
        setCardsPlayed(input, state, 60, CardColour.ORANGE);
        setCardsPlayed(input, state, 66, CardColour.GREEN);
        setCardsPlayed(input, state, 72, CardColour.WHITE);
        setCardsPlayed(input, state, 78, CardColour.BLUE);
        
        // Set discarded
        setDiscarded(input, state, 84, CardColour.RED);
        setDiscarded(input, state, 94, CardColour.ORANGE);
        setDiscarded(input, state, 104, CardColour.GREEN);
        setDiscarded(input, state, 114, CardColour.WHITE);
        setDiscarded(input, state, 84, CardColour.BLUE);
        
        // Get hand for self
        state.setCardAt(playerBit, 0, getCard(input, 581));
        state.setCardAt(playerBit, 1, getCard(input, 606));
        state.setCardAt(playerBit, 2, getCard(input, 631));
        state.setCardAt(playerBit, 3, getCard(input, 656));
        state.setCardAt(playerBit, 4, getCard(input, 681));
        
        // Get hand for other player
        state.setCardAt(otherPlayer, 0, getCard(input, 134));
        state.setCardAt(otherPlayer, 1, getCard(input, 159));
        state.setCardAt(otherPlayer, 2, getCard(input, 184));
        state.setCardAt(otherPlayer, 3, getCard(input, 209));
        state.setCardAt(otherPlayer, 4, getCard(input, 234));
        
        // Deal with the deck
        Deck deck = new Deck();
        for (int i = 706; i < 1731; i += 25) {
        	Card card = getCard(input, i);
        	if (card == null) {
        		break; // No more cards to process
        	}
        	deck.add(card);
        }
        state.setDeck(deck);
        
        // Get the possible cards vector from the input
        int[][][][] possibleCards = new int[2][5][5][6];
        int cursor = 259;
        for (int player = 0; player < 2; player++) {
    		for (int card = 0; card < 5; card++) {
    			for (int colour = 0; colour < 5; colour++) {
    				for (int value = 1; value < 6; value++) {
    					int absolutePlayer = player;
    					if (playerBit == 1) absolutePlayer = 1 - absolutePlayer;
    					possibleCards[absolutePlayer][card][colour][value] = input[cursor];
    					cursor++;
    				}
    			}
    		}
    	}
        
        // Now get the action
        Action action = getAction(input, 561, otherPlayer);
        if (!action.isLegal(playerBit, state)) {
        	// Illegal action, print all 0s
        	for (int i = 0; i < 1735; i++) {
            	System.out.print(0);
            }
            System.exit(1);
        }
        GameState lastState = state.getCopy(); // Remember the last state
        action.apply(playerBit, state);

        // Print the resulting state + action
        int[] output = new int[1735];
        for (int i = 0; i < 1735; i++) {
        	output[i] = 0; // Safe zero-ing
        }
        // Process the action first to update possible cards 
        Vectorization.processAction(output, possibleCards, true, true, 509, action, lastState, playerBit, state);
        // Process the state
        Vectorization.basicGameState(state, output, possibleCards, otherPlayer, 2);
        // Process hand for "self", "self" for this state becomes the other player
        Vectorization.getObservedCard(otherPlayer, 581, output, state);
        // Process new deck info
        Vectorization.getDeckInfo(706, output, state);
        // Output
        for (int i = 0; i < 1735; i++) {
        	System.out.print(output[i]);
        }
	}
	
	
    private static void setCardsPlayed(int[] input, BasicState state, int begin, CardColour colour) {
    	for (int i = 0; i < 6; i++) {
    		if (input[begin + i] == 1) {
    			state.setTableValue(colour, i);
    			return;
    		}
    	}
    }
    
    private static void setDiscarded(int[] input, BasicState state, int begin, CardColour colour) {
    	if (input[begin] == 1) state.addToDiscard(new Card(1, colour));
    	if (input[begin + 1] == 1) state.addToDiscard(new Card(1, colour));
    	if (input[begin + 2] == 1) state.addToDiscard(new Card(1, colour));
    	if (input[begin + 3] == 1) state.addToDiscard(new Card(2, colour));
    	if (input[begin + 4] == 1) state.addToDiscard(new Card(2, colour));
    	if (input[begin + 5] == 1) state.addToDiscard(new Card(3, colour));
    	if (input[begin + 6] == 1) state.addToDiscard(new Card(3, colour));
    	if (input[begin + 7] == 1) state.addToDiscard(new Card(4, colour));
    	if (input[begin + 8] == 1) state.addToDiscard(new Card(4, colour));
    	if (input[begin + 9] == 1) state.addToDiscard(new Card(5, colour));
    }
    
    private static Card getCard (int[] input, int begin) {
    	// Given 25 bits, return what card it is
    	int value = -1; // Default value to shut up compiler
    	CardColour colour = CardColour.RED; // Default value
    	for (int i = 0; i < 25; i++) {
    		if (input[begin + i] == 1) {
    			switch(i/5) {
    				case 0:
    					colour = CardColour.RED;
    					break;
    				case 1: 
    					colour = CardColour.ORANGE;
    					break;
    				case 2:
    					colour = CardColour.GREEN;
    					break;
    				case 3:
    					colour = CardColour.WHITE;
    					break;
    				case 4:
    					colour = CardColour.BLUE;
    					break;
    			}
    			value = (i % 5) + 1;
    			break;
    		}
    	}
    	if (value == -1) {
    		return null;
    	}
    	return new Card(value, colour);
    }
    
    private static Action getAction (int[] input, int begin, int target) {
    	int hot = -1;
    	for (int i = begin; i < begin + 20; i++) {
    		if (input[i] == 1) {
    			hot = i - begin;
    			break;
    		}
    	}
    	if (hot == -1) return null;
    	if (hot < 5) {
			// Playing a card
			return new PlayCard(hot);
		} else if (hot < 10) {
			// Discard a card
			return new DiscardCard(hot - 5);
		} else if (hot < 15) {
			CardColour whichColor = CardColour.RED;
			if (hot == 11)
				whichColor = CardColour.ORANGE;
			if (hot == 12)
				whichColor = CardColour.GREEN;
			if (hot == 13)
				whichColor = CardColour.WHITE;
			if (hot == 14)
				whichColor = CardColour.BLUE;
			return new TellColour(target, whichColor);
		} else {
			return new TellValue(target, hot - 14);
		}
    }

}
