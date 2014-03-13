package com.celigo.axon.service.netsuite.beans;

public class URLDTO {

	private String restDomain;
	private String systemDomain;
	private String webservicesDomain;
	
	public URLDTO(String restDomain, String systemDomain,
			String webservicesDomain) {
		super();
		this.restDomain = restDomain;
		this.systemDomain = systemDomain;
		this.webservicesDomain = webservicesDomain;
	}
	public String getRestDomain() {
		return restDomain;
	}
	public void setRestDomain(String restDomain) {
		this.restDomain = restDomain;
	}
	public String getSystemDomain() {
		return systemDomain;
	}
	public void setSystemDomain(String systemDomain) {
		this.systemDomain = systemDomain;
	}
	public String getWebservicesDomain() {
		return webservicesDomain;
	}
	public void setWebservicesDomain(String webservicesDomain) {
		this.webservicesDomain = webservicesDomain;
	}
	
	@Override
	public String toString() {
		return "URLData [restDomain=" + restDomain + ", systemDomain="
				+ systemDomain + ", webservicesDomain=" + webservicesDomain
				+ "]";
	}
}
