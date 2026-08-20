package com.alwaysmoveforward.configurationmanager.web.Models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Names a secret only — see ConfigurationValueRequest for setting its value in a specific environment. */
public record ConfigurationRequest(@NotBlank @Size(max = 255) String name) {
}

