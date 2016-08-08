package com.fossgalaxy.games.fireworks.ai.osawa;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.rule.DiscardIfUseless;
import com.fossgalaxy.games.fireworks.ai.rule.PlayIfCertian;
import com.fossgalaxy.games.fireworks.ai.rule.ProductionRuleAgent;
import com.fossgalaxy.games.fireworks.ai.rule.TellPlayableCardOuter;
import com.fossgalaxy.games.fireworks.ai.rule.TellUnknown;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellPlayableCard;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;

/**
 * Factory for describing agents defined in Osawa's paper.
 * 
 * the paper is entitled, "Solving Hanabi: Estimating Hands by Opponent's Actions in Cooperative Game with Incomplete Information"
 * and was published in 2015 in "Computer Poker and Imperfect Information: Papers from the 2015 AAAI Workshop"
 */
public class OsawaFactory {

	/**
	 * Internal-State Strategy as described by Osawa
	 * 
	 * @return a Rule based agent which implements this strategy
	 */
	public static Agent buildInternalState() {
		ProductionRuleAgent ruleAgent = new ProductionRuleAgent();
		
		//1. if we have a playable card, play it
		ruleAgent.addRule(new PlayIfCertian());
		
		//2. if we have a useless card, discard it
		ruleAgent.addRule(new DiscardIfUseless());
		
		//3. if there a useful card, tell the player about it
		ruleAgent.addRule(new TellPlayableCard());
		
		//4. if there is an information token, tell the other player about a random card
		ruleAgent.addRule(new TellRandomly());
		
		//5. randomly discard a card from my hand
		ruleAgent.addRule(new DiscardRandomly());
		
		return ruleAgent;
	}
	
	/**
	 * Outer-State Strategy as described by Osawa
	 * 
	 * @return a Rule based agent which implements this strategy
	 */
	public static Agent buildOuterState() {
		ProductionRuleAgent ruleAgent = new ProductionRuleAgent();
		
		//1. if we have a playable card, play it
		ruleAgent.addRule(new PlayIfCertian());
		
		//2. if we have a useless card, discard it
		ruleAgent.addRule(new DiscardIfUseless());
		
		//3. if there a useful card, tell the player about it
		ruleAgent.addRule(new TellPlayableCardOuter());
		
		//4. if there is an information token, tell the other player about an unknown attribute
		ruleAgent.addRule(new TellUnknown());
		
		//5. randomly discard a card from my hand
		ruleAgent.addRule(new DiscardRandomly());
		
		return ruleAgent;
	}
	
	
	/**
	 * Self-recognition Strategy as described by Osawa
	 * 
	 * @return a Rule based agent which implements this strategy
	 */
	public static Agent buildSelfRecog() {
		ProductionRuleAgent ruleAgent = new ProductionRuleAgent();
		
		//1. if we have a playable card, play it
		ruleAgent.addRule(new PlayIfCertian());
		
		//2. if we have a useless card, discard it
		ruleAgent.addRule(new DiscardIfUseless());
		
		//3. if there a useful card, tell the player about it
		ruleAgent.addRule(new TellPlayableCardOuter());
		
		//4. if there is an information token, tell the other player about an unknown attribute
		ruleAgent.addRule(new TellUnknown());
		
		//5. randomly discard a card from my hand
		ruleAgent.addRule(new DiscardRandomly());
		
		return ruleAgent;
	}
	
}
