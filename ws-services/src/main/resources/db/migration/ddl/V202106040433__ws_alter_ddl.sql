ALTER TABLE eg_ws_connection ADD COLUMN  IF NOT EXISTS  proposedusagecategory VARCHAR(256);
ALTER TABLE eg_ws_connection_audit ADD COLUMN  IF NOT EXISTS  proposedusagecategory VARCHAR(256);
ALTER TABLE eg_ws_connection ADD COLUMN  IF NOT EXISTS  proposedsubusagecategory VARCHAR(256);
ALTER TABLE eg_ws_connection_audit ADD COLUMN  IF NOT EXISTS  proposedsubusagecategory VARCHAR(256);