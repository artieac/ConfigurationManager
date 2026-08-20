package com.alwaysmoveforward.configurationmanager.web.Models;

import jakarta.validation.constraints.NotBlank;

public record ConfigurationValueRequest(@NotBlank String value) {
}

