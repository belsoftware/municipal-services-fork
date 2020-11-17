package org.egov.lams.web.controllers;

import java.util.List;

import javax.validation.Valid;

import org.egov.lams.model.SearchCriteria;
import org.egov.lams.service.LamsService;
import org.egov.lams.util.ResponseInfoFactory;
import org.egov.lams.web.models.LamsRequest;
import org.egov.lams.web.models.LamsResponse;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1")
public class LamsController {

	@Autowired
	private LamsService lamsService;

	@Autowired
	private ResponseInfoFactory responseInfoFactory;

	@PostMapping("/_create")
	public ResponseEntity<LamsResponse> create(@RequestBody LamsRequest lamsRequest) {
		System.out.println("in service");
		List<LeaseAgreementRenewal> leaseRenewal = lamsService.create(lamsRequest);
		LamsResponse response = LamsResponse.builder().leases(leaseRenewal).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(lamsRequest.getRequestInfo(), true))
                .build();
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	
	@RequestMapping(value = { "/_search"}, method = RequestMethod.POST)
    public ResponseEntity<LamsResponse> search(@RequestBody RequestInfoWrapper requestInfoWrapper,
                                                       @Valid @ModelAttribute SearchCriteria criteria,
                                                       @PathVariable(required = false) String servicename
            , @RequestHeader HttpHeaders headers) {
        List<LeaseAgreementRenewal> leaseAgreements = lamsService.search(criteria, requestInfoWrapper.getRequestInfo(), servicename, headers);
        LamsResponse response = LamsResponse.builder().leases(leaseAgreements).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(requestInfoWrapper.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
	
	@RequestMapping(value = { "/_update"}, method = RequestMethod.POST)
    public ResponseEntity<LamsResponse> update( @RequestBody LamsRequest lamsRequest,
                                                       @PathVariable(required = false) String servicename) {
        List<LeaseAgreementRenewal> licenses = lamsService.update(lamsRequest, servicename);

        LamsResponse response = LamsResponse.builder().leases(licenses).responseInfo(
                responseInfoFactory.createResponseInfoFromRequestInfo(lamsRequest.getRequestInfo(), true))
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
