package net.rezxis.mchosting.bungee.commands;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class HubCommand extends Command {

	public HubCommand() {
		super("hub");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender arg0, String[] arg1) {
		ProxiedPlayer player = (ProxiedPlayer) arg0;
		player.sendMessage(ChatColor.AQUA+"接続中");
		player.connect(BungeeCord.getInstance().getConfig().getServers().get("lobby"));
	}
}
