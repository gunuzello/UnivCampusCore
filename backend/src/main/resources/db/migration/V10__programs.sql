CREATE TABLE programs (
 id BIGSERIAL PRIMARY KEY, organization_id BIGINT NOT NULL REFERENCES organizations(id),
 title VARCHAR(200) NOT NULL, content TEXT NOT NULL, category VARCHAR(80) NOT NULL,
 tags VARCHAR(500), application_url VARCHAR(2000), deadline TIMESTAMPTZ NOT NULL,
 starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL, published BOOLEAN NOT NULL DEFAULT false,
 CHECK(starts_at < ends_at), CHECK(deadline <= ends_at)
);
