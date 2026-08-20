package com.alwaysmoveforward.configurationmanager.web.Models;

/** The only response shape in the API that ever carries a decrypted secret value. */
public record ConfigurationValueViewModel(Long configurationId, Long environmentId, String configurationName, String environmentName, String value) {
}

