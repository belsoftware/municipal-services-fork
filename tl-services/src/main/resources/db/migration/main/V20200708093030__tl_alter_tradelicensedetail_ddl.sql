ALTER TABLE eg_tl_tradelicensedetail
ADD COLUMN cbrNumber character varying(256),
ADD COLUMN approverDate  bigint,
ADD COLUMN selectedTradeType character varying(256)
