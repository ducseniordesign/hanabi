package com.fossgalaxy.games.fireworks.ai.rule.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

public class DeckUtils {
	
	public static Map<Integer, List<Card>> bindCard(int player, Hand hand, List<Card> deck) {
		
		Map<Integer, List<Card>> possible = new HashMap<>();
		
		for (int slot=0; slot<hand.getSize(); slot++) {
			final int slotF = slot;
			List<Card> possibleCards = deck.stream().filter((Card c) -> hand.isPossible(slotF, c)).collect(Collectors.toList());
			possible.put(slot, possibleCards);
		}
		
		return possible;
	}
	
	public static boolean isDiscardable(List<Card> cards, GameState state) {
		return cards.stream().allMatch((Card c) -> isDiscardable(c, state));
	}
	
	public static boolean isDiscardable(Card card, GameState state) {
		int tableValue = state.getTableValue(card.colour);
		if (tableValue >= card.value) {
			return true;
		}
		
		//TODO factor in duplicate cards and wrecked decks
		return false;
	}
	
	public static double getProbablity(List<Card> cards, Card target) {
		return getProbablity(cards, (Card c) -> target.equals(c));
	}

	public static double getProbablity(List<Card> cards, CardColour target) {
		return getProbablity(cards, (Card c) -> target.equals(c.colour));
	}
	
	public static double getProbablity(List<Card> cards, Integer target) {
		return getProbablity(cards, (Card c) -> target.equals(c.value));
	}
	
	public static double getProbablity(List<Card> cards, Predicate<Card> rule) {
		double matchingCards = cards.stream().filter(rule).count() * 1.0;
		return matchingCards/cards.size();
	}
	
}