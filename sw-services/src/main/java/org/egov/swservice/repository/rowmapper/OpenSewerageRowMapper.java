package org.egov.swservice.repository.rowmapper;

import org.egov.swservice.util.SWConstants;
import org.egov.swservice.web.models.*;
import org.egov.swservice.web.models.Connection.StatusEnum;
import org.egov.swservice.web.models.workflow.ProcessInstance;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenSewerageRowMapper implements ResultSetExtractor<List<SewerageConnection>> {
	
	@Override
    public List<SewerageConnection> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String, SewerageConnection> connectionListMap = new HashMap<>();
        SewerageConnection sewarageConnection = new SewerageConnection();
        while (rs.next()) {
            String Id = rs.getString("connection_Id");
            if (connectionListMap.getOrDefault(Id, null) == null) {
                sewarageConnection = new SewerageConnection();
                sewarageConnection.setTenantId(rs.getString("tenantid"));
                sewarageConnection.setId(rs.getString("connection_Id"));
                sewarageConnection.setApplicationNo(rs.getString("applicationNo"));
                sewarageConnection
                        .setApplicationStatus(rs.getString("applicationstatus"));
                sewarageConnection.setStatus(StatusEnum.fromValue(rs.getString("status")));
                sewarageConnection.setConnectionNo(rs.getString("connectionNo"));
                sewarageConnection.setOldConnectionNo(rs.getString("oldConnectionNo"));
                sewarageConnection.setOldApplication(rs.getBoolean("isoldapplication"));
                sewarageConnection.setPropertyOwnership(rs.getString("propertyownership"));
                // get property id and get property object
                HashMap<String, Object> addtionalDetails = new HashMap<>();
                addtionalDetails.put(SWConstants.APP_CREATED_DATE, rs.getBigDecimal("appCreatedDate"));
                addtionalDetails.put(SWConstants.LOCALITY, rs.getString("locality"));
                sewarageConnection.setAdditionalDetails(addtionalDetails);
                sewarageConnection.processInstance(ProcessInstance.builder().action((rs.getString("action"))).build());
                sewarageConnection.setApplicationType(rs.getString("applicationType"));
                sewarageConnection.setDateEffectiveFrom(rs.getLong("dateEffectiveFrom"));
                sewarageConnection.setPropertyId(rs.getString("property_id"));

                AuditDetails auditdetails = AuditDetails.builder()
                        .createdBy(rs.getString("sw_createdBy"))
                        .createdTime(rs.getLong("sw_createdTime"))
                        .lastModifiedBy(rs.getString("sw_lastModifiedBy"))
                        .lastModifiedTime(rs.getLong("sw_lastModifiedTime"))
                        .build();
                sewarageConnection.setAuditDetails(auditdetails);

                // Add documents id's
                connectionListMap.put(Id, sewarageConnection);
            }

			addHoldersDeatilsToSewerageConnection(rs, sewarageConnection);
			addTaxHeadRaoadTypeDetailsToSewerageConnection(rs, sewarageConnection);
        }
        return new ArrayList<>(connectionListMap.values());
    }



    private void addHoldersDeatilsToSewerageConnection(ResultSet rs, SewerageConnection sewerageConnection) throws SQLException {
        String uuid = rs.getString("userid");
        List<OwnerInfo> connectionHolders = sewerageConnection.getConnectionHolders();
        if (!CollectionUtils.isEmpty(connectionHolders)) {
            for (OwnerInfo connectionHolderInfo : connectionHolders) {
                if (!StringUtils.isEmpty(connectionHolderInfo.getUuid()) && !StringUtils.isEmpty(uuid) && connectionHolderInfo.getUuid().equals(uuid))
                    return;
            }
        }
        if (!StringUtils.isEmpty(uuid)) {
            Double holderShipPercentage = rs.getDouble("holdershippercentage");
            if (rs.wasNull()) {
                holderShipPercentage = null;
            }
            Boolean isPrimaryOwner = rs.getBoolean("isprimaryholder");
            if (rs.wasNull()) {
                isPrimaryOwner = null;
            }
            OwnerInfo connectionHolderInfo = OwnerInfo.builder()
                    .relationship(Relationship.fromValue(rs.getString("holderrelationship")))
                    .status(Status.fromValue(rs.getString("holderstatus")))
                    .tenantId(rs.getString("holdertenantid")).ownerType(rs.getString("connectionholdertype"))
                    .isPrimaryOwner(isPrimaryOwner).uuid(uuid).build();
            sewerageConnection.addConnectionHolderInfo(connectionHolderInfo);
        }
    }
    
    private void addTaxHeadRaoadTypeDetailsToSewerageConnection(ResultSet rs, SewerageConnection sewerageConnection) throws SQLException {
   	 if(rs.getString("taxhead_id")!=null && rs.getBoolean("taxhead_active")){
            WsTaxHeads wsTaxHeads = WsTaxHeads.builder()
                    .taxHeadCode(rs.getString("taxhead"))
                    .amount(rs.getBigDecimal("taxhead_amt"))
                    .id(rs.getString("taxhead_id"))
                    .active(rs.getBoolean("taxhead_active"))
                    .build();
            sewerageConnection.addWsTaxHead(wsTaxHeads);
        }
   	 
   	 if(rs.getString("roadtype_id")!=null && rs.getBoolean("roadtype_active")){
            RoadTypeEst roadTypeEst = RoadTypeEst.builder()
                    .roadType(rs.getString("roadtype1"))
                    .length(rs.getBigDecimal("roadtype_length"))
                    .breadth(rs.getBigDecimal("roadtype_breadth"))
                    .depth(rs.getBigDecimal("roadtype_depth"))
                    .rate(rs.getBigDecimal("roadtype_rate"))
                    .id(rs.getString("roadtype_id"))
                    .active(rs.getBoolean("roadtype_active"))
                    .build();
            sewerageConnection.addRoadTypeEst(roadTypeEst);
        }
   }
}
