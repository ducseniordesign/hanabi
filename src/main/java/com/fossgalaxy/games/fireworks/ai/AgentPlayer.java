package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.BasicState;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

public class AgentPlayer implements Player {
	private GameState state;
	private Agent agent;
	private int playerID;

	public AgentPlayer(int playerID, Agent agent) {
		this.playerID = playerID;
		this.agent = agent;
		this.state = new BasicState(4, 4);
	}

	@Override
	public Action getAction() {
		return agent.doMove(playerID, state);
	}

	@Override
	public void sendMessage(GameEvent msg) {
		msg.apply(state);
	}

}
