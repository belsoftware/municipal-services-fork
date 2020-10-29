package org.egov.lams.service;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.lams.config.LamsConfiguration;
import org.egov.lams.models.Idgen.IdResponse;
import org.egov.lams.repository.IdGenRepository;
import org.egov.lams.repository.LamsRepository;
import org.egov.lams.util.CommonUtils;
import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.AuditDetails;
import org.egov.lams.web.models.LamsRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class EnrichmentService {

    private IdGenRepository idGenRepository;
    private LamsConfiguration config;
    private CommonUtils commUtils;
    private UserService userService;
    private LamsRepository lamsRepository;

    public void enrichCreateRequest(LamsRequest lamsRequset) {
        RequestInfo requestInfo = lamsRequset.getRequestInfo();
        String uuid = requestInfo.getUserInfo().getUuid();
        AuditDetails auditDetails = commUtils.getAuditDetails(uuid, true);
        lamsRequset.getLeases().forEach(leaseRenewals -> {
	        leaseRenewals.setAuditDetails(auditDetails);
	        leaseRenewals.setId(UUID.randomUUID().toString());
	        leaseRenewals.setStatus(LRConstants.LR_APPLIED);
	        leaseRenewals.setFilestoreid(null);
	        if (requestInfo.getUserInfo().getType().equalsIgnoreCase(LRConstants.ROLE_CITIZEN))
	        	leaseRenewals.setAccountId(requestInfo.getUserInfo().getUuid());
        });
        setIdgenIds(lamsRequset);
    }

    private List<String> getIdList(RequestInfo requestInfo, String tenantId, String idKey,
                                   String idformat, int count) {
        List<IdResponse> idResponses = idGenRepository.getId(requestInfo, tenantId, idKey, idformat, count).getIdResponses();

        if (CollectionUtils.isEmpty(idResponses))
            throw new CustomException("IDGEN ERROR", "No ids returned from idgen Service");

        return idResponses.stream()
                .map(IdResponse::getId).collect(Collectors.toList());
    }

    private void setIdgenIds(LamsRequest request) {
        RequestInfo requestInfo = request.getRequestInfo();
        String tenantId = request.getLeases().get(0).getTenantId();
        request.getLeases().forEach(lease -> {
        	String applicationNumber = getIdList(requestInfo, tenantId, config.getLamsLRApplNumIdgenName(), config.getLamsLRApplNumIdgenFormat(), 1).get(0);
        	lease.setApplicationNumber(applicationNumber);
        });
    }

}
