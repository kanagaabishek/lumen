package com.lumen.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.datastax.oss.driver.api.core.CqlSession;

@Configuration
public class CassandraConfig {

    @Bean
    @ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true", matchIfMissing = false)
    public CqlSession cqlSession() {
        return CqlSession.builder()
            .withLocalDatacenter("datacenter1")
            .build();
    }
}
