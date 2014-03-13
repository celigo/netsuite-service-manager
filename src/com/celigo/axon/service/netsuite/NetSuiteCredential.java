package com.celigo.axon.service.netsuite;

public class NetSuiteCredential {
	
	private String email;
	private String password;
	private String account;
	private String roleId;
	private int numberOfSeats = 1;
	private String id;
	
	public NetSuiteCredential() {};
	
	public NetSuiteCredential(String email, String password, String account, String roleId) {
		this(email, password, account, roleId, 1);
	}
	
	public NetSuiteCredential(String email, String password, String account, String roleId, int numberOfSeats) {
		this.email = email;
		this.password = password;
		this.account = account;
		this.roleId = roleId;
		this.numberOfSeats = numberOfSeats;
	}
	
	public String getAccount() {
		return account;
	}
	public String getEmail() {
		return email;
	}
	public String getPassword() {
		return password;
	}
	public String getRoleId() {
		return roleId;
	}
	public int getNumberOfSeats() {
		return numberOfSeats;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setNumberOfSeats(int numberOfSeats) {
		this.numberOfSeats = numberOfSeats;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
