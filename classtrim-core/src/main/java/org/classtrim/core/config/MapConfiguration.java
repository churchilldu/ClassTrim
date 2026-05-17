package org.classtrim.core.config;

import java.util.Map;

public class MapConfiguration implements Configuration {
    private final Map<String, String> props;

    public MapConfiguration(Map<String, String> props) {
        this.props = props;
    }

    @Override
    public String getString(String key) {
        String value = props.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = props.get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
