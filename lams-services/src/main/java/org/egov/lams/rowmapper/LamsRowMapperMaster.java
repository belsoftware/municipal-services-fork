package org.egov.lams.rowmapper;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.egov.lams.web.models.LeaseAgreementRenewalDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;



@Component
public class LamsRowMapperMaster  implements ResultSetExtractor<List<LeaseAgreementRenewal>> {
	@Autowired
    private ObjectMapper mapper;
	
	@Override
	public List<LeaseAgreementRenewal> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, LeaseAgreementRenewal> leaseAgreementMap = new LinkedHashMap<>();
		while (rs.next()) {
            String id = rs.getString("survey_id");
            LeaseAgreementRenewal currentRenewal = leaseAgreementMap.get(id);

            if(currentRenewal == null){
                currentRenewal = LeaseAgreementRenewal.builder().build();
                LeaseAgreementRenewalDetail detail = LeaseAgreementRenewalDetail.builder()
    					.area(((Float) rs.getObject("area")).doubleValue())
    					.id(id)
    					.lesseAsPerGLR(rs.getString("lesse"))
    					.surveyNo(rs.getString("surveyno"))
    					.build();
    			currentRenewal.setLeaseDetails(detail);
                leaseAgreementMap.put(id,currentRenewal);
            }

        }
        return new ArrayList<>(leaseAgreementMap.values());
	}

	}
