package com.celigo.axon.service.netsuite.beans;

public class AccountDTO {

	private String internalId;
	private String name;
	
	public AccountDTO(String internalId, String name) {
		super();
		this.internalId = internalId;
		this.name = name;
	}
	public String getInternalId() {
		return internalId;
	}
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String toString() {
		return "AccountData [internalId=" + internalId + ", name=" + name + "]";
	}
}
