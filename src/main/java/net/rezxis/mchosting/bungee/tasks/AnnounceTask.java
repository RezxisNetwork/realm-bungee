package net.rezxis.mchosting.bungee.tasks;


import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.rezxis.mchosting.bungee.Bungee;

public class AnnounceTask implements Runnable {

	private static int i = 0;
	
	@Override
	public void run() {
		if (i == Bungee.instance.messages.size())
			i = 0;
		TextComponent msg = Bungee.instance.messages.get(i);
		for (ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
			player.sendMessage(msg);
		i++;
	}
}
