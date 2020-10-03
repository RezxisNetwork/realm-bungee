package net.rezxis.mchosting.bungee.commands;

import java.util.ArrayList;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.rezxis.mchosting.bungee.KanaConverter;

public class GTellCommand extends Command implements TabExecutor {

	private static final String prefix = ChatColor.GRAY+"[GTELL]";
	
	public GTellCommand() {
		super("gtell");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length < 2) {
			sender.sendMessage(new TextComponent(ChatColor.RED+"Usage : /gtell <target> <message>"));
			return;
		}
		ProxiedPlayer player = BungeeCord.getInstance().getPlayer(args[0]);
		if (player != null && player.isConnected()) {
			String s = sender.getName();
			String message = "";
			for (int i = 1; i < args.length; i++) {
				if (i != 1)
					message += " ";
				message += args[i];
			}
			message = message.replace("&", "§");
			if (!message.startsWith("#"))
				message = KanaConverter.fixBrackets(KanaConverter.conv(message));
			if (message.startsWith("#"))
				message = message.replaceFirst("#", "");
			TextComponent comp = new TextComponent(prefix+"["+s+"->"+player.getName()+"] "+message);
			sender.sendMessage(comp);
			player.sendMessage(comp);
		} else
			sender.sendMessage(new TextComponent(prefix+ChatColor.RED+"指定されたプレイヤーはオフラインです。"));
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender arg0, String[] args) {
		ArrayList<String> tabs = new ArrayList<String>();
		for (ProxiedPlayer pp : BungeeCord.getInstance().getPlayers())
			if (pp.isConnected())
				tabs.add(pp.getName());
		return tabs;
	}
}
