package net.knightsandkings.knk.paper.gates;

import net.knightsandkings.knk.core.domain.gates.AnimationState;
import net.knightsandkings.knk.core.domain.gates.CachedGate;
import net.knightsandkings.knk.core.gates.GateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Renders each gate's configurable name/health/status info as a floating {@link TextDisplay}
 * above the gate. A TextDisplay (not a LivingEntity, unlike an ArmorStand) has no AI/physics
 * baggage, has no hitbox so it can't be pushed or punched, and natively supports multi-line
 * text - which is needed here since each of the three info topics can be toggled independently.
 */
public class GateDisplayManager {
    private static final Logger LOGGER = Logger.getLogger(GateDisplayManager.class.getName());

    private final NamespacedKey gateIdKey;
    private final Map<Integer, TextDisplay> displays = new HashMap<>();

    public GateDisplayManager(Plugin plugin) {
        this.gateIdKey = new NamespacedKey(plugin, "gate_display_id");
    }

    /**
     * Recompute and (re)spawn the display for every cached gate, in whichever of its world(s)
     * is currently loaded.
     */
    public void syncAll(GateManager gateManager) {
        for (CachedGate gate : gateManager.getAllGates().values()) {
            syncDisplay(gate);
        }
    }

    /**
     * Recompute the display for a single gate: spawns/moves/updates its TextDisplay, or removes
     * it if nothing is currently visible.
     */
    public void syncDisplay(CachedGate gate) {
        if (gate == null) {
            return;
        }

        World world = resolveWorld(gate);
        if (world == null || !gate.isActive()) {
            removeDisplay(gate.getId());
            return;
        }

        Component text = buildDisplayText(gate);
        if (text == null) {
            removeDisplay(gate.getId());
            return;
        }

        Location location = calculateDisplayLocation(gate, world);
        if (location == null) {
            removeDisplay(gate.getId());
            return;
        }

        TextDisplay display = displays.get(gate.getId());
        if (display == null || display.isDead() || !world.equals(display.getWorld())) {
            if (display != null) {
                display.remove();
            }
            final int gateId = gate.getId();
            display = world.spawn(location, TextDisplay.class, entity -> configureDisplay(entity, gateId));
            displays.put(gate.getId(), display);
        } else {
            display.teleport(location);
        }

        display.text(text);
    }

    private void configureDisplay(TextDisplay entity, int gateId) {
        entity.setBillboard(Display.Billboard.CENTER);
        entity.setGravity(false);
        entity.setPersistent(true);
        entity.setInvulnerable(true);
        entity.getPersistentDataContainer().set(gateIdKey, PersistentDataType.INTEGER, gateId);
    }

    /**
     * Remove the display entity for a single gate, if one exists.
     */
    public void removeDisplay(int gateId) {
        TextDisplay display = displays.remove(gateId);
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    /**
     * Remove every display this manager currently tracks. Used on plugin shutdown.
     */
    public void removeAll() {
        for (TextDisplay display : displays.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        displays.clear();
    }

    /**
     * Scan a world for gate-display TextDisplays left over from a previous run (crash, or a gate
     * that has since been deleted) and remove the ones that no longer match a cached gate.
     * Entities that still match a live gate are adopted into the tracking map so startup doesn't
     * spawn duplicates. Must run before the first {@link #syncAll(GateManager)} at startup.
     */
    public void cleanupOrphans(World world, GateManager gateManager) {
        if (world == null) {
            return;
        }

        for (TextDisplay entity : world.getEntitiesByClass(TextDisplay.class)) {
            Integer gateId = entity.getPersistentDataContainer().get(gateIdKey, PersistentDataType.INTEGER);
            if (gateId == null) {
                continue; // Not a gate display entity.
            }

            if (gateManager.getGate(gateId) == null) {
                entity.remove();
                LOGGER.info("Removed orphaned gate display entity for gate ID " + gateId);
            } else {
                displays.put(gateId, entity);
            }
        }
    }

    private World resolveWorld(CachedGate gate) {
        String worldName = gate.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        return Bukkit.getWorld(worldName);
    }

    /**
     * Width/Depth are measured along the gate's local u/n basis (see
     * {@code GateFrameCalculator.isWithinGeometryBounds}, which projects world positions onto
     * uAxis/vAxis/nAxis and compares against Width/Height/Depth) - that basis is derived from
     * AnchorPoint/ReferencePoint1/ReferencePoint2 by {@code GateLoaderAdapter.precomputeBasisVectors}
     * and accounts for diagonal FaceDirections. Offsetting along world X/Z instead only happens to
     * be correct for axis-aligned (north/south/east/west) gates and places the display off to the
     * side for a diagonally-facing (e.g. north-east) gate.
     */
    private Location calculateDisplayLocation(CachedGate gate, World world) {
        Vector anchor = gate.getAnchorPoint();
        if (anchor == null) {
            return null;
        }

        Vector uAxis = gate.getUAxis();
        Vector nAxis = gate.getNAxis();

        Vector widthOffset = uAxis != null && uAxis.lengthSquared() > 0
            ? uAxis.clone().multiply(gate.getGeometryWidth() / 2.0)
            : new Vector(gate.getGeometryWidth() / 2.0, 0, 0);
        Vector depthOffset = nAxis != null && nAxis.lengthSquared() > 0
            ? nAxis.clone().multiply(gate.getGeometryDepth() / 2.0)
            : new Vector(0, 0, gate.getGeometryDepth() / 2.0);

        Vector position = anchor.clone()
            .add(widthOffset)
            .add(depthOffset)
            .add(new Vector(0, gate.getHealthDisplayYOffset(), 0));

        return new Location(world, position.getX(), position.getY(), position.getZ());
    }

    private Component buildDisplayText(CachedGate gate) {
        Component text = null;

        if (isTopicVisible(gate.getGateNameDisplayMode(), gate)) {
            text = appendLine(text, Component.text(gate.getName(), NamedTextColor.WHITE));
        }

        if (isHealthVisible(gate)) {
            text = appendLine(text, buildHealthLine(gate));
        }

        if (isTopicVisible(gate.getStatusDisplayMode(), gate)) {
            text = appendLine(text, buildStatusLine(gate));
        }

        return text;
    }

    private Component appendLine(Component existing, Component line) {
        return existing == null ? line : existing.append(Component.newline()).append(line);
    }

    /** Shared visibility rule for the Gate Name / Status topics (ALWAYS / NEVER / SIEGE_ONLY). */
    private boolean isTopicVisible(String mode, CachedGate gate) {
        String resolved = mode != null ? mode : "ALWAYS";
        return switch (resolved) {
            case "NEVER" -> false;
            case "SIEGE_ONLY" -> gate.getCurrentSiegeId() != null;
            default -> true;
        };
    }

    /** Health has its own rule since it has the extra DAMAGED_ONLY mode and a master toggle. */
    private boolean isHealthVisible(CachedGate gate) {
        if (!gate.isShowHealthDisplay()) {
            return false;
        }

        String mode = gate.getHealthDisplayMode() != null ? gate.getHealthDisplayMode() : "ALWAYS";
        return switch (mode) {
            case "NEVER" -> false;
            case "DAMAGED_ONLY" -> gate.getHealthCurrent() < gate.getHealthMax();
            case "SIEGE_ONLY" -> gate.getCurrentSiegeId() != null;
            default -> true;
        };
    }

    private Component buildHealthLine(CachedGate gate) {
        double max = Math.max(1.0, gate.getHealthMax());
        double ratio = gate.getHealthCurrent() / max;

        NamedTextColor color;
        if (gate.isDestroyed()) {
            color = NamedTextColor.DARK_RED;
        } else if (ratio >= 0.75) {
            color = NamedTextColor.GREEN;
        } else if (ratio >= 0.5) {
            color = NamedTextColor.YELLOW;
        } else if (ratio >= 0.25) {
            color = NamedTextColor.GOLD;
        } else {
            color = NamedTextColor.RED;
        }

        String healthText = String.format("%.0f/%.0f hp", gate.getHealthCurrent(), gate.getHealthMax());
        return Component.text(healthText, color);
    }

    private Component buildStatusLine(CachedGate gate) {
        if (gate.isDestroyed()) {
            return Component.text("DESTROYED", NamedTextColor.DARK_RED);
        }
        if (gate.isInvincible()) {
            return Component.text("INVINCIBLE", NamedTextColor.AQUA);
        }

        AnimationState state = gate.getCurrentState();
        if (state == null) {
            return Component.text("UNKNOWN", NamedTextColor.GRAY);
        }

        return switch (state) {
            case OPEN -> Component.text("OPEN", NamedTextColor.GREEN);
            case OPENING -> Component.text("OPENING", NamedTextColor.GOLD);
            case CLOSING -> Component.text("CLOSING", NamedTextColor.GOLD);
            case CLOSED -> Component.text("CLOSED", NamedTextColor.RED);
        };
    }
}
