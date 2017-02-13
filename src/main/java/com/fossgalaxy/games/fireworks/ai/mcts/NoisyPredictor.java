package com.fossgalaxy.games.fireworks.ai.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Created by webpigeon on 27/01/17.
 */
public class NoisyPredictor implements Agent {
    private double threshold;
    private Agent policy;
    private Random random;

    public NoisyPredictor(double threshold, Agent policy) {
        this.threshold = threshold;
        this.policy = policy;
        this.random = new Random();
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        if (random.nextDouble() > threshold) {
            return policy.doMove(agentID, state);
        } else {
            Collection<Action> actions = Utils.generateActions(agentID, state);
            List<Action> listAction = new ArrayList<>(actions);
            return listAction.get(random.nextInt(listAction.size()));
        }
    }

    @Override
    public void receiveID(int agentID) {
        policy.receiveID(agentID);
    }
}
