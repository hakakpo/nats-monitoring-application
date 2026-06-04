package com.natsmonitor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NatsConnectionOptionsFactoryTest {

    private final NatsConnectionOptionsFactory factory = new NatsConnectionOptionsFactory();

    @TempDir
    Path tempDir;

    @Test
    void shouldCreatePlainOptionsWhenTlsDisabled() {
        NatsMonitoringConfig config = new NatsMonitoringConfig();
        config.setServerUrl("nats://example:4222");
        config.setUsername("user");
        config.setPassword("secret");

        var options = factory.create(config);

        assertFalse(options.isTLSRequired());
        assertNotNull(options.getServers());
    }

    @Test
    void shouldRejectTlsEnabledWithoutStores() {
        NatsTlsConfig tls = new NatsTlsConfig();
        tls.setEnabled(true);
        NatsMonitoringConfig config = new NatsMonitoringConfig();
        config.setTls(tls);

        assertThrows(IllegalStateException.class, () -> factory.create(config));
    }

    @Test
    void shouldCreateTlsOptionsWhenTrustStoreConfigured() throws Exception {
        Path trustStorePath = tempDir.resolve("truststore.p12");
        createEmptyPkcs12Store(trustStorePath, "changeit");

        NatsTlsConfig tls = new NatsTlsConfig();
        tls.setEnabled(true);
        tls.setTrustStorePath(trustStorePath.toString());
        tls.setTrustStorePassword("changeit");

        NatsMonitoringConfig config = new NatsMonitoringConfig();
        config.setTls(tls);

        var options = factory.create(config);

        assertTrue(options.isTLSRequired());
        assertNotNull(options.getSslContext());
    }

    private void createEmptyPkcs12Store(Path path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password.toCharArray());
        try (OutputStream outputStream = java.nio.file.Files.newOutputStream(path)) {
            keyStore.store(outputStream, password.toCharArray());
        }
    }
}
