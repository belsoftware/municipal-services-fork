/*
 * eChallan System
 * ### API Specs For eChallan System ### 1. Generate the new challan. 2. Update the details of existing challan 3. Search the existing challan 4. Generate the demand and bill for the challan amount so that collection can be done in online and offline mode. 
 *
 * OpenAPI spec version: 1.0.0
 * Contact: contact@egovernments.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package org.egov.echallan.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

/**
 * Representation of a address. Indiavidual APIs may choose to extend from this using allOf if more details needed to be added in their case. 
 */
@ApiModel(description = "Representation of a address. Indiavidual APIs may choose to extend from this using allOf if more details needed to be added in their case. ")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2018-09-18T17:06:11.263+05:30")

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address   {

        @Size(max=64)
        @JsonProperty("id")
        private String id;

        @Size(max=64)
        @JsonProperty("tenantId")
        private String tenantId = null;

        @Size(max=64)
        @JsonProperty("doorNo")
        private String doorNo = null;

        @JsonProperty("latitude")
        private Double latitude = null;

        @JsonProperty("longitude")
        private Double longitude = null;

        @Size(max=64)
        @JsonProperty("addressId")
        private String addressId = null;

        @Size(max=64)
        @JsonProperty("addressNumber")
        private String addressNumber = null;

        @Size(max=64)
        @JsonProperty("type")
        private String type = null;

        @JsonProperty("addressLine1")
        private String addressLine1 = null;

        @Size(max=256)
        @JsonProperty("addressLine2")
        private String addressLine2 = null;

        @Size(max=64)
        @JsonProperty("landmark")
        private String landmark = null;

        @Size(max=64)
        @JsonProperty("city")
        private String city = null;

        @Size(max=64)
        @JsonProperty("pincode")
        private String pincode = null;

        @Size(max=64)
        @JsonProperty("detail")
        private String detail = null;

        @Size(max=64)
        @JsonProperty("buildingName")
        private String buildingName = null;

        @Size(max=64)
        @JsonProperty("street")
        private String street = null;

        @Valid
        @JsonProperty("locality")
        private Boundary locality = null;


}