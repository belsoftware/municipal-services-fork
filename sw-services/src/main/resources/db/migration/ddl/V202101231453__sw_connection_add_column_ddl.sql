ALTER TABLE eg_sw_service ADD COLUMN IF NOT EXISTS drainagesize decimal ;
ALTER TABLE eg_sw_service ADD COLUMN IF NOT EXISTS proposeddrainagesize decimal;
ALTER TABLE eg_sw_service_audit ADD COLUMN IF NOT EXISTS drainagesize decimal;
ALTER TABLE eg_sw_service_audit ADD COLUMN IF NOT EXISTS drainagesize decimal;