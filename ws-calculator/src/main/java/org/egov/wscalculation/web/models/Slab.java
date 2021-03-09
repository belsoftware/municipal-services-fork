package org.egov.wscalculation.web.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Slab {
	private double from;
	private double to;
	private double charge;
	private double meterCharge;
	private String type;
	
	public double getFrom() {
		return from;
	}
	public void setFrom(double from) {
		this.from = from;
	}
	public double getTo() {
		return to;
	}
	public void setTo(double to) {
		this.to = to;
	}
	public double getCharge() {
		return charge;
	}
	public void setCharge(double charge) {
		this.charge = charge;
	}
	public double getMeterCharge() {
		return meterCharge;
	}
	public void setMeterCharge(double meterCharge) {
		this.meterCharge = meterCharge;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	
}