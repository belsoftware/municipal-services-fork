CREATE TABLE public.eg_lams_mst_office
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    officeid character varying(25) COLLATE pg_catalog."default" NOT NULL,
    officename character varying(128) COLLATE pg_catalog."default",
    officelevel character varying(25) COLLATE pg_catalog."default",
    shortname character varying(25) COLLATE pg_catalog."default",
    commandid character varying(25) COLLATE pg_catalog."default",
    parentdeoid character varying(25) COLLATE pg_catalog."default",
    emailid character varying(25) COLLATE pg_catalog."default",
    CONSTRAINT eg_lams_mst_office_pkey PRIMARY KEY (id)
)

CREATE TABLE public.eg_lams_property_location
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    location character varying(50) COLLATE pg_catalog."default",
    CONSTRAINT eg_lams_property_location_pkey PRIMARY KEY (id)
)


CREATE TABLE public.eg_lams_survey_no_details
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    mstofficeid character varying(64) COLLATE pg_catalog."default" NOT NULL,
    surveyno character varying(25) COLLATE pg_catalog."default" NOT NULL,
    lesse character varying(64) COLLATE pg_catalog."default",
    area real,
    ownedby character varying(25) COLLATE pg_catalog."default",
    propertylocationid character varying(64) COLLATE pg_catalog."default",
    CONSTRAINT eg_lams_survey_no_details_pkey PRIMARY KEY (id),
    CONSTRAINT fk_eg_lams_mst_office FOREIGN KEY (mstofficeid)
        REFERENCES public.eg_lams_mst_office (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID,
    CONSTRAINT fk_eg_lams_property_location FOREIGN KEY (propertylocationid)
        REFERENCES public.eg_lams_property_location (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
)


CREATE TABLE public.eg_lams_leaserenewal
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    accountid character varying(64) COLLATE pg_catalog."default",
    tenantid character varying(64) COLLATE pg_catalog."default",
    applicationnumber character varying(64) COLLATE pg_catalog."default",
    applicationdate bigint,
    action character varying(64) COLLATE pg_catalog."default",
    status character varying(64) COLLATE pg_catalog."default",
    createdby character varying(64) COLLATE pg_catalog."default",
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT eg_lams_leaserenewal_pkey PRIMARY KEY (id)
)

CREATE TABLE public.eg_lams_leaserenewaldetail
(
    id character varying(64) COLLATE pg_catalog."default" NOT NULL,
    surveyno character varying(25) COLLATE pg_catalog."default",
    termno character varying(25) COLLATE pg_catalog."default",
    termexpirydate bigint,
    annualrent real,
    leaserenewalid character varying(64) COLLATE pg_catalog."default",
    createdby character varying(64) COLLATE pg_catalog."default",
    lastmodifiedby character varying(64) COLLATE pg_catalog."default",
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT eg_lams_leaserenewaldetail_pkey PRIMARY KEY (id),
    CONSTRAINT fk_eg_lams_leaserenewal_id FOREIGN KEY (leaserenewalid)
        REFERENCES public.eg_lams_leaserenewal (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)


