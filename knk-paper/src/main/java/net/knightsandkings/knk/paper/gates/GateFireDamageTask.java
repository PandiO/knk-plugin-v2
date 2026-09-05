package net.knightsandkings.knk.paper.gates;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Thin BukkitRunnable wrapper that ticks GateFireSystem on a configurable interval
 * (gates.fire-tick-interval-seconds), so continuous fire damage keeps applying to burning gates'
 * door blocks independent of the main animation task's 1-tick cadence. All the actual
 * damage/pruning/particle logic lives in GateFireSystem - kept out of this class so it stays
 * unit-testable without a live Bukkit scheduler.
 */
public class GateFireDamageTask extends BukkitRunnable {
    private final GateFireSystem fireSystem;

    public GateFireDamageTask(GateFireSystem fireSystem) {
        this.fireSystem = fireSystem;
    }

    @Override
    public void run() {
        fireSystem.tick();
    }
}
