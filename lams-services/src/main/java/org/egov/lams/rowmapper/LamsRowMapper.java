package org.egov.lams.rowmapper;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.egov.lams.web.models.LeaseAgreementRenewal;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;



@Component
public class LamsRowMapper  implements ResultSetExtractor<List<LeaseAgreementRenewal>> {

	@Override
	public List<LeaseAgreementRenewal> extractData(ResultSet rs) throws SQLException, DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}}
