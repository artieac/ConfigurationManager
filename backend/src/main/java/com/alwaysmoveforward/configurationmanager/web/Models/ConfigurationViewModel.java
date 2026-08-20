package com.alwaysmoveforward.configurationmanager.web.Models;

import com.alwaysmoveforward.configurationmanager.domainmodel.Configuration;
import com.alwaysmoveforward.configurationmanager.domainmodel.ConfigurationValue;

import java.util.List;

/** Deliberately has no value field — see ConfigurationValueViewModel for the reveal-only response shape. */
public record ConfigurationViewModel(Long id, Long systemId, String name, List<Long> valuesSetInEnvironmentIds,
                               ChangeStampViewModel created, ChangeStampViewModel updated) {

    public static ConfigurationViewModel from(Configuration secret, List<ConfigurationValue> values) {
        return new ConfigurationViewModel(secret.id(), secret.systemId(), secret.name(),
                values.stream().map(ConfigurationValue::environmentId).toList(),
                ChangeStampViewModel.from(secret.created()), ChangeStampViewModel.from(secret.updated()));
    }
}

