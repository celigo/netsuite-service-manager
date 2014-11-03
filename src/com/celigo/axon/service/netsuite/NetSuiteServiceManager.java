package com.celigo.axon.service.netsuite;

import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.rpc.soap.SOAPFaultException;
import javax.xml.soap.SOAPException;

import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.celigo.axon.service.netsuite.adaptors.GetCustomizationIdRequest;
import com.celigo.axon.service.netsuite.adaptors.GetSelectValueRequestHelper;
import com.celigo.axon.service.netsuite.adaptors.GetSelectValueResultHelper;
import com.netledger.forpartners.encryption.NLrsa;
import com.netledger.forpartners.encryption.Utils;
import com.netsuite.webservices.platform.NetSuiteBindingStub;
import com.netsuite.webservices.platform.NetSuitePortType;
import com.netsuite.webservices.platform.NetSuiteServiceLocator;
import com.netsuite.webservices.platform.core.AsyncStatusResult;
import com.netsuite.webservices.platform.core.AttachReference;
import com.netsuite.webservices.platform.core.BaseRef;
import com.netsuite.webservices.platform.core.CustomizationType;
import com.netsuite.webservices.platform.core.GetAllRecord;
import com.netsuite.webservices.platform.core.GetAllResult;
import com.netsuite.webservices.platform.core.GetCustomizationIdResult;
import com.netsuite.webservices.platform.core.GetDeletedFilter;
import com.netsuite.webservices.platform.core.GetDeletedResult;
import com.netsuite.webservices.platform.core.GetItemAvailabilityResult;
import com.netsuite.webservices.platform.core.GetSavedSearchRecord;
import com.netsuite.webservices.platform.core.GetSavedSearchResult;
import com.netsuite.webservices.platform.core.GetSelectValueResult;
import com.netsuite.webservices.platform.core.InitializeRecord;
import com.netsuite.webservices.platform.core.ItemAvailabilityFilter;
import com.netsuite.webservices.platform.core.Passport;
import com.netsuite.webservices.platform.core.Record;
import com.netsuite.webservices.platform.core.RecordRef;
import com.netsuite.webservices.platform.core.SearchRecord;
import com.netsuite.webservices.platform.core.SearchResult;
import com.netsuite.webservices.platform.core.SsoCredentials;
import com.netsuite.webservices.platform.core.SsoPassport;
import com.netsuite.webservices.platform.core.Status;
import com.netsuite.webservices.platform.core.StatusDetail;
import com.netsuite.webservices.platform.faults.InvalidCredentialsFault;
import com.netsuite.webservices.platform.faults.InvalidSessionFault;
import com.netsuite.webservices.platform.faults.types.StatusDetailCodeType;
import com.netsuite.webservices.platform.messages.AsyncResult;
import com.netsuite.webservices.platform.messages.Preferences;
import com.netsuite.webservices.platform.messages.ReadResponse;
import com.netsuite.webservices.platform.messages.ReadResponseList;
import com.netsuite.webservices.platform.messages.SearchPreferences;
import com.netsuite.webservices.platform.messages.SessionResponse;
import com.netsuite.webservices.platform.messages.WriteResponse;
import com.netsuite.webservices.platform.messages.WriteResponseList;


/**
 * A wrapper of the generated NetSuitePortType class that provides the following functionality.<br><br>  
 * 1.)  Batch Processing.<br> 
 * 2.)  Robust Request Processing.<br> 
 * 3.)  Concurrent Request Processing.<br><br>
 * 
 * The NetSuiteServiceManager class provides a very useful interface to the operations supported by <br>
 * NetSuite�s web services.  All session management is seamlessly handled under the covers�including <br>
 * logging in, session validation, fail and retry attempts, and more.  The service manager class is 100% <br>
 * thread safe�instantiate one object and share it with as many threads as needed.  Built in support <br>
 * for batch processing is also included�at instantiation, simply specify the desired batch sizes for <br>
 * adds, updates, and deletes.  This class is a great starting point for any java based application that <br>
 * needs access to NetSuite�s web services.
 * 
 * @author Celigo Technologies
 */
public class NetSuiteServiceManager {

	protected static transient Log log = LogFactory.getLog(NetSuiteServiceManager.class);

	private NetSuiteCredential netSuiteCredential;
	
	private int retryCount = 3;
	private int retriesBeforeLogin = 2;
	private int retryInterval = 5;
	private int timeout = 10;
	
	private int addRequestSize = 10;
	private int updateRequestSize = 10;
	private int deleteRequestSize = 10;
	private int searchPageSize = 1000;	
	private boolean bodyFieldsOnly = true;
	
	private boolean treatWarningsAsErrors = false;
	private boolean disableMandatoryCustomFieldValidation = false;

	private boolean useRequestLevelCredentials = false;
	
	private String endpointUrl = "https://webservices.netsuite.com/services/NetSuitePort_2012_1";
	
	private NetSuiteServiceLocator nss;
	private NetSuitePortType nsPort;

	private SearchPreferences searchPreferences;
	private Preferences preferences;
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	
	public NetSuiteLoginResponse login() throws NsException {
		getLock().writeLock().lock();
		try {
			return relogin();
		} finally {
			getLock().writeLock().unlock();
		}
	}
	
	/**
	 * 
	 * @throws NsException
	 */
	public WriteResponse add(Record record, Boolean ignoreReadOnlyFields) throws NsException {
		if (record == null)
			return new WriteResponse();
		Preferences p = new Preferences();
		p.setIgnoreReadOnlyFields(ignoreReadOnlyFields);
        return (WriteResponse)submitRobustly(record, new Exception().getStackTrace()[0].getMethodName(), null, p);
    }
	
	/**
	 * 
	 * @throws NsException
	 */
	public WriteResponse add(Record record) throws NsException {
		if (record == null)
			return new WriteResponse();
        return (WriteResponse)submitRobustly(record, new Exception().getStackTrace()[0].getMethodName());
    }
	
	/**
	 * 
	 * @throws NsException
	 */
    public ReadResponse initialize(InitializeRecord iRecord) throws NsException {
    	if (iRecord == null)
			return new ReadResponse();
        return (ReadResponse)submitRobustly(iRecord, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse attach(AttachReference attachReference) throws NsException {
    	if (attachReference == null)
    		return new WriteResponse();
        return (WriteResponse)submitRobustly(attachReference, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse detach(AttachReference attachReference) throws NsException {
    	if (attachReference == null)
    		return new WriteResponse();
        return (WriteResponse)submitRobustly(attachReference, new Exception().getStackTrace()[0].getMethodName());
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList addList(Record[] records, Boolean ignoreReadOnlyFields) throws NsException {
    	if (records == null || records.length == 0) {
    		WriteResponseList wrl = new WriteResponseList();
    		WriteResponse[] wr = new WriteResponse[0];
    		wrl.setWriteResponse(wr);
    		return wrl;
    	}
    	Preferences p = new Preferences();
		p.setIgnoreReadOnlyFields(ignoreReadOnlyFields);
    	return processRecordAddsInBatchMode(records, new Exception().getStackTrace()[0].getMethodName(), p);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList addList(Record[] records) throws NsException {
    	return addList(records, null);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse delete(BaseRef baseRef) throws NsException {
    	if (baseRef == null)
    		return new WriteResponse();
        return (WriteResponse)submitRobustly(baseRef, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList deleteList(BaseRef[] baseRefs) throws NsException {
    	if (baseRefs == null || baseRefs.length == 0) {
    		WriteResponseList wrl = new WriteResponseList();
    		WriteResponse[] wr = new WriteResponse[0];
    		wrl.setWriteResponse(wr);
    		return wrl;
    	}
        return processRecordDeletesInBatchMode(baseRefs, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public SessionResponse mapSso(SsoCredentials ssoCredentials) throws NsException {
        return (SessionResponse)submitRobustly(ssoCredentials, new Exception().getStackTrace()[0].getMethodName());
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public ReadResponse get(BaseRef baseRef) throws NsException {
    	if (baseRef == null)
    		return new ReadResponse();
        return (ReadResponse)submitRobustly(baseRef, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public ReadResponseList getList(BaseRef[] baseRefs) throws NsException {
    	if (baseRefs == null || baseRefs.length == 0) {
    		ReadResponseList rrl = new ReadResponseList();
    		ReadResponse[] rr = new ReadResponse[0];
    		rrl.setReadResponse(rr);
    		return rrl;
    	}
        return (ReadResponseList)submitRobustly(baseRefs, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public GetDeletedResult getDeleted(GetDeletedFilter getDeletedFilter) throws NsException {
        return (GetDeletedResult)submitRobustly(getDeletedFilter, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult search(SearchRecord searchRecord) throws NsException {
        return search(searchRecord, getSearchPreferences());
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult search(SearchRecord searchRecord, boolean bodyFieldOnly) throws NsException {
    	SearchPreferences prefs = new SearchPreferences();
    	prefs.setBodyFieldsOnly(bodyFieldOnly);
    	prefs.setPageSize(getSearchPageSize());
        return search(searchRecord, prefs);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult search(SearchRecord searchRecord, SearchPreferences preferences) throws NsException {
        return (SearchResult)submitRobustly(searchRecord, new Exception().getStackTrace()[0].getMethodName(), preferences, null);
    }

    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchMore(int i) throws NsException {
        return searchMore(new Integer(i), getSearchPreferences());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchMore(int i, boolean bodyFieldOnly) throws NsException {
    	SearchPreferences prefs = new SearchPreferences();
    	prefs.setBodyFieldsOnly(bodyFieldOnly);
    	prefs.setPageSize(getSearchPageSize());
        return searchMore(new Integer(i), prefs);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchMore(int i, SearchPreferences preferences) throws NsException {
        return (SearchResult)submitRobustly(new Integer(i), new Exception().getStackTrace()[0].getMethodName(), preferences, null);
    }

    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchMoreWithId(String searchId, int pageIndex) throws NsException {
        return searchMoreWithId(searchId, pageIndex, getSearchPreferences());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchMoreWithId(String searchId, int pageIndex, boolean bodyFieldOnly) throws NsException {
    	SearchPreferences prefs = new SearchPreferences();
    	prefs.setBodyFieldsOnly(bodyFieldOnly);
    	prefs.setPageSize(getSearchPageSize());
        return searchMoreWithId(searchId, pageIndex, prefs);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchMoreWithId(String searchId, int pageIndex, SearchPreferences preferences) throws NsException {
        return (SearchResult)submitRobustly(new InputWrapper(searchId, pageIndex), new Exception().getStackTrace()[0].getMethodName(), preferences, null);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchNext() throws NsException {
        return searchNext(getSearchPreferences());
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchNext(boolean bodyFieldOnly) throws NsException {
    	SearchPreferences prefs = new SearchPreferences();
    	prefs.setBodyFieldsOnly(bodyFieldOnly);
    	prefs.setPageSize(getSearchPageSize());
        return searchNext(prefs);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public SearchResult searchNext(SearchPreferences preferences) throws NsException {
        return (SearchResult)submitRobustly(null, new Exception().getStackTrace()[0].getMethodName(), preferences, null);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
	public WriteResponse update(Record record, Boolean ignoreReadOnlyFields) throws NsException {
		if (record == null)
			return new WriteResponse();
		Preferences p = new Preferences();
		p.setIgnoreReadOnlyFields(ignoreReadOnlyFields);
        return (WriteResponse)submitRobustly(record, new Exception().getStackTrace()[0].getMethodName(), null, p);
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponse update(Record record) throws NsException {
    	if (record == null)
			return new WriteResponse();
        return (WriteResponse)submitRobustly(record, new Exception().getStackTrace()[0].getMethodName());
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList updateList(Record[] records, Boolean ignoreReadOnlyFields) throws NsException {
    	if (records == null || records.length == 0) {
    		WriteResponseList wrl = new WriteResponseList();
    		WriteResponse[] wr = new WriteResponse[0];
    		wrl.setWriteResponse(wr);
    		return wrl;
    	}
    	Preferences p = new Preferences();
		p.setIgnoreReadOnlyFields(ignoreReadOnlyFields);
    	return processRecordUpdatesInBatchMode(records, new Exception().getStackTrace()[0].getMethodName(), p);
    }

    /**
	 * 
	 * @throws NsException
	 */
    public WriteResponseList updateList(Record[] records) throws NsException{
    	return updateList(records, null);
    }

    /**
	 * 
	 * @throws NsException
	 */
    public GetAllResult getAll(GetAllRecord getAllRecord) throws NsException {
        return (GetAllResult)submitRobustly(getAllRecord, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public GetSelectValueResultHelper getSelectValue(GetSelectValueRequestHelper getSelectValueRequestHelper) throws NsException {
        GetSelectValueResult getSelectValueResult = (GetSelectValueResult) submitRobustly(getSelectValueRequestHelper, new Exception().getStackTrace()[0].getMethodName());
        
        GetSelectValueResultHelper getSelectValueResultHelper = new GetSelectValueResultHelper();
        getSelectValueResultHelper.setGetSelectValueResult(getSelectValueResult);
        
        return getSelectValueResultHelper;
    }

    /**
	 * 
	 * @throws NsException
	 */
    public GetCustomizationIdResult getCustomizationId(GetCustomizationIdRequest getCustomizationIdRequest) throws NsException {
    	return (GetCustomizationIdResult)submitRobustly(getCustomizationIdRequest, new Exception().getStackTrace()[0].getMethodName());
    }
    
    /**
	 * 
	 * @throws NsException
	 */
    public GetSavedSearchResult getSavedSearch(GetSavedSearchRecord getSavedSearchRecord) throws NsException {
    	return (GetSavedSearchResult)submitRobustly(getSavedSearchRecord, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public GetItemAvailabilityResult getItemAvailability(ItemAvailabilityFilter itemAvailabilityFilter) throws NsException {
        return (GetItemAvailabilityResult)submitRobustly(itemAvailabilityFilter, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncAddList(Record[] records) throws NsException {
        return (AsyncStatusResult)submitRobustly(records, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncUpdateList(Record[] records) throws NsException {
        return (AsyncStatusResult)submitRobustly(records, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncDeleteList(BaseRef[] baseRefs) throws NsException {
        return (AsyncStatusResult)submitRobustly(baseRefs, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncGetList(BaseRef[] baseRefs) throws NsException {
        return (AsyncStatusResult)submitRobustly(baseRefs, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult asyncSearch(SearchRecord searchRecord) throws NsException {
        return (AsyncStatusResult)submitRobustly(searchRecord, new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncResult getAsyncResult(String s, int i) throws NsException {
        return (AsyncResult)submitRobustly(new InputWrapper(s, i), new Exception().getStackTrace()[0].getMethodName());
    }

    /**
	 * 
	 * @throws NsException
	 */
    public AsyncStatusResult checkAsyncStatus(String s) throws NsException {
        return (AsyncStatusResult)submitRobustly(s, new Exception().getStackTrace()[0].getMethodName());
    }

    private class InputWrapper {
        public String s;
        public int i;

        public InputWrapper(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }

    private WriteResponseList processRecordDeletesInBatchMode(BaseRef[] refs, String methodName) {
    	ArrayList<WriteResponse> responses = new ArrayList<WriteResponse>();
		
    	BaseRef[] batch = new BaseRef[getDeleteRequestSize()];
    	for (int i=0; i<refs.length; i++) {    		
    		if (i != 0 && (i%getDeleteRequestSize() == 0)) {
    			processDeleteBatch(batch, responses, methodName);  			
    			batch = new BaseRef[getDeleteRequestSize()];    			
    		}    		
    		batch[i%getDeleteRequestSize()] = refs[i];
    	}   	
    	
    	int leftOverCount = 0;
    	for (int j=0; j<batch.length; j++)
    		if (batch[j] != null)
    			leftOverCount++;
    	
    	BaseRef[] leftOvers = new BaseRef[leftOverCount];
    	for (int j=0; j<leftOvers.length; j++)
    		leftOvers[j] = batch[j];
    	
    	processDeleteBatch(leftOvers, responses, methodName); 
		
		WriteResponseList wrl = new WriteResponseList();
    	wrl.setWriteResponse(new WriteResponse[refs.length]);
    	for (int i=0; i<responses.size();i++)
    		wrl.getWriteResponse()[i] = responses.get(i); 
    	return wrl;
    }
    
    private void processDeleteBatch(BaseRef[] batch, ArrayList<WriteResponse> responses, String methodName) {
    	log.debug("Processing Batch -- Size: " + batch.length + " Ref(s), Operation: " + methodName);
    	try {
			WriteResponseList wrlBatch = (WriteResponseList)submitRobustly(batch, methodName);
			for (int j=0; j<batch.length; j++)
				responses.add(wrlBatch.getWriteResponse()[j]);    
		} catch (Exception e) {
			log.debug("Processing Batch Error: " + e.getMessage());
			for (int j=0; j<batch.length; j++) {				
				WriteResponse wr = new WriteResponse();
				Status s = new Status();
				s.setIsSuccess(false);
				StatusDetail sd = new StatusDetail();
				sd.setCode(StatusDetailCodeType.UNEXPECTED_ERROR);
				sd.setMessage("Batch threw Exception");
				s.setStatusDetail(new StatusDetail[]{sd});
				wr.setStatus(s);
				responses.add(wr);
			}
		}  
    }
    
    private WriteResponseList processRecordAddsInBatchMode(Record[] records, String methodName, Preferences preferences) {
    	return processRecordsInBatchMode(records, methodName, getAddRequestSize(), preferences);
    }
    
    private WriteResponseList processRecordUpdatesInBatchMode(Record[] records, String methodName, Preferences preferences) {
    	return processRecordsInBatchMode(records, methodName, getUpdateRequestSize(), preferences);
    }
    
    private WriteResponseList processRecordsInBatchMode(Record[] records, String methodName, int batchSize, Preferences preferences) {
    	ArrayList<WriteResponse> responses = new ArrayList<WriteResponse>();
		
		Record[] batch = new Record[batchSize];
    	for (int i=0; i<records.length; i++) {    		
    		if (i != 0 && (i%batchSize == 0)) {
    			processBatch(batch, responses, methodName, preferences);  			
    			batch = new Record[batchSize];    			
    		}    		
    		batch[i%batchSize] = records[i];
    	}   	
    	
    	int leftOverCount = 0;
    	for (int j=0; j<batch.length; j++)
    		if (batch[j] != null)
    			leftOverCount++;
    	
    	Record[] leftOvers = new Record[leftOverCount];
    	for (int j=0; j<leftOvers.length; j++)
    		leftOvers[j] = batch[j];
    	
    	processBatch(leftOvers, responses, methodName, preferences); 
		
		WriteResponseList wrl = new WriteResponseList();
    	wrl.setWriteResponse(new WriteResponse[records.length]);
    	for (int i=0; i<responses.size();i++)
    		wrl.getWriteResponse()[i] = responses.get(i); 
    	return wrl;
    }
    
    private void processBatch(Record[] batch, ArrayList<WriteResponse> responses, String methodName, Preferences preferences) {
    	log.debug("Processing Batch -- Size: " + batch.length + " Record(s), Operation: " + methodName);
    	try {
			WriteResponseList wrlBatch = (WriteResponseList)submitRobustly(batch, methodName, null, preferences);
			for (int j=0; j<batch.length; j++)
				responses.add(wrlBatch.getWriteResponse()[j]);    
		} catch (Exception e) {
			log.debug("Processing Batch Error: " + e.getMessage());
			for (int j=0; j<batch.length; j++) {				
				WriteResponse wr = new WriteResponse();
				Status s = new Status();
				s.setIsSuccess(false);
				StatusDetail sd = new StatusDetail();
				sd.setCode(StatusDetailCodeType.UNEXPECTED_ERROR);
				sd.setMessage("Batch threw Exception");
				s.setStatusDetail(new StatusDetail[]{sd});
				wr.setStatus(s);
				responses.add(wr);
			}
		}  
    }
    
    private Object submitRobustly (Object arg, String method) throws NsException {
    	return submitRobustly(arg, method, null, null);
    }
    
    private Object submitRobustly (Object arg, String method, SearchPreferences searchPreferences, Preferences preferences) throws NsException {
    	if (!isUseRequestLevelCredentials()) {
    		return submitRobustlyUsingLoginOperation(arg, method, searchPreferences, preferences);
    	} else {
    		return submitRobustlyUsingRequestLevelCredentials(arg, method, searchPreferences, preferences);
    	}
    }
    
    private Object submitRobustlyUsingLoginOperation (Object arg, String method, SearchPreferences searchPreferences, Preferences preferences) throws NsException {

    	getLock().writeLock().lock();
		try {
	    	implicitLogin();
	    	
	    	setHeaders(searchPreferences, preferences);
	    		
	        Class<?> argClass = null;
	
	        if (arg instanceof SearchRecord)
	            argClass = SearchRecord.class;
	        else if (arg instanceof Record)
	            argClass = Record.class;
	        else if (arg instanceof BaseRef)
	            argClass = BaseRef.class;
	        else if (arg instanceof Record[])
	            argClass = Record[].class;
	        else if (arg instanceof BaseRef[])
	            argClass = BaseRef[].class;
	        else if (arg instanceof CustomizationType)
	            argClass = CustomizationType.class;
	        else if (arg instanceof AttachReference)
	            argClass = AttachReference.class;
	        else if (arg == null);
	
	        else
	            argClass = arg.getClass();
	
	        Object res = null;
	        for (int i=0; i<getRetryCount(); i++) {
	        
	            try {
	                if (method.equals("searchMore")) {
	                    try {
	                        res = nsPort.searchMore(((Integer)arg).intValue());
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else if (method.equals("searchMoreWithId")) {
	                    try {
	                        res = nsPort.searchMoreWithId(((InputWrapper)arg).s, ((InputWrapper)arg).i);
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else if (method.equals("getAsyncResult")) {
	                    try {
	                        res = nsPort.getAsyncResult(((InputWrapper)arg).s, ((InputWrapper)arg).i);
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else {
	                    if (argClass != null)
	                        res = nsPort.getClass().getMethod(method, new Class[]{argClass}).invoke(nsPort, new Object[]{arg});
	                    else
	                        res = nsPort.getClass().getMethod(method, new Class[]{}).invoke(nsPort, new Object[]{});
	                }
	                
	                if (sessionTimedOut(res))
	                	throw new InvocationTargetException(new CeligoSessionTimedOut());
	
	            } catch (InvocationTargetException e) {            	
	            	    	
	            	if (errorCanBeWorkedAround (e.getTargetException())) { 
	            		log.debug("Operation Failed with Exception: " + e.getTargetException().getClass().getName());          	
	            		log.debug(e.getTargetException());  
	            		log.debug("Attempting Work Around.");
	            		waitForRetyInterval();                    
	            		if (errorRequiresANewLogin(e.getTargetException()) || i>=getRetriesBeforeLogin()-1) {
	                    	relogin();
	                    	setHeaders(searchPreferences, preferences);
	            		}
	            		continue;            		
	                } else {
	                	log.error("Operation Failed with Exception: " + e.getTargetException().getClass().getName());          	
	                	log.error(e.getTargetException()); 
	                	throw new NsException(e.getTargetException().getMessage());
	                }
	
	            } catch (Exception e) {
	            	log.error(e.getClass().getName());             	
	            	throw new NsException(e.getMessage());
	            }
	            
	            break;
	        }
	        
	        return res;   
		} finally {
			getLock().writeLock().unlock();
		}
    }

    
    private Object submitRobustlyUsingRequestLevelCredentials (Object arg, String method, SearchPreferences searchPreferences, Preferences preferences) throws NsException {

    	getLock().writeLock().lock();
		try {
			System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
			
			nss = new NetSuiteServiceLocator();
			nss.setMaintainSession(false);
			
			try {
				nsPort = nss.getNetSuitePort(new URL(endpointUrl));
			} catch (Exception e) {throw new NsException(e.getMessage());} 
			
			Passport ppt = new Passport();
			ppt.setAccount(getNetSuiteCredential().getAccount());
			ppt.setEmail(getNetSuiteCredential().getEmail());
			ppt.setPassword(getNetSuiteCredential().getPassword());
			RecordRef rr = new RecordRef();
			rr.setInternalId(getNetSuiteCredential().getRoleId());
			ppt.setRole(rr);

			SOAPHeaderElement passportHeader = new SOAPHeaderElement("urn:messages.platform.webservices.netsuite.com", "passport");
			try {
				passportHeader.setObjectValue(ppt);
			} catch (SOAPException e) {
				{throw new NsException(e.getMessage());} 
			}
			
			org.apache.axis.client.Stub st = (org.apache.axis.client.Stub)nsPort;
			st.setTimeout(1000 * 60 * getTimeout()); 
			
			this.searchPreferences = new SearchPreferences();
			this.searchPreferences.setPageSize(searchPageSize);
			this.searchPreferences.setBodyFieldsOnly(Boolean.valueOf(isBodyFieldsOnly()));
			
			this.preferences = new Preferences();
			this.preferences.setDisableMandatoryCustomFieldValidation(isDisableMandatoryCustomFieldValidation());
			this.preferences.setWarningAsError(isTreatWarningsAsErrors());
	    	
	    	setHeaders(searchPreferences, preferences);
	    	st.setHeader(passportHeader);
	    		
	        Class<?> argClass = null;
	
	        if (arg instanceof SearchRecord)
	            argClass = SearchRecord.class;
	        else if (arg instanceof Record)
	            argClass = Record.class;
	        else if (arg instanceof BaseRef)
	            argClass = BaseRef.class;
	        else if (arg instanceof Record[])
	            argClass = Record[].class;
	        else if (arg instanceof BaseRef[])
	            argClass = BaseRef[].class;
	        else if (arg instanceof CustomizationType)
	            argClass = CustomizationType.class;
	        else if (arg instanceof AttachReference)
	            argClass = AttachReference.class;
	        else if (arg == null);
	
	        else
	            argClass = arg.getClass();
	
	        Object res = null;
	        for (int i=0; i<getRetryCount(); i++) {
	        
	            try {
	                if (method.equals("searchMore")) {
	                    try {
	                        res = nsPort.searchMore(((Integer)arg).intValue());
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else if (method.equals("searchMoreWithId")) {
	                    try {
	                        res = nsPort.searchMoreWithId(((InputWrapper)arg).s, ((InputWrapper)arg).i);
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else if (method.equals("getAsyncResult")) {
	                    try {
	                        res = nsPort.getAsyncResult(((InputWrapper)arg).s, ((InputWrapper)arg).i);
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else if (method.equals("getSelectValue")) {
	                    try {
	                    	res = nsPort.getSelectValue(((GetSelectValueRequestHelper)arg).getGetSelectValueField(), ((GetSelectValueRequestHelper)arg).getPageIndex());
	                    } catch (Exception e) {
	                        throw new InvocationTargetException(e);
	                    }
	                } else {
	                    if (argClass != null)
	                        res = nsPort.getClass().getMethod(method, new Class[]{argClass}).invoke(nsPort, new Object[]{arg});
	                    else
	                        res = nsPort.getClass().getMethod(method, new Class[]{}).invoke(nsPort, new Object[]{});
	                }
	                
	            } catch (InvocationTargetException e) {            	
	            	    	
	            	if (errorCanBeWorkedAround (e.getTargetException())) { 
	            		log.debug("Operation Failed with Exception: " + e.getTargetException().getClass().getName());          	
	            		log.debug(e.getTargetException());  
	            		log.debug("Attempting Work Around.");
	            		waitForRetyInterval();                    
	            		continue;            		
	                } else {
	                	log.error("Operation Failed with Exception: " + e.getTargetException().getClass().getName());          	
	                	log.error(e.getTargetException()); 
	                	throw new NsException(e.getTargetException().getMessage());
	                }
	
	            } catch (Exception e) {
	            	log.error(e.getClass().getName());             	
	            	throw new NsException(e.getMessage());
	            }
	            
	            break;
	        }
	        
	        return res;   
		} finally {
			getLock().writeLock().unlock();
		}
    }
    
    private boolean sessionTimedOut(Object res) {
    	if (res == null)
    		return false;
    	
    	if (res instanceof WriteResponse) {
        	WriteResponse wr = (WriteResponse)res;
        	if (wr.getStatus() != null && 
        		!wr.getStatus().isIsSuccess() && 
        		wr.getStatus().getStatusDetail() != null &&
    			wr.getStatus().getStatusDetail().length > 0 && 
    			StatusDetailCodeType.SESSION_TIMED_OUT.equals(wr.getStatus().getStatusDetail()[0].getCode()))
        		return true;
        }
        
    	else if (res instanceof ReadResponse) {
        	ReadResponse rr = (ReadResponse)res;
        	if (rr.getStatus() != null && 
        		!rr.getStatus().isIsSuccess() && 
        		rr.getStatus().getStatusDetail() != null &&
        		rr.getStatus().getStatusDetail().length > 0 && 
    			StatusDetailCodeType.SESSION_TIMED_OUT.equals(rr.getStatus().getStatusDetail()[0].getCode()))
        		return true;
        }
        
    	else if (res instanceof SearchResult) {
        	SearchResult sr = (SearchResult)res;
        	if (sr.getStatus() != null && 
        		!sr.getStatus().isIsSuccess() && 
        		sr.getStatus().getStatusDetail() != null &&
        		sr.getStatus().getStatusDetail().length > 0 && 
    			StatusDetailCodeType.SESSION_TIMED_OUT.equals(sr.getStatus().getStatusDetail()[0].getCode()))
        		return true;
        }
        
    	else if (res instanceof GetSelectValueResult) {
        	GetSelectValueResult gsvr = (GetSelectValueResult)res;
        	if (gsvr.getStatus() != null && 
        		!gsvr.getStatus().isIsSuccess() && 
        		gsvr.getStatus().getStatusDetail() != null &&
        		gsvr.getStatus().getStatusDetail().length > 0 && 
    			StatusDetailCodeType.SESSION_TIMED_OUT.equals(gsvr.getStatus().getStatusDetail()[0].getCode()))
        		return true;
        }
        
    	else if (res instanceof WriteResponseList) {
        	WriteResponseList wrl = (WriteResponseList)res;
        	if (wrl.getWriteResponse() != null && 
        		wrl.getWriteResponse().length > 0 && 
        		sessionTimedOut(wrl.getWriteResponse()[0])) {
        			for (int i=1; i<wrl.getWriteResponse().length; i++)
        				if (!sessionTimedOut(wrl.getWriteResponse()[i]))
        					return false;
        			return true;
        	}
        }
        
        return false;
    }
    
    private void setHeaders(SearchPreferences searchPreferences, Preferences preferences) {
    	NetSuiteBindingStub stub = (NetSuiteBindingStub)nsPort;
    	stub.clearHeaders();
    	if (searchPreferences != null) {    		
    		if (searchPreferences.getPageSize() == null) {
    			searchPreferences.setPageSize(getSearchPageSize());
    		}
    		if (searchPreferences.getBodyFieldsOnly() == null) {
    			searchPreferences.setBodyFieldsOnly(isBodyFieldsOnly());
    		}
    		
    		stub.setHeader("urn:messages.platform.webservices.netsuite.com",
    				"searchPreferences", searchPreferences);
    	} else {
    		stub.setHeader("urn:messages.platform.webservices.netsuite.com",
    				"searchPreferences", getSearchPreferences());
    	}
    	
    	if (preferences != null) {
    		if (preferences.getDisableMandatoryCustomFieldValidation() == null) {
    			preferences.setDisableMandatoryCustomFieldValidation(isDisableMandatoryCustomFieldValidation());
    		}
    		if (preferences.getWarningAsError() == null) {
    			preferences.setWarningAsError(isTreatWarningsAsErrors());
    		}
    		
    		stub.setHeader("urn:messages.platform.webservices.netsuite.com",
    				"preferences", preferences);
    	} else {
    		stub.setHeader("urn:messages.platform.webservices.netsuite.com",
    				"preferences", getPreferences());
    	}
    }
    
    private boolean errorCanBeWorkedAround (Throwable t) {
        if (t instanceof InvalidSessionFault || 
    		t instanceof RemoteException ||
        	t instanceof SOAPFaultException ||
        	t instanceof CeligoSessionTimedOut ||
        	t instanceof SocketException)
        	return true;
        
        return false;
    }
    
    private class CeligoSessionTimedOut extends Exception {

		private static final long serialVersionUID = 1L;
    	
    }
    
    private boolean errorRequiresANewLogin (Throwable t) {
	    if (t instanceof InvalidSessionFault || t instanceof SocketException || t instanceof CeligoSessionTimedOut)
	    	return true;
  
	    return false;
	}

    private void waitForRetyInterval() {
		try {
			Thread.sleep(getRetryInterval() * 1000);
		} catch (InterruptedException ie) {log.error(ie.getMessage());}
	}

    private NetSuiteLoginResponse implicitLogin() throws NsException {
		return login(false);
	}
	
	private NetSuiteLoginResponse relogin() throws NsException {
		return login(true);
	}
	
	private boolean loggedIn = false;
	private NetSuiteLoginResponse login(boolean relogin) throws NsException {	
		
		if (relogin)
			loggedIn = false;
		
		if (loggedIn)
			return null;
		
		if (nsPort != null) {
			try {
				log.debug("Calling: nsPort.logout()");
				nsPort.logout();
			} catch (Exception e) {}
		}
		
		System.setProperty("axis.socketSecureFactory", "org.apache.axis.components.net.SunFakeTrustSocketFactory");
		
		nss = new NetSuiteServiceLocator();
		nss.setMaintainSession(true);
		
		try {
			nsPort = nss.getNetSuitePort(new URL(endpointUrl));
		} catch (Exception e) {throw new NsException(e.getMessage());} 
		
		Passport ppt = new Passport();
		SsoPassport ssoPassport = new SsoPassport();
		RecordRef rr = new RecordRef();
		rr.setInternalId(getNetSuiteCredential().getRoleId());
		
		if (!getNetSuiteCredential().isUseSsoLogin()) {
			ppt.setAccount(getNetSuiteCredential().getAccount());
			ppt.setEmail(getNetSuiteCredential().getEmail());
			ppt.setPassword(getNetSuiteCredential().getPassword());
			ppt.setRole(rr);
			log.debug("Logging into NetSuite [Username=" + ppt.getEmail() + ", Account=" + ppt.getAccount() + ", RoleId=" + ppt.getRole().getInternalId() + "]");
		} else {
			ssoPassport.setPartnerId(getNetSuiteCredential().getPartnerId());
			String time = Long.toString(Calendar.getInstance().getTimeInMillis());
			String unencryptedToken = getNetSuiteCredential().getCompanyId() + " " + getNetSuiteCredential().getUserId() + " " + time;
			String authenticationToken = null;
			try {
				byte[] privKeyBytes = Utils.fileNameToByteArray(getNetSuiteCredential().getPrivateKey());
				byte[] encryptedData = NLrsa.privateEncrypt(unencryptedToken.getBytes(), privKeyBytes);
				authenticationToken = Utils.byteArrayToHexString(encryptedData, false);
			} catch (Exception e) {
				throw new NsException("Unable to generate authentication token", e);
			}
			ssoPassport.setAuthenticationToken(authenticationToken);
			log.debug("Logging (SSO) into NetSuite [Company=" + getNetSuiteCredential().getCompanyId() + ", User=" + getNetSuiteCredential().getUserId() + "]");
		}
		
		Status status = null;
		SessionResponse sessionResponse = null;
		String exceptionMessage = null;
		for (int i=0; i<getRetryCount(); i++) {
			try {
				if (!getNetSuiteCredential().isUseSsoLogin()) {
					sessionResponse = nsPort.login(ppt);
				} else {
					sessionResponse = nsPort.ssoLogin(ssoPassport);
				}
				status = sessionResponse.getStatus();
			
			} catch (InvalidCredentialsFault f) {
				throw new NsException(f.getFaultString());
			} catch (Exception e) {
				exceptionMessage = e.getMessage();
				log.debug("Login Failed with Exception. " + e.getMessage());
			}
			
			if (status != null)
				break;
			
			if (i != getRetryCount()-1) waitForRetyInterval();
		}
		
		if (status == null || !status.isIsSuccess()) {
			String message = "Login Failed:";
			if (status != null && status.getStatusDetail() != null && status.getStatusDetail().length > 0) {
				message = message + " " + status.getStatusDetail()[0].getCode();
				message = message + " " + status.getStatusDetail()[0].getMessage();
			} else if (exceptionMessage != null) {
				message = message + " " + exceptionMessage;
			}
			
			throw new NsException(message);
		}
		
		org.apache.axis.client.Stub st = (org.apache.axis.client.Stub)nsPort;
		st.setTimeout(1000 * 60 * getTimeout()); 
		
		SearchPreferences searchPreferences = new SearchPreferences();
		searchPreferences.setPageSize(searchPageSize);
		searchPreferences.setBodyFieldsOnly(Boolean.valueOf(isBodyFieldsOnly()));
		
		this.searchPreferences = searchPreferences;
		
		Preferences preferences = new Preferences();
		preferences.setDisableMandatoryCustomFieldValidation(isDisableMandatoryCustomFieldValidation());
		preferences.setWarningAsError(isTreatWarningsAsErrors());
		
		this.preferences = preferences;
		
		loggedIn = true;
		
		return new NetSuiteLoginResponse(sessionResponse, ((NetSuiteBindingStub)nsPort).getResponseHeaders());
	}

	public NetSuiteCredential getNetSuiteCredential() {
		return netSuiteCredential;
	}

	public void setNetSuiteCredential(NetSuiteCredential netSuiteCredential) {
		this.netSuiteCredential = netSuiteCredential;
	}

	private int getRetryCount() {
		return retryCount;
	}

	/**
	 * Sets the number of retry attempts made when an operation fails.
	 */
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	private int getRetryInterval() {
		return retryInterval;
	}

	/**
	 * Sets the length of time (in seconds) that a session will sleep before attempting the retry of a failed operation.
	 */
	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}

	private int getTimeout() {
		return timeout;
	}

	/**
	 * Sets the client side timeout (in minutes).
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * Sets the url that this object uses to establish a session.
	 */
	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	private int getAddRequestSize() {
		return addRequestSize;
	}

	/**
	 * Sets the batch size for the addList operation.
	 */
	public void setAddRequestSize(int addRequestSize) {
		this.addRequestSize = addRequestSize;
	}

	private int getUpdateRequestSize() {
		return updateRequestSize;
	}

	/**
	 * Sets the batch size for the updateList operation.
	 */
	public void setUpdateRequestSize(int updateRequestSize) {
		this.updateRequestSize = updateRequestSize;
	}

	public boolean isBodyFieldsOnly() {
		return bodyFieldsOnly;
	}

	public void setBodyFieldsOnly(boolean bodyFieldsOnly) {
		this.bodyFieldsOnly = bodyFieldsOnly;
	}

	public int getDeleteRequestSize() {
		return deleteRequestSize;
	}

	public void setDeleteRequestSize(int deleteRequestSize) {
		this.deleteRequestSize = deleteRequestSize;
	}

	private SearchPreferences getSearchPreferences() {
		return searchPreferences;
	}

	public void setSearchPageSize(int searchPageSize) {
		this.searchPageSize = searchPageSize;
	}
	
	public int getSearchPageSize() {
		return searchPageSize;
	}

	private ReentrantReadWriteLock getLock() {
		return lock;
	}

	public int getRetriesBeforeLogin() {
		return retriesBeforeLogin;
	}

	public void setRetriesBeforeLogin(int retriesBeforeLogin) {
		this.retriesBeforeLogin = retriesBeforeLogin;
	}

	public boolean isTreatWarningsAsErrors() {
		return treatWarningsAsErrors;
	}

	public void setTreatWarningsAsErrors(boolean treatWarningsAsErrors) {
		this.treatWarningsAsErrors = treatWarningsAsErrors;
	}

	public boolean isDisableMandatoryCustomFieldValidation() {
		return disableMandatoryCustomFieldValidation;
	}

	public void setDisableMandatoryCustomFieldValidation(
			boolean disableMandatoryCustomFieldValidation) {
		this.disableMandatoryCustomFieldValidation = disableMandatoryCustomFieldValidation;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public boolean isUseRequestLevelCredentials() {
		return useRequestLevelCredentials;
	}

	public void setUseRequestLevelCredentials(boolean useRequestLevelCredentials) {
		this.useRequestLevelCredentials = useRequestLevelCredentials;
	}

}

