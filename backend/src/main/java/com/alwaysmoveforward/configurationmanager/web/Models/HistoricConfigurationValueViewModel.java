package com.alwaysmoveforward.configurationmanager.web.Models;

/** The decrypted value as of one specific history entry — revealed on demand, like ConfigurationValueViewModel. */
public record HistoricConfigurationValueViewModel(Long historyId, String value) {
}

