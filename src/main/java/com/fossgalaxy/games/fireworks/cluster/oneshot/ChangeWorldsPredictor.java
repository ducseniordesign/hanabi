package com.fossgalaxy.games.fireworks.cluster.oneshot;

import com.fossgalaxy.games.fireworks.App;
import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Experiment to see how the exploration constant changes MCTS performance with a fixed budget.
 *
 * Created by webpigeon on 01/08/17.
 */
public class ChangeWorldsPredictor {
    private static final int ITR_BUDGET = 10_000;

    public static void main(String[] args) {
        String[] agentsPaired = SetupUtils.getPairedNames();
        int numSeeds = SetupUtils.getSeedCount();

        printMatchups(agentsPaired, numSeeds);
    }

    static void printMatchups(String[] agentsPaired, int numSeeds) {

        //allow generation of known seeds (useful for comparisons between pure and mixed games)
        Random r;
        String metaSeed = System.getenv("FIREWORKS_META_SEED");
        if (metaSeed != null) {
            r = new Random(Long.parseLong(metaSeed));
        } else {
            r = new Random();
        }


        for (int seedID = 0; seedID < numSeeds; seedID++) {
            long seed = r.nextLong();

            for (int worlds=100; worlds<10_000; worlds += 500) {
                    for (String agentPaired : agentsPaired) {
                        String agentUnderTest = String.format(App.PREDICTOR_MCTSND+"Fixed[%d:%d:%s]", ITR_BUDGET, worlds, agentPaired);
                        System.out.println(String.format("%s %s %d", agentUnderTest, agentPaired, seed));
                    }
                }
            }
        }
}
