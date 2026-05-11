CREATE TABLE IF NOT EXISTS lumen.services(
    service_name TEXT, 
    last_seen_ms BIGINT,
    PRIMARY KEY (service_name)
) WITH default_time_to_live = 2592000;