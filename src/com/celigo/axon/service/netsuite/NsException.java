package com.celigo.axon.service.netsuite;

import java.lang.Exception;

public class NsException extends Exception {
	private static final long serialVersionUID = -7681894019445014530L;

	public NsException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public NsException(String arg0) {
		super(arg0);
	}

}
