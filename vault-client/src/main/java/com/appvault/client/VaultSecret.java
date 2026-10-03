package com.appvault.client;

import java.lang.annotation.*;

/**
 * Annotation for injecting vault secrets directly into fields.
 * 
 * Usage:
 *   @VaultSecret(path = "database/credentials", key = "password")
 *   private String dbPassword;
 * 
 * The VaultSecretBeanPostProcessor will resolve and inject these values
 * after the application context is initialized.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VaultSecret {

    /**
     * The vault secret path (e.g., "database/credentials")
     */
    String path();

    /**
     * The secret key within the path (e.g., "password")
     */
    String key();

    /**
     * Default value if the secret cannot be fetched
     */
    String defaultValue() default "";
}
