package com.fossgalaxy.games.fireworks.utils;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.RandomAgent;
import com.fossgalaxy.games.fireworks.ai.ga.RMHC;
import com.fossgalaxy.games.fireworks.ai.hat.HatGuessing;
import com.fossgalaxy.games.fireworks.ai.iggi.IGGIFactory;
import com.fossgalaxy.games.fireworks.ai.mcs.MonteCarloSearch;
import com.fossgalaxy.games.fireworks.ai.mcts.MCTS;
import com.fossgalaxy.games.fireworks.ai.mcts.NoisyPredictor;
import com.fossgalaxy.games.fireworks.ai.osawa.OsawaFactory;
import com.fossgalaxy.games.fireworks.ai.rule.ProductionRuleAgent;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.ai.rule.RuleSet;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.ai.vanDenBergh.VanDenBerghFactory;
import com.fossgalaxy.games.fireworks.annotations.Beta;
import com.fossgalaxy.games.fireworks.utils.agentbuilder.AgentFactory;
import com.fossgalaxy.games.fireworks.utils.agentbuilder.AgentFinder;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 01/12/16.
 */
public class AgentUtils {
    public static final String PARAM_SEPERATOR = ":";

    private static final AgentFinder finder = buildFinder();

    private AgentUtils() {

    }


    private static AgentFinder buildFinder() {
        AgentFinder finder = new AgentFinder();

        //add all rule based agent factory methods - it doesn't make sense to make classes for each of these...
        //for (Map.Entry<String, Supplier<Agent>> ruleBased : buildMap().entrySet()) {
        //    finder.addFactory(ruleBased.getKey(), i(ruleBased.getValue()));
        //}

        return finder;
    }

    public static void main(String[] args) {
        Agent agent2 = finder.buildAgent("pmcts", "noisy,0.9,iggi|noisy,0.9,legal_random|noisy,0.9,legal_random|noisy,0.9,legal_random|noisy,0.9,legal_random");
        System.out.println(agent2);
    }

    private static AgentFactory i(Supplier<Agent> s){
        return (x -> s.get());
    }

    private static Map<String, Supplier<Agent>> buildMap() {
        Map<String, Supplier<Agent>> map = new HashMap<>();
        map.put("pure_random", RandomAgent::new);
        map.put("random", OsawaFactory::buildRandom);
        map.put("internal", OsawaFactory::buildInternalState);
        map.put("outer", OsawaFactory::buildOuterState);
        map.put("cautious", IGGIFactory::buildCautious);
        map.put("iggi", IGGIFactory::buildIGGIPlayer);
        map.put("iggi_risky", IGGIFactory::buildRiskyPlayer);
        map.put("legal_random", IGGIFactory::buildRandom);
        map.put("mcts", MCTS::new);
        map.put("cautiousMCTS", IGGIFactory::buildCautiousMCTS);
        map.put("hat", HatGuessing::new);
        map.put("piers", IGGIFactory::buildPiersPlayer);
        map.put("flatmc-legal_random", () -> new MonteCarloSearch(buildAgent("legal_random")));
        map.put("flatmc-inner", () -> new MonteCarloSearch(buildAgent("inner")));
        map.put("flatmc-iggi", () -> new MonteCarloSearch(buildAgent("iggi")));
        map.put("flatmc-flawed", () -> new MonteCarloSearch(buildAgent("flawed")));
        map.put("vandenbergh", VanDenBerghFactory::buildAgent);
        map.put("flawed", IGGIFactory::buildFlawedPlayer);
        map.put("rmhc", RMHC::new);


        //Non-depth limited mcts versions
        map.put("mctsND", () -> new MCTS(MCTS.DEFAULT_ITERATIONS, MCTS.NO_LIMIT, MCTS.NO_LIMIT));

        return map;
    }

    public static Agent buildAgent(String name, String... args){
        return finder.buildAgent(name, args);
    }


    public static Agent buildAgent(String name) {
        return finder.buildAgent(name);
    }

    /**
     * Allow creation of other forms of predictors
     *
     * This allows the creation of noisey/learned models to be injected into the agent.
     *
     * @param args the name to generate the predictor from
     * @return the new predictor
     */
    public static Agent buildPredictor(String ... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("You must supply a model to use");
        }

        String name = args[1];

        if (name.startsWith("noisy")) {
            String[] parts = name.split(":");
            double th = Double.parseDouble(parts[1]);
            return new NoisyPredictor(th, buildAgent(parts[2]));
        }

        if (name.startsWith("model")) {
            String[] parts = name.split(":");
            Integer[] rules = Arrays.stream(parts[1].split(",")).map(Integer::parseInt).collect(Collectors.toList()).toArray(new Integer[0]);
            return buildAgent(rules);
        }

        return buildAgent(name);
    }

    public static Agent[] buildPredictors(int myID, int size, String paired) {
        Agent[] agents = new Agent[size];
        for (int i = 0; i < size; i++) {
            if (i == myID) {
                agents[i] = null;
            } else {
                agents[i] = buildPredictor(paired);
            }
        }

        return agents;
    }

    public static Agent[] buildPredictors(int myID, String... paired) {
        Agent[] agents = new Agent[paired.length];
        for (int i = 0; i < paired.length; i++) {
            if (i == myID) {
                agents[i] = null;
            } else {
                agents[i] = buildAgent(paired[i]);
            }
        }

        return agents;
    }

    public static Agent buildAgent(Integer[] rules){
        ProductionRuleAgent pra = new ProductionRuleAgent();
        ArrayList<Rule> actualRules = RuleSet.getRules();
        for(int rule : rules){
            if(rule == -1) break;
            pra.addRule(actualRules.get(rule));
        }

        actualRules.add(new TellRandomly());
        actualRules.add(new DiscardRandomly());

        return pra;
    }

    public static Agent buildAgent(int[] rules){
        ProductionRuleAgent pra = new ProductionRuleAgent();
        ArrayList<Rule> actualRules = RuleSet.getRules();
        for(int rule : rules){
            if(rule == -1) break;
            pra.addRule(actualRules.get(rule));
        }

        actualRules.add(new TellRandomly());
        actualRules.add(new DiscardRandomly());

        return pra;
    }

}
