package com.celigo.axon.service.netsuite;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.celigo.axon.service.netsuite.beans.AccountInfoDTO;
import com.celigo.axon.service.netsuite.beans.AccountRoleDTO;
import com.celigo.axon.service.netsuite.beans.NetSuiteErrorWrapper;
import com.celigo.axon.service.netsuite.beans.UserDTO;
import com.google.gson.Gson;

public class NetSuiteURLFinder {

	protected static transient Log log = LogFactory.getLog(NetSuiteURLFinder.class);
	public static final String NS_ENDPOINT = "NetSuitePort_2013_2";
	
	public static AccountInfoDTO generateAccountInfo(boolean isSandbox, boolean isBeta, NetSuiteCredential credential) {
		return generateAccountInfo(isSandbox, isBeta, credential.getEmail(), 
				credential.getPassword(), credential.getAccount(), credential.getRoleId());
	}
	
	private static AccountInfoDTO generateAccountInfo(boolean isSandbox, boolean isBeta, String email, String password, String account, String role) {
		AccountInfoDTO accountInfoDTO = new AccountInfoDTO();
		String servicesEndPoint = "/services/" + NS_ENDPOINT;
		
		String defaultDomain = "https://webservices.netsuite.com";
		if (isSandbox) {
			defaultDomain = "https://webservices.sandbox.netsuite.com";
		} else if (isBeta) {
			defaultDomain = "https://webservices.beta.netsuite.com";
		}
		
		UserDTO userDTO = null;
		try {
			userDTO = loadUserLoginData(isSandbox, isBeta, email, password);
			log.info(email + ": " + userDTO);
			
			if (userDTO == null) {
				throw new Exception("NetSuite roles RESTlet Service returned null data for " + email);
			}
			
			AccountRoleDTO accountRoleDTO = findAccountRoleData(userDTO, account, role);
			if (accountRoleDTO == null || accountRoleDTO.getDataCenterURLs() == null 
					|| accountRoleDTO.getDataCenterURLs().getWebservicesDomain() == null) {
				throw new Exception("Unable to determine NetSuite Webservices Domain for " + email);
			}
			
			accountInfoDTO.setAccountName(accountRoleDTO.getAccount().getName());
			defaultDomain = accountRoleDTO.getDataCenterURLs().getWebservicesDomain();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		accountInfoDTO.setEndPointUrl(defaultDomain + servicesEndPoint);
		return accountInfoDTO;
	}
	
	private static UserDTO loadUserLoginData(boolean isSandbox, boolean isBeta, String email, String password) throws Exception {
		String rolesServiceURL = "https://system.netsuite.com/rest/roles";
		if (isSandbox) {
			rolesServiceURL = "https://system.sandbox.netsuite.com/rest/roles";
		} else if (isBeta) {
			rolesServiceURL = "https://system.beta.netsuite.com/rest/roles";
		}
		
		URL url = new URL(rolesServiceURL);
		HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
		conn.setConnectTimeout(30000);
		conn.setReadTimeout(60000);
		conn.setRequestProperty ("Authorization", "NLAuth nlauth_email=" + email + ", nlauth_signature=" + password);
		conn.connect();

		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			String errorResponse = IOUtils.toString(conn.getErrorStream());
			NetSuiteErrorWrapper netSuiteErrorWrapper = new Gson().fromJson(errorResponse, NetSuiteErrorWrapper.class);
			log.error(netSuiteErrorWrapper);
			throw new Exception(netSuiteErrorWrapper.getError().getCode() + ": " + netSuiteErrorWrapper.getError().getMaskedMessage(password));
		}
		
		String response = IOUtils.toString(conn.getInputStream());
		response = "{\"accountRoleDTOs\":" + response + "}";
		log.debug(response);
		return new Gson().fromJson(response, UserDTO.class);
	}
	
	private static AccountRoleDTO findAccountRoleData(UserDTO userData, String account, String role) {
		if (userData == null || userData.getAccountRoleDTOs() == null || userData.getAccountRoleDTOs().size() == 0) {
			return null;
		}
		
		AccountRoleDTO temp = null;
		for (AccountRoleDTO accountRoleData : userData.getAccountRoleDTOs()) {
			if (account.equals(accountRoleData.getAccount().getInternalId())) {
				temp = accountRoleData;
				break;
			}
		}
		
		return temp;
	}
}
