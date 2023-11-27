package com.example.yspringcore.web.utils;

import com.example.yspringcore.ioc.scan.PropertyResolver;
import com.example.yspringcore.ioc.utils.ClassPathUtils;
import com.example.yspringcore.ioc.utils.YamlUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;
@Slf4j
public class WebUtils {
    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0";
    static final String CONFIG_APP_YAML = "/application.yml";
    static final String CONFIG_APP_PROP = "/application.properties";
    /**
     * Try load property resolver from /application.yml or /application.properties.
     */
    public  static PropertyResolver createPropertyResolver(){
        final Properties props = new Properties();
        // try load application.yml:
        try {
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            log.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                // try load application.properties:
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, (input) -> {
                    log.info("load config: {}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);

    }

}
