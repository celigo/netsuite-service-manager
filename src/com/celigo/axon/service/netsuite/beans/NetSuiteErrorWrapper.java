package com.celigo.axon.service.netsuite.beans;

public class NetSuiteErrorWrapper {

	private NetSuiteError error;

	public NetSuiteError getError() {
		return error;
	}

	public void setError(NetSuiteError error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "NetSuiteErrorWrapper [error=" + error + "]";
	}
	
}
