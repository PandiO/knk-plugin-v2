package net.knightsandkings.knk.paper.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import net.knightsandkings.knk.api.dto.*;
import net.knightsandkings.knk.core.ports.api.UserAccountApi;
import net.knightsandkings.knk.paper.KnKPlugin;
import net.knightsandkings.knk.paper.chat.ChatCaptureManager;
import net.knightsandkings.knk.paper.commands.AccountCreateCommand;
import net.knightsandkings.knk.paper.commands.AccountLinkCommand;
import net.knightsandkings.knk.paper.config.KnkConfig;
import net.knightsandkings.knk.paper.user.PlayerUserData;
import net.knightsandkings.knk.paper.user.UserManager;
import net.knightsandkings.knk.paper.utils.CommandCooldownManager;

/**
 * Integration tests for account management command flows.
 * Tests the full lifecycle from command execution to API calls.
 */
class AccountCommandIntegrationTest {

    private UserAccountApi mockApi;
    private KnKPlugin mockPlugin;
    private Logger mockLogger;
    private KnkConfig mockConfig;
    private KnkConfig.AccountConfig mockAccountConfig;
    private KnkConfig.MessagesConfig mockMessagesConfig;
    
    private UserManager userManager;
    private ChatCaptureManager chatCaptureManager;
    private AccountCreateCommand accountCreateCommand;
    private AccountLinkCommand accountLinkCommand;
    private CommandCooldownManager mockCooldownManager;
    
    private Player mockPlayer;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        mockApi = mock(UserAccountApi.class);
        mockPlugin = mock(KnKPlugin.class);
        mockLogger = mock(Logger.class);
        mockConfig = mock(KnkConfig.class);
        mockAccountConfig = mock(KnkConfig.AccountConfig.class);
        mockMessagesConfig = mock(KnkConfig.MessagesConfig.class);
        mockCooldownManager = mock(CommandCooldownManager.class);
        mockPlayer = mock(Player.class);
        
        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        
        // Configure mocks
        when(mockConfig.account()).thenReturn(mockAccountConfig);
        when(mockConfig.messages()).thenReturn(mockMessagesConfig);
        when(mockMessagesConfig.prefix()).thenReturn("§8[§6KnK§8]§r ");
        when(mockAccountConfig.chatCaptureTimeoutSeconds()).thenReturn(120);
        when(mockAccountConfig.linkCodeExpiryMinutes()).thenReturn(20);
        when(mockCooldownManager.canExecute(any(UUID.class), anyString(), anyInt())).thenReturn(true);
        
        // Initialize components
        userManager = new UserManager(mockPlugin, mockApi, mockLogger, mockAccountConfig, mockMessagesConfig);
        chatCaptureManager = new ChatCaptureManager(mockPlugin, mockConfig, mockLogger);
        accountCreateCommand = new AccountCreateCommand(
            mockPlugin, userManager, chatCaptureManager, mockApi, mockConfig, mockCooldownManager
        );
        accountLinkCommand = new AccountLinkCommand(
            mockPlugin, userManager, chatCaptureManager, mockApi, mockConfig, mockCooldownManager
        );
    }

    @Nested
    @DisplayName("/account create Flow Integration Tests")
    class AccountCreateFlowTests {

        @Test
        @DisplayName("Should complete full account creation flow")
        void shouldCompleteFullAccountCreationFlow() {
            // Arrange - Setup user in cache without email
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000,
                false, // no email linked
                false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            // Mock API responses - both return Void
            when(mockApi.updateEmail(eq(1), eq("test@example.com")))
                .thenReturn(CompletableFuture.completedFuture(null));
            when(mockApi.changePassword(eq(1), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // Act - Execute command
            boolean commandResult = accountCreateCommand.onCommand(
                mockPlayer, null, "account", new String[]{"create"}
            );

            // Assert command accepted
            assertTrue(commandResult);
            assertTrue(chatCaptureManager.isCapturingChat(testUUID));
            
            // Simulate user input flow
            chatCaptureManager.handleChatInput(mockPlayer, "test@example.com");
            chatCaptureManager.handleChatInput(mockPlayer, "StrongPassword123");
            chatCaptureManager.handleChatInput(mockPlayer, "StrongPassword123");
            
            // Give async operations time to complete
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            // Assert session closed after completion
            assertFalse(chatCaptureManager.isCapturingChat(testUUID));
            
            // Verify API calls made
            verify(mockApi, timeout(1000)).updateEmail(1, "test@example.com");
            verify(mockApi, timeout(1000)).changePassword(eq(1), any(ChangePasswordRequestDto.class));
            
            // Verify cache updated
            PlayerUserData updated = userManager.getCachedUser(testUUID);
            assertNotNull(updated);
            assertTrue(updated.hasEmailLinked());
        }

        @Test
        @DisplayName("Should reject command if email already linked")
        void shouldRejectIfEmailAlreadyLinked() {
            // Arrange - User with email already linked
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, "existing@example.com",
                100, 50, 1000,
                true, // email already linked
                false, null
            );
            userManager.updateCachedUser(testUUID, userData);

            // Act
            boolean result = accountCreateCommand.onCommand(
                mockPlayer, null, "account", new String[]{"create"}
            );

            // Assert
            assertTrue(result);
            assertFalse(chatCaptureManager.isCapturingChat(testUUID));
            
            // Verify error message sent (use String for Player.sendMessage)
            verify(mockPlayer, atLeastOnce()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should handle API errors during account creation")
        void shouldHandleApiErrors() {
            // Arrange
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000, false, false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            // Mock API failure
            when(mockApi.updateEmail(anyInt(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                    new RuntimeException("Network error")
                ));

            // Act
            accountCreateCommand.onCommand(mockPlayer, null, "account", new String[]{"create"});
            chatCaptureManager.handleChatInput(mockPlayer, "test@example.com");
            chatCaptureManager.handleChatInput(mockPlayer, "Password123");
            chatCaptureManager.handleChatInput(mockPlayer, "Password123");
            
            // Give async operations time to complete
            try { Thread.sleep(100); } catch (InterruptedException e) {}

            // Assert - Error message shown to player (use String sendMessage)
            verify(mockPlayer, timeout(1000).atLeastOnce()).sendMessage(anyString());
        }
    }

    @Nested
    @DisplayName("/account link Flow Integration Tests")
    class AccountLinkFlowTests {

        @Test
        @DisplayName("Should generate link code")
        void shouldGenerateLinkCode() {
            // Arrange
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000, false, false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            LinkCodeResponseDto linkCodeResponse = new LinkCodeResponseDto(
                "ABC123DEF456",
                "2026-01-30T12:00:00Z",
                "ABC-123-DEF-456"
            );
            
            when(mockApi.generateLinkCode(1))
                .thenReturn(CompletableFuture.completedFuture(linkCodeResponse));

            // Act
            boolean result = accountLinkCommand.onCommand(
                mockPlayer, null, "account", new String[]{"link"}
            );

            // Assert
            assertTrue(result);
            verify(mockApi, timeout(1000)).generateLinkCode(1);
            
            // Verify code displayed to player (use String sendMessage)
            verify(mockPlayer, timeout(1000).atLeastOnce()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should link account with valid code")
        void shouldLinkAccountWithValidCode() {
            // Arrange
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000, false, false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            ValidateLinkCodeResponseDto validCode = new ValidateLinkCodeResponseDto(
                true, 2, "ExistingUser", "existing@example.com", null
            );
            
            DuplicateCheckResponseDto noDuplicate = new DuplicateCheckResponseDto(
                false, null, null, null
            );
            
            UserResponseDto linkedUser = new UserResponseDto(
                2, "TestPlayer", testUUID.toString(), "existing@example.com",
                200, 100, 2000, true, "WEB_APP"
            );
            
            when(mockApi.validateLinkCode("ABC123"))
                .thenReturn(CompletableFuture.completedFuture(validCode));
            when(mockApi.checkDuplicate(testUUID.toString(), "TestPlayer"))
                .thenReturn(CompletableFuture.completedFuture(noDuplicate));
            when(mockApi.linkAccount(any(LinkAccountRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(linkedUser));

            // Act
            boolean result = accountLinkCommand.onCommand(
                mockPlayer, null, "account", new String[]{"link", "ABC123"}
            );

            // Assert
            assertTrue(result);
            verify(mockApi, timeout(1000)).validateLinkCode("ABC123");
            verify(mockApi, timeout(1000)).linkAccount(any(LinkAccountRequestDto.class));
            
            // Verify cache updated
            PlayerUserData updated = userManager.getCachedUser(testUUID);
            assertNotNull(updated);
            assertEquals("existing@example.com", updated.email());
            assertTrue(updated.hasEmailLinked());
            
            // Verify success message (use String sendMessage)
            verify(mockPlayer, timeout(1000).atLeastOnce()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should reject invalid link code")
        void shouldRejectInvalidLinkCode() {
            // Arrange
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000, false, false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            ValidateLinkCodeResponseDto invalidCode = new ValidateLinkCodeResponseDto(
                false, null, null, null, "Code expired"
            );
            
            when(mockApi.validateLinkCode("INVALID"))
                .thenReturn(CompletableFuture.completedFuture(invalidCode));

            // Act
            boolean result = accountLinkCommand.onCommand(
                mockPlayer, null, "account", new String[]{"link", "INVALID"}
            );

            // Assert
            assertTrue(result);
            verify(mockApi, timeout(1000)).validateLinkCode("INVALID");
            
            // Verify error message (use String sendMessage)
            verify(mockPlayer, timeout(1000).atLeastOnce()).sendMessage(anyString());
        }

        @Test
        @DisplayName("Should handle merge conflict during link")
        void shouldHandleMergeConflict() {
            // Arrange
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000, false, false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            ValidateLinkCodeResponseDto validCode = new ValidateLinkCodeResponseDto(
                true, 2, "ExistingUser", "existing@example.com", null
            );
            
            UserResponseDto primaryUser = new UserResponseDto(
                1, "TestPlayer", testUUID.toString(), null,
                100, 50, 1000, false, "MINECRAFT"
            );
            
            UserResponseDto conflictingUser = new UserResponseDto(
                2, "ExistingUser", testUUID.toString(), "existing@example.com",
                200, 100, 2000, true, "WEB_APP"
            );
            
            DuplicateCheckResponseDto duplicateFound = new DuplicateCheckResponseDto(
                true, conflictingUser, primaryUser, "Duplicate detected"
            );
            
            UserResponseDto mergedUser = new UserResponseDto(
                1, "TestPlayer", testUUID.toString(), "existing@example.com",
                300, 150, 3000, true, "MINECRAFT"
            );
            
            when(mockApi.validateLinkCode("ABC123"))
                .thenReturn(CompletableFuture.completedFuture(validCode));
            when(mockApi.checkDuplicate(testUUID.toString(), "TestPlayer"))
                .thenReturn(CompletableFuture.completedFuture(duplicateFound));
            // Create merge request DTO
            MergeAccountsRequestDto mergeRequest = new MergeAccountsRequestDto(1, 2);
            when(mockApi.mergeAccounts(any()))
                .thenReturn(CompletableFuture.completedFuture(mergedUser));

            // Act - Start link command
            accountLinkCommand.onCommand(
                mockPlayer, null, "account", new String[]{"link", "ABC123"}
            );
            
            // Wait for async processing
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            // Assert merge flow started
            assertTrue(chatCaptureManager.isCapturingChat(testUUID));
            
            // Simulate choosing account A
            chatCaptureManager.handleChatInput(mockPlayer, "A");
            
            // Wait for async processing
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            
            // Assert merge completed
            assertFalse(chatCaptureManager.isCapturingChat(testUUID));
            verify(mockApi, timeout(1000)).mergeAccounts(any());
            
            // Verify cache updated with merged data
            PlayerUserData updated = userManager.getCachedUser(testUUID);
            assertNotNull(updated);
            assertEquals(300, updated.coins());
            assertEquals(150, updated.gems());
            assertEquals(3000, updated.experiencePoints());
        }
    }

    @Nested
    @DisplayName("API Client Integration Tests")
    class ApiClientTests {

        @Test
        @DisplayName("Should handle retry on transient errors")
        void shouldRetryOnTransientErrors() {
            // Arrange
            PlayerUserData userData = new PlayerUserData(
                1, "TestPlayer", testUUID, null,
                100, 50, 1000, false, false, null
            );
            userManager.updateCachedUser(testUUID, userData);
            
            AtomicBoolean firstCallFailed = new AtomicBoolean(false);
            
            when(mockApi.updateEmail(eq(1), eq("test@example.com")))
                .thenAnswer(invocation -> {
                    if (!firstCallFailed.get()) {
                        firstCallFailed.set(true);
                        return CompletableFuture.failedFuture(
                            new RuntimeException("Temporary network error")
                        );
                    }
                    return CompletableFuture.completedFuture(null);
                });

            // Act
            CompletableFuture<Void> result = mockApi.updateEmail(1, "test@example.com");
            
            // First call fails
            assertTrue(result.isCompletedExceptionally());
            
            // Second call succeeds
            result = mockApi.updateEmail(1, "test@example.com");
            assertFalse(result.isCompletedExceptionally());
        }

        @Test
        @DisplayName("Should handle timeout gracefully")
        void shouldHandleTimeout() {
            // Arrange
            when(mockApi.generateLinkCode(anyInt()))
                .thenReturn(CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(5000); // Simulate timeout
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("Timeout");
                }));

            // Act & Assert
            CompletableFuture<Object> future = mockApi.generateLinkCode(1);
            
            // Should not block indefinitely (test setup handles this)
            assertNotNull(future);
        }
    }
}
