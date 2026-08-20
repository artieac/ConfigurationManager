package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.ChangeMetadata;

import java.time.Instant;

public record ChangeStampViewModel(Long userId, String userDisplayName, Instant at) {

    public static ChangeStampViewModel from(ChangeMetadata metadata) {
        return new ChangeStampViewModel(metadata.userId(), metadata.userDisplayName(), metadata.at());
    }
}

