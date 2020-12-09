package org.egov.lams.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class LRConstants {

	public static  final String businessService_LAMS = "LAMS";
	
    public static final String APPLICATION_TYPE_RENEWAL = "RENEWAL";
    
    public static final String APPLICATION_TYPE_EXTENSION = "EXTENSION";

    public static final String APPLICATION_TYPE_NEW = "NEW";
    
    public static final String LR_APPLIED = "APPLIED";
    
    public static final String ACTION_APPLY  = "APPLY";
    
    public static final String LR_COUNTER_EMPLOYEE = "LR_CEMP";
    
    public static final String ROLE_CITIZEN = "CITIZEN";

    public static final String ACTION_STATUS_APPLIED  = "APPLY_APPLIED";
    
    public static final String ACTION_STATUS_CITIZENREVIEW  = "SENDBACK_CITIZEN-REVIEW";
    
    public static final String ACTION_STATUS_APPROVED  = "APPROVE_APPROVED";
    
    public static final String ACTION_STATUS_REJECTED  = "REJECT_REJECTED";
    
    public static final String ACTION_STATUS_CEOEXAMINATION  = "CEO-EXAMINATION_CEO-EXAMINATION";
    
    public static final String ACTION_STATUS_DEOEXAMINATION  = "DEO-EXAMINATION_DEO-EXAMINATION";
    
    public static final String ACTION_STATUS_PDDEEXAMINATION  = "PDDE-EXAMINATION_PDDE-EXAMINATION";
    
    public static final String ACTION_STATUS_DGDEEXAMINATION  = "DGDE-EXAMINATION_DGDE-EXAMINATION";
    
    public static final String ACTION_STATUS_MODEXAMINATION  = "MOD-EXAMINATION_MOD-EXAMINATION";
    
    public static final String NOTIFICATION_APPLIED = "lams.lr.submit";
    
    public static final String NOTIFICATION_CITIZENREVIEW = "lams.lr.citizenreview";
    
    public static final String NOTIFICATION_APPROVED = "lams.lr.approved";
    
    public static final String NOTIFICATION_REJECTED = "lams.lr.rejected";
    
    public static final String NOTIFICATION_CEOEXAMINATION = "lams.lr.ceoexamination";
    
    public static final String NOTIFICATION_DEOEXAMINATION = "lams.lr.deoexamination";
    
    public static final String NOTIFICATION_PDDEEXAMINATION = "lams.lr.pddeexamination";
    
    public static final String NOTIFICATION_DGDEEXAMINATION = "lams.lr.dgdeexamination";
    
    public static final String NOTIFICATION_MODEXAMINATION = "lams.lr.modexamination";
    
    public static final String NOTIF_OWNER_NAME_KEY = "{OWNER_NAME}";
    
    public static final String NOTIFICATION_LOCALE = "en_IN";
    
    public static final String LEASE_RENEWAL_MODULE = "LAMS";

    public static final String LEASE_RENEWAL_MODULE_CODE = "LR";
    
    public static final String MODULE = "rainmaker-lr";
    
    public static final List<String> NOTIFICATION_CODES = Collections.unmodifiableList(Arrays.asList(NOTIFICATION_APPLIED,
    		NOTIFICATION_APPLIED ,NOTIFICATION_CITIZENREVIEW ,NOTIFICATION_APPROVED ,NOTIFICATION_REJECTED ,NOTIFICATION_CEOEXAMINATION
    		,NOTIFICATION_DEOEXAMINATION ,NOTIFICATION_PDDEEXAMINATION ,NOTIFICATION_DGDEEXAMINATION ,NOTIFICATION_MODEXAMINATION ));

	public static final String CITIZEN_SENDBACK_ACTION = "SENDBACK";
	
	public static final String STATUS_APPROVED  = "APPROVED";
	
	public static final String ACTION_APPROVE  = "APPROVE";
	
	public static final String STATUS_APPLIED  = "APPLIED";
	
	public static final String ACTION_REJECT  = "REJECT";
	
	public static final String STATUS_REJECTED  = "REJECTED";
}
