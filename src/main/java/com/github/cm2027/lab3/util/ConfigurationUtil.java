package com.github.cm2027.lab3.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationUtil {

    protected static final String DEFAULT_CONFIG_FILE = "application.properties";
    protected static Properties properties = new Properties();

    static {
        load(DEFAULT_CONFIG_FILE);
    }

    private ConfigurationUtil() {
    }

    private static void load(String fileName) {
        try (InputStream input = ConfigurationUtil.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Configuration file '" + fileName + "' not found in classpath.");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load configuration file: " + fileName, ex);
        }
    }

    public static String getString(String key) {
        return properties.getProperty(key);
    }

    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    public static void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public static void reload() {
        properties.clear();
        load(DEFAULT_CONFIG_FILE);
    }
}
