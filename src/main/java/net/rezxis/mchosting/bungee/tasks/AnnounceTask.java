package net.rezxis.mchosting.bungee.tasks;

import java.util.Random;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.rezxis.mchosting.bungee.Bungee;

public class AnnounceTask implements Runnable {

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		String msg = Bungee.instance.messages.get(new Random().nextInt(Bungee.instance.messages.size()-1));
		for (ProxiedPlayer player : BungeeCord.getInstance().getPlayers()) {
			player.sendMessage(msg);
		}
	}
}
