package org.egov.waterconnection.repository.rowmapper;

import org.apache.commons.lang3.StringUtils;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.web.models.*;
import org.egov.waterconnection.web.models.Connection.StatusEnum;
import org.egov.waterconnection.web.models.workflow.ProcessInstance;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenWaterRowMapper implements ResultSetExtractor<List<WaterConnection>> {
	@Override
    public List<WaterConnection> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String, WaterConnection> connectionListMap = new HashMap<>();
        WaterConnection currentWaterConnection = new WaterConnection();
        while (rs.next()) {
            String Id = rs.getString("connection_Id");
            if (connectionListMap.getOrDefault(Id, null) == null) {
                currentWaterConnection = new WaterConnection();
                currentWaterConnection.setTenantId(rs.getString("tenantid"));
                currentWaterConnection.setConnectionCategory(rs.getString("connectionCategory"));
                currentWaterConnection.setConnectionType(rs.getString("connectionType"));
                currentWaterConnection.setWaterSource(rs.getString("waterSource"));
                currentWaterConnection.setSourceInfo(rs.getString("sourceInfo"));
                currentWaterConnection.setMeterId(rs.getString("meterId"));
                currentWaterConnection.setMeterInstallationDate(rs.getLong("meterInstallationDate"));
                currentWaterConnection.setId(rs.getString("connection_Id"));
                currentWaterConnection.setApplicationNo(rs.getString("applicationNo"));
                currentWaterConnection.setApplicationStatus(rs.getString("applicationstatus"));
                currentWaterConnection.setStatus(StatusEnum.fromValue(rs.getString("status")));
                currentWaterConnection.setConnectionNo(rs.getString("connectionNo"));
                currentWaterConnection.setOldConnectionNo(rs.getString("oldConnectionNo"));
                currentWaterConnection.setOldApplication(rs.getBoolean("isoldapplication"));
                currentWaterConnection.setAuthorizedConnection(rs.getString("authorizedconnection"));
                currentWaterConnection.setMotorInfo(rs.getString("motorinfo"));
                currentWaterConnection.setPropertyOwnership(rs.getString("propertyownership"));
                HashMap<String, Object> additionalDetails = new HashMap<>();
                additionalDetails.put(WCConstants.INITIAL_METER_READING_CONST, rs.getBigDecimal("initialmeterreading"));
                additionalDetails.put(WCConstants.APP_CREATED_DATE, rs.getBigDecimal("appCreatedDate"));
                additionalDetails.put(WCConstants.LOCALITY, rs.getString("locality"));
                currentWaterConnection.setAdditionalDetails(additionalDetails);
                currentWaterConnection
                        .processInstance(ProcessInstance.builder().action((rs.getString("action"))).build());
                currentWaterConnection.setPropertyId(rs.getString("property_id"));

                currentWaterConnection.setConnectionExecutionDate(rs.getLong("connectionExecutionDate"));
                currentWaterConnection.setApplicationType(rs.getString("applicationType"));
                currentWaterConnection.setDateEffectiveFrom(rs.getLong("dateEffectiveFrom"));

                AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("ws_createdBy"))
                        .createdTime(rs.getLong("ws_createdTime")).lastModifiedBy(rs.getString("ws_lastModifiedBy"))
                        .lastModifiedTime(rs.getLong("ws_lastModifiedTime")).build();
                currentWaterConnection.setAuditDetails(auditdetails);

                connectionListMap.put(Id, currentWaterConnection);
            }
            addChildrenToProperty(rs, currentWaterConnection);
        }
        return new ArrayList<>(connectionListMap.values());
    }

    private void addChildrenToProperty(ResultSet rs, WaterConnection waterConnection) throws SQLException {
        addHoldersDeatilsToWaterConnection(rs, waterConnection);
        addTaxHeadRaoadTypeDetailsToWaterConnection(rs, waterConnection);
    }



    private void addHoldersDeatilsToWaterConnection(ResultSet rs, WaterConnection waterConnection) throws SQLException {
        String uuid = rs.getString("userid");
        List<OwnerInfo> connectionHolders = waterConnection.getConnectionHolders();
        if (!CollectionUtils.isEmpty(connectionHolders)) {
            for (OwnerInfo connectionHolderInfo : connectionHolders) {
                if (!StringUtils.isEmpty(connectionHolderInfo.getUuid()) && !StringUtils.isEmpty(uuid) && connectionHolderInfo.getUuid().equals(uuid))
                    return;
            }
        }
        if(!StringUtils.isEmpty(uuid)){
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
            waterConnection.addConnectionHolderInfo(connectionHolderInfo);
        }
    }
    
    private void addTaxHeadRaoadTypeDetailsToWaterConnection(ResultSet rs, WaterConnection waterConnection) throws SQLException {
    	 if(rs.getString("taxhead_id")!=null){
             WsTaxHeads wsTaxHeads = WsTaxHeads.builder()
                     .taxHeadCode(rs.getString("taxhead"))
                     .amount(rs.getBigDecimal("taxhead_amt"))
                     .id(rs.getString("taxhead_id"))
                     .active(rs.getBoolean("taxhead_active"))
                     .build();
             waterConnection.addWsTaxHead(wsTaxHeads);
         }
    	 
    	 if(rs.getString("roadtype_id")!=null){
             RoadTypeEst roadTypeEst = RoadTypeEst.builder()
                     .roadType(rs.getString("roadtype1"))
                     .length(rs.getBigDecimal("roadtype_length"))
                     .breadth(rs.getBigDecimal("roadtype_breadth"))
                     .depth(rs.getBigDecimal("roadtype_depth"))
                     .rate(rs.getBigDecimal("roadtype_rate"))
                     .id(rs.getString("roadtype_id"))
                     .active(rs.getBoolean("roadtype_active"))
                     .build();
             waterConnection.addRoadTypeEst(roadTypeEst);
         }
    }
}
