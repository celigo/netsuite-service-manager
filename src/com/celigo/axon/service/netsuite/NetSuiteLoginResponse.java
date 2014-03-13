package com.celigo.axon.service.netsuite;


import org.apache.axis.message.SOAPHeaderElement;
import com.netsuite.webservices.platform.messages.SessionResponse;

public class NetSuiteLoginResponse {

	private SessionResponse sessionResponse;
	private SOAPHeaderElement[] headers;

	public NetSuiteLoginResponse(SessionResponse sessionResponse, SOAPHeaderElement[] headers) {
		this.sessionResponse = sessionResponse;
		this.headers = headers;
	}
	
	public String getUserId() {
		if (getHeaders() == null)
			return null;
		for (int i=0; i<getHeaders().length; i++) {
			String userId = getUserId(getHeaders()[i]);
			if (userId != null && !"".equals(userId))
				return userId;
		}
		return null;		
	}
	
	private String getUserId(SOAPHeaderElement header) {
		if (header == null ||
			header.getElementsByTagName("userId").getLength() != 1 ||
			header.getElementsByTagName("userId").item(0).getChildNodes().getLength() != 1)
			return null;
		return header.getElementsByTagName("userId").item(0).getChildNodes().item(0).getNodeValue();
	}

	public SessionResponse getSessionResponse() {
		return sessionResponse;
	}

	public SOAPHeaderElement[] getHeaders() {
		return headers;
	}
}
