package net.rezxis.mchosting.bungee.commands;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PingCommand extends Command {

	public PingCommand() {
		super("ping");
	}

	@Override
	public void execute(CommandSender arg0, String[] arg1) {
		ProxiedPlayer player = (ProxiedPlayer)arg0;
		if (arg1.length == 1) {
			ProxiedPlayer target = BungeeCord.getInstance().getPlayer(arg1[0]);
			if (target == null) {
				player.sendMessage(ChatColor.RED+arg1[0]+" isn't online.");
				return;
			}
			if (!target.isConnected()) {
				player.sendMessage(ChatColor.RED+arg1[0]+" isn't online.");
			} else {
				player.sendMessage(ChatColor.GREEN+target.getName()+" ping : "+target.getPing());
			}
		} else {
			player.sendMessage(ChatColor.GREEN+"ping : "+player.getPing());
		}
	}

}
