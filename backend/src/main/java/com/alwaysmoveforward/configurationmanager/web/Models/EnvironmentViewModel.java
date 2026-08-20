package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.Environment;

public record EnvironmentViewModel(Long id, Long systemId, String name, String externalId,
                                    ChangeStampViewModel created, ChangeStampViewModel updated) {

    public static EnvironmentViewModel from(Environment environment) {
        return new EnvironmentViewModel(environment.id(), environment.systemId(), environment.name(), environment.externalId(),
                ChangeStampViewModel.from(environment.created()), ChangeStampViewModel.from(environment.updated()));
    }
}

