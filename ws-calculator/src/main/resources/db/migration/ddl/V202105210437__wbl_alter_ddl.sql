ALTER TABLE eg_ws_failed_bill ALTER COLUMN lastreading DROP NOT NULL;
ALTER TABLE eg_ws_failed_bill ALTER COLUMN fromdate DROP NOT NULL;
ALTER TABLE eg_ws_failed_bill ALTER COLUMN currentreading DROP NOT NULL;
ALTER TABLE eg_ws_failed_bill ALTER COLUMN todate DROP NOT NULL;
ALTER TABLE eg_ws_failed_bill ALTER COLUMN lastreading DROP NOT NULL;
ALTER TABLE public.eg_ws_failed_bill ALTER COLUMN connectionno SET NOT NULL;
ALTER TABLE public.eg_ws_failed_bill ALTER COLUMN tenantid SET NOT NULL;