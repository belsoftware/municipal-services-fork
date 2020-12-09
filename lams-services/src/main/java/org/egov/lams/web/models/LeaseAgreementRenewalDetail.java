package org.egov.lams.web.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Object holds the basic data for a Lease 
 */
@ApiModel(description = "A Object holds the basic data for a Lease")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2020-10-10T17:06:11.263+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaseAgreementRenewalDetail   {

        @JsonProperty("id")
        @Size(max=64)
        private String id;

        @NotNull
        @JsonProperty("surveyNo")
        @Size(max=64)
        private String surveyNo;

        @Size(max=64)
        @JsonProperty("termNo")
        private String termNo;
        
        @JsonProperty("located")
        private String located;
        
        @Size(max=64)
        @JsonProperty("category")
        private String category;

        @NotNull
        @Size(max=256)
        @JsonProperty("lesseAsPerGLR")
        private String lesseAsPerGLR;

        @NotNull
        @JsonProperty("area")
        private Double area;

        @JsonProperty("annualRent")
        private Double annualRent;

        @JsonProperty("termExpiryDate")
        private Long termExpiryDate;
        
        @JsonProperty("finalTermExpiryDate")
        private Long finalTermExpiryDate;

        @JsonProperty("surveyId")
        private String surveyId;
        
        @JsonProperty("applicationDocuments")
        @Valid
        private List<Document> applicationDocuments = null;

        @JsonProperty("verificationDocuments")
        @Valid
        private List<Document> verificationDocuments = null;

        @JsonProperty("additionalDetail")
        private JsonNode additionalDetail = null;


        @JsonProperty("auditDetails")
        private AuditDetails auditDetails = null;

        public LeaseAgreementRenewalDetail addApplicationDocumentsItem(Document applicationDocumentsItem) {
            if (this.applicationDocuments == null) {
            this.applicationDocuments = new ArrayList<>();
            }
            if(!this.applicationDocuments.contains(applicationDocumentsItem))
                this.applicationDocuments.add(applicationDocumentsItem);
            return this;
        }

        public LeaseAgreementRenewalDetail addVerificationDocumentsItem(Document verificationDocumentsItem) {
            if (this.verificationDocuments == null) {
            this.verificationDocuments = new ArrayList<>();
            }
            if(!this.verificationDocuments.contains(verificationDocumentsItem))
                this.verificationDocuments.add(verificationDocumentsItem);
            return this;
        }

}

