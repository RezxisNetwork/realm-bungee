package net.rezxis.mchosting.bungee.tasks;

import java.util.Random;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.rezxis.mchosting.bungee.Bungee;
import net.rezxis.mchosting.database.crates.CrateTypes;
import net.rezxis.mchosting.database.object.player.DBPlayer;

public class RewardTask implements Runnable {

	@SuppressWarnings("deprecation")
	public void run() {
		for (ProxiedPlayer player : BungeeCord.getInstance().getPlayers()) {
			if (player.getServer().getInfo().getName().equalsIgnoreCase("lobby"))
				continue;
			int coin = 50;
			DBPlayer dp = Bungee.instance.pTable.get(player.getUniqueId());
			player.sendMessage(ChatColor.AQUA+"おつかれさまでした! "+ChatColor.LIGHT_PURPLE+coin+"枚"+ChatColor.AQUA+"のコインが手に入った");
			player.sendMessage(ChatColor.GREEN+"報酬箱を手に入れました！ロビーでチェストをクリックして、開けます！");
			dp.addCoin(coin);
			dp.setVault(dp.getVault()+1);
			dp.update();
			if (new Random().nextInt(100)<= 80) {
				Bungee.instance.cTable.giveCrate(player.getUniqueId(), CrateTypes.NORMAL);
			} else {
				Bungee.instance.cTable.giveCrate(player.getUniqueId(), CrateTypes.RARE);
			}
		}
		System.out.println("gived rewards!");
	}
}
