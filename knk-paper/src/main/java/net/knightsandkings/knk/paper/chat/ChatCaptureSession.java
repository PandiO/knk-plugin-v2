package net.knightsandkings.knk.paper.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a single chat capture session for a player.
 * Holds all state needed for multi-step input flows.
 */
public class ChatCaptureSession {
    private final UUID playerId;
    private final CaptureFlow flow;
    private CaptureStep currentStep;
    private final long startTime;
    private final Map<String, String> data;
    private Consumer<Map<String, String>> onComplete;
    private Runnable onCancel;
    
    /**
     * Creates a new chat capture session.
     *
     * @param playerId the UUID of the player
     * @param flow the type of flow (create, merge, etc.)
     * @param currentStep the initial step in the flow
     */
    public ChatCaptureSession(UUID playerId, CaptureFlow flow, CaptureStep currentStep) {
        this.playerId = playerId;
        this.flow = flow;
        this.currentStep = currentStep;
        this.startTime = System.currentTimeMillis();
        this.data = new HashMap<>();
        this.onComplete = null;
        this.onCancel = null;
    }
    
    // Getters
    public UUID getPlayerId() {
        return playerId;
    }
    
    public CaptureFlow getFlow() {
        return flow;
    }
    
    public CaptureStep getCurrentStep() {
        return currentStep;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000L;
    }
    
    public Map<String, String> getData() {
        return data;
    }
    
    // Setters
    public void setCurrentStep(CaptureStep step) {
        this.currentStep = step;
    }
    
    public void putData(String key, String value) {
        this.data.put(key, value);
    }
    
    public String getData(String key) {
        return this.data.get(key);
    }
    
    public void setOnComplete(Consumer<Map<String, String>> onComplete) {
        this.onComplete = onComplete;
    }
    
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }
    
    // Callbacks
    public Consumer<Map<String, String>> getOnComplete() {
        return onComplete;
    }
    
    public Runnable getOnCancel() {
        return onCancel;
    }
    
    /**
     * Clear sensitive data from the session.
     * Should be called after completion or cancellation.
     */
    public void clearSensitiveData() {
        data.clear();
    }
}
