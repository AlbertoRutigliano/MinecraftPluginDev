package plugin.spigot.defaulpackage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.apache.commons.lang.NullArgumentException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerKickEvent;

import net.md_5.bungee.api.ChatColor;

public class PlayerManager implements Listener {
	
	private String vPlayersListFilePath;
	private String vKickedPlayersFilePath;
	
	public PlayerManager(String playerListFilePath, String kickedPlayersFilePath)
	{
		if(playerListFilePath == null)
		{
			throw new NullArgumentException("playerListFilePath");
		}
		if(kickedPlayersFilePath == null)
		{
			throw new NullArgumentException("kickedPlayersFilePath");
		}
		
		this.vPlayersListFilePath = playerListFilePath;
		this.vKickedPlayersFilePath = kickedPlayersFilePath;
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		Entity damageTaker = e.getEntity();
	
		if (damageTaker instanceof Player) {
		    //DamageTaker is a Player
		    Player taker = (Player) damageTaker;
		    if (damager instanceof Player) {
		        //Damage Causer is also a player
		        Player damagerPlayer = (Player) damager;
		        String l_Message = getRandomDamageText();
		        taker.sendMessage(ChatColor.DARK_RED + damagerPlayer.getDisplayName() + ChatColor.WHITE + l_Message);
		    }
		}
		
	}

	@EventHandler
	public void onPlayerWorldChange(PlayerPortalEvent e) {
		Entity player = e.getPlayer();
		Location toLocation = e.getTo();
		
		if (toLocation.getWorld().getName().equalsIgnoreCase("world_nether")) {
			Bukkit.broadcastMessage(player.getName() + " � entrato nel Nether");
		} else if (toLocation.getWorld().getName().equalsIgnoreCase("world")) {
			Bukkit.broadcastMessage(player.getName() + " � tornato nel nostro Mondo");
		}
	}
	
	@EventHandler
	public void onPlayerEnchant(EnchantItemEvent e) {
		Entity player = e.getEnchanter();
		// ItemStack itemEnchanted = e.getItem();
		Bukkit.broadcastMessage("Il bastardo " + player.getName() + " ha incantato un oggetto");
	}
	
	@EventHandler
	public void onPlayerSleep(PlayerBedEnterEvent e) {
		Entity player = e.getPlayer();
		if (e.getBedEnterResult() == BedEnterResult.OK) {
			Bukkit.broadcastMessage(player.getName() + " sta dormendo");
		}
	}
	
	@EventHandler
	public void onPlayerWakeUp(PlayerBedLeaveEvent e) {
		Entity player = e.getPlayer();
		if (ServerManager.IsDay() == false)
		{
			Bukkit.broadcastMessage(player.getName() + " bastardo si � alzato");			
		}
		else
		{
			player.sendMessage("Buongiorno, ben svegliato");
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player l_Player = e.getPlayer();
		this.WritePlayerJoined(l_Player, this.vPlayersListFilePath);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player l_Player = e.getPlayer();
		ServerManager.ResetScoreboard(l_Player);
		this.WritePlayerQuit(l_Player, this.vPlayersListFilePath);
	}
	
	@EventHandler
	public void onPlayerKicked(PlayerKickEvent e)
	{
		Player l_Player = e.getPlayer();
		String l_KickReason = e.getReason();
		this.WritePlayerKicked(l_Player, l_KickReason, this.vKickedPlayersFilePath);
	}
	
	
	// Add the player name to the playerListFilePath
	private void WritePlayerJoined(Player player, String playersListFilePath) {
		String l_PlayerName = player.getName();
		FileManager.AppendStringOnFile(playersListFilePath, l_PlayerName);
	}
	
	// Removes the player name from the playerListFilePath
	private void WritePlayerQuit(Player player, String playersListFilePath) {
		String l_StringToReplace = player.getName() + System.lineSeparator();
		
		FileManager.ReplaceStringOnFile(playersListFilePath, l_StringToReplace, "");
	}
	
	// Writes kick datetime, kicked player, kick reason
	private void WritePlayerKicked(Player player, String reason, String kickedPlayersFilePath)
	{
		SimpleDateFormat l_DateFormatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date l_Date = new Date(System.currentTimeMillis());
		String l_FormattedDate = l_DateFormatter.format(l_Date);
		String l_PlayerName = player.getName();

		StringBuilder l_FileContent = new StringBuilder();
		l_FileContent.append(l_FormattedDate);
		l_FileContent.append(" ");
		l_FileContent.append(l_PlayerName);
		l_FileContent.append(" ");
		l_FileContent.append(reason);
		
		FileManager.AppendStringOnFile(kickedPlayersFilePath, l_FileContent.toString());
	}
	
	//Return random damage message
	private String getRandomDamageText() {
		String[] randomDamageTexts = {" ti ha inchiappettato.", " ti ha dato una botta.", " ti sta menando.", " ti ha fatto la bua.", " ha voglia di te."};
		Random r = new Random();
		int random = r.nextInt(randomDamageTexts.length);
		return " " + randomDamageTexts[random];
	}
}
