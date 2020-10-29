package org.egov.lams.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;


@ApiModel(description = "Contract class to receive request. Array of lease items are used in case of create, whereas single lease item is used for update")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2020-10-10T17:06:11.263+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LamsRequest   {
        @JsonProperty("RequestInfo")
        private RequestInfo requestInfo = null;

        @JsonProperty("leases")
        @Valid
        private List<LeaseAgreementRenewal> leases = null;


        public LamsRequest addLicensesItem(LeaseAgreementRenewal leaseItem) {
            if (this.leases == null) {
            this.leases = new ArrayList<>();
            }
        this.leases.add(leaseItem);
        return this;
        }

}

