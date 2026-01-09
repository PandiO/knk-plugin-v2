package net.knightsandkings.knk.paper.utils;

public class ScoreboarUtil {
    private static Scoreboard scoreboard;

    public ScoreboardUtil() {}

    public static Scoreboard getScoreboard() {
        if (scoreboard == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            scoreboard = setTeams(scoreboard);
        }

        return scoreboard;
    }

    public static Scoreboard setTeams(Scoreboard scoreboard) {
		Team def = scoreboard.registerNewTeam("default");
		def.setPrefix("§7");
		def.setColor(ChatColor.GRAY);

		Team owner = scoreboard.registerNewTeam("owner");
	    owner.setPrefix("§5");
	    owner.setColor(ChatColor.DARK_PURPLE);

	    Objective health = scoreboard.registerNewObjective("Health", "health");
	    health.setDisplaySlot(DisplaySlot.BELOW_NAME);
	    health.setDisplayName(ColorOptions.error + "❤");

	    return scoreboard;
	}

    public static void setScoreboard(List<Player> players) {
        Scoreboard scoreboard = getScoreboard();

        for (Player p : players) {
            Team team = scoreboard.getTeam("default");
            if (player.hasPermission("k&k.*")) {
                team = scoreboard.getTeam("owner");
            }

            team.addPlayer(p);
            player.setScoreboard(scoreboard);
        }
    }
}