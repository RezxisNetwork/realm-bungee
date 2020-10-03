package net.rezxis.mchosting.bungee.tasks;

import java.util.Random;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.crates.CrateTypes;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.player.DBVote;

public class RewardTask implements Runnable {

	@SuppressWarnings("deprecation")
	public void run() {
		for (ProxiedPlayer player : BungeeCord.getInstance().getPlayers()) {
			if (player.getServer().getInfo().getName().equalsIgnoreCase("lobby"))
				continue;
			int coin = 50;
			int box = 1;
			DBPlayer dp = Tables.getPTable().get(player.getUniqueId());
			DBVote dv = Tables.getVTable().getVoteByUUID(player.getUniqueId());
			if (dv != null)
				if (dv.hasRank()) {
					coin *=2;
					box *=2;
				}
			player.sendMessage(ChatColor.AQUA+"おつかれさまでした! "+ChatColor.LIGHT_PURPLE+coin+"枚"+ChatColor.AQUA+"のコインが手に入った");
			player.sendMessage(ChatColor.GREEN+String.valueOf(box)+"報酬箱を手に入れました！ロビーでチェストをクリックして、開けます！");
			dp.addCoin(coin);
			dp.setVault(dp.getVault()+box);
			dp.update();
			if (new Random().nextInt(100)<= 80)
				Tables.getCTable().giveCrate(player.getUniqueId(), CrateTypes.NORMAL);
			else
				Tables.getCTable().giveCrate(player.getUniqueId(), CrateTypes.RARE);
			
		}
	}
}
