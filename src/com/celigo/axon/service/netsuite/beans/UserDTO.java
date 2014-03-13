package com.celigo.axon.service.netsuite.beans;

import java.util.List;

public class UserDTO {

	private List<AccountRoleDTO> accountRoleDTOs;

	public List<AccountRoleDTO> getAccountRoleDTOs() {
		return accountRoleDTOs;
	}

	public void setAccountRoleDatas(List<AccountRoleDTO> accountRoleDTOs) {
		this.accountRoleDTOs = accountRoleDTOs;
	}

	@Override
	public String toString() {
		return "UserData [accountRoleDTOs=" + accountRoleDTOs + "]";
	}
	
	
}
