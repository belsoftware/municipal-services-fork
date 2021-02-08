package org.egov.lams.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.egov.tracer.config.TracerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.TimeZone;


@Import({TracerConfiguration.class})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Component
public class LamsConfiguration {


    @Value("${app.timezone}")
    private String timeZone;

    @PostConstruct
    public void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    }

    @Bean
    @Autowired
    public MappingJackson2HttpMessageConverter jacksonConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    // User Config
    @Value("${egov.user.host}")
    private String userHost;

    @Value("${egov.user.context.path}")
    private String userContextPath;

    @Value("${egov.user.create.path}")
    private String userCreateEndpoint;

    @Value("${egov.user.search.path}")
    private String userSearchEndpoint;

    @Value("${egov.user.update.path}")
    private String userUpdateEndpoint;

    @Value("${egov.user.username.prefix}")
    private String usernamePrefix;

    @Value("${egov.collectionservice.host}")
    private String collectionserviceHost;
    
    @Value("${egov.tlservice.path}")
    private String tlserviceHost;
    
    @Value("${egov.waterService.host}")
    private String waterServiceHost;
    
    @Value("${egov.dss.limit}")
    private Integer dssSearchLimit;
    
    @Value("${egov.dss.offset}")
    private Integer dssSearchOffset;
    
    @Value("${egov.dss.collectionsBreakingLimit}")
    private Integer collectionsBreakingLimit;
    
    @Value("${egov.dss.tlBreakingLimit}")
    private Integer tlBreakingLimit;
    
    @Value("${egov.dss.waterBreakingLimit}")
    private Integer waterBreakingLimit;
    
    @Value("${egov.dss.sewerageBreakingLimit}")
    private Integer sewerageBreakingLimit;
    
    @Value("${egov.dss.leaseBreakingLimit}")
    private Integer leaseBreakingLimit;
    
    @Value("${egov.elasticSearch.path}")
    private String elasticSearch;

    //org.egov.lams.models.Idgen Config
    @Value("${egov.idgen.host}") 
    private String idGenHost;

    @Value("${egov.idgen.path}")
    private String idGenPath;

    @Value("${egov.idgen.lamsLRApplNum.name}")
    private String lamsLRApplNumIdgenName;

    @Value("${egov.idgen.lamsLRApplNum.format}")
    private String lamsLRApplNumIdgenFormat;


    //Persister Config
    @Value("${persister.save.lamsLR.topic}")
    private String saveLamsLRTopic;

    @Value("${persister.update.lamsLR.topic}")
    private String updateLamsLRTopic;
    
    @Value("${persister.update.lamsLR.workflow.topic}")
    private String updateLamsLRWorkflowTopic;

    @Value("${persister.update.lamssurvey.topic}")
    private String updateLamsSurveyTopic;

    // Workflow
    
    @Value("${workflow.context.path}")
    private String wfHost;

    @Value("${workflow.transition.path}")
    private String wfTransitionPath;

    @Value("${workflow.businessservice.search.path}")
    private String wfBusinessServiceSearchPath;


    @Value("${is.external.workflow.enabled}")
    private Boolean isExternalWorkFlowEnabled;
    
    @Value("${create.lams.workflow.name}")
    private String lamsBusinessServiceValue;

    //Localization
    @Value("${egov.localization.host}")
    private String localizationHost;

    @Value("${egov.localization.context.path}")
    private String localizationContextPath;

    @Value("${egov.localization.search.endpoint}")
    private String localizationSearchEndpoint;

    @Value("${egov.localization.statelevel}")
    private Boolean isLocalizationStateLevel;

    //MDMS
    @Value("${egov.mdms.host}")
    private String mdmsHost;

    @Value("${egov.mdms.search.endpoint}")
    private String mdmsEndPoint;

    @Value("${egov.lams.default.limit}")
    private Integer defaultLamsLimit;

    @Value("${egov.lams.default.offset}")
    private Integer defaultOffset;

    @Value("${egov.lams.max.limit}")
    private Integer maxSearchLimit;
    
    @Value("${kafka.topics.notification.sms}")
    private String smsNotifTopic;

    @Value("${notification.sms.enabled}")
    private Boolean isSMSEnabled;
    
    @Value("${egov.propertyService.host}")
    private String propertyServiceHost;
    
    @Value("${egov.locationService.host}")
    private String locationServiceHost;
    
    
    @Value("${egov.dss.pgrServiceSearchLimit}")
    private Integer pgrServiceSearchLimit;
    
    @Value("${egov.dss.pgrBreakingLimit}")
    private Integer pgrBreakingLimit;
    
    @Value("${egov.dss.leaseServiceSearchLimit}")
    private Integer leaseServiceSearchLimit;
    
    @Value("${egov.dss.sewerageServiceSearchLimit}")
    private Integer sewerageServiceSearchLimit;
    
    @Value("${egov.dss.waterServiceSearchLimit}")
    private Integer waterServiceSearchLimit;
    
    @Value("${egov.dss.collectionsServiceSearchLimit}")
    private Integer collectionsServiceSearchLimit;
    
    @Value("${egov.dss.tlServiceSearchLimit}")
    private Integer tlServiceSearchLimit;
    
    @Value("${egov.sewerageService.host}")
    private String sewerageServiceHost;
    
  
}
