package org.egov.common.entity.edcr;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VentilationRequirement extends MdmsFeatureRule {
	
	  @JsonProperty("ventilationValueOne")
	    private BigDecimal ventilationValueOne;
	    @JsonProperty("ventilationValueTwo")
	    private BigDecimal ventilationValueTwo;
	    @JsonProperty("laundryRecreationPercent")
	    private BigDecimal laundryRecreationPercent;
	    @JsonProperty("commonRoomPercent")
	    private BigDecimal commonRoomPercent;
		public BigDecimal getVentilationValueOne() {
			return ventilationValueOne;
		}
		public BigDecimal getLaundryRecreationPercent() {
			return laundryRecreationPercent;
		}
		public void setLaundryRecreationPercent(BigDecimal laundryRecreationPercent) {
			this.laundryRecreationPercent = laundryRecreationPercent;
		}
		public void setVentilationValueOne(BigDecimal ventilationValueOne) {
			this.ventilationValueOne = ventilationValueOne;
		}
		public BigDecimal getCommonRoomPercent() {
			return commonRoomPercent;
		}
		public void setCommonRoomPercent(BigDecimal commonRoomPercent) {
			this.commonRoomPercent = commonRoomPercent;
		}
		public BigDecimal getVentilationValueTwo() {
			return ventilationValueTwo;
		}
		public void setVentilationValueTwo(BigDecimal ventilationValueTwo) {
			this.ventilationValueTwo = ventilationValueTwo;
		}
		@Override
		public String toString() {
			return "VentilationRequirement [ventilationValueOne=" + ventilationValueOne + ", ventilationValueTwo="
					+ ventilationValueTwo + "]";
		}

}
