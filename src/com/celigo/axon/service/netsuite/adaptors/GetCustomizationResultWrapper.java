package com.celigo.axon.service.netsuite.adaptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.netsuite.webservices.platform.core.GetCustomizationIdResult;
import com.netsuite.webservices.platform.core.Record;
import com.netsuite.webservices.platform.messages.ReadResponse;
import com.netsuite.webservices.platform.messages.ReadResponseList;

@SuppressWarnings("serial")
public class GetCustomizationResultWrapper implements Serializable {

	private GetCustomizationIdResult gcir;
	private ReadResponseList rrl;
	
	public GetCustomizationResultWrapper(GetCustomizationIdResult gcir, ReadResponseList rrl) {
		super();
		this.gcir = gcir;
		this.rrl = rrl;
	}

	public GetCustomizationIdResult getGcir() {
		return gcir;
	}

	public void setGcir(GetCustomizationIdResult gcir) {
		this.gcir = gcir;
	}
	
	public ReadResponseList getRrl() {
		return rrl;
	}

	public void setRrl(ReadResponseList rrl) {
		this.rrl = rrl;
	}

	public Record[] getRecords() {
		if (this.rrl == null || this.rrl.getReadResponse() == null) {
			return null;
		}
		
		Record[] records = null;
		try {
			records = getRecords(this.rrl);
		} catch (Exception e) {
			throw new RuntimeException("Unable to retrieve customization records from ReadResponseList.", e);
		}
		
		return records;
	}
	
	private Record[] getRecords(ReadResponseList rrl) throws Exception {
		ReadResponse[] readResponse = rrl.getReadResponse();
		List<Record> temp = new ArrayList<Record>();
		for (int i = 0; i < readResponse.length; i++) {
			Record tempRecord = getRecord(readResponse[i]);
			if (tempRecord != null) {
				temp.add(tempRecord);
			}
		}
		return temp.toArray(new Record[]{});
	}
	
	public static Record getRecord(ReadResponse rr) throws Exception {
		if (rr == null || rr.getStatus() == null || !rr.getStatus().isIsSuccess() || rr.getRecord() == null) {
			return null;
		}

		return rr.getRecord();
	}
}
