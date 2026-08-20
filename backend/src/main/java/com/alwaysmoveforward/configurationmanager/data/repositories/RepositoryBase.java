package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.exceptions.NotFoundException;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class RepositoryBase {

    protected <T> T orNotFound(Optional<T> value, Supplier<String> message) {
        return value.orElseThrow(() -> new NotFoundException(message.get()));
    }
}

