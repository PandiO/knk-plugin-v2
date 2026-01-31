package net.knightsandkings.knk.core.domain.gates;

import org.bukkit.util.Vector;

/**
 * Represents a single block in a gate structure.
 * Contains relative position and block type information.
 */
public class BlockSnapshot {
    private final int id;
    private final Vector relativePosition;
    private final int minecraftBlockRefId;
    private final String blockData;
    private final int sortOrder;

    public BlockSnapshot(int id, Vector relativePosition, int minecraftBlockRefId, String blockData, int sortOrder) {
        this.id = id;
        this.relativePosition = relativePosition;
        this.minecraftBlockRefId = minecraftBlockRefId;
        this.blockData = blockData;
        this.sortOrder = sortOrder;
    }

    public int getId() {
        return id;
    }

    public Vector getRelativePosition() {
        return relativePosition;
    }

    public int getMinecraftBlockRefId() {
        return minecraftBlockRefId;
    }

    public String getBlockData() {
        return blockData;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
