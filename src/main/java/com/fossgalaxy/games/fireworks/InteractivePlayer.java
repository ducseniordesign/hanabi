package com.fossgalaxy.games.fireworks;

import java.util.Arrays;
import java.util.Scanner;

import com.fossgalaxy.games.fireworks.engine.Card;
import com.fossgalaxy.games.fireworks.engine.CardColour;
import com.fossgalaxy.games.fireworks.engine.Hand;
import com.fossgalaxy.games.fireworks.state.GameState;

public class InteractivePlayer implements Player {
	private GameState state;	
	private Scanner in;
	
	public InteractivePlayer(Scanner in) {
		this.in = in;
		this.state = new GameState(3, 5);
	}
	
	@Override
	public void sendMessage(String msg) {
		String[] parts = msg.split(" ");
		int who = Integer.parseInt(parts[0]);
		String action = parts[1];
		
		switch(action) {
			case "DRAW":
			case "draw": {
				int slot = Integer.parseInt(parts[2]);
				CardColour colour = CardColour.valueOf(parts[3]);
				int value = Integer.parseInt(parts[4]);
				
				state.setInfomation(who, slot, value, colour);
				break;
			}
			case "PLAY":
			case "play": {
				int slot = Integer.parseInt(parts[2]);
				CardColour colour = CardColour.valueOf(parts[3]);
				int value = Integer.parseInt(parts[4]);
				
				System.out.println(String.format("%d plays slot %d (%d, %s)", who, slot, value, colour));
				state.play(who, slot, new Card(value, colour));
				break;
			}
			case "DISCARD":
			case "discard": {
				int slot = Integer.parseInt(parts[2]);
				CardColour colour = CardColour.valueOf(parts[3]);
				int value = Integer.parseInt(parts[4]);
				
				System.out.println(String.format("%d discards slot %d (%d, %s)", who, slot, value, colour));
				state.discard(who, slot, new Card(value, colour));
				break;
			}
			case "TELL_COLOUR":
			case "tell_colour": {
				int playerID = Integer.parseInt(parts[2]);
				CardColour colour = CardColour.valueOf(parts[3]);
				
				String[] slotStrs = parts[4].split(",");
				int[] slots = new int[slotStrs.length];
				for (int i=0; i<slotStrs.length; i++) {
					slots[i] = Integer.parseInt(slotStrs[i]);
				}
				
				state.tell(playerID, colour, slots);		
				System.out.println(String.format("%d points out %d's %s cards", who, playerID, colour));
				break;
			}
			case "TELL_VALUE":
			case "tell_value": {
				int playerID = Integer.parseInt(parts[2]);
				int value = Integer.parseInt(parts[3]);
				
				String[] slotStrs = parts[4].split(",");
				int[] slots = new int[slotStrs.length];
				for (int i=0; i<slotStrs.length; i++) {
					slots[i] = Integer.parseInt(slotStrs[i]);
				}
				
				state.tell(playerID, value, slots);		
				System.out.println(String.format("%d points out %d's %d cards", who, playerID, value));
				break;
			}
			default: {
				System.err.println("unknown action "+action+" for "+who+" parts: "+Arrays.toString(parts));
			}
				
		}
		
	}

	@Override
	public String getAction() {
		
		System.out.println("Your move");
		for (int player=0; player<state.getPlayerCount(); player++) {
			Hand hand = state.getHand(player);
			System.out.println("  Player "+player+": "+hand);
		}
		System.out.println("  You have "+state.getLives()+" lives");
		System.out.println("  You have "+state.getInfomation()+" infomation token(s)");
		System.out.println("  The table is: "+state.getTable());
		System.out.println();
		System.out.println("  What would you like to do? ");
		return in.nextLine();
	}

}