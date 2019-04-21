package com.fossgalaxy.games.fireworks.ai.ganabi;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.*;
import java.lang.*;
import java.io.*;

public class Ganabi implements Agent {

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
		String arg = Arrays.toString(input);
		ProcessBuilder pb = new ProcessBuilder("Python", "GanAgent.py");
		pb.directory(new File("/Users/ducpham/Desktop/"));
		Process p = pb.start();
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
		bw.write(arg);
		bw.flush();
		bw.close();
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String s = null;
		int counter = 0, maxLoc = 0;
		float maxVal = -1;
		while ((s = stdInput.readLine()) != null && counter < 20) {
			//System.out.println(s);
			float val = Float.valueOf(s.trim()).floatValue();
			if (val > maxVal) {
				maxVal = val;
				maxLoc = counter;
			}
			counter++;
		}
		p.destroy();
		
		// Determine which action to do
		if (maxLoc < 5) {
			// Playing a card
			return new PlayCard(maxLoc);
		} else if (maxLoc < 10) {
			// Discard a card
			return new DiscardCard(maxLoc - 5);
		} else if (maxLoc < 15) {
			CardColour whichColor = CardColour.RED;
			if (maxLoc == 11)
				whichColor = CardColour.ORANGE;
			if (maxLoc == 12)
				whichColor = CardColour.GREEN;
			if (maxLoc == 13)
				whichColor = CardColour.WHITE;
			if (maxLoc == 14)
				whichColor = CardColour.BLUE;
			return new TellColour((agentID + 1) % 2, whichColor);
		} else {
			return new TellValue((agentID + 1) % 2, maxLoc - 14);
		}

	}

}
