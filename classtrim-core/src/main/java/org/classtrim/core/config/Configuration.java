package org.classtrim.core.config;

public interface Configuration {
    String getString(String key);

    String getString(String key, String defaultValue);
}
