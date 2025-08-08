package org.egov.common.entity.edcr;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RearSetBackRequirement extends MdmsFeatureRule {
	
	    public BigDecimal getPermissibleLight() {
		return permissibleLight;
	}

	public void setPermissibleLight(BigDecimal permissibleLight) {
		this.permissibleLight = permissibleLight;
	}

	public BigDecimal getPermissibleMedium() {
		return permissibleMedium;
	}

	public void setPermissibleMedium(BigDecimal permissibleMedium) {
		this.permissibleMedium = permissibleMedium;
	}

	public BigDecimal getPermissibleFlattered() {
		return permissibleFlattered;
	}

	public void setPermissibleFlattered(BigDecimal permissibleFlattered) {
		this.permissibleFlattered = permissibleFlattered;
	}

		@JsonProperty("permissibleLight")
	    private BigDecimal permissibleLight;
	    
	    @JsonProperty("permissibleMedium")
	    private BigDecimal permissibleMedium;
	    
	    @JsonProperty("permissibleFlattered")
	    private BigDecimal permissibleFlattered;
	    
	    @JsonProperty("permissibleNursery")
	    private BigDecimal permissibleNursery;
	    
	    @JsonProperty("permissiblePrimary")
	    private BigDecimal permissiblePrimary;
	    
	    @JsonProperty("permissibleHighSchool")
	    private BigDecimal permissibleHighSchool;
	    
	    @JsonProperty("permissibleCollege")
	    private BigDecimal permissibleCollege;

		public BigDecimal getPermissibleNursery() {
			return permissibleNursery;
		}

		public void setPermissibleNursery(BigDecimal permissibleNursery) {
			this.permissibleNursery = permissibleNursery;
		}

	
		public BigDecimal getPermissiblePrimary() {
			return permissiblePrimary;
		}

		public void setPermissiblePrimary(BigDecimal permissiblePrimary) {
			this.permissiblePrimary = permissiblePrimary;
		}

		public BigDecimal getPermissibleHighSchool() {
			return permissibleHighSchool;
		}

		public void setPermissibleHighSchool(BigDecimal permissibleHighSchool) {
			this.permissibleHighSchool = permissibleHighSchool;
		}

		public BigDecimal getPermissibleCollege() {
			return permissibleCollege;
		}

		public void setPermissibleCollege(BigDecimal permissibleCollege) {
			this.permissibleCollege = permissibleCollege;
		}

		@Override
		public String toString() {
			return "RearSetBackRequirement [permissibleLight=" + permissibleLight + ", permissibleMedium="
					+ permissibleMedium + ", permissibleFlattered=" + permissibleFlattered + "]";
		}
	    


}
