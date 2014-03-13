package com.celigo.axon.service.netsuite.adaptors;

import com.netsuite.webservices.platform.core.CustomizationType;

public class GetCustomizationIdRequest {

	private CustomizationType customizationType;
	private boolean includeInactives = true;
	
	public GetCustomizationIdRequest(CustomizationType customizationType) {
		super();
		this.customizationType = customizationType;
	}
	public GetCustomizationIdRequest(CustomizationType customizationType,
			boolean includeInactives) {
		super();
		this.customizationType = customizationType;
		this.includeInactives = includeInactives;
	}
	public CustomizationType getCustomizationType() {
		return customizationType;
	}
	public void setCustomizationType(CustomizationType customizationType) {
		this.customizationType = customizationType;
	}
	public boolean isIncludeInactives() {
		return includeInactives;
	}
	public void setIncludeInactives(boolean includeInactives) {
		this.includeInactives = includeInactives;
	}
	
}
