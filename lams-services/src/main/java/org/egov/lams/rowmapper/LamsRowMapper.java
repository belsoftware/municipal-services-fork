package org.egov.lams.rowmapper;


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.lams.util.LRConstants;
import org.egov.lams.web.models.AuditDetails;
import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.LeaseAgreementRenewalDetail;
import org.egov.tracer.model.CustomException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



@Component
public class LamsRowMapper  implements ResultSetExtractor<List<LeaseAgreementRenewal>> {
	
	@Autowired
    private ObjectMapper mapper;

	@Override
	public List<LeaseAgreementRenewal> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, LeaseAgreementRenewal> leaseAgreementMap = new LinkedHashMap<>();
		while (rs.next()) {
            String id = rs.getString("renewal_id");
            LeaseAgreementRenewal currentRenewal = leaseAgreementMap.get(id);

            if(currentRenewal == null){
                Long lastModifiedTime = rs.getLong("renewal_lastModifiedTime");
                if(rs.wasNull()){lastModifiedTime = null;}

                Long applicationDate = (Long) rs.getObject("applicationdate");
                AuditDetails auditdetails = AuditDetails.builder()
                        .createdBy(rs.getString("renewal_createdBy"))
                        .createdTime(rs.getLong("renewal_createdTime"))
                        .lastModifiedBy(rs.getString("renewal_lastModifiedBy"))
                        .lastModifiedTime(lastModifiedTime)
                        .build();
                currentRenewal = LeaseAgreementRenewal.builder().auditDetails(auditdetails)
                		.accountId(rs.getString("uuid"))
                		.applicationNumber(rs.getString("applicationnumber"))
                		.applicationDate(applicationDate)
                		.action(rs.getString("action"))
                		.tenantId(rs.getString("tenantid"))
                		.status(rs.getString("status"))
                		.filestoreid(rs.getString("filestoreid"))
                        .id(id)
                        .businessService(LRConstants.businessService_LAMS)
                        .build();
                leaseAgreementMap.put(id,currentRenewal);
            }
            addChildrenToProperty(rs, currentRenewal);

        }
        return new ArrayList<>(leaseAgreementMap.values());
	}

	private void addChildrenToProperty(ResultSet rs, LeaseAgreementRenewal currentRenewal) throws SQLException {
		
		String renewalDtlId=rs.getString("renewaldetail_id");
		AuditDetails auditDetails = AuditDetails.builder()
                .createdBy(rs.getString("renewaldetail_createdBy"))
                .createdTime(rs.getLong("renewaldetail_createdTime"))
                .lastModifiedBy(rs.getString("renewaldetail_lastModifiedBy"))
                .lastModifiedTime(rs.getLong("renewaldetail_createdTime"))
                .build();
		LeaseAgreementRenewalDetail detail = LeaseAgreementRenewalDetail.builder()
				.annualRent((Double) rs.getObject("annualrent"))
				.area((Double) rs.getObject("area"))
				.auditDetails(auditDetails)
				.id(renewalDtlId)
				.lesseAsPerGLR(rs.getString("lesse"))
				.surveyNo(rs.getString("surveyno"))
				.termExpiryDate((Long) rs.getObject("termexpirydate"))
				.termno(rs.getString("termno"))
				.build();
		currentRenewal.setLeaseDetails(detail);
	}}
