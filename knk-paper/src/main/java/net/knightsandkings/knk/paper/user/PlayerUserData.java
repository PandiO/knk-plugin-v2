package net.knightsandkings.knk.paper.user;

import java.util.UUID;

/**
 * Immutable record representing a player's account data cached during their session.
 * This data is fetched from the API on player join and updated when account operations occur.
 * 
 * Lifecycle:
 * - Created: On PlayerJoinEvent (via UserManager)
 * - Updated: When user runs account link command (/account link)
 * - Cleared: On PlayerQuitEvent
 * 
 * Thread Safety:
 * - Record is immutable, thread-safe by design
 * - Stored in ConcurrentHashMap by UserManager
 */
public record PlayerUserData(
    /**
     * User ID from the API.
     */
    Integer userId,
    
    /**
     * Player's username.
     */
    String username,
    
    /**
     * Player's Minecraft UUID.
     */
    UUID uuid,
    
    /**
     * Player's email address (null if not set).
     */
    String email,
    
    /**
     * Player's coin balance.
     */
    Integer coins,
    
    /**
     * Player's gem balance.
     */
    Integer gems,
    
    /**
     * Player's experience points.
     */
    Integer experiencePoints,
    
    /**
     * Whether the account has an email linked.
     */
    boolean hasEmailLinked,
    
    /**
     * Whether this user has a duplicate account that needs to be resolved.
     */
    boolean hasDuplicateAccount,
    
    /**
     * ID of the conflicting/duplicate account (if any).
     */
    Integer conflictingUserId
) {
    /**
     * Create a minimal user data entry (used when creating a new user without email).
     */
    public static PlayerUserData minimal(UUID uuid, String username, Integer userId) {
        return new PlayerUserData(
            userId,
            username,
            uuid,
            null,        // no email
            0,           // 0 coins
            0,           // 0 gems
            0,           // 0 exp
            false,       // no email linked
            false,       // no duplicate
            null         // no conflicting user
        );
    }
    
    /**
     * Create user data with duplicate conflict information.
     */
    public static PlayerUserData withConflict(
        UUID uuid, 
        String username, 
        Integer userId,
        Integer conflictingUserId
    ) {
        return new PlayerUserData(
            userId,
            username,
            uuid,
            null,
            0,
            0,
            0,
            false,
            true,        // has duplicate
            conflictingUserId
        );
    }
    
    /**
     * Create a copy with updated email-linked status.
     */
    public PlayerUserData withEmailLinked(String email) {
        return new PlayerUserData(
            userId,
            username,
            uuid,
            email,
            coins,
            gems,
            experiencePoints,
            true,        // email now linked
            hasDuplicateAccount,
            conflictingUserId
        );
    }
    
    /**
     * Create a copy with updated balances (after merge).
     */
    public PlayerUserData withBalances(Integer coins, Integer gems, Integer experiencePoints) {
        return new PlayerUserData(
            userId,
            username,
            uuid,
            email,
            coins,
            gems,
            experiencePoints,
            hasEmailLinked,
            false,       // merge resolves duplicates
            null         // no conflict after merge
        );
    }
}
