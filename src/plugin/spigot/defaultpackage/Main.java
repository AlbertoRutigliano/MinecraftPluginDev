package plugin.spigot.defaultpackage;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

import static plugin.spigot.defaultpackage.Commands.*;

import org.bukkit.Server;

public class Main extends JavaPlugin implements Listener {
	public static Server MyServer;
	
	private String vPlayersListFilePath;
	private String vKickedPlayersFilePath;
	
	private PlayerManager vPlayerManager;

	@Override
	public void onEnable() {
		ConfigManager.CreateCustomConfig();
		MyServer = getServer();

		this.vPlayersListFilePath = "onlinePlayers.txt";
		this.vKickedPlayersFilePath = "kickedPlayers.txt";
		this.vPlayerManager = new PlayerManager(this.vPlayersListFilePath, this.vKickedPlayersFilePath);
		
		MyServer.getPluginManager().registerEvents(this, this);
		MyServer.getPluginManager().registerEvents(vPlayerManager, this);

		this.getCommand(COORDS).setExecutor(new CoordsCommand());
		this.getCommand(COORDS).setTabCompleter(new CoordsCommand());
		
		this.getCommand(PLAYERPOS).setExecutor(new PlayerposCommand());
		
		this.getCommand(JOKE).setExecutor(new JokeCommand());

		ServerManager.setTestServerPort(ConfigManager.GetCustomConfig().getInt(ConfigProperties.SERVER_TEST_PORT.name()));

		ServerManager.InitScoreboard();
		
		ServerManager.InitRandomQuote();

	}
	
	@Override
	public void onDisable() {
		ServerManager.RemoveObjectives();
	}
	
}
