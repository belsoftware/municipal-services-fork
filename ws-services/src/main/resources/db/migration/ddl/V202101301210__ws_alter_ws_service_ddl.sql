ALTER TABLE eg_ws_service ADD COLUMN IF NOT EXISTS sourceinfo character varying(64);

ALTER TABLE eg_ws_service_audit ADD COLUMN IF NOT EXISTS sourceinfo character varying(64);