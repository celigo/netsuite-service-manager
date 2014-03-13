package com.celigo.axon.service.netsuite.beans;

public class RoleDTO {

	private String internalId;
	private String name;
	
	public RoleDTO(String internalId, String name) {
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
		return "RoleData [internalId=" + internalId + ", name=" + name + "]";
	}
}
