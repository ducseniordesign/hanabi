package com.fossgalaxy.games.fireworks.cluster.oneshot;

import com.fossgalaxy.games.fireworks.utils.SetupUtils;

import java.util.Random;

/**
 * Experiment to see how the number of worlds changes how MCTS behaves
 *
 * Created by webpigeon on 01/08/17.
 */
public class ChangeExpConst {
    private static final double[] EXP_VALUES = {
            0.00,
            0.25,
            0.50,
            0.75,
            1.00,
            1.25,
            Math.sqrt(2), //Default value (comparison)
            1.50,
            1.75,
            2.00,
            2.25,
            2.50,
            2.75,
            3.00,
            3.25,
            3.50,
            4.00
    };

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

            for (int i=0; i<EXP_VALUES.length; i++) {
                    String agentUnderTest = String.format("mctsExpConstND[%f]", EXP_VALUES[i]);

                    for (String agentPaired : agentsPaired) {
                        System.out.println(String.format("%s %s %d", agentUnderTest, agentPaired, seed));
                    }
                }
            }
        }
}
