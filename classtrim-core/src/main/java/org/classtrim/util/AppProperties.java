package org.classtrim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public final class AppProperties {
    private static final Logger log = LoggerFactory.getLogger(AppProperties.class);
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppProperties.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            } else {
                log.error("config.properties not found on classpath; using defaults");
            }
        } catch (Exception e) {
            log.error("Failed to load config.properties: {}", e.getMessage());
        }
    }

    private AppProperties() {}

    public static String getString(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isEmpty()) return sys;
        String val = PROPS.getProperty(key);
        if (val == null || val.isEmpty()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return val;
    }

    public static String getString(String key, String defaultValue) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isEmpty()) return sys;
        String val = PROPS.getProperty(key);
        return (val == null || val.isEmpty()) ? defaultValue : val;
    }
}


