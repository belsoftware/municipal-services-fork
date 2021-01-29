ALTER TABLE eg_ws_service ADD COLUMN IF NOT EXISTS motorinfo character varying(64);
ALTER TABLE eg_ws_service_audit ADD COLUMN IF NOT EXISTS motorinfo character varying(64);
ALTER TABLE eg_ws_connectionholder ADD COLUMN IF NOT EXISTS propertyownership character varying(64);
ALTER TABLE eg_ws_connection ADD COLUMN IF NOT EXISTS authorizedconnection character varying(64);
ALTER TABLE eg_ws_connection_audit ADD COLUMN IF NOT EXISTS authorizedconnection character varying(64);

