package net.rezxis.mchosting.bungee.commands;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;

public class PayCommand extends Command {

	private HashMap<UUID,Long> coins = new HashMap<>();
	private HashMap<UUID,UUID> dests = new HashMap<>();
	
	public PayCommand() {
		super("pay");
		// TODO Auto-generated constructor stub
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length == 2)  {
			DBPlayer player = Tables.getPTable().get(BungeeCord.getInstance().getPlayer(sender.getName()).getUniqueId());
			DBPlayer target = Tables.getPTable().get(BungeeCord.getInstance().getPlayer(args[0]).getUniqueId());
			if (target == null) {
				sender.sendMessage(ChatColor.RED+"指定されたプレイヤーは存在しません");
				return;
			}
			int c = 0;
			try {
				c = Integer.valueOf(args[1]);
			} catch (Exception ex) {
				sender.sendMessage(ChatColor.RED+"正しい値を入れていください。");
				return;
			}
			if (c <= 0) {
				sender.sendMessage(ChatColor.RED+"正しい値を入れていください。");
				return;
			}
			if (player.getCoin() < c) {
				sender.sendMessage(ChatColor.RED+"コインが足りません。");
				return;
			}
			if (args[0].equalsIgnoreCase(sender.getName())) {
				sender.sendMessage(ChatColor.RED+"自分には送金できません。");
				return;
			}
			coins.put(player.getUUID(), Long.valueOf(c));
			dests.put(player.getUUID(), target.getUUID());
			sender.sendMessage(new TextComponent(ChatColor.GREEN+"/pay confirm で承認してください。"));
		}  else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("confirm")) {
				UUID uuid = BungeeCord.getInstance().getPlayer(sender.getName()).getUniqueId();
				DBPlayer player = Tables.getPTable().get(uuid);
				UUID a = null;
				for (Entry<UUID,UUID> e : dests.entrySet()) {
					if (e.getKey().toString().equalsIgnoreCase(uuid.toString())) {
						a = e.getValue();
					}
				}
				if (a == null) {
					sender.sendMessage(ChatColor.RED+"使い方 : /pay 送金先 金額");
					return;
				}
				DBPlayer target = Tables.getPTable().get(a);
				long c = 0;//coins.get(player.getUUID());
				for (Entry<UUID, Long> e : coins.entrySet()) {
					if (e.getKey().toString().equalsIgnoreCase(uuid.toString())) {
						c = e.getValue();
					}
				}
				player.setCoin(player.getCoin()-c);
				target.setCoin(target.getCoin()+c);
				player.update();
				target.update();
				sender.sendMessage(ChatColor.GREEN+(c+"RealmCoinを"+args[0]+"に送金しました。"));
				BungeeCord.getInstance().getPlayer(target.getUUID()).sendMessage(ChatColor.GREEN+""+sender.getName()+"から"+c+"RealmCoinを受け取りました。");
				coins.remove(player.getUUID());
				dests.remove(player.getUUID());
			} else {
				sender.sendMessage(ChatColor.RED+"使い方 : /pay 送金先 金額");
			}
		} else {
			sender.sendMessage(ChatColor.RED+"使い方 : /pay 送金先 金額");
		}
	}
}
