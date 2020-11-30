package org.egov.lams.web.models.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import lombok.NoArgsConstructor;
import org.egov.common.contract.request.User;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.lams.model.UserInfo;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UserResponse {
	
    @JsonProperty("responseInfo")
	ResponseInfo responseInfo;

    @JsonProperty("user")
    List<UserInfo> user;
}
