package com.celigo.axon.service.netsuite.beans;

public class AccountRoleDTO {

	private AccountDTO account;
	private RoleDTO role;
	private URLDTO dataCenterURLs;
	
	public AccountRoleDTO(AccountDTO account, RoleDTO role,
			URLDTO dataCenterURLs) {
		super();
		this.account = account;
		this.role = role;
		this.dataCenterURLs = dataCenterURLs;
	}
	public AccountDTO getAccount() {
		return account;
	}
	public void setAccount(AccountDTO account) {
		this.account = account;
	}
	public RoleDTO getRole() {
		return role;
	}
	public void setRole(RoleDTO role) {
		this.role = role;
	}
	public URLDTO getDataCenterURLs() {
		return dataCenterURLs;
	}
	public void setDataCenterURLs(URLDTO dataCenterURLs) {
		this.dataCenterURLs = dataCenterURLs;
	}
	
	@Override
	public String toString() {
		return "AccountRoleData [account=" + account + ", role=" + role
				+ ", dataCenterURLs=" + dataCenterURLs + "]";
	}
}
