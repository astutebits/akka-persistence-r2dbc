DROP TABLE IF EXISTS public.journal_event;
CREATE TABLE IF NOT EXISTS public.journal_event (
  ordering BIGSERIAL,
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  deleted BOOLEAN DEFAULT FALSE NOT NULL,
  message BYTEA NOT NULL,
  PRIMARY KEY(persistence_id, sequence_number)
);
CREATE UNIQUE INDEX journal_ordering_idx ON public.journal_event(ordering);

DROP TABLE IF EXISTS public.tag;
CREATE TABLE IF NOT EXISTS public.tag (
 id bigserial,
 event_id BIGINT NOT NULL,
 tag VARCHAR(255) NOT NULL
);

DROP TABLE IF EXISTS public.snapshot;
CREATE TABLE IF NOT EXISTS public.snapshot (
  persistence_id VARCHAR(255) NOT NULL,
  sequence_number BIGINT NOT NULL,
  timestamp BIGINT NOT NULL,
  snapshot BYTEA NOT NULL,
  CONSTRAINT pk_snapshot_persistence_id_sequence_number PRIMARY KEY(persistence_id, sequence_number)
);