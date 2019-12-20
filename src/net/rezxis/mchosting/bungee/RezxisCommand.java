package net.rezxis.mchosting.bungee;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.DBPlayer;

public class RezxisCommand extends Command {

	public RezxisCommand() {
		super("rezxis", "rezxis.admin");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args[0].equalsIgnoreCase("ban")) {
			//Usage : /rezxis ban <target> <reason>
			if (args.length != 3) {
				sender.sendMessage(ChatColor.RED+"Usage /rezxis ban <target> <reason>");
			} else {
				ProxiedPlayer player = BungeeCord.getInstance().getPlayer(args[1]);
				DBPlayer dp = Bungee.instance.pTable.get(player.getUniqueId());
				dp.setBan(true);
				dp.setReason(args[2]);
				dp.update();
			}
		}
	}
}
