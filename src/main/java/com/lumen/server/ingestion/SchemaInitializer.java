package com.lumen.server.ingestion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.datastax.oss.driver.api.core.CqlSession;

@Component
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true")
public class SchemaInitializer {

    private final CqlSession session;

    public SchemaInitializer(CqlSession session) {
        this.session = session;
        initialize();
    }

    private void initialize() {
        // Create keyspace
        session.execute(
            "CREATE KEYSPACE IF NOT EXISTS lumen " +
            "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3}"
        );

        // Create spans table
        session.execute(
            "CREATE TABLE IF NOT EXISTS lumen.spans (" +
            "trace_id TEXT, " +
            "start_time_nano BIGINT, " +
            "span_id TEXT, " +
            "parent_span_id TEXT, " +
            "service_name TEXT, " +
            "operation_name TEXT, " +
            "end_time_nano BIGINT, " +
            "duration_ms BIGINT, " +
            "attributes MAP<TEXT,TEXT>, " +
            "has_error BOOLEAN, " +
            "status TEXT, " +
            "PRIMARY KEY (trace_id, start_time_nano, span_id)" +
            ") WITH default_time_to_live = 604800"
        );

        // Create trace_index table
        session.execute(
            "CREATE TABLE IF NOT EXISTS lumen.trace_index (" +
            "service_name TEXT, " +
            "bucket BIGINT, " +
            "start_time_ms BIGINT, " +
            "trace_id TEXT, " +
            "duration_ms BIGINT, " +
            "has_error BOOLEAN, " +
            "root_operation TEXT, " +
            "PRIMARY KEY ((service_name, bucket), start_time_ms, trace_id)" +
            ") WITH CLUSTERING ORDER BY (start_time_ms DESC, trace_id ASC) " +
            "AND default_time_to_live = 604800"
        );

        // Create services table
        session.execute(
            "CREATE TABLE IF NOT EXISTS lumen.services (" +
            "service_name TEXT, " +
            "last_seen_ms BIGINT, " +
            "PRIMARY KEY (service_name)" +
            ") WITH default_time_to_live = 2592000"
        );

        System.out.println("Schema initialized successfully");
    }
}