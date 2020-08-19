package net.rezxis.mchosting.bungee.commands;

import java.util.ArrayList;
import java.util.Iterator;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.rezxis.mchosting.database.object.ServerWrapper;

public class ServerCommand extends Command implements TabExecutor {

	public ServerCommand() {
		super("server");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		boolean all = false;
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("all")) {
				all = true;
			} else {
				//connect
				ArrayList<ServerWrapper> list = ServerWrapper.getServers(all, "players", false);
				for (ServerWrapper w : list) {
					if (ChatColor.stripColor(w.getDisplayName()).equalsIgnoreCase(args[0])) {
						((ProxiedPlayer)sender).connect(BungeeCord.getInstance().getServerInfo(w.getDisplayName()));
					}
				}
				return;
			}
		}
		ArrayList<ServerWrapper> list = ServerWrapper.getServers(all, "players", false);
		ArrayList<TextComponent> texts = new ArrayList<>();
		if (list.size() == 0) {
			TextComponent zero = new TextComponent("オンラインのサーバーがありません。");
			zero.setColor(ChatColor.RED);
			zero.setBold(true);
			texts.add(zero);
		} else {
			TextComponent text = new TextComponent("Servers("+String.valueOf(list.size())+") : ");
			text.setColor(ChatColor.GREEN);
			texts.add(text);
			Iterator<ServerWrapper> ite = list.iterator();
			ServerWrapper s = ite.next();
			TextComponent connect = new TextComponent("クリックで接続");
			connect.setColor(ChatColor.AQUA);
			connect.setBold(true);
			TextComponent z = new TextComponent(s.getDisplayName()+ChatColor.GREEN+" ("+String.valueOf(s.getPlayers())+")");
			z.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/server "+ChatColor.stripColor(s.getDisplayName())));
			z.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {connect}));
			texts.add(z);
			while (ite.hasNext()) {
				ServerWrapper ts = ite.next();
				TextComponent tx = new TextComponent(", "+ts.getDisplayName()+ChatColor.RESET+""+ChatColor.GREEN+" ("+String.valueOf(ts.getPlayers())+")");
				tx.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/server "+ChatColor.stripColor(ts.getDisplayName())));
				tx.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {connect}));
				texts.add(tx);
			}
		}
		TextComponent[] tcs = new TextComponent[texts.size()];
		int i = 0;
		for (TextComponent tc : texts) {
			tcs[i] = tc;
			i++;
		}
		sender.sendMessage(tcs);
	}
	
	@Override
	public Iterable<String> onTabComplete(CommandSender arg0, String[] args) {
		ArrayList<String> tab = new ArrayList<>();
		ArrayList<ServerWrapper> list = ServerWrapper.getServers(true, "players", false);
		for (ServerWrapper s : list) {
			if (args.length == 0) {
				tab.add(ChatColor.stripColor(s.getDisplayName()));
			} else {
				if (ChatColor.stripColor(s.getDisplayName()).startsWith(args[0])) {
					tab.add(ChatColor.stripColor(s.getDisplayName()));
				}
			}
		}
		return tab;
	}
}
