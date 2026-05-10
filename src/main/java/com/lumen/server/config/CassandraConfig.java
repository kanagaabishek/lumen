package com.lumen.server.config;

import java.net.InetSocketAddress;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class CassandraConfig {

    @Bean
    @ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true")
    public CqlSession cqlSession(
            @Value("${CASSANDRA_CONTACT_POINTS:localhost}") String contactPoints,
            @Value("${CASSANDRA_PORT:9042}") int port,
            @Value("${CASSANDRA_DC:datacenter1}") String dc) {
        CqlSession session = CqlSession.builder()
            .addContactPoint(new InetSocketAddress(contactPoints, port))
            .withLocalDatacenter(dc)
            .build();

        session.execute(
            "CREATE KEYSPACE IF NOT EXISTS lumen " +
            "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}"
        );

        session.execute(
            "CREATE TABLE IF NOT EXISTS lumen.spans (" +
            "trace_id TEXT, start_time_nano BIGINT, span_id TEXT, " +
            "parent_span_id TEXT, service_name TEXT, operation_name TEXT, " +
            "end_time_nano BIGINT, duration_ms BIGINT, " +
            "attributes MAP<TEXT,TEXT>, has_error BOOLEAN, status TEXT, " +
            "PRIMARY KEY (trace_id, start_time_nano, span_id)" +
            ") WITH default_time_to_live = 604800"
        );
        session.execute(
            "CREATE TABLE IF NOT EXISTS lumen.trace_index (" +
            "service_name TEXT, bucket BIGINT, start_time_ms BIGINT, " +
            "trace_id TEXT, duration_ms BIGINT, has_error BOOLEAN, root_operation TEXT, " +
            "PRIMARY KEY ((service_name, bucket), start_time_ms, trace_id)" +
            ") WITH CLUSTERING ORDER BY (start_time_ms DESC, trace_id ASC) " +
            "AND default_time_to_live = 604800"
        );
        session.execute(
            "CREATE TABLE IF NOT EXISTS lumen.services (" +
            "service_name TEXT, last_seen_ms BIGINT, " +
            "PRIMARY KEY (service_name)" +
            ") WITH default_time_to_live = 2592000"
        );

        System.out.println("Schema initialized successfully");

        session.close();

        return CqlSession.builder()
            .addContactPoint(new InetSocketAddress(contactPoints, port))
            .withLocalDatacenter(dc)
            .withKeyspace("lumen")  // keyspace now exists
            .build();
    }
}
