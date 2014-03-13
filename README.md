netsuite-service-manager
========================

NetSuite Suitetalk for Java Applications.

``` java
	NetSuiteCredential netSuiteCredential = new NetSuiteCredential("email", "password", "account#", "roleID");
//	netSuiteCredential.setNumberOfSeats(1); // Set to more than 1 if SuiteCloud Plus is enabled
	
	NetSuiteServicePoolManager netSuiteServicePoolManager = new NetSuiteServicePoolManager();
	netSuiteServicePoolManager.setCredential(netSuiteCredential);
//	netSuiteServicePoolManager.setUseRequestLevelCredentials(false); // Set to true if using request level credentials

	NetSuiteServicePool netSuiteServicePool = new NetSuiteServicePool();
	netSuiteServicePool.setServicePoolManager(netSuiteServicePoolManager);
	
	RecordRef recordRef = new RecordRef();
	recordRef.setInternalId("42");
	recordRef.setType(RecordType.contact);
	
	ReadResponse readResponse = netSuiteServicePool.get(recordRef);
	Contact contact = (Contact) readResponse.getRecord();
	log.info(contact.getFirstName());
```
