package com.celigo.axon.service.netsuite;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Maintain a pool of NetSuite Service Managers.
 * @author Celigo Technologies
 */
public class NetSuiteServicePoolManager {
	
	private static ArrayList<NetSuiteServicePoolManager> managers = new ArrayList<NetSuiteServicePoolManager>();
	
	protected static transient Log log = LogFactory.getLog(NetSuiteServicePoolManager.class);
	
	private int retryCount = 6;
	private int retryInterval = 5;
	private int timeout = 10;
	
	private int addRequestSize = 10;
	private int updateRequestSize = 10;
	private int deleteRequestSize = 10;
	private int searchPageSize = 1000;	
	private boolean bodyFieldsOnly = true;
	
	private String mailUser;
	private String mailPassword;
	private String mailRecipientTo;
	private String mailRecipientCC1;
	private String mailRecipientCC2;
	
	private String endpointUrl = "https://webservices.netsuite.com/services/NetSuitePort_2012_1";
	private boolean useRequestLevelCredentials = false;
	
	private NetSuiteCredential credential;
	private ArrayBlockingQueue<NetSuiteServiceManager> freeServiceManagers;
	private ArrayList<NetSuiteServiceManager> referenceServiceManagers;
	
	private ReentrantReadWriteLock reInitializationLock = new ReentrantReadWriteLock();
	
	public NetSuiteServicePoolManager() {
		managers.add(this);
	}
	
	public static NetSuiteServicePoolManager[] getAllManagers() {
		return managers.toArray(new NetSuiteServicePoolManager[]{});
	}
	
	public synchronized void reInitialize() throws NsException {
		log.debug("Re-Initializing Pool");
		getReInitializationLock().writeLock().lock();
		try {
			if (initialized)
				while (!getFreeServiceManagers().containsAll(getReferenceServiceManagers()))
					Thread.sleep(10000);
			initialize(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			getReInitializationLock().writeLock().unlock();
			log.debug("Initialization Complete");
		}
	}
	
	/**
	 * Removes a service manager from the pool.
	 * @throws NsException
	 */
    public NetSuiteServiceManager getServiceManager() throws NsException {
    	if (!tryLock())
    		throw new RuntimeException("This process does contain a lock on this service pool resource.  Allocation of a service manager is not allowed.");
    	getReInitializationLock().writeLock().lock();
    	try {
    		initialize(false);
    		return getFreeServiceManagers().take();
    	} catch (InterruptedException e) {
    		throw new RuntimeException(e);
    	} finally {
    		getReInitializationLock().writeLock().unlock();
    	}
    }
    
    /**
	 * Adds the service manager to the pool.  The number of service managers in the pool cannot exceed the number of service mangers initially loaded into the pool.
	 * @throws NsException
	 */
    public void releaseServiceManager(NetSuiteServiceManager serviceManager) {
    	if (serviceManager == null) {
    		log.error("Cannot release a null service manager.");
    		return;
    	}
    	
    	if (!initialized) {
    		log.error("Cannot release a service manager to this pool until it has been initialized.");
    		return;
    	}
    	
    	if (!getReferenceServiceManagers().contains(serviceManager)) {
    		log.error("Cannot release a service manager that was not originally initiated in this pool.");
    		return;
    	}
    	
    	if (getFreeServiceManagers().contains(serviceManager)) {
    		log.error("Cannot release a service manager that has already been released in this pool.");
    		return;
    	}
    	
    	getFreeServiceManagers().add(serviceManager);
    }
	
	private boolean initialized = false;
	
	private void initialize(boolean reInitialize) throws NsException {
		
		if (reInitialize)
			initialized = false;
			
		if (initialized)
			return;
		
		setReferenceServiceManagers(new ArrayList<NetSuiteServiceManager>());
		loadCredentialProperties(getCredential());
		
		for (int j=0; j<getCredential().getNumberOfSeats(); j++) {
			NetSuiteServiceManager svcMgr = new NetSuiteServiceManager();
			svcMgr.setAccount(getCredential().getAccount());
			svcMgr.setAddRequestSize(getAddRequestSize());
			svcMgr.setBodyFieldsOnly(isBodyFieldsOnly());
			svcMgr.setEmail(getCredential().getEmail());
			svcMgr.setEndpointUrl(getEndpointUrl());
			svcMgr.setPassword(getCredential().getPassword());
			svcMgr.setRetryCount(getRetryCount());
			svcMgr.setRetryInterval(getRetryInterval());
			svcMgr.setRole(getCredential().getRoleId());
			svcMgr.setSearchPageSize(getSearchPageSize());
			svcMgr.setTimeout(getTimeout());
			svcMgr.setUpdateRequestSize(getUpdateRequestSize());
			svcMgr.setDeleteRequestSize(getDeleteRequestSize());
			svcMgr.setMailPassword(getMailPassword());
			svcMgr.setMailRecipientCC1(getMailRecipientCC1());
			svcMgr.setMailRecipientCC2(getMailRecipientCC2());
			svcMgr.setMailRecipientTo(getMailRecipientTo());
			svcMgr.setMailUser(getMailUser());
			svcMgr.setUseRequestLevelCredentials(isUseRequestLevelCredentials());
			if (!isUseRequestLevelCredentials()) {
				svcMgr.login();
			}
			getReferenceServiceManagers().add(svcMgr);
		}
		
		setFreeServiceManagers(new ArrayBlockingQueue<NetSuiteServiceManager>(getReferenceServiceManagers().size()));
		getFreeServiceManagers().addAll(getReferenceServiceManagers());
		
		initialized = true;
	}
    
	protected void loadCredentialProperties(NetSuiteCredential credential) {
	}
	
	public boolean start() {
		return true;
	}
	
	public void finish() {
		return;
	}
	
	protected boolean tryLock() {
		return true;
	}
	
	private ArrayBlockingQueue<NetSuiteServiceManager> getFreeServiceManagers() {
		return freeServiceManagers;
	}

	public int getAddRequestSize() {
		return addRequestSize;
	}

	public void setAddRequestSize(int addRequestSize) {
		this.addRequestSize = addRequestSize;
	}

	public boolean isBodyFieldsOnly() {
		return bodyFieldsOnly;
	}

	public void setBodyFieldsOnly(boolean bodyFieldsOnly) {
		this.bodyFieldsOnly = bodyFieldsOnly;
	}

	public String getEndpointUrl() {
		return endpointUrl;
	}

	public void setEndpointUrl(String endpointUrl) {
		this.endpointUrl = endpointUrl;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}

	public int getSearchPageSize() {
		return searchPageSize;
	}

	public void setSearchPageSize(int searchPageSize) {
		this.searchPageSize = searchPageSize;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getUpdateRequestSize() {
		return updateRequestSize;
	}

	public void setUpdateRequestSize(int updateRequestSize) {
		this.updateRequestSize = updateRequestSize;
	}

	public int getDeleteRequestSize() {
		return deleteRequestSize;
	}

	public void setDeleteRequestSize(int deleteRequestSize) {
		this.deleteRequestSize = deleteRequestSize;
	}

	private ReentrantReadWriteLock getReInitializationLock() {
		return reInitializationLock;
	}

	private void setFreeServiceManagers(
			ArrayBlockingQueue<NetSuiteServiceManager> freeServiceManagers) {
		this.freeServiceManagers = freeServiceManagers;
	}

	private ArrayList<NetSuiteServiceManager> getReferenceServiceManagers() {
		return referenceServiceManagers;
	}

	private void setReferenceServiceManagers(
			ArrayList<NetSuiteServiceManager> referenceServiceManagers) {
		this.referenceServiceManagers = referenceServiceManagers;
	}

	public NetSuiteCredential getCredential() {
		return credential;
	}

	public void setCredential(NetSuiteCredential credential) {
		loadCredentialProperties(credential);
		this.credential = credential;
	}

	public String getMailUser() {
		return mailUser;
	}

	public void setMailUser(String mailUser) {
		this.mailUser = mailUser;
	}

	public String getMailPassword() {
		return mailPassword;
	}

	public void setMailPassword(String mailPassword) {
		this.mailPassword = mailPassword;
	}

	public String getMailRecipientTo() {
		return mailRecipientTo;
	}

	public void setMailRecipientTo(String mailRecipientTo) {
		this.mailRecipientTo = mailRecipientTo;
	}

	public String getMailRecipientCC1() {
		return mailRecipientCC1;
	}

	public void setMailRecipientCC1(String mailRecipientCC1) {
		this.mailRecipientCC1 = mailRecipientCC1;
	}
	
	public String getMailRecipientCC2() {
		return mailRecipientCC2;
	}

	public void setMailRecipientCC2(String mailRecipientCC2) {
		this.mailRecipientCC2 = mailRecipientCC2;
	}

	public boolean isUseRequestLevelCredentials() {
		return useRequestLevelCredentials;
	}

	public void setUseRequestLevelCredentials(boolean useRequestLevelCredentials) {
		this.useRequestLevelCredentials = useRequestLevelCredentials;
	}

}



