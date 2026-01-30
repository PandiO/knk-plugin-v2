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
import org.mockito.ArgumentCaptor;

import net.knightsandkings.knk.paper.config.KnkConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ChatCaptureManager.
 * Tests secure chat capture flows for account creation and merging.
 */
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
    @DisplayName("Account Create Flow Tests")
    class AccountCreateFlowTests {

        @Test
        @DisplayName("Should start account create flow and prompt for email")
        void shouldStartAccountCreateFlow() {
            // Arrange
            AtomicBoolean completeCalled = new AtomicBoolean(false);
            AtomicBoolean cancelCalled = new AtomicBoolean(false);

            // Act
            manager.startAccountCreateFlow(
                mockPlayer,
                data -> completeCalled.set(true),
                () -> cancelCalled.set(true)
            );

            // Assert
            assertTrue(manager.isCapturingChat(testUUID), "Session should be active");
            assertFalse(completeCalled.get(), "Complete should not be called yet");
            assertFalse(cancelCalled.get(), "Cancel should not be called yet");
            
            // Verify player received prompts
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(2)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Step 1/3") && msg.contains("email")));
        }

        @Test
        @DisplayName("Should accept valid email and move to password step")
        void shouldAcceptValidEmail() {
            // Arrange
            AtomicBoolean completeCalled = new AtomicBoolean(false);
            manager.startAccountCreateFlow(mockPlayer, data -> completeCalled.set(true), () -> {});

            // Act
            boolean handled = manager.handleChatInput(mockPlayer, "test@example.com");

            // Assert
            assertTrue(handled, "Input should be handled");
            assertTrue(manager.isCapturingChat(testUUID), "Session should still be active");
            
            // Verify moved to password step
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Step 2/3") && msg.contains("password")));
        }

        @Test
        @DisplayName("Should reject invalid email format")
        void shouldRejectInvalidEmail() {
            // Arrange
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});

            // Act
            manager.handleChatInput(mockPlayer, "not-an-email");

            // Assert
            assertTrue(manager.isCapturingChat(testUUID), "Session should still be active");
            
            // Verify error message sent
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("Invalid email")));
        }

        @Test
        @DisplayName("Should reject weak password (less than 8 characters)")
        void shouldRejectWeakPassword() {
            // Arrange
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
            manager.handleChatInput(mockPlayer, "test@example.com");

            // Act
            manager.handleChatInput(mockPlayer, "weak");

            // Assert
            assertTrue(manager.isCapturingChat(testUUID), "Session should still be active");
            
            // Verify error message sent
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("at least 8 characters")));
        }

        @Test
        @DisplayName("Should reject password confirmation mismatch")
        void shouldRejectPasswordMismatch() {
            // Arrange
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
            manager.handleChatInput(mockPlayer, "test@example.com");
            manager.handleChatInput(mockPlayer, "StrongPassword123");

            // Act
            manager.handleChatInput(mockPlayer, "DifferentPassword456");

            // Assert
            assertTrue(manager.isCapturingChat(testUUID), "Session should still be active");
            
            // Verify error message sent and reset to password step
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
            assertTrue(messageCaptor.getAllValues().stream()
                .anyMatch(msg -> msg.contains("don't match") || msg.contains("Starting over")));
        }

        @Test
        @DisplayName("Should complete flow with valid email and matching passwords")
        void shouldCompleteFlowSuccessfully() {
            // Arrange
            AtomicReference<Map<String, String>> capturedData = new AtomicReference<>();
            manager.startAccountCreateFlow(mockPlayer, capturedData::set, () -> {});

            // Act - Complete full flow
            manager.handleChatInput(mockPlayer, "test@example.com");
            manager.handleChatInput(mockPlayer, "StrongPassword123");
            manager.handleChatInput(mockPlayer, "StrongPassword123");

            // Assert
            assertFalse(manager.isCapturingChat(testUUID), "Session should be closed");
            assertNotNull(capturedData.get(), "Complete callback should be called");
            assertEquals("test@example.com", capturedData.get().get("email"));
            assertEquals("StrongPassword123", capturedData.get().get("password"));
        }

        @Test
        @DisplayName("Should cancel flow when user types 'cancel'")
        void shouldCancelFlowOnCancelCommand() {
            // Arrange
            AtomicBoolean completeCalled = new AtomicBoolean(false);
            AtomicBoolean cancelCalled = new AtomicBoolean(false);
            manager.startAccountCreateFlow(
                mockPlayer,
                data -> completeCalled.set(true),
                () -> cancelCalled.set(true)
            );

            // Act
            manager.handleChatInput(mockPlayer, "cancel");

            // Assert
            assertFalse(manager.isCapturingChat(testUUID), "Session should be closed");
            assertTrue(cancelCalled.get(), "Cancel callback should be called");
            assertFalse(completeCalled.get(), "Complete should not be called");
        }
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
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
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
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
            assertEquals(1, manager.getActiveSessionCount());

            // Act - Start second session
            Player player2 = mock(Player.class);
            when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player2.getName()).thenReturn("TestPlayer2");
            manager.startAccountCreateFlow(player2, data -> {}, () -> {});
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
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});

            // Act - Try to start another session
            manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});

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
                manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
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
                manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
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
                manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
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
                manager.startAccountCreateFlow(mockPlayer, data -> {}, () -> {});
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
