package org.egov.waterconnection.validator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.service.MeterInfoValidator;
import org.egov.waterconnection.service.PropertyValidator;
import org.egov.waterconnection.service.WaterFieldValidator;
import org.egov.waterconnection.web.models.RoadTypeEst;
import org.egov.waterconnection.web.models.ValidatorResult;
import org.egov.waterconnection.web.models.WaterConnection;
import org.egov.waterconnection.web.models.WaterConnectionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class WaterConnectionValidator {

	@Autowired
	private PropertyValidator propertyValidator;
	
	@Autowired
	private WaterFieldValidator waterFieldValidator;
	
	@Autowired
	private MeterInfoValidator meterInfoValidator;


	/**Used strategy pattern for avoiding multiple if else condition
	 * 
	 * @param waterConnectionRequest
	 * @param reqType
	 */
	public void validateWaterConnection(WaterConnectionRequest waterConnectionRequest, int reqType) {
		Map<String, String> errorMap = new HashMap<>();
		if (StringUtils.isEmpty(waterConnectionRequest.getWaterConnection().getProcessInstance())
				|| StringUtils.isEmpty(waterConnectionRequest.getWaterConnection().getProcessInstance().getAction())) {
			errorMap.put("INVALID_ACTION", "Workflow obj can not be null or action can not be empty!!");
			throw new CustomException(errorMap);
		}
		ValidatorResult isPropertyValidated = propertyValidator.validate(waterConnectionRequest, reqType);
		if (!isPropertyValidated.isStatus())
			errorMap.putAll(isPropertyValidated.getErrorMessage());
		ValidatorResult isWaterFieldValidated = waterFieldValidator.validate(waterConnectionRequest, reqType);
		if (!isWaterFieldValidated.isStatus())
			errorMap.putAll(isWaterFieldValidated.getErrorMessage());
		ValidatorResult isMeterInfoValidated = meterInfoValidator.validate(waterConnectionRequest, reqType);
		if (!isMeterInfoValidated.isStatus())
			errorMap.putAll(isMeterInfoValidated.getErrorMessage());

		if (!errorMap.isEmpty())
			throw new CustomException(errorMap);
	}
	
	public void validatePropertyForConnection(List<WaterConnection> waterConnectionList) {
		waterConnectionList.forEach(waterConnection -> {
			if (StringUtils.isEmpty(waterConnection.getId())) {
				StringBuilder builder = new StringBuilder();
				builder.append("PROPERTY UUID NOT FOUND FOR ")
						.append(waterConnection.getConnectionNo() == null ? waterConnection.getApplicationNo()
								: waterConnection.getConnectionNo());
				log.error(builder.toString());
			}
		});
	}
	
	/**
	 * Validate for previous data to current data
	 * 
	 * @param request water connection request
	 * @param searchResult water connection search result
	 */
	public void validateUpdate(WaterConnectionRequest request, WaterConnection searchResult, int reqType) {
		validateAllIds(request.getWaterConnection(), searchResult);
		validateDuplicateDocuments(request);
		setFieldsFromSearch(request, searchResult, reqType);
		
	}
   
	/**
	 * Validates if all ids are same as obtained from search result
	 * 
	 * @param updateWaterConnection The water connection request from update request 
	 * @param searchResult The water connection from search result
	 */
	private void validateAllIds(WaterConnection updateWaterConnection, WaterConnection searchResult) {
		Map<String, String> errorMap = new HashMap<>();
		if (!searchResult.getApplicationNo().equals(updateWaterConnection.getApplicationNo()))
			errorMap.put("INVALID UPDATE", "The application number from search: " + searchResult.getApplicationNo()
					+ " and from update: " + updateWaterConnection.getApplicationNo() + " does not match");
		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);
	}
    
    /**
     * Validates application documents for duplicates
     * 
     * @param request The waterConnection Request
     */
	private void validateDuplicateDocuments(WaterConnectionRequest request) {
		if (request.getWaterConnection().getDocuments() != null) {
			List<String> documentFileStoreIds = new LinkedList<>();
			request.getWaterConnection().getDocuments().forEach(document -> {
				if (documentFileStoreIds.contains(document.getFileStoreId()))
					throw new CustomException("DUPLICATE_DOCUMENT_ERROR",
							"Same document cannot be used multiple times");
				else
					documentFileStoreIds.add(document.getFileStoreId());
			});
		}
	}
	/**
	 * Enrich Immutable fields
	 * 
	 * @param request Water connection request
	 * @param searchResult water connection search result
	 */
	private void setFieldsFromSearch(WaterConnectionRequest request, WaterConnection searchResult, int reqType) {
		if (reqType == WCConstants.UPDATE_APPLICATION) {
			request.getWaterConnection().setConnectionNo(searchResult.getConnectionNo());
		}
	}
	
	private boolean isZero(BigDecimal fld) {
		return fld==null || fld.compareTo(BigDecimal.ZERO)==0 ? true : false;
	}
	private boolean isNotZero(BigDecimal fld) {
		return fld!=null && fld.compareTo(BigDecimal.ZERO)!=0 ? true : false;
	}
	
	public void validateCalcAttr(WaterConnectionRequest waterConnectionRequest, WaterConnection searchResult) {
		WaterConnection connection = waterConnectionRequest.getWaterConnection();
		if(connection.getRoadTypeEst()!=null) {
			for (RoadTypeEst roadTypeEst : connection.getRoadTypeEst()) {
				if(roadTypeEst.getRoadType()==null || roadTypeEst.getRoadType().trim().length()==0) {
					throw new CustomException("ROADTYPE_REQUIRED",
							"Road Type cannot be null");
				}
				if(isNotZero(roadTypeEst.getDepth())|| isNotZero(roadTypeEst.getBreadth())  || isNotZero(roadTypeEst.getLength())  || isNotZero(roadTypeEst.getRate())) {
					if(isZero(roadTypeEst.getDepth()) || isZero(roadTypeEst.getBreadth())  || isZero(roadTypeEst.getLength())  || isZero(roadTypeEst.getRate()))
						throw new CustomException("Calculationa_attr",
								"Please enter all the parameter(Length,Breadth,Depth & Rate) to calculate road cutting charges");
				
				}else { 
					roadTypeEst.setDepth(new BigDecimal(0));
					roadTypeEst.setBreadth(new BigDecimal(0));
					roadTypeEst.setLength(new BigDecimal(0));
					roadTypeEst.setRate(new BigDecimal(0));
				}
				
			}
			}
		if(connection.getProcessInstance().getAction().equalsIgnoreCase(WCConstants.APPROVE_CONNECTION_CONST)) {
			if(connection.getRoadTypeEst().size()==0 && connection.getWsTaxHeads().size()==0) {
				throw new CustomException("ESTIMATE_NOT_DONE",
						"Application cannot be approved without estimation");
			}
		}
		
		if(connection.getProcessInstance().getAction().equalsIgnoreCase(WCConstants.APPROVE_CONNECTION_CONST) || 
				(connection.getProcessInstance().getAction().equalsIgnoreCase(WCConstants.VERIFY_AND_FORWARD_CONST) && searchResult.getApplicationStatus().equalsIgnoreCase(WCConstants.PENDING_FOR_FIELD_INSPECTION_CONST))) {
			if(connection.getRoadTypeEst().size()==0 && connection.getWsTaxHeads().size()==0) {
				throw new CustomException("ESTIMATE_NOT_DONE",
						"Application cannot be processed without estimation");
			}
		}
		
	}
}
