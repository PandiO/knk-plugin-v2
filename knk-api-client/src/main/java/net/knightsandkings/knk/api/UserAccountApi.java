package net.knightsandkings.knk.api;

import net.knightsandkings.knk.api.dto.user.*;

import java.util.concurrent.CompletableFuture;

/**
 * API for user account management operations.
 * Handles account creation, linking, and merge operations.
 */
public interface UserAccountApi {
    /**
     * Create a new user account.
     * Supports multiple flows: Minecraft-only, web app with email/password, or linking via code.
     * 
     * @param request User creation request
     * @return Created user data
     */
    CompletableFuture<UserResponse> createUser(CreateUserRequest request);
    
    /**
     * Check for duplicate accounts by UUID and username.
     * Used to detect if a player has multiple accounts that need merging.
     * 
     * @param uuid Player's Minecraft UUID
     * @param username Player's username
     * @return Duplicate check result with conflicting accounts if found
     */
    CompletableFuture<DuplicateCheckResponse> checkDuplicate(String uuid, String username);
    
    /**
     * Generate a link code for a user.
     * The code can be used to link web app credentials to a Minecraft account.
     * 
     * @param userId User ID to generate code for
     * @return Link code with expiration
     */
    CompletableFuture<LinkCodeResponse> generateLinkCode(Integer userId);
    
    /**
     * Validate a link code.
     * Checks if the code is valid and not expired.
     * 
     * @param code Link code to validate
     * @return Validation result with user info if valid
     */
    CompletableFuture<ValidateLinkCodeResponse> validateLinkCode(String code);
    
    /**
     * Update a user's email address.
     * 
     * @param userId User ID
     * @param email New email address
     * @return True if updated successfully
     */
    CompletableFuture<Boolean> updateEmail(Integer userId, String email);
    
    /**
     * Change a user's password.
     * 
     * @param userId User ID
     * @param request Password change request with current and new passwords
     * @return True if changed successfully
     */
    CompletableFuture<Boolean> changePassword(Integer userId, ChangePasswordRequest request);
    
    /**
     * Merge two user accounts.
     * The primary account is kept, the secondary is soft-deleted.
     * 
     * @param primaryId ID of account to keep
     * @param secondaryId ID of account to delete
     * @return Merged user data
     */
    CompletableFuture<UserResponse> mergeAccounts(Integer primaryId, Integer secondaryId);
    
    /**
     * Link an existing Minecraft account with web app credentials via link code.
     * 
     * @param request Link account request with code and credentials
     * @return Linked user data
     */
    CompletableFuture<UserResponse> linkAccount(LinkAccountRequest request);
}
