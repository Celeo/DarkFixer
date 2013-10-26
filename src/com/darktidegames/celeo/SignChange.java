package com.darktidegames.celeo;

import org.bukkit.entity.Player;

public class SignChange
{

	private final Player player;
	private final int line;
	private final String message;

	public SignChange(Player player, int line, String message)
	{
		this.player = player;
		this.line = line;
		this.message = message;
	}

	public Player getPlayer()
	{
		return player;
	}

	public int getLine()
	{
		return line;
	}

	public String getMessage()
	{
		return message;
	}

}