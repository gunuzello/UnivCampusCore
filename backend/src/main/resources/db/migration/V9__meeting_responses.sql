CREATE TABLE meeting_responses (
 id BIGSERIAL PRIMARY KEY,
 meeting_id BIGINT NOT NULL REFERENCES meetings(id),
 user_id BIGINT NOT NULL REFERENCES app_users(id),
 status VARCHAR(30) NOT NULL CHECK (status IN ('GOING','NOT_GOING','UNDECIDED')),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE (meeting_id,user_id)
);
