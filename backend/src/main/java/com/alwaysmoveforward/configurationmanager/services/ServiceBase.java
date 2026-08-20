package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;
import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;

import java.time.Instant;

public abstract class ServiceBase {

    protected ChangeMetadata changeMetadataFor(AuthenticatedPrincipal principal) {
        return new ChangeMetadata(principal.userId(), principal.displayName(), Instant.now());
    }
}

