package com.alwaysmoveforward.configurationmanager.domainmodel;

import java.time.Instant;

/**
 * Who changed something and when — reused by both {@link ConfigurationSystem} and
 * {@link Configuration} for their created/updated stamps, and by {@link ConfigurationValueHistoryEntry}.
 */
public record ChangeMetadata(Long userId, String userDisplayName, Instant at) {
}

