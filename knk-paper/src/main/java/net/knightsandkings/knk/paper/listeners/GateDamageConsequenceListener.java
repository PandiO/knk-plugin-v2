package net.knightsandkings.knk.paper.listeners;

import net.knightsandkings.knk.paper.events.GateDoorDamageEvent;
import net.knightsandkings.knk.paper.gates.HealthSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * Consequence listener for GateDoorDamageEvent: owns the damage amount per Cause and applies it
 * via HealthSystem, which already no-ops for invincible/destroyed gates. Kept separate from
 * detection (GateDoorHitService) so future causes/amounts can be tuned here without touching
 * how a hit is detected.
 */
public class GateDamageConsequenceListener implements Listener {
    private static final Map<GateDoorDamageEvent.Cause, Double> DAMAGE_BY_CAUSE = Map.of(
        GateDoorDamageEvent.Cause.LEFT_CLICK, 10.0,
        GateDoorDamageEvent.Cause.PROJECTILE, 10.0,
        GateDoorDamageEvent.Cause.EXPLOSION, 10.0,
        GateDoorDamageEvent.Cause.BLOCK_BREAK, 10.0
    );

    private final HealthSystem healthSystem;

    public GateDamageConsequenceListener(HealthSystem healthSystem) {
        this.healthSystem = healthSystem;
    }

    @EventHandler
    public void onGateDoorDamage(GateDoorDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        double amount = DAMAGE_BY_CAUSE.getOrDefault(event.getCause(), 10.0);
        healthSystem.applyDamage(event.getGate(), amount);
    }
}
