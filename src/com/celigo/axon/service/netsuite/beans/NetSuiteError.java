package com.celigo.axon.service.netsuite.beans;


public class NetSuiteError {

	private String code;
	private String message;
	
	public String getMaskedMessage(String password) {
		if (getMessage() == null || "".equals(getMessage())) {
			return getMessage();
		}
		
		if (!getMessage().contains(password)) {
			return getMessage();
		}
		
		String mask = "";
		for (int i = 0; i < password.length(); i++) {
			mask += "*";
		}
		
		return "You have entered an invalid email address. Please try again. [" + getMessage().replaceAll(password, mask) + "]";
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "NetSuiteError [code=" + code + ", message=" + message + "]";
	}
}
