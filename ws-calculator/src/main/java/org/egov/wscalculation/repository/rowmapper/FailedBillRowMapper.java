package org.egov.wscalculation.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.egov.wscalculation.web.models.AuditDetails;
import org.egov.wscalculation.web.models.BillFailureNotificationObj;
import org.egov.wscalculation.web.models.MeterReading;
import org.egov.wscalculation.web.models.MeterReading.MeterStatusEnum;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class FailedBillRowMapper implements ResultSetExtractor<List<BillFailureNotificationObj>> {

	@Override
	public List<BillFailureNotificationObj> extractData(ResultSet rs) throws SQLException, DataAccessException {
		List<BillFailureNotificationObj> billFailureNotificationObjs = new ArrayList<>();
		
		while (rs.next()) {
			BillFailureNotificationObj billFailureNotificationObj = new BillFailureNotificationObj();
			billFailureNotificationObj.setApplicationNo(rs.getString("applicationno"));
			billFailureNotificationObj.setAssessmentYear(rs.getString("assessmentYear"));
			billFailureNotificationObj.setId(rs.getString("id"));
			billFailureNotificationObj.setConnectionNo(rs.getString("connectionno"));
			billFailureNotificationObj.setConsumption(rs.getDouble("consumption"));
			billFailureNotificationObj.setCreatedBy(rs.getString("createdby"));
			billFailureNotificationObj.setCreatedTime(rs.getLong("createdtime"));
			billFailureNotificationObj.setCurrentReading(rs.getDouble("currentreading"));
			billFailureNotificationObj.setFromDate(rs.getLong("fromdate"));
			billFailureNotificationObj.setLastModifiedBy(rs.getString("lastmodifiedby"));
			billFailureNotificationObj.setLastModifiedTime(rs.getLong("lastmodifiedtime"));
			billFailureNotificationObj.setLastReading(rs.getDouble("lastreading"));
			billFailureNotificationObj.setReason(rs.getString("reason"));
			billFailureNotificationObj.setStatus(rs.getString("status"));
			billFailureNotificationObj.setTenantId(rs.getString("tenantid"));
			billFailureNotificationObj.setToDate(rs.getLong("todate"));
			billFailureNotificationObjs.add(billFailureNotificationObj);
			
//			meterReading.setId(rs.getString("id"));
//			meterReading.setConnectionNo(rs.getString("connectionId"));
//			meterReading.setBillingPeriod(rs.getString("billingPeriod"));
//			meterReading.setCurrentReading(rs.getDouble("currentReading"));
//			meterReading.setCurrentReadingDate(rs.getLong("currentReadingDate"));
//			meterReading.setLastReading(rs.getDouble("lastReading"));
//			meterReading.setLastReadingDate(rs.getLong("lastReadingDate"));
//			meterReading.setMeterStatus(MeterStatusEnum.fromValue(rs.getString("meterStatus")));
//			meterReading.setTenantId(rs.getString("tenantid"));
//			AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("mr_createdBy"))
//					.createdTime(rs.getLong("mr_createdTime")).lastModifiedBy(rs.getString("mr_lastModifiedBy"))
//					.lastModifiedTime(rs.getLong("mr_lastModifiedTime")).build();
//			meterReading.setAuditDetails(auditdetails);
//			meterReadingLists.add(meterReading);
		}
		return billFailureNotificationObjs;
	}
}
