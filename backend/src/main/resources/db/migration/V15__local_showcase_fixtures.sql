-- Local demonstration data has stable identifiers so startup never overwrites edited records.
CREATE TABLE local_showcase_fixtures (
 fixture_key VARCHAR(160) PRIMARY KEY,
 entity_type VARCHAR(40) NOT NULL,
 entity_id BIGINT,
 seeded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
