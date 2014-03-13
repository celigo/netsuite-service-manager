package com.celigo.axon.service.netsuite.beans;

public class AccountInfoDTO {

	private String endPointUrl;
	private String accountName;
	
	public String getEndPointUrl() {
		return endPointUrl;
	}
	public void setEndPointUrl(String endPointUrl) {
		this.endPointUrl = endPointUrl;
	}
	public String getAccountName() {
		return accountName;
	}
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	
	@Override
	public String toString() {
		return "AccountInfoDTO [endPointUrl=" + endPointUrl + ", accountName="
				+ accountName + "]";
	}
	
}
