package dev.drme.rugdar.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {

    private Log() {
    }

    public static Logger get(Class<?> type) {
        return LoggerFactory.getLogger(type);
    }
}
