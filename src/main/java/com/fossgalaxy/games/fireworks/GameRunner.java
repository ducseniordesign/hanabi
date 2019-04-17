package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.CardDrawn;
import com.fossgalaxy.games.fireworks.state.events.CardReceived;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.state.events.GameInformation;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;

/**
 * A basic runner for the game of Hanabi.
 */
public class GameRunner {
    private static final int RULE_STRIKES = 1; //how many times can a player return an illegal move before we give up?
    private static final int[] HAND_SIZE = {-1, -1, 5, 5, 4, 4};
    private final Logger logger = LoggerFactory.getLogger(GameRunner.class);
    private final String gameID;

    protected final Player[] players;
    protected final String[] playerNames;

    protected final GameState state;

    private int nPlayers;
    private int moves;

    private int nextPlayer;
    
    private Action lastAction;
    private int lastPlayer;
    private GameState lastState;

    /**
     * Create a game runner with a given ID and a number of players.
     *
     * @param id           the Id of the game
     * @param playersCount the number of players that will be playing
     * @deprecated use string IDs instead
     */
    @Deprecated
    public GameRunner(UUID id, int playersCount) {
        this(id.toString(), playersCount, false);
    }

    /**
     * Create a game runner with a given ID and number of players.
     *
     * This is backwards compatable (ie, no lives means score at last move).
     *
     * @param id the game id
     * @param expectedPlayers the number of players that will be in the game
     */
    public GameRunner(String id, int expectedPlayers){
        this(id, expectedPlayers, false);
    }

    /**
     * Create a game runner with a given ID and number of players.
     *
     * @param gameID          the ID of the game
     * @param expectedPlayers the number of players we expect to be playing.
     * @param noLivesMeansZero true if no lives means the players get zero.
     */
    public GameRunner(String gameID, int expectedPlayers, boolean noLivesMeansZero) {
        this(gameID, noLivesMeansZero ? new NoLifeState(HAND_SIZE[expectedPlayers], expectedPlayers) : new BasicState(HAND_SIZE[expectedPlayers], expectedPlayers));
    }

    public GameRunner(String gameID, GameState state){
        this.players = new Player[state.getPlayerCount()];
        this.playerNames = new String[state.getPlayerCount()];
        this.state = Objects.requireNonNull(state);
        this.nPlayers = 0;
        this.nextPlayer = 0;
        this.moves = 0;
        this.gameID = gameID;
    }

    /**
     * Add a player to the game.
     * <p>
     * This should not be attempted once the game has started.
     *
     * @param player the player to add to the game
     */
    public void addPlayer(Player player) {
        logger.info("player {} is {}", nPlayers, player);
        players[nPlayers++] = Objects.requireNonNull(player);
    }

    /**
     * Add a named player.
     *
     * Player names will be revealed to all players at the start of a game.
     *
     * @param name the name of the player
     * @param player the player object
     */
    public void addNamedPlayer(String name, Player player) {
        addPlayer(player);
        playerNames[nPlayers - 1] = name;
    }

    /**
     * Initialise the game for the players.
     * <p>
     * This method does the setup phase for the game.
     * <p>
     * this method is responsible for:
     * 1) telling player their IDs
     * 2) initialising the game state and deck order
     * 3) informing players about the number of players and starting resource values
     * 4) dealing and declaring the values in the player's initial hands.
     * <p>
     * You should <b>not</b> call this method directly - calling playGame calls it for you on your behalf!
     *
     * @param seed the random seed to use for deck ordering.
     */
    protected void init(Long seed) {
        logger.info("game init started - {} player game with seed {}", players.length, seed);
        long startTime = getTick();

        //step 1: tell all players their IDs
        for (int i = 0; i < players.length; i++) {
            logger.info("player {} is {}", i, players[i]);
            players[i].setID(i, players.length, playerNames);
        }

        state.init(seed);

        //keep track of the messages that should be sent as part of the game setup
        List<GameEvent> initEvents = new ArrayList<>();

        // tell the players the rules
        GameEvent gameInfo = new GameInformation(nPlayers, HAND_SIZE[nPlayers], state.getInfomation(), state.getLives());
        initEvents.add(gameInfo);

        //tell players about the initial state
        for (int player = 0; player < players.length; player++) {
            Hand hand = state.getHand(player);

            for (int slot = 0; slot < hand.getSize(); slot++) {
                Card cardInSlot = hand.getCard(slot);

                GameEvent cardDrawn = new CardDrawn(player, slot, cardInSlot.colour, cardInSlot.value, 0);
                GameEvent cardRecv = new CardReceived(player, slot, state.getDeck().hasCardsLeft(), 0);
                initEvents.add(cardDrawn);
                initEvents.add(cardRecv);
            }
        }

        //dispatch the events to the players
        notifyAction(-2, null, initEvents);

        long endTime = getTick();
        logger.info("Game init complete: took {} ms", endTime - startTime);
    }


    //TODO find a better way of doing this logging.
    protected void writeState(GameState state) {
        DebugUtils.printState(logger, state);        
    }

    private long getTick() {
        return System.currentTimeMillis();
    }

    //TODO time limit the agent

    /**
     * Ask the next player for their move.
     */
    protected void nextMove() {
        Player player = players[nextPlayer];
        assert player != null : "that player is not valid";

        logger.debug("asking player {} for their move", nextPlayer);
        long startTime = getTick();

        //get the action and try to apply it
        Action action = player.getAction();

        long endTime = getTick();
        logger.debug("agent {} took {} ms to make their move", nextPlayer, endTime - startTime);
        logger.debug("move {}: player {} made move {}", moves, nextPlayer, action);

        //if the more was illegal, throw a rules violation
        if (!action.isLegal(nextPlayer, state)) {
            throw new RulesViolation(action);
        }

        //perform the action and get the effects
        logger.info("player {} made move {} as turn {}", nextPlayer, action, moves);
        moves++;


        Collection<GameEvent> events = action.apply(nextPlayer, state);
        notifyAction(nextPlayer, action, events);
        
        // Save the information of the action and the player for further processing
        lastAction = action;
        lastPlayer = nextPlayer;
        lastState = state.getCopy();

        //make sure it's the next player's turn
        nextPlayer = (nextPlayer + 1) % players.length;
    }
    
    private void getCardsPlayed(int[] dataPoint, int begin, int end, CardColour colour) {
    	int cardsPlayed = state.getTableValue(colour);
    	for (int i = begin; i <= end; i++) {
    		// Not necessary since this should all be 0 by default, but just to be safe
    		dataPoint[i] = 0; 
    	}
    	dataPoint[begin + cardsPlayed] = 1;
    }
    
    private void vectorizedDiscards (int[] dataPoint, int begin, int[] discarded) {
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
    
    private void getDiscardedCards(int[] dataPoint) {
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
    	vectorizedDiscards(dataPoint, 85, redDiscards);
    	vectorizedDiscards(dataPoint, 95, orangeDiscards);
    	vectorizedDiscards(dataPoint, 105, greenDiscards);
    	vectorizedDiscards(dataPoint, 115, whiteDiscards);
    	vectorizedDiscards(dataPoint, 125, blueDiscards);
    }
    
    private void vectorizeObservedCard (Hand hand, int cardPos, int begin, int[] dataPoint) {
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
    
    private void getObservedCard (int playerID, int begin, int[] dataPoint) {
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
    private void basicGameState(GameState state, int[] dataPoint, int[][][][] possibleCards) {
    	// Lives left - Bits 1 to 4
    	int lives = state.getLives();
    	for (int i = 1; i < 5; i++) {
    		if (i <= lives) {
    			dataPoint[i] = 1;
    		} else {
    			dataPoint[i] = 0;
    		}
    	}
    	// Hints left - Bits 5 to 13
    	int hints = state.getInfomation();
    	for (int i = 0; i < 9; i++) {
    		if (i <= hints) {
    			dataPoint[5 + i] = 1;
    		} else {
    			dataPoint[5 + i] = 0;
    		}
    	}
    	// Deck size remaining - Bits 14 to 54
    	int deckSize = state.getDeck().getCardsLeft();
    	for (int i = 0; i < 41; i++) {
    		if (i <= deckSize) {
    			dataPoint[14 + i] = 1;
    		} else {
    			dataPoint[14 + i] = 0;
    		}
    	}
    	
    	// Red cards played on table - Bits 55 to 60
    	getCardsPlayed(dataPoint, 55, 60, CardColour.RED);
    	// Orange cards played on table - Bits 55 to 60
    	getCardsPlayed(dataPoint, 61, 66, CardColour.ORANGE);
    	// Green cards played on table - Bits 61 to 66
    	getCardsPlayed(dataPoint, 67, 72, CardColour.GREEN);
    	// White cards played on table - Bits 67 to 72
    	getCardsPlayed(dataPoint, 73, 78, CardColour.WHITE);
    	// Blue cards played on table - Bits 73 to 78
    	getCardsPlayed(dataPoint, 79, 84, CardColour.BLUE);
    	
    	// Get discarded cards
    	getDiscardedCards(dataPoint);
    	
    	// Get observed cards of other players
    	getObservedCard((nextPlayer + 1) % players.length, 135, dataPoint);
    	
    	// Push the possible cards vector onto the data point as well
    	int curr = 260;
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
					dataPoint[curr] = possibleCards[(nextPlayer + 1) % players.length][card][colour][value];
    				curr++;
				}
			}
		}
    	
    	// Done with the game state representation, the rest will be action representation
    	return;
    	
    }
    
    private int colourToNumber(CardColour colour) {
    	// Convert a colour into a number, for ease of calculation later on
    	if (colour == CardColour.RED) return 0; 
    	if (colour == CardColour.ORANGE) return 1; 
    	if (colour == CardColour.GREEN) return 2;
    	if (colour == CardColour.WHITE) return 3; 
    	return 4;
    }
    
    private void processPlayAction(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin) {
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
    
    private void processDiscardAction(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin) {
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
    
    private void processTellColour(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin) {
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
    	for (int i = 0; i < 4; i++) {
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
    
    private void processTellValue(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin) {
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
    	for (int i = 0; i < 4; i++) {
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
    private void processAction(int[] dataPoints, int[][][][] possibleCards, 
    		boolean generateOutcome, boolean updatePossibleCards, int begin) {
    	if (lastAction instanceof PlayCard) {
    		// If the last action was playing a card
    		processPlayAction(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin);
    	}
    	if (lastAction instanceof DiscardCard) {
    		// If the last action was discarding a card
    		processDiscardAction(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin);
    	}
    	if (lastAction instanceof TellColour) {
    		// If the last action was hinting a colour
    		processTellColour(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin);
    	}
    	if (lastAction instanceof TellValue) {
    		// If the last action was hinting a value
    		processTellValue(dataPoints, possibleCards, generateOutcome, updatePossibleCards, begin);
    	}
    }

    /**
     * Play the game and generate the outcome.
     * <p>
     * This will play the game and generate a result.
     *
     * @param seed the seed to use for deck ordering
     * @return the result of the game
     */
    public GameStats playGame(Long seed) {
        int strikes = 0;

        try {
            assert nPlayers == players.length;
            init(seed);
            // Possible cards as perceived by each player
            // 2 players, 5 cards each player, 25 values each card (5x5)
            int[][][][] possibleCards = new int[2][5][5][6];
            // In the beginning, everything is possible for all players
            for (int player = 0; player < 2; player++) {
        		for (int card = 0; card < 5; card++) {
        			for (int colour = 0; colour < 5; colour++) {
        				for (int value = 1; value < 6; value++) {
        					possibleCards[player][card][colour][value] = 1;
        				}
        			}
        		}
        	}
            // Random noise for this game, Uniform randomness
            Random r = new Random();
            r.setSeed(System.currentTimeMillis() / 1000L);
            int[] randomNoise = new int[10];
            for (int i = 0; i < 10; i++) {
            	randomNoise[i] = r.nextInt(2);
            }
            while (!state.isGameOver()) {
                try {
                	int[] dataPoint = new int[592]; // Array to hold the bits of this data point
                	dataPoint[0] = nextPlayer; // 2 - person game, next player is 0 or 1
                    writeState(state);
                    basicGameState(state, dataPoint, possibleCards);
                    // Process the action last made by the previous agent
                    processAction(dataPoint, possibleCards, true, false, 510);
                    // Adding in the random noise - Uniform randomness
                    for (int i = 562; i < 571; i++) {
                    	dataPoint[i] = randomNoise[i-562];
                    }
                    nextMove();
                    processAction(dataPoint, possibleCards, false, true, 572);
                    // Write to file the data point
                    FileWriter output = null;
                    try {
                    	output= new FileWriter("vdb-paper.txt", true);
                    	BufferedWriter writer=new BufferedWriter(output);
                    	for (int i = 0; i < 592; i++) {
                    		String ss = String.valueOf(dataPoint[i]);
                    		writer.append(ss);
                    	}
                    	writer.close();
                    } catch (Exception e) {
                    	logger.warn("File write error - Can't write");
                    } finally {
                    	if (output != null) {
                    		try {
                    			output.close();
                    		} catch (IOException e) {
                    			logger.warn("File write error - Can't flush and close");
                    		}
                    	}
                    }
                } catch (RulesViolation rv) {
                    logger.warn("got rules violation when processing move", rv);
                    strikes++;

                    //If we're not being permissive, end the game.
                    if (strikes <= RULE_STRIKES) {
                        logger.error("Maximum strikes reached, ending game");
                        break;
                    }
                }
            }
            // Append a delimiter line
            FileWriter output = null;
            try {
            	output= new FileWriter("vdb-paper.txt", true);
            	BufferedWriter writer=new BufferedWriter(output);
            	for (int i = 0; i < 592; i++) {
            		String ss = "-";
            		writer.append(ss);
            	}
            	writer.close();
            } catch (Exception e) {
            	logger.warn("File write error - Can't write");
            } finally {
            	if (output != null) {
            		try {
            			output.close();
            		} catch (IOException e) {
            			logger.warn("File write error - Can't flush and close");
            		}
            	}
            }
            return new GameStats(gameID, players.length, state.getScore(), state.getLives(), moves, state.getInfomation(), strikes);
        } catch (Exception ex) {
            logger.error("the game went bang", ex);
            return new GameStats(gameID, players.length, state.getScore(), state.getLives(), moves, state.getInfomation(), 1);
        }

    }

    /**
     * Tell the players about an action that has occurred
     *
     * @param actor the player who performed the action
     * @param action the action the player performed
     * @param events the events that resulted from that action
     */
    protected void notifyAction(int actor, Action action, Collection<GameEvent> events) {

        for (int i = 0; i < players.length; i++) {
            int currPlayer = i; // use of lambda expression must be effectively final

            // filter events to just those that are visible to the player
            List<GameEvent> visibleEvents = events.stream().filter(e -> e.isVisibleTo(currPlayer)).collect(Collectors.toList());
            players[i].resolveTurn(actor, action, visibleEvents);

            logger.debug("for {}, sent {} to {}", action, visibleEvents, currPlayer);
        }

    }

    //send messages as soon as they are available
/*    protected void send(GameEvent event) {
        logger.debug("game sent event: {}", event);
        for (int i = 0; i < players.length; i++) {
            if (event.isVisibleTo(i)) {
                players[i].sendMessage(event);
            }
        }
    }*/

    public static void main(String[] args) {
        Random random = new Random();
        List<GameStats> results = new ArrayList<>();
        for(int players = 2; players <= 5; players++){
            for(int gameNumber = 0; gameNumber < 10; gameNumber++){
                GameRunner runner = new GameRunner("IGGI2-" + gameNumber, players, true);

                int evalAgent = random.nextInt(players);

                for(int i = 0; i < players; i++){
                    if (evalAgent == i) {
                        runner.addPlayer(new AgentPlayer("eval", AgentUtils.buildAgent("pmctsND[iggi|iggi|iggi|iggi|iggi]")));
                    } else{
                        runner.addPlayer(new AgentPlayer("iggi", AgentUtils.buildAgent("iggi")));
                    }
                }

                GameStats stats = runner.playGame(random.nextLong());
                results.add(stats);
            }
        }

        System.out.println(results.stream().mapToInt(x -> x.score).summaryStatistics());
    }

}
