package org.egov.lams.web.models;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.common.contract.response.ResponseInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@ApiModel(description = "Contract class to send response. Array of lease items are used in case of search results or response for create, whereas single lease item is used for update")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2020-10-18T17:06:11.263+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LamsResponseMaster   {
        @JsonProperty("ResponseInfo")
        private ResponseInfo responseInfo = null;

        @JsonProperty("leases")
        @Valid
        private List<LeaseAgreementRenewalDetail> leases = null;


        public LamsResponseMaster addLeaseItem(LeaseAgreementRenewalDetail leaseItem) {
            if (this.leases == null) {
            this.leases = new ArrayList<>();
            }
        this.leases.add(leaseItem);
        return this;
        }

}

