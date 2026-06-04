package com.natsmonitor.config;

import io.nats.client.Options;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Objects;

@Component
public class NatsConnectionOptionsFactory {

    public Options create(NatsMonitoringConfig config) {
        Objects.requireNonNull(config, "config must not be null");

        var builder = new Options.Builder()
                .server(config.getServerUrl())
                .connectionName("nats-monitoring-application")
                .connectionTimeout(Duration.ofSeconds(5))
                .maxReconnects(-1);

        if (hasText(config.getUsername())) {
            builder.userInfo(config.getUsername(), config.getPassword() != null ? config.getPassword() : "");
        }

        if (config.getTls() != null && config.getTls().isEnabled()) {
            builder.sslContext(createSslContext(config.getTls()));
        }

        return builder.build();
    }

    SSLContext createSslContext(NatsTlsConfig tls) {
        Objects.requireNonNull(tls, "tls must not be null");
        if (!hasText(tls.getKeyStorePath()) && !hasText(tls.getTrustStorePath())) {
            throw new IllegalStateException("NATS TLS is enabled but no keyStorePath or trustStorePath is configured");
        }

        try {
            var context = SSLContext.getInstance(valueOrDefault(tls.getProtocol(), "TLS"));
            context.init(loadKeyManagers(tls), loadTrustManagers(tls), null);
            return context;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to create NATS SSL context: " + e.getMessage(), e);
        }
    }

    private KeyManager[] loadKeyManagers(NatsTlsConfig tls)
            throws IOException, GeneralSecurityException {
        if (!hasText(tls.getKeyStorePath())) {
            return null;
        }

        var keyStore = loadStore(tls.getKeyStorePath(), tls.getKeyStorePassword(), valueOrDefault(tls.getKeyStoreType(), "PKCS12"));
        var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passwordChars(valueOrDefault(tls.getKeyPassword(), tls.getKeyStorePassword())));
        return keyManagerFactory.getKeyManagers();
    }

    private TrustManager[] loadTrustManagers(NatsTlsConfig tls)
            throws IOException, GeneralSecurityException {
        if (!hasText(tls.getTrustStorePath())) {
            return null;
        }

        var trustStore = loadStore(tls.getTrustStorePath(), tls.getTrustStorePassword(), valueOrDefault(tls.getTrustStoreType(), "PKCS12"));
        var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }

    private KeyStore loadStore(String storePath, String password, String type)
            throws IOException, GeneralSecurityException {
        var keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = Files.newInputStream(Path.of(storePath))) {
            keyStore.load(inputStream, passwordChars(password));
        }
        return keyStore;
    }

    private char[] passwordChars(String password) {
        return password == null ? new char[0] : password.toCharArray();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
