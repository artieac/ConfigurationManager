package com.alwaysmoveforward.configurationmanager.domainmodel;

/**
 * The subset of the Auth0 userinfo/ID-token claims this application cares about.
 */
public record Auth0UserProfile(String subject, String email, String name) {
}

