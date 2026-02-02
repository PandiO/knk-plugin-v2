package net.knightsandkings.knk.paper.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;

import net.knightsandkings.knk.paper.config.KnkConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ChatCaptureManager.
 * Tests secure chat capture flows for account creation and merging.
 */
@ExtendWith(MockitoExtension.class)
@Tag("requires-bukkit")
class ChatCaptureManagerTest {

    private ChatCaptureManager manager;
    private JavaPlugin mockPlugin;
    private KnkConfig mockConfig;
    private Logger mockLogger;
    private Player mockPlayer;
    private UUID testUUID;

    @BeforeEach
    void setUp() {
        mockPlugin = mock(JavaPlugin.class);
        mockConfig = mock(KnkConfig.class);
        mockLogger = mock(Logger.class);
        mockPlayer = mock(Player.class);
        
        testUUID = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(testUUID);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        
        // Mock config messages
        KnkConfig.MessagesConfig messagesConfig = mock(KnkConfig.MessagesConfig.class);
        when(messagesConfig.prefix()).thenReturn("§8[§6KnK§8]§r ");
        when(mockConfig.messages()).thenReturn(messagesConfig);
        
        // Mock account config
        KnkConfig.AccountConfig accountConfig = mock(KnkConfig.AccountConfig.class);
        when(accountConfig.chatCaptureTimeoutSeconds()).thenReturn(120);
        when(mockConfig.account()).thenReturn(accountConfig);
        
        manager = new ChatCaptureManager(mockPlugin, mockConfig, mockLogger);
    }

    @Nested
    @DisplayName("Account Merge Flow Tests")
    class AccountMergeFlowTests {

        @Test
        @DisplayName("Should start merge flow and display account comparison")
        void shouldStartMergeFlow() {
            // Arrange
            AtomicBoolean completeCalled = new AtomicBoolean(false);

            // Act
            manager.startMergeFlow(
                mockPlayer,
                100, 50, 1000, "account-a@example.com",
                200, 75, 2000, null,
                data -> completeCalled.set(true),
                () -> {}
            );

            // Assert
            assertTrue(manager.isCapturingChat(testUUID), "Session should be active");
            
            // Verify account comparison displayed
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(5)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Account Merge Required")));
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Account A")));
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Account B")));
        }

        @Test
        @DisplayName("Should accept choice 'A' and complete merge flow")
        void shouldAcceptChoiceA() {
            // Arrange
            AtomicReference<Map<String, String>> capturedData = new AtomicReference<>();
            manager.startMergeFlow(
                mockPlayer,
                100, 50, 1000, "account-a@example.com",
                200, 75, 2000, null,
                capturedData::set,
                () -> {}
            );

            // Act
            manager.handleChatInput(mockPlayer, "A");

            // Assert
            assertFalse(manager.isCapturingChat(testUUID), "Session should be closed");
            assertNotNull(capturedData.get(), "Complete callback should be called");
            assertEquals("A", capturedData.get().get("choice"));
        }

        @Test
        @DisplayName("Should accept choice 'B' and complete merge flow")
        void shouldAcceptChoiceB() {
            // Arrange
            AtomicReference<Map<String, String>> capturedData = new AtomicReference<>();
            manager.startMergeFlow(
                mockPlayer,
                100, 50, 1000, "account-a@example.com",
                200, 75, 2000, null,
                capturedData::set,
                () -> {}
            );

            // Act
            manager.handleChatInput(mockPlayer, "B");

            // Assert
            assertFalse(manager.isCapturingChat(testUUID), "Session should be closed");
            assertNotNull(capturedData.get(), "Complete callback should be called");
            assertEquals("B", capturedData.get().get("choice"));
        }

        @Test
        @DisplayName("Should reject invalid choice (not A or B)")
        void shouldRejectInvalidChoice() {
            // Arrange
            manager.startMergeFlow(
                mockPlayer,
                100, 50, 1000, "account-a@example.com",
                200, 75, 2000, null,
                data -> {},
                () -> {}
            );

            // Act
            manager.handleChatInput(mockPlayer, "C");

            // Assert
            assertTrue(manager.isCapturingChat(testUUID), "Session should still be active");
            
            // Verify error message sent
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("type 'A' or 'B'")));
        }

        @Test
        @DisplayName("Should accept lowercase choices")
        void shouldAcceptLowercaseChoices() {
            // Arrange
            AtomicReference<Map<String, String>> capturedData = new AtomicReference<>();
            manager.startMergeFlow(
                mockPlayer,
                100, 50, 1000, "account-a@example.com",
                200, 75, 2000, null,
                capturedData::set,
                () -> {}
            );

            // Act
            manager.handleChatInput(mockPlayer, "a");

            // Assert
            assertFalse(manager.isCapturingChat(testUUID), "Session should be closed");
            assertEquals("A", capturedData.get().get("choice"), "Choice should be uppercase");
        }
    }

    @Nested
    @DisplayName("Session Management Tests")
    class SessionManagementTests {

        @Test
        @DisplayName("Should return false for isCapturingChat when no session active")
        void shouldReturnFalseWhenNoSession() {
            assertFalse(manager.isCapturingChat(testUUID));
        }

        @Test
        @DisplayName("Should handle chat input only for active sessions")
        void shouldOnlyHandleActiveSessionInput() {
            // Arrange
            UUID otherUUID = UUID.randomUUID();
            Player otherPlayer = mock(Player.class);
            when(otherPlayer.getUniqueId()).thenReturn(otherUUID);

            // Act
            boolean handled = manager.handleChatInput(otherPlayer, "test");

            // Assert
            assertFalse(handled, "Input should not be handled without active session");
        }

        @Test
        @DisplayName("Should clear all sessions")
        void shouldClearAllSessions() {
            // Arrange
            manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
            assertTrue(manager.isCapturingChat(testUUID));

            // Act
            manager.clearAllSessions();

            // Assert
            assertFalse(manager.isCapturingChat(testUUID));
            assertEquals(0, manager.getActiveSessionCount());
        }

        @Test
        @DisplayName("Should track active session count")
        void shouldTrackActiveSessionCount() {
            // Arrange
            assertEquals(0, manager.getActiveSessionCount());

            // Act - Start first session
            manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
            assertEquals(1, manager.getActiveSessionCount());

            // Act - Start second session
            Player player2 = mock(Player.class);
            when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player2.getName()).thenReturn("TestPlayer2");
            manager.startMergeFlow(player2, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
            assertEquals(2, manager.getActiveSessionCount());

            // Act - Complete first session
            manager.handleChatInput(mockPlayer, "test@example.com");
            manager.handleChatInput(mockPlayer, "Password123");
            manager.handleChatInput(mockPlayer, "Password123");
            assertEquals(1, manager.getActiveSessionCount());
        }

        @Test
        @DisplayName("Should prevent multiple simultaneous sessions for same player")
        void shouldPreventMultipleSessions() {
            // Arrange
            manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});

            // Act - Try to start another session
            manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});

            // Assert - Should still be 1 session (second call overwrites first)
            assertEquals(1, manager.getActiveSessionCount());
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmails() {
            String[] validEmails = {
                "test@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user_123@sub.example.com",
                "UPPERCASE@EXAMPLE.COM"
            };

            for (String email : validEmails) {
                manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
                manager.handleChatInput(mockPlayer, email);
                
                // If email is valid, should move to password step
                ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockPlayer, atLeastOnce()).sendMessage(messageCaptor.capture());
                
                boolean movedToPasswordStep = messageCaptor.getAllValues().stream()
                    .anyMatch(msg -> msg.contains("Step 2/3"));
                
                assertTrue(movedToPasswordStep, "Email should be accepted: " + email);
                
                // Clean up for next iteration
                manager.clearAllSessions();
                reset(mockPlayer);
                when(mockPlayer.getUniqueId()).thenReturn(testUUID);
                when(mockPlayer.getName()).thenReturn("TestPlayer");
            }
        }

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmails() {
            String[] invalidEmails = {
                "not-an-email",
                "@example.com",
                "user@",
                "user@.com",
                "user@domain",
                "user domain@example.com",
                ""
            };

            for (String email : invalidEmails) {
                manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
                manager.handleChatInput(mockPlayer, email);
                
                // Session should still be active (email rejected)
                assertTrue(manager.isCapturingChat(testUUID), 
                    "Session should remain active for invalid email: " + email);
                
                // Clean up for next iteration
                manager.clearAllSessions();
                reset(mockPlayer);
                when(mockPlayer.getUniqueId()).thenReturn(testUUID);
                when(mockPlayer.getName()).thenReturn("TestPlayer");
            }
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should accept passwords with 8+ characters")
        void shouldAcceptValidPasswords() {
            String[] validPasswords = {
                "Password123",
                "12345678",
                "!@#$%^&*",
                "LongPasswordWithSpecialChars!123"
            };

            for (String password : validPasswords) {
                manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
                manager.handleChatInput(mockPlayer, "test@example.com");
                manager.handleChatInput(mockPlayer, password);
                
                // Should move to confirmation step
                ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockPlayer, atLeastOnce()).sendMessage(messageCaptor.capture());
                
                boolean movedToConfirmStep = messageCaptor.getAllValues().stream()
                    .anyMatch(msg -> msg.contains("Step 3/3"));
                
                assertTrue(movedToConfirmStep, "Password should be accepted: " + password);
                
                // Clean up
                manager.clearAllSessions();
                reset(mockPlayer);
                when(mockPlayer.getUniqueId()).thenReturn(testUUID);
                when(mockPlayer.getName()).thenReturn("TestPlayer");
            }
        }

        @Test
        @DisplayName("Should reject passwords with less than 8 characters")
        void shouldRejectWeakPasswords() {
            String[] weakPasswords = {
                "Pass123",
                "1234567",
                "short",
                ""
            };

            for (String password : weakPasswords) {
                manager.startMergeFlow(mockPlayer, 0, 0, 0, null, 0, 0, 0, null, data -> {}, () -> {});
                manager.handleChatInput(mockPlayer, "test@example.com");
                manager.handleChatInput(mockPlayer, password);
                
                // Session should still be active (password rejected)
                assertTrue(manager.isCapturingChat(testUUID), 
                    "Session should remain active for weak password: " + password);
                
                // Clean up
                manager.clearAllSessions();
                reset(mockPlayer);
                when(mockPlayer.getUniqueId()).thenReturn(testUUID);
                when(mockPlayer.getName()).thenReturn("TestPlayer");
            }
        }
    }
}
