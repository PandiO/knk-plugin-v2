package net.knightsandkings.knk.core.ports.gates;

import java.util.UUID;

/**
 * Port for controlling gate operations (opening/closing).
 * Core defines the contract; knk-paper implements with actual block manipulation.
 */
public interface GateControlPort {
    /**
     * Open a gate when a player enters it.
     * 
     * @param gateId the gate entity ID
     * @param triggeringPlayerId the player who triggered the gate opening
     */
    void openGate(UUID gateId, UUID triggeringPlayerId);

    /**
     * Close a gate when a player leaves it.
     * 
     * @param gateId the gate entity ID
     * @param triggeringPlayerId the player who triggered the gate closing
     */
    void closeGate(UUID gateId, UUID triggeringPlayerId);
}
