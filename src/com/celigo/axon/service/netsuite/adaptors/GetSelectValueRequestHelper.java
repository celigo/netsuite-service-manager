package com.celigo.axon.service.netsuite.adaptors;

import com.netsuite.webservices.platform.core.GetSelectValueFieldDescription;

public class GetSelectValueRequestHelper {

	private GetSelectValueFieldDescription getSelectValueField;
	private int pageIndex;
	
	public GetSelectValueFieldDescription getGetSelectValueField() {
		return getSelectValueField;
	}

	public void setGetSelectValueField(GetSelectValueFieldDescription getSelectValueField) {
		this.getSelectValueField = getSelectValueField;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}

	
}
