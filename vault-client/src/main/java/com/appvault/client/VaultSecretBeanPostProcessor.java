package com.appvault.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Processes @VaultSecret annotations on bean fields.
 * 
 * After each bean is initialized, this processor scans for @VaultSecret annotations
 * and injects the corresponding secret values from the vault.
 * 
 * This provides a convenient alternative to using @Value with vault properties:
 * 
 *   // Instead of:
 *   @Value("${database.credentials.password}")
 *   private String password;
 * 
 *   // You can use:
 *   @VaultSecret(path = "database/credentials", key = "password")
 *   private String password;
 */
@Component
@ConditionalOnBean(VaultClient.class)
public class VaultSecretBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(VaultSecretBeanPostProcessor.class);

    private final VaultClient vaultClient;

    public VaultSecretBeanPostProcessor(VaultClient vaultClient) {
        this.vaultClient = vaultClient;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            VaultSecret annotation = field.getAnnotation(VaultSecret.class);
            if (annotation != null) {
                try {
                    String value = vaultClient.getSecret(annotation.path(), annotation.key());

                    if (value == null && !annotation.defaultValue().isEmpty()) {
                        value = annotation.defaultValue();
                    }

                    if (value != null) {
                        field.setAccessible(true);
                        field.set(bean, value);
                        log.debug("Injected @VaultSecret {}/{} into {}.{}",
                                annotation.path(), annotation.key(), beanName, field.getName());
                    } else {
                        log.warn("@VaultSecret {}/{} returned null for {}.{}",
                                annotation.path(), annotation.key(), beanName, field.getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to inject @VaultSecret into {}.{}: {}",
                            beanName, field.getName(), e.getMessage());
                }
            }
        }

        return bean;
    }
}
