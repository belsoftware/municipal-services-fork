package org.egov.lams.web.models;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.*;

import org.egov.lams.model.Citizen;
import org.egov.lams.util.LRConstants;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.annotations.ApiModel;

@ApiModel(description = "A Object holds the basic data for a Lease")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2018-10-10T17:06:11.263+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class  LeaseAgreementRenewal   {
        @Size(max=64)
        @JsonProperty("id")
        private String id = null;

        @NotNull
        @Size(max=64)
        @JsonProperty("tenantId")
        private String tenantId = null;

              

    public enum ApplicationTypeEnum {
        NEW(LRConstants.APPLICATION_TYPE_NEW),

        RENEWAL(LRConstants.APPLICATION_TYPE_RENEWAL);

        private String value;

        ApplicationTypeEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static ApplicationTypeEnum fromValue(String text) {
            for (ApplicationTypeEnum b : ApplicationTypeEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

        @JsonProperty("businessService")
        private String businessService;

        @JsonProperty("applicationType")
        private ApplicationTypeEnum applicationType = null;

        @JsonProperty("workflowCode")
        private String workflowCode = null;

        @NotNull
        @Size(max=64)
        @JsonProperty("surveyNo")
        private String surveyNo;

        @Size(max=64)
        @JsonProperty("applicationNumber")
        private String applicationNumber;

        @JsonProperty("applicationDate")
        private Long applicationDate;


        @NotNull
        @Size(max=64)
        @JsonProperty("action")
        private String action ;

        @JsonProperty("assignee")
        private List<String> assignee = null;

        @Valid
        @JsonProperty("wfDocuments")
        private List<Document> wfDocuments;

        @Size(max=64)
        @JsonProperty("status")
        private String status ;

        @Valid
        @NotNull
        @JsonProperty("leaseDetails")
        private LeaseAgreementRenewalDetail leaseDetails;

        @JsonProperty("auditDetails")
        private AuditDetails auditDetails;
        
        @JsonProperty("filestoreid")
        private String filestoreid = null;
        
        @Size(max=128)
        private String comment;
        
        @JsonProperty("citizen")
        @Valid
        private Citizen citizen;
        
        @JsonProperty("accountId")
        private String accountId;

        @JsonProperty("approvedDate")
        private Long approvedDate = null;
}

