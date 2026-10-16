package com.sapan.chat.auth.initializer;

import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.sapan.chat.auth.utils.Utils;

public class SecurityEnvironmentInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String TAG = "SecurityEnvironmentInitializer";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String cloudEnv = environment.getProperty(Utils.KEY_CLOUD_ENV);
        
        if (cloudEnv == null || cloudEnv.isBlank()) {
            throw new IllegalStateException("STARTUP Error: 'CLOUD_ENV' environment property is missing.");
        }

        // programatically inject dynamic properties to the environment
        Map<String, Object> dynamicProperties = Map.of(
            Utils.APP_BOOTSTRAP_STATUS, "VERIFIED",
            Utils.APP_RUNTIME_TARGET, cloudEnv
        );

        environment
            .getPropertySources()
            .addLast(new MapPropertySource("securityDynamicDefaults", dynamicProperties));

        System.out.println(TAG + ": cloud envitoment verfied successfully.");
    }
}
