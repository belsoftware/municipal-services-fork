CREATE TABLE IF NOT EXISTS eg_ws_failed_bill
(
  id character varying(64),
  connectionno character varying(64),
  applicationno character varying(64),
  assessmentyear character varying(64) NOT NULL, 
  lastreading decimal NOT NULL,
  fromdate bigint NOT NULL,
  currentreading decimal NOT NULL,
  todate bigint NOT NULL,
  consumption decimal,
  createdby character varying(64),
  lastmodifiedby character varying(64),
  createdtime bigint,
  lastmodifiedtime bigint,
  tenantid character varying(64),
  reason character varying(1000),
  status character varying(64),
  CONSTRAINT uk_eg_ws_failed_bill UNIQUE (id)
);

CREATE INDEX IF NOT EXISTS index_eg_ws_failed_bill_tenantId ON eg_ws_meterreading (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_ws_failed_bill_connectionNo ON eg_ws_meterreading (connectionno);