CREATE TABLE journal_event
(
    id             SERIAL,
    persistence_id VARCHAR(255)          NOT NULL,
    seq_nr         BIGINT UNSIGNED       NOT NULL,
    event          BLOB                  NOT NULL,
    deleted        BOOLEAN DEFAULT FALSE NOT NULL,
    PRIMARY KEY (persistence_id, seq_nr)
);

CREATE TABLE event_tag
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    journal_event_id BIGINT UNSIGNED NOT NULL,
    tag              VARCHAR(255)    NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS snapshot
(
    `persistence_id` VARCHAR(255)    NOT NULL,
    `seq_nr`         BIGINT UNSIGNED NOT NULL,
    `time`           BIGINT UNSIGNED NOT NULL,
    `snapshot`       BLOB            NOT NULL,
    PRIMARY KEY (persistence_id, seq_nr)
);
