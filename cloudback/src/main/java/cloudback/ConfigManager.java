package cloudback;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static cloudback.Logs.*;

@ApplicationScoped
public class ConfigManager {
    private final CompositeConfiguration config = new CompositeConfiguration();
    private final Map<String, Object> runtimeConfig = new HashMap<>();

    void init(@Observes StartupEvent ev) {
        config.addConfiguration(new MapConfiguration(runtimeConfig));
        config.addConfiguration(new SystemConfiguration());
        
        // Load properties from classpath
        String profile = System.getProperty("quarkus.profile", "dev");
        loadPropertiesFromClasspath("application.properties");
        loadPropertiesFromClasspath("application-" + profile + ".properties");
        
        // Set defaults
        if (!config.containsKey("cb.base-path")) {
            runtimeConfig.put("cb.base-path", ".");
        }
        
        log().debug("Configuration initialized");
    }

    private void loadPropertiesFromClasspath(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is != null) {
                PropertiesConfiguration props = new PropertiesConfiguration();
                props.read(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                config.addConfiguration(props);
                log().debugf("Loaded configuration from %s", filename);
            }
        } catch (Exception e) {
            log().debugf("Could not load %s: %s", filename, e.getMessage());
        }
    }

    public String getString(String key) {
        return config.getString(key);
    }

    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    public Path getPath(String key) {
        return Paths.get(getString(key));
    }

    public Path getPath(String key, String defaultValue) {
        return Paths.get(getString(key, defaultValue));
    }

    public void setProperty(String key, Object value) {
        runtimeConfig.put(key, value);
    }

    public Iterator<String> getKeys() {
        return config.getKeys();
    }

    public Iterator<String> getKeys(String prefix) {
        return config.getKeys(prefix);
    }
}
