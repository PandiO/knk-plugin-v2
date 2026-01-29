package net.knightsandkings.knk.paper.chat;

/**
 * Enum representing different chat capture flows.
 * Each flow represents a different account management scenario.
 */
public enum CaptureFlow {
    /**
     * Account creation flow: Email → Password → Password Confirm
     */
    ACCOUNT_CREATE,
    
    /**
     * Account merge flow: Display accounts → Choice (A or B)
     */
    ACCOUNT_MERGE
}
