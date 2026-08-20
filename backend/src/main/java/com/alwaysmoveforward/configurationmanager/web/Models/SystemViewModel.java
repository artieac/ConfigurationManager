package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationSystem;

/** Created-only — see SystemHistoryViewModel for who changed what since. */
public record SystemViewModel(Long id, String name, String externalId, String description, ChangeStampViewModel created) {

    public static SystemViewModel from(ConfigurationSystem system) {
        return new SystemViewModel(system.id(), system.name(), system.externalId(), system.description(),
                ChangeStampViewModel.from(system.created()));
    }
}

