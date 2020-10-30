package org.egov.lams.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class LRConstants {

	public static  final String businessService_LAMS = "LAMS";
	
    public static final String APPLICATION_TYPE_RENEWAL = "RENEWAL";

    public static final String APPLICATION_TYPE_NEW = "NEW";
    
    public static final String LR_APPLIED = "APPLIED";
    
    public static final String LR_COUNTER_EMPLOYEE = "LR_CEMP";
    
    public static final String ROLE_CITIZEN = "CITIZEN";

    public static final String ACTION_STATUS_APPLIED  = "APPLY_APPLIED";
    
    public static final String NOTIFICATION_APPLIED = "lams.lr.submit";
    
    public static final String NOTIF_OWNER_NAME_KEY = "{OWNER_NAME}";
    
    public static final String NOTIFICATION_LOCALE = "en_IN";
    
    public static final String LEASE_RENEWAL_MODULE = "LAMS";

    public static final String LEASE_RENEWAL_MODULE_CODE = "LR";
    
    public static final String MODULE = "rainmaker-lr";
    
    public static final List<String> NOTIFICATION_CODES = Collections.unmodifiableList(Arrays.asList(NOTIFICATION_APPLIED));
}
