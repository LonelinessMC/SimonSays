package it.loneliness.mc.Custom;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Event;

import net.md_5.bungee.api.chat.ComponentBuilder;

abstract class Action {
    private int points;
    private int repetitions;
    FileConfiguration config;
    private Map<String, Integer> playersRepetition;
    private int expireMinutes;
    private LocalDateTime creationTime;

    Action(int points, int repetitions, int expireMinutes, FileConfiguration config) {
        this.points = points;
        this.repetitions = repetitions;
        this.config = config;
        this.expireMinutes = expireMinutes;
        this.playersRepetition = new HashMap<String, Integer>();
        this.creationTime = LocalDateTime.now();
    }

    abstract boolean checkEvent(Event event);

    abstract ComponentBuilder getProgressMessage(String player);

    abstract ComponentBuilder getSuccessMessage(String player);

    abstract ComponentBuilder getTaskDescription(String player);

    public int getPoints() {
        return points;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void addPlayerRepetition(String player, int amount) {
        int done = this.playersRepetition.getOrDefault(player, 0) + amount;
        this.playersRepetition.put(player, Math.min(done, repetitions));
    }

    public boolean playerHasAlreadyCompleted(String player) {
        return this.playersRepetition.getOrDefault(player, 0) >= this.repetitions;
    }

    public int getPlayerRepetitions(String player) {
        return this.playersRepetition.getOrDefault(player, 0);
    }

    String formatString(String player, String source) {
        int playerRepetitions = this.getPlayerRepetitions(player);
        int playerRemainingsRepetitions = repetitions - playerRepetitions;

        return source
                .replace("{awarded_points}", points + "")
                .replace("{needed_repetitions}", repetitions + "")
                .replace("{player_name}", player)
                .replace("{player_repetitions}", playerRepetitions + "")
                .replace("{player_remaining_repetitions}", playerRemainingsRepetitions + "")
                .replace("{remaining_minutes}", this.getMinutesUntilEnd() + "");
    }

    public long getMinutesUntilEnd() {
        LocalDateTime endTime = creationTime.plusMinutes(expireMinutes);
        Duration duration = Duration.between(LocalDateTime.now(), endTime);
        return duration.toMinutes();
    }
}