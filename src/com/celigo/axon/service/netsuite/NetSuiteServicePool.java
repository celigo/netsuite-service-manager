package com.celigo.axon.service.netsuite;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.celigo.axon.service.netsuite.adaptors.GetCustomizationIdRequest;
import com.celigo.axon.service.netsuite.adaptors.GetSelectValueRequestHelper;
import com.celigo.axon.service.netsuite.adaptors.GetSelectValueResultHelper;
import com.netsuite.webservices.platform.core.AsyncStatusResult;
import com.netsuite.webservices.platform.core.AttachReference;
import com.netsuite.webservices.platform.core.BaseRef;
import com.netsuite.webservices.platform.core.GetAllRecord;
import com.netsuite.webservices.platform.core.GetAllResult;
import com.netsuite.webservices.platform.core.GetCustomizationIdResult;
import com.netsuite.webservices.platform.core.GetDeletedFilter;
import com.netsuite.webservices.platform.core.GetDeletedResult;
import com.netsuite.webservices.platform.core.GetItemAvailabilityResult;
import com.netsuite.webservices.platform.core.InitializeRecord;
import com.netsuite.webservices.platform.core.ItemAvailabilityFilter;
import com.netsuite.webservices.platform.core.Record;
import com.netsuite.webservices.platform.core.SsoCredentials;
import com.netsuite.webservices.platform.messages.AsyncResult;
import com.netsuite.webservices.platform.messages.ReadResponse;
import com.netsuite.webservices.platform.messages.ReadResponseList;
import com.netsuite.webservices.platform.messages.SessionResponse;
import com.netsuite.webservices.platform.messages.WriteResponse;
import com.netsuite.webservices.platform.messages.WriteResponseList;

/**
 * A wrapper of the NetSuiteServiceManager class that provides the following functionality.<br><br>  
 * 1.)  Concurrent Request Processing Across Multiple NetSuite Service Managers.<br><br>
 * 
 * The NetSuiteServicePool class extends the functionality of the NetSuiteServiceManager class to <br>
 * provide the ability to submit requests across multiple service managers.  This class is very <br>
 * useful for applications that need more than one NetSuite web services session.  Stateless requests <br>
 * can be made directly against the pool.  State-full requests can be made by first allocating an <br>
 * available NetSuiteServiceManager instance�don�t forget to release it when done.
 * 
 * @author Celigo Technologies
 */
public class NetSuiteServicePool {
	
	protected static transient Log log = LogFactory.getLog(NetSuiteServicePool.class);
	private ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
	private NetSuiteServicePoolManager servicePoolManager;
	
	/**
	 * 
	 * @throws NsException
	 */
	public WriteResponse add(Record record, Boolean ignoreReadOnlyFields) throws NsException {
		NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.add(record, ignoreReadOnlyFields);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}
	
	/**
	 * 
	 * @throws NsException
	 */
	public WriteResponse add(Record record) throws NsException {
		NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.add(record);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}
	
	/**
	 * 
	 * @throws NsException
	 */
    public ReadResponse initialize(InitializeRecord iRecord) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.initialize(iRecord);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse attach(AttachReference attachReference) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.attach(attachReference);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse detach(AttachReference attachReference) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.detach(attachReference);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList addList(Record[] records, Boolean ignoreReadOnlyFields) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.addList(records, ignoreReadOnlyFields);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
    }
    
    public WriteResponseList addList(Record[] records) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.addList(records);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse delete(BaseRef baseRef) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.delete(baseRef);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList deleteList(BaseRef[] baseRefs) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.deleteList(baseRefs);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
    }

    /**
	 * 
	 * @throws NsException
	 */
    public SessionResponse mapSso(SsoCredentials ssoCredentials) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.mapSso(ssoCredentials);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}
    
    /**
	 * 
	 * @throws NsException
	 */
    public ReadResponse get(BaseRef baseRef) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.get(baseRef);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public ReadResponseList getList(BaseRef[] baseRefs) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getList(baseRefs);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public GetDeletedResult getDeleted(GetDeletedFilter getDeletedFilter) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getDeleted(getDeletedFilter);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
	public WriteResponse update(Record record, Boolean ignoreReadOnlyFields) throws NsException {
		NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.update(record, ignoreReadOnlyFields);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}
	
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse update(Record record) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.update(record);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}
    
    public WriteResponseList updateList(Record[] records) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.updateList(records);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
    }
    
    public WriteResponseList updateList(Record[] records, Boolean ignoreReadOnlyFields) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.updateList(records, ignoreReadOnlyFields);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
    }

    /**
	 * 
	 * @throws NsException
	 */
    public GetAllResult getAll(GetAllRecord getAllRecord) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getAll(getAllRecord);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public GetSelectValueResultHelper getSelectValue(GetSelectValueRequestHelper getSelectValueRequestHelper) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getSelectValue(getSelectValueRequestHelper);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public GetCustomizationIdResult getCustomizationId(GetCustomizationIdRequest getCustomizationIdRequest) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getCustomizationId(getCustomizationIdRequest);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public GetItemAvailabilityResult getItemAvailability(ItemAvailabilityFilter itemAvailabilityFilter) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getItemAvailability(itemAvailabilityFilter);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncAddList(Record[] records) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.asyncAddList(records);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncUpdateList(Record[] records) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.asyncUpdateList(records);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncDeleteList(BaseRef[] baseRefs) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.asyncDeleteList(baseRefs);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncGetList(BaseRef[] baseRefs) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.asyncGetList(baseRefs);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncResult getAsyncResult(String s, int i) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.getAsyncResult(s, i);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult checkAsyncStatus(String s) throws NsException {
    	NetSuiteServiceManager svcMan = getServicePoolManager().getServiceManager();
    	try {
    		return svcMan.checkAsyncStatus(s);
    	} finally {
    		getServicePoolManager().releaseServiceManager(svcMan);
    	}
	}

	public NetSuiteServicePoolManager getServicePoolManager() {
		return servicePoolManager;
	}

	public void setServicePoolManager(NetSuiteServicePoolManager servicePoolManager) {
		this.servicePoolManager = servicePoolManager;
	}

	public ReentrantReadWriteLock getCacheLock() {
		return cacheLock;
	}
}
