package com.interviewlab.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Forces springdoc's Try-it-out off in production, independent of SWAGGER_TRY_IT — a
 * misconfigured/forgotten SWAGGER_TRY_IT=true must not leave live "Execute" buttons on
 * a production Swagger UI. No-op (and safe) if swagger-ui is fully disabled via
 * SWAGGER_ENABLED=false, since SwaggerUiConfigProperties then never gets created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SwaggerProductionHardener {

    private final DeploymentProperties deploymentProperties;
    private final ObjectProvider<SwaggerUiConfigProperties> swaggerUiConfigProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void hardenForProduction() {
        if (!deploymentProperties.isProduction()) {
            return;
        }
        SwaggerUiConfigProperties properties = swaggerUiConfigProperties.getIfAvailable();
        if (properties == null) {
            return;
        }
        properties.setTryItOutEnabled(false);
        log.info("DEPLOYMENT_MODE=production — swagger-ui try-it-out forcibly disabled");
    }
}
