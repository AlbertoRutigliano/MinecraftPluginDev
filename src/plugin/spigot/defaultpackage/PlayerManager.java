package plugin.spigot.defaultpackage;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerBedEnterEvent.BedEnterResult;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import net.md_5.bungee.api.ChatColor;
/*
 * Event Handlers list: https://github.com/Bukkit/Bukkit/tree/master/src/main/java/org/bukkit/event
 */

public class PlayerManager implements Listener {
	
	private String vPlayersListFilePath;
	private String vKickedPlayersFilePath;
    public static HashMap<Player, PlayerProperties> vPlayerProperties;
    
    private final Main plugin;


	public PlayerManager(Main plugin, String playerListFilePath, String kickedPlayersFilePath) throws Exception {
		this.plugin = plugin;
		
		if(playerListFilePath == null) {
			throw new Exception("playerListFilePath");
		}
		if(kickedPlayersFilePath == null) {
			throw new Exception("kickedPlayersFilePath");
		}
		
		this.vPlayersListFilePath = playerListFilePath;
		this.vKickedPlayersFilePath = kickedPlayersFilePath;
		
		vPlayerProperties = new HashMap<Player, PlayerProperties>();
		for(Player p:Main.MyServer.getOnlinePlayers()) {
			vPlayerProperties.put(p, new PlayerProperties());
		}
		
		Main.MyServer.getScheduler().scheduleSyncRepeatingTask(Main.getPlugin(Main.class),  new Runnable() {
			public void run() {
				for (Player player : Main.MyServer.getOnlinePlayers()) {
					Timestamp now = new Timestamp(new Date().getTime());
					PlayerProperties l_CurrentPlayer = vPlayerProperties.get(player);
					if(l_CurrentPlayer != null) {
						int seconds = (int) ((now.getTime() - l_CurrentPlayer.getLastMoveTimestamp().getTime()) / 1000) % 60 ;
						if (seconds > ConfigManager.GetCustomConfig().getInt(ConfigProperties.SECONDS_TO_AFK.name())) {
							l_CurrentPlayer.setAfk(true);
						}	
					}
				}
			}
		}, 20, 20);
		
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
		        taker.sendMessage(MSG.GET_DAMAGE.getMessage(damagerPlayer));
		    }
		}
	}

	@EventHandler
	public void onPlayerWorldChange(PlayerPortalEvent e) {
		Player player = e.getPlayer();
		Location toLocation = e.getTo();
		
		if (toLocation.getWorld().getName().equalsIgnoreCase("world_nether")) {
			Bukkit.broadcastMessage(MSG.IN_NETHER.getMessage(player));
		} else if (toLocation.getWorld().getName().equalsIgnoreCase("world")) {
			Bukkit.broadcastMessage(MSG.IN_OVERWORLD.getMessage(player));
		}
	}
	
	@EventHandler
	public void onPlayerEnchant(EnchantItemEvent e) {
		Player player = e.getEnchanter();
		Bukkit.broadcastMessage(MSG.ENCHANTMENT.getMessage(player));
	}
	
	@EventHandler
	public void onPlayerSleep(PlayerBedEnterEvent e) {
		Player player = e.getPlayer();
		PlayerProperties l_CurrentPlayer = vPlayerProperties.get(player);
		if (e.getBedEnterResult() == BedEnterResult.OK) {
			Bukkit.broadcastMessage(MSG.SLEEP.getMessage(player) + ChatColor.GREEN + " zZz");
			l_CurrentPlayer.setSleeping(true);
		} else {
			if (e.getBedEnterResult() == BedEnterResult.NOT_SAFE) {
				Bukkit.broadcastMessage(MSG.CANT_SLEEP.getMessage(player));
			}
			l_CurrentPlayer.setSleeping(false);
		}
	}
	
	@EventHandler
	public void onPlayerWakeUp(PlayerBedLeaveEvent e) {
		Player player = e.getPlayer();
		PlayerProperties l_CurrentPlayer = vPlayerProperties.get(player);
		if (ServerManager.IsDay() == false){
			ServerManager.SendMessageToAllPlayers(vKickedPlayersFilePath);			
		} else{
			player.sendMessage(MSG.GOOD_MORNING.getMessage());
		}
		l_CurrentPlayer.setSleeping(false);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player l_Player = e.getPlayer();
		e.setJoinMessage(MSG.PLAYER_JOIN.getMessage(l_Player));
		this.WritePlayerJoined(l_Player, this.vPlayersListFilePath);		
		vPlayerProperties.put(l_Player, new PlayerProperties());
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player l_Player = e.getPlayer();
		ServerManager.ResetScoreboard(l_Player);
		e.setQuitMessage(MSG.PLAYER_LEFT.getMessage(l_Player));
		// Stop all track running
		if(plugin.getTrackRunner().isTracking(l_Player.getUniqueId())) {
			plugin.getTrackRunner().unsetTracking(l_Player.getUniqueId());
	    }
		this.WritePlayerQuit(l_Player, this.vPlayersListFilePath);		
		vPlayerProperties.remove(l_Player);
	}
	
	@EventHandler
	public void onPlayerKicked(PlayerKickEvent e)
	{
		Player l_Player = e.getPlayer();
		String l_KickReason = e.getReason();
		this.WritePlayerKicked(l_Player, l_KickReason, this.vKickedPlayersFilePath);
	}
	
	@EventHandler
	public void onPlayerMoveEvent(PlayerMoveEvent e) {
		Player l_Player = e.getPlayer();
		Timestamp now = new Timestamp(new Date().getTime());
		
		PlayerProperties l_CurrentProperties = vPlayerProperties.get(l_Player); 
		if(l_CurrentProperties != null) {
			vPlayerProperties.get(l_Player).setAfk(false);
			vPlayerProperties.get(l_Player).setLastMoveTimestamp(now);
		}
	}
	
	// TODO Testare onPlayerDeath e onPlayerRespawnEvent
	/*
	 * Location prova;
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e){
		Player deathPlayer = e.getEntity().getPlayer();
		prova =  deathPlayer.getLocation();
		plugin.getTrackRunner().unsetTracking(deathPlayer.getUniqueId());
		deathPlayer.sendMessage(deathPlayer.getLocation().toString());
	}
	
	@EventHandler
	public void onPlayerRespawnEvent(PlayerRespawnEvent e){
		Player player = e.getPlayer();
		plugin.getTrackRunner().setTracking(player.getUniqueId(), prova);
		player.sendMessage(ChatColor.GRAY + "Stai seguendo " + ChatColor.GOLD + " punto di morte");
    }
	*/
	
	@EventHandler
    public void inventoryclick(InventoryClickEvent event){
		if (event.getClick().equals(ClickType.DOUBLE_CLICK)) {
			InventoryType inventoryType = event.getView().getType();
			
			if (inventoryType.equals(InventoryType.CHEST) || inventoryType.equals(InventoryType.BARREL) || inventoryType.equals(InventoryType.ENDER_CHEST) ) {
				Player player = (Player) event.getWhoClicked();
				Inventory chest = event.getView().getTopInventory();
			
				ArrayList<ItemStack> chestInventory = new ArrayList<>();
				ArrayList<ItemStack> chestInventoryCopy = new ArrayList<>();
				
				for (int i = 0; i < event.getView().getTopInventory().getSize() ; i++) {
		        		chestInventory.add(event.getView().getItem(i));
		        		chestInventoryCopy.add(event.getView().getItem(i));
		    	}
				
				ItemStackComparator l_SortingType = new ItemStackComparator(SortingType.SIMPLE_ASC);
				
				chestInventory.sort(l_SortingType);
		    					
				// Se gi� ordinato, inverti l'ordinamento
				if (chestInventoryCopy.equals(chestInventory)) {
					l_SortingType.setSortingType(SortingType.SIMPLE_DESC);
				} else {
					l_SortingType.setSortingType(SortingType.SIMPLE_ASC);
				}
				
				chestInventory.sort(l_SortingType);
				
		    	// Compact stack
		    	for(int i = 0; i < chestInventory.size() - 1; i++) {
		    		boolean l_CompactAgain = false;
		    		do{
		    			l_CompactAgain = ChestManager.CompactStack(chestInventory.get(i), chestInventory.get(i+1));
		    			chestInventory.sort(l_SortingType);
		    		} while(l_CompactAgain == true);
		    	}
		    	
				chestInventory.sort(l_SortingType);

		    	ItemStack[] sortedInventory = new ItemStack[chestInventory.size()];
				// Prepara e mostra l'inventario aggiornato con l'ordinamento
		    	for(int i = 0; i < sortedInventory.length ; i++) {
		    		sortedInventory[i] = chestInventory.get(i);
		    	}

		    	chest.setContents(sortedInventory);
			    event.setCancelled(true);
			    player.updateInventory();
			    player.playNote(player.getLocation(), Instrument.CHIME, Note.natural(1, Tone.A));

			}
			
		}
		
	}
	
	@EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
		 Player player = event.getPlayer();
	     String message = event.getMessage();
	     event.setFormat(ChatColor.GOLD + player.getDisplayName() + "�8: " + ChatColor.WHITE + message);
	     
	     for(String messageWord: message.split(" ")) {
	    	 for(String thanksWord: ThanksCommand.THANKS_WORDS){
	    		 if (messageWord.equalsIgnoreCase(thanksWord)) {
	        		 ThanksCommand.makeHeartEffect(player);
	        	 }
	          }
	     }  
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
	
	public static HashMap<Player, PlayerProperties> getSleepingPlayers()
	{
		HashMap<Player, PlayerProperties> sleepingPlayers = new HashMap<>();
		for(Map.Entry<Player, PlayerProperties> playerProp : vPlayerProperties.entrySet())
		{
			if(playerProp.getValue().isSleeping() == true)
			{
				sleepingPlayers.put(playerProp.getKey(), playerProp.getValue());
			}
		}
		return sleepingPlayers; 
	}
}
