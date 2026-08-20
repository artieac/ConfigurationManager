package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.SystemHistoryEntry;

public record SystemHistoryViewModel(Long id, Long systemId, String systemName, String externalId, String description,
                                      String action, ChangeStampViewModel changed) {

    public static SystemHistoryViewModel from(SystemHistoryEntry entry) {
        return new SystemHistoryViewModel(entry.id(), entry.systemId(), entry.systemName(), entry.externalId(), entry.description(),
                entry.action().name(), ChangeStampViewModel.from(entry.changed()));
    }
}

