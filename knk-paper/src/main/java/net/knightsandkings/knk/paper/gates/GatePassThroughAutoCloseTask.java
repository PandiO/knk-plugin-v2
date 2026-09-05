package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.gates.GateManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * One-shot task that closes a gate after its pass-through hold-open window elapses. Kept as a
 * small named class (rather than an inline lambda) so GatePassThroughService can hold a reference
 * to the scheduled BukkitTask per gate ID and cancel/reschedule it if the gate is re-triggered
 * while still open - matching the GateFireDamageTask/GateFireSystem split.
 */
public class GatePassThroughAutoCloseTask extends BukkitRunnable {
    private final GateManager gateManager;
    private final int gateId;

    public GatePassThroughAutoCloseTask(GateManager gateManager, int gateId) {
        this.gateManager = gateManager;
        this.gateId = gateId;
    }

    @Override
    public void run() {
        gateManager.closeGate(gateId);
    }
}
