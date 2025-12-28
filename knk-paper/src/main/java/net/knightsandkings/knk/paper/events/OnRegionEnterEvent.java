package net.knightsandkings.knk.paper.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OnRegionEnterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String regionId;

    public OnRegionEnterEvent(Player player, String regionId) {
        this.player = player;
        this.regionId = regionId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getRegionId() {
        return regionId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}