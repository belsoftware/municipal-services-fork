package org.egov.wscalculation.web.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingSlab {
	private String id;
	private String buildingType = null;
	private String connectionType = null;
	private String propertyLocation =null;
	private String ownershipCategory=null;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBuildingType() {
		return buildingType;
	}

	public void setBuildingType(String buildingType) {
		this.buildingType = buildingType;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getCalculationAttribute() {
		return calculationAttribute;
	}

	public void setCalculationAttribute(String calculationAttribute) {
		this.calculationAttribute = calculationAttribute;
	}

	public double getMinimumCharge() {
		return minimumCharge;
	}

	public void setMinimumCharge(double minimumCharge) {
		this.minimumCharge = minimumCharge;
	}

	public List<Slab> getSlabs() {
		return slabs;
	}

	public void setSlabs(List<Slab> slabs) {
		this.slabs = slabs;
	}

	public String getPropertyLocation() {
		return propertyLocation;
	}

	public void setPropertyLocation(String propertyLocation) {
		this.propertyLocation = propertyLocation;
	}

	public double getUnAuthorizedConnection() {
		return unAuthorizedConnection;
	}

	public void setUnAuthorizedConnection(double unAuthorizedConnection) {
		this.unAuthorizedConnection = unAuthorizedConnection;
	}

	public String getOwnershipCategory() {
		return ownershipCategory;
	}

	public void setOwnershipCategory(String ownershipCategory) {
		this.ownershipCategory = ownershipCategory;
	}

	private String calculationAttribute = null;
	private double minimumCharge;
	private double unAuthorizedConnection;
	private List<Slab> slabs = new ArrayList<>();
}