package com.fossgalaxy.games.fireworks.ai.ganabi;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.*;
import java.lang.*;
import java.io.*;

public class Ganabi implements Agent {

	ProcessBuilder pb;

	public Ganabi() {
		this.pb = new ProcessBuilder();
		pb.command("python", "GanAgent.py");
		pb.directory(new File("./gan_research/"));
	}

	/**
	 * Standardised interface for game playing agents.
	 * <p>
	 * The agent gets a copy of the game state and it's agent ID and should return a
	 * move.
	 *
	 * @param agentID the ID of this agent
	 * @param state   the current state of the game
	 * @return the move this agent would like to make
	 */
	public Action doMove(int agentID, GameState currentState) {
		return new DiscardCard(0);
	}

	public Action doPythonMove(int agentID, int[] input) throws IOException {
		StringBuilder builder = new StringBuilder();
		for (int bit : input) {
				builder.append(bit == 0 ? '0' : '1');
		}
		String arg = builder.toString();
		Process p = this.pb.start();
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		bw.write(arg);
		bw.flush();
		bw.close();
		String action = br.readLine();
		p.destroy();
		int hot = 0;
		for (int i = 0 ; i < action.length() ; i++) {
			if (action.charAt(i) == '1') {
				hot = i;
				break;
			}
		}
		// Determine which action to do
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
			return new TellColour((agentID + 1) % 2, whichColor);
		} else {
			return new TellValue((agentID + 1) % 2, hot - 14);
		}

	}

}
