package com.celigo.axon.service.netsuite;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.celigo.axon.service.netsuite.beans.AccountInfoDTO;

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
	
	private String endpointUrl;
	private boolean useRequestLevelCredentials = false;
	
	private NetSuiteCredential credential;
	private ArrayBlockingQueue<NetSuiteServiceManager> freeServiceManagers;
	private ArrayList<NetSuiteServiceManager> referenceServiceManagers;
	
	private boolean sandbox = false;
	private boolean beta = false;
	
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
	
	private AccountInfoDTO cachedAccountInfo = null;
	private AccountInfoDTO getAccountInfo() throws NsException {
		if (cachedAccountInfo != null) {
			return cachedAccountInfo;
		}
		
		AccountInfoDTO accountInfoTemp = null;
		if (getCredential() != null) {
			String account = null;
			if (getCredential().getAccount() != null) {
				account = getCredential().getAccount();
			}
			
			accountInfoTemp = NetSuiteURLFinder.generateAccountInfo(isSandbox(), isBeta(), getCredential());
			
			if (accountInfoTemp != null && account != null) {
				accountInfoTemp.setEndPointUrl(accountInfoTemp.getEndPointUrl() + "?c=" + account);
			}
			log.info(accountInfoTemp);
		}
		
		cachedAccountInfo = accountInfoTemp;
		return accountInfoTemp;
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

	private String getEndpointUrl() throws NsException {
		if (endpointUrl != null) {
			return endpointUrl;
		}
		
		AccountInfoDTO accountInfoDTO = getAccountInfo();
		if (accountInfoDTO != null) {
			endpointUrl = accountInfoDTO.getEndPointUrl();
		}
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

	public boolean isUseRequestLevelCredentials() {
		return useRequestLevelCredentials;
	}

	public void setUseRequestLevelCredentials(boolean useRequestLevelCredentials) {
		this.useRequestLevelCredentials = useRequestLevelCredentials;
	}

	public boolean isSandbox() {
		return sandbox;
	}

	public void setSandbox(boolean sandbox) {
		this.sandbox = sandbox;
	}

	public boolean isBeta() {
		return beta;
	}

	public void setBeta(boolean beta) {
		this.beta = beta;
	}
}