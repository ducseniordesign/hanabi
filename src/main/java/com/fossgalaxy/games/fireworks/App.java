package com.fossgalaxy.games.fireworks;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.IGGIFactory;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTS;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTSPredictor;
import com.fossgalaxy.games.fireworks.ai.ganabi.Ganabi;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.GameUtils;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import com.fossgalaxy.stats.BasicStats;
import com.fossgalaxy.stats.StatsSummary;
import com.fossgalaxy.games.fireworks.vectorization.Vectorization;


import java.util.Random;
import java.io.*;


/**
 * Hello world!
 */
public class App {

    public static final String PREDICTOR_MCTS = "pmcts";
    public static final String IGGI_RISKY = "iggi_risky";
    public static final String MCTS = "mcts";
    public static final String PREDICTOR_MCTSND = "pmctsND";
    public static final String GANABI = "ganabi";

    //utility class - don't create instances of it
    private App() {

    }

    /**
     * Plays a series of games with a single agent mixed with another agent
     *
     * @param args Ignored
     */
    public static void main(String[] args) throws IOException {

        double sum = 0;
        int games = 0;
        System.out.println("Start");

        Random r = new Random();

        String[] agents = new String[] {
            "iggi", "piers", "flawed", "outer", "vdb-paper", "legal_random",
        };

        long[] seeds = new long[100];


        //values from the IS-MCTS paper
        double[] p = new double[] {
                0,
                0.25,
                0.5,
                0.75,
                1,
                1.25,
                1.5,
                1.75,
                2
        };

        /*double[] p = new double[]{
            0.0,
            0.1,
            0.2,
            0.3,
            0.4,
            0.5,
            0.6,
            0.7,
            0.8,
            0.9,
            1.0,
            2.0,
            3.0,
            4.0,
            5.0,
            6.0,
            7.0
        };*/

        StatsSummary[][] ss = new StatsSummary[agents.length][p.length];
        for (int j=0; j<agents.length; j++) {
            for (int i = 0; i < ss.length; i++) {
                ss[j][i] = new BasicStats();
            }
        }
        
        // For the purpose of getting training data, game statistics is not needed for now, 
        // so we will be just running the game here and logging the data into a file
//        for (int i = 0; i < 1; i++) {
//        	long seed = r.nextLong();
//        	playMixed("vdb-paper", "vdb-paper", seed);
//        	System.out.print("Done with game number ");
//        	System.out.println(i);
//        }
       
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
        
        
        
        

//        for (int i=0; i<seeds.length; i++) {
//            seeds[i] = r.nextLong();
//        }
//
//        for (int agent=0; agent<agents.length; agent++) {
//            for (int i = 0; i < p.length; i++) {
//
//                for (int run = 0; run < seeds.length; run++) {
//                    GameStats stats = playMixed("mctsExpConstND["+p[i]+"]", agents[agent], seeds[i]);
//                	//GameStats stats = playMixed("legal_random", agents[agent], seeds[i]);
//                    sum += stats.score;
//                    games++;
//                    ss[agent][i].add(stats.score);
//                    System.out.println(String.format("line,%f,%s,%d,%d,%d,%d,%d", p[i], agents[agent], seeds[i], stats.score, stats.lives, stats.moves, stats.disqal));
//                }
//
//                if (games == 0) {
//                    return;
//                }
//
//                System.out.println("exp: " + p[i] + "avg: " + sum / games);
//                System.out.println("exp: " + p[i] + " stats: " + ss[i]);
//
//                StatsSummary ssi = ss[agent][i];
//                System.out.println(String.format("summary,%f,%s,%f,%f,%f,%f", p[i], agents[agent], ssi.getMin(), ssi.getMax(), ssi.getMean(), ssi.getRange()));
//            }
//        }
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

    /**
     * Plays a game with the given agent
     *
     * @param agent The given agent to play the game
     * @return GameStats for the game.
     */
    public static GameStats playGame(String agent) {
        String[] names = new String[5];
        Agent[] players = new Agent[5];

        for (int i = 0; i < 5; i++) {
            names[i] = agent;
            players[i] = AgentUtils.buildAgent(names[i]);
        }

        GameStats stats = GameUtils.runGame("", null, SetupUtils.toPlayers(names, players));
        System.out.println("the agents scored: " + stats);
        return stats;
    }

    /**
     * Plays a mixed game with the agent under test and all other agents as the agent
     *
     * @param agentUnderTest The agent to be player 0
     * @param agent          The agent to make all the others
     * @param seed the seed for the deck ordering
     * @return GameStats for the game
     */
    public static GameStats playMixed(String agentUnderTest, String agent, long seed) {
        Random r = new Random(seed);
        int whereToPlace = r.nextInt(2);

        String[] names = new String[2];
        for (int i = 0; i < names.length; i++) {
            names[i] = whereToPlace == i ? agentUnderTest : agent;
        }

        Agent[] players = new Agent[2];
        for (int i = 0; i < names.length; i++) {
            players[i] = buildAgent(names[i], i, agent, names.length);
        }

        GameStats stats = GameUtils.runGame("", seed, SetupUtils.toPlayers(names, players));
        System.out.println("the agents scored: " + stats);
        return stats;
    }

    /**
     * Build an agent
     *
     * @param name    The name the agent will believe it has
     * @param agentID The AgentID it will have
     * @param paired  Who it is paired with
     * @param size    The size of the game
     * @return The agent created
     */
    public static Agent buildAgent(String name, int agentID, String paired, int size) {
        switch (name) {
            case PREDICTOR_MCTS:
            case PREDICTOR_MCTSND:
                Agent[] agents = AgentUtils.buildPredictors(agentID, size, paired);
                if (name.contains("ND")) {
                    return new MCTSPredictor(agents, 50_000, 100, 100);
                }
                return new MCTSPredictor(agents);
            case GANABI:
            	return new Ganabi();
            default:
                return AgentUtils.buildAgent(name);
        }
    }

    /**
     * Build an agent
     *
     * @param name    The name the agent will believe it has
     * @param agentID The AgentID it will have
     * @param paired  Who it is paired with
     * @return The agent created
     */
    public static Agent buildAgent(String name, int agentID, String[] paired) {
        switch (name) {
            case PREDICTOR_MCTS:
            case PREDICTOR_MCTSND:
                Agent[] agents = AgentUtils.buildPredictors(agentID, paired);
                if (name.contains("ND")) {
                    return new MCTSPredictor(agents, 50_000, 100, 100);
                }
                return new MCTSPredictor(agents);
            case GANABI:
            	return new Ganabi();
            default:
                return AgentUtils.buildAgent(name);
        }
    }

    public static Agent buildAgent(String name, int agentID, int[] model){
        switch (name){
            case PREDICTOR_MCTS:
            case PREDICTOR_MCTSND:
                Agent[] agents = new Agent[5];
                for(int i = 0; i < agents.length; i++){
                    if(i != agentID) {
                        agents[i] = AgentUtils.buildAgent(model);
                    }
                }
                if(name.contains("ND")){
                    return new MCTSPredictor(agents, 50_000, 100, 100);
                }
                return new MCTSPredictor(agents);
            case GANABI:
            	return new Ganabi();
            default:
                return AgentUtils.buildAgent(name);
        }
    }


    /**
     * Allows for creating MCTS specifically with some fields
     *
     * @param name         The name of the agent
     * @param roundLength  The round length to use for MCTS
     * @param rolloutDepth The rollout depth to use for MCTS
     * @param treeDepth    The tree depth to use for MCTS
     * @return The Agent
     */
    public static Agent buildAgent(String name, int roundLength, int rolloutDepth, int treeDepth) {
        return MCTS.equals(name) ? new MCTS(roundLength, rolloutDepth, treeDepth) : AgentUtils.buildAgent(name);
    }

    /**
     * Allows for creating Predictor MCTS with some fields
     *
     * @param name         The name for the agent
     * @param agentID      The agent id
     * @param paired       Who the agent is paired with
     * @param size         The size of the game
     * @param roundLength  The round length to use for MCTS
     * @param rolloutDepth The rollout depth to use for MCTS
     * @param treeDepth    The tree depth to use for MCTS
     * @return The agent
     */
    public static Agent buildAgent(String name, int agentID, String paired, int size, int roundLength, int rolloutDepth, int treeDepth) {
        if (!PREDICTOR_MCTS.equals(name)) {
            return AgentUtils.buildAgent(name);
        }
        Agent[] agents = new Agent[size];
        for (int i = 0; i < size; i++) {
            if (i == agentID) {
                agents[i] = null;
            }
            //TODO is this ever paired with MCTS? if not this should be AgentUtils.buildAgent(agentID, size, paired)
            agents[i] = buildAgent(paired, roundLength, rolloutDepth, treeDepth);
        }
        return new MCTSPredictor(agents);
    }

    /**
     * Builds a risky agent with a given threshold
     *
     * @param name      The name for the agent
     * @param threshold The threshold to give to the agent
     * @return The agent
     */
    public static Agent buildAgent(String name, double threshold) {
        return IGGI_RISKY.equals(name) ? IGGIFactory.buildRiskyPlayer(threshold) : AgentUtils.buildAgent(name);
    }


}
