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
public class LamsRowMapperMaster  implements ResultSetExtractor<List<LeaseAgreementRenewalDetail>> {
	@Autowired
    private ObjectMapper mapper;
	
	@Override
	public List<LeaseAgreementRenewalDetail> extractData(ResultSet rs) throws SQLException, DataAccessException {
		Map<String, LeaseAgreementRenewalDetail> leaseAgreementMap = new LinkedHashMap<>();
		while (rs.next()) {
            String id = rs.getString("survey_id");
            LeaseAgreementRenewalDetail currentRenewal = leaseAgreementMap.get(id);

            if(currentRenewal == null){
                currentRenewal = LeaseAgreementRenewalDetail.builder()
    					.area(((Float) rs.getObject("area")).doubleValue())
    					.surveyId(id)
    					.lesseAsPerGLR(rs.getString("lesse"))
    					.surveyNo(rs.getString("surveyno"))
    					.finalTermExpiryDate((Long) rs.getObject("termexpirydate"))
    					.build();
                leaseAgreementMap.put(id,currentRenewal);
            }

        }
        return new ArrayList<>(leaseAgreementMap.values());
	}

	}
