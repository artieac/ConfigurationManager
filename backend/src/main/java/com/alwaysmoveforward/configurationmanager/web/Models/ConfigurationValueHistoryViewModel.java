package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValueHistoryEntry;

/** Who/what/when/which-environment only — never carries a value, encrypted or otherwise. */
public record ConfigurationValueHistoryViewModel(Long id, Long configurationId, Long systemId, Long environmentId,
                                           String configurationName, String systemName, String environmentName,
                                           String action, ChangeStampViewModel changed) {

    public static ConfigurationValueHistoryViewModel from(ConfigurationValueHistoryEntry entry) {
        return new ConfigurationValueHistoryViewModel(entry.id(), entry.configurationId(), entry.systemId(), entry.environmentId(),
                entry.configurationName(), entry.systemName(), entry.environmentName(), entry.action().name(),
                ChangeStampViewModel.from(entry.changed()));
    }
}

