CREATE TABLE IF NOT EXISTS eg_sw_connection_taxhead
(
  id character varying(64) NOT NULL,
  taxhead character varying(200),
  amount numeric(12,2),
  connection_id character varying(64) NOT NULL,
  active character varying(64),
  additionalDetail JSONB,
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint,
  CONSTRAINT eg_sw_connection_taxhead_pkey PRIMARY KEY (id),
  CONSTRAINT eg_sw_connection_taxhead_fkey FOREIGN KEY (connection_id) REFERENCES eg_sw_connection (id)
);


CREATE TABLE IF NOT EXISTS eg_sw_connection_roadtype
(
  id character varying(64) NOT NULL,
  roadtype character varying(200),
  length numeric(12,2),
  breadth numeric(12,2),
  depth numeric(12,2),
  rate numeric(12,2),
  connection_id character varying(64) NOT NULL,
  active character varying(64),
  additionalDetail JSONB,
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint,
  CONSTRAINT eg_sw_connection_roadtype_pkey PRIMARY KEY (id),
  CONSTRAINT eg_sw_connection_roadtype_fkey FOREIGN KEY (connection_id) REFERENCES eg_sw_connection (id)
);