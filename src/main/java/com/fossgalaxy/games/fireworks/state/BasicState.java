package com.fossgalaxy.games.fireworks.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BasicState implements GameState {
	private static final int MAX_INFOMATION = 8;
	private static final int MAX_LIVES = 3;

	private final int handSize;

	private final Hand[] hands;
	private final Deck deck;
	private final Map<CardColour, Integer> table;
	private final List<Card> discard;

	private int infomation;
	private int lives;
	private int movesLeft;

	public BasicState(BasicState state) {
		this.handSize = state.handSize;
		this.deck = state.deck;
		this.discard = new ArrayList<>(state.discard);
		this.infomation = state.infomation;
		this.lives = state.lives;
		this.movesLeft = state.movesLeft;

		this.table = null; // TODO find a way to copy this safely

		this.hands = new Hand[state.hands.length];
		for (int i = 0; i < hands.length; i++) {
			hands[i] = new Hand(state.hands[i]);
		}
	}

	public BasicState(int handSize, int playerCount) {
		this.handSize = handSize;
		this.hands = new Hand[playerCount];
		this.deck = new Deck();
		this.table = new EnumMap<>(CardColour.class);
		this.discard = new ArrayList<Card>();
		this.movesLeft = playerCount;

		this.infomation = MAX_INFOMATION;
		this.lives = MAX_LIVES;

		for (int i = 0; i < playerCount; i++) {
			hands[i] = new Hand(handSize);
		}
	}

	public void init(){
		init(null);
	}
	
	public void init(Long seed){
		deck.init();
		if (seed != null) {
			deck.shuffle(seed);
		} else {
			deck.shuffle();
		}
		dealHands();
	}
	
	private void dealHands() {
		for (int hand=0; hand<hands.length; hand++) {
			deal(hand);
		}
	}
	
	public GameState getCopy() {
		return new BasicState(this);
	}
	
	@Override
	public void addToDiscard(Card card) {
		discard.add(card);
	}

	@Override
	public Card drawFromDeck() {
		return deck.getTopCard();
	}

	@Override
	public Card getCardAt(int player, int slot) {
		assert player >= 0 : "playerID must be bigger than -1";
		assert player < hands.length : "player ID higher than number of players";

		return hands[player].getCard(slot);
	}

	@Override
	public Deck getDeck() {
		return deck;
	}

	@Override
	public Collection<Card> getDiscards() {
		return Collections.unmodifiableList(discard);
	}

	@Override
	public Hand getHand(int player) {
		assert player >= 0 : "playerID must be bigger than -1";
		assert player < hands.length : "player ID higher than number of players";

		return hands[player];
	}

	@Override
	public int getHandSize() {
		return handSize;
	}

	@Override
	public int getInfomation() {
		return infomation;
	}

	@Override
	public int getLives() {
		return lives;
	}

	@Override
	public int getPlayerCount() {
		return hands.length;
	}

	@Override
	public int getScore() {
		int total = 0;
		
		for (Integer val : table.values()) {
			total += val;
		}
		return total;
	}

	@Override
	public int getStartingInfomation() {
		return MAX_INFOMATION;
	}

	@Override
	public int getStartingLives() {
		return MAX_LIVES;
	}

	@Override
	public int getTableValue(CardColour colour) {
		Integer curr = table.get(colour);
		return curr == null ? 0 : curr;
	}

	@Override
	public boolean isGameOver() {
		if (lives <= 0) {
			return true;
		}

		if (!deck.hasCardsLeft() && movesLeft != 0) {
			return true;
		}
		
		return lives <= 0 || !deck.hasCardsLeft();
	}

	@Override
	public void setCardAt(int player, int slot, Card card) {
		Hand hand = hands[player];
		hand.setCard(slot, card);
	}

	@Override
	public void setInfomation(int newValue) {
		assert newValue < MAX_INFOMATION;
		assert newValue >= 0;
		infomation = newValue;
	}

	@Override
	public void setKnownValue(int player, int slot, Integer value, CardColour colour) {
		assert player >= 0 : "playerID must be bigger than -1";
		assert player < hands.length : "player ID higher than number of players";

		Hand hand = hands[player];
		hand.setCard(slot, new Card(value, colour));
	}

	@Override
	public void setLives(int newValue) {
		assert newValue < MAX_LIVES;
		assert newValue >= 0;
		lives = newValue;
	}

	@Override
	public void setTableValue(CardColour colour, int value) {
		table.put(colour, value);
	}

	@Override
	public void tick() {
		if (!deck.hasCardsLeft()) {
			movesLeft--;
		}
	}

	@Override
	public void deal(int playerID) {
		for (int slot=0; slot<handSize; slot++) {
			hands[playerID].setCard(slot, deck.getTopCard());
		}
	}

}
