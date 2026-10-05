-- Apply once after V002; invalidate snapshots after member status transitions.
BEGIN;
ALTER TABLE member ADD COLUMN authentication_version bigint NOT NULL DEFAULT 0;
INSERT INTO schema_version(version, description) VALUES ('003', 'Member authentication version');
COMMIT;
