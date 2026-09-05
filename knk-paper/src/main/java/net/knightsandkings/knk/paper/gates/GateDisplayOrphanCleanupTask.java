package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic runtime repair pass for gate info displays: removes orphaned (gate deleted) and
 * duplicate (e.g. left behind by a chunk unload/reload, see {@link GateDisplayManager#syncDisplay})
 * TextDisplay entities, then re-syncs the survivors. Complements the one-shot cleanup that already
 * runs at plugin startup by catching duplicates that appear while the server keeps running, without
 * requiring a restart.
 */
public class GateDisplayOrphanCleanupTask extends BukkitRunnable {
    private final GateDisplayManager displayManager;
    private final GateManager gateManager;

    public GateDisplayOrphanCleanupTask(GateDisplayManager displayManager, GateManager gateManager) {
        this.displayManager = displayManager;
        this.gateManager = gateManager;
    }

    @Override
    public void run() {
        displayManager.repairOrphans(gateManager);
    }
}
