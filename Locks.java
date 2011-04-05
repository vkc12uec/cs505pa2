
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

//TODO:	this has to extend thread
public class Locks extends UnicastRemoteObject implements LockServer {

	public static List <String> valid_locks;
	public static HashMap acquired_locks;		// Lock name and 0/1 acquired / release
	public static HashMap<String, Lock> lock_map ;			// lock name / lock object
												// these 3 states are replicas	, shud they be in Paxos class ?


	public static Object [][] lock2d;
	//public static Object lock2;
	//public static Object lock3;
	public static boolean  createLock_run;
	public static boolean  acquireLock_run;
	public static boolean  releaseLock_run;

	public Paxos myPaxos;
	String msgDelimiter = "#";

	public Locks () {
		valid_locks = new ArrayList<String> ();
		acquired_locks = new HashMap();
		lock_map = new HashMap();
		myPaxos = new Paxos();
		lock2d = new Object[100][100];		// propNum / instNum
		createLock_run = true;


		//		try {
		//		Naming.rebind("Locks", new Locks());	// correct ?	see RMI example
		//		}
		//		catch (Exception e) {
		//			System.out.println ("Locks: unable to rebind " + e.printStackTrace());
		//		}

		// start DLM thread here ?
	}

	public String prepare_msg (String op , String lock_name) {
		if (op.equals("acquireLock")) {
			//todo
		}

		if (op.equals("releaseLock")) {
			//todo
		}
		return null;
	}	

	public boolean createLock(String lock) throws RemoteException {
		if (lock_map.containsKey(lock))
			return false;	// lock already exists

		int leaseTime = 0;	//int(Math.random() * 10);  // It returns a lease time between 1 - 10 secs
		Lock t = new Lock (lock, leaseTime);
		if (t != null)
		{ 
			// Send a message to the Paxos node to be forwarded to other servers
			int instance_Num = myPaxos.getNextInstance();
			int prop_num = myPaxos.getProposalNumber();
			// TODO how to find out the client ID here.
			String msg = Integer.toString(instance_Num) + msgDelimiter + lock + msgDelimiter + 
				Integer.toString(0) + msgDelimiter + Integer.toString(leaseTime); // 0 - create lock

			myPaxos.start_Accept(msg);
			createLock_run = true;
			Object temp_lock_obj = lock2d[prop_num][instance_Num];
			synchronized(temp_lock_obj){
				while (createLock_run) {
					try {
						temp_lock_obj.wait();           
					} catch (Exception e) {
						e.printStackTrace();
					}
				}	// whiel
			}

			valid_locks.add(lock);	
			lock_map.put (lock, t);		// new lock in map with timer = 0

			return myPaxos.return_response_info(instance_Num, prop_num);
		}
		else 
			return false;
	}

	public List<String> getAllLocks() throws RemoteException {
		return valid_locks;
	}

	public Lock acquireLock(String lock_Name, int minTime) throws RemoteException {

		int lTime = lock_map.get(lock_Name).leaseTime;
		int bTime = lock_map.get(lock_Name).birthTime;

		long curTime = System.currentTimeMillis();

		if(! ((bTime == 0) || ((bTime + (lTime  * 1000)) <= curTime ))) {
			Exception e = new Exception ("acquireLock: lock already taken by someone");
			throw e;
		}
		else {
			int instance_Num = myPaxos.getNextInstance();
			int prop_num = myPaxos.getProposalNumber();
			String msg = Integer.toString(instance_Num) + msgDelimiter + lock_Name + msgDelimiter + Integer.toString(1) + msgDelimiter + Integer.toString(minTime);
			// 1 - aquire lock

			myPaxos.start_Accept(msg);
			acquireLock_run = true;
			Object temp_lock_obj = lock2d[prop_num][instance_Num];
			synchronized(temp_lock_obj){
				while (acquireLock_run) {		// this bool then shud be access using propN and instN
					try {
						temp_lock_obj.wait();           
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			LockMain.lock_map.get(lock_Name).birthTime = System.currentTimeMillis(); 
			LockMain.lock_map.get(lock_Name).leaseTime = minTime;	// correct ?
			return lock_map.get(lock_Name);
		}
	}

	public boolean releaseLock(String lock_Name) throws RemoteException {

		int instance_Num = myPaxos.getNextInstance();
		int prop_num = myPaxos.getProposalNumber();
		String msg = Integer.toString(instance_Num) + msgDelimiter + lock_Name + msgDelimiter + Integer.toString(2); // 2 - release lock

		myPaxos.start_Accept(msg);
		releaseLock_run = true;
		Object temp_lock_obj = lock2d[prop_num][instance_Num];

		synchronized(temp_lock_obj){
			while (acquireLock_run) {		// TODO: if thr is another requires then , bool acquireLock_run is shared b/w them.
				try {
					temp_lock_obj.wait();           
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		LockMain.lock_map.get(lock_Name).birthTime = 0; 
	}

	public Lock renewLock(String Lock, int renewalTime) throws RemoteException {
		// is Paxos consensus needed her e?
		int instance_Num = myPaxos.getNextInstance();
		int prop_num = myPaxos.getProposalNumber();

		// call acquirelock here TODO
		//myPaxos.start_accept (msg);
		return new Lock("maya", 0);
	}

}	// end class Locks


//class LockClient() { /* applications can use this class to acquire and release locks */
//	
//	public List<String> valid_locks;
//
//	public LockClient () {
//		valid_locks = new List<String> ();
//	}
//
//	public static debug (String msg) {
//		System.out.println (msg);
//	}
//
//	public LockServer getRemoteObject () {	// static ?
//		String DLM_server = "sac07.cs.purdue.edu";
//		try {
//		LockServer ls = (LockServer) Naming.lookup (DLM_server+"/Lock");
//		return ls;
//		}
//		catch (Exception e) {
//			System.out.println ("getRemoteObject: exception "+ e);
//		}
//		return null;
//	}
//
//	public boolean createLock(String lock) {
//		LockServer temp = getRemoteObject();
//
//		if( temp.createLock(lock) ) {
//			System.out.println ("createLock: for " + lock + " success");
//			valid_locks.add(lock);		// add
//		}
//		else {
//			System.out.println ("createLock: for " + lock + " fail");
//		}
//	}
//
//	public List<String> getAllLocks() {
//		LockServer temp = getRemoteObject();
//		List<String> list;
//		list = temp.getAllLocks();
//		return list;
//	}
//
//	public Lock acquireLock(String lock, int leaseTime) {
//		LockServer temp = getRemoteObject();
//		return temp.acquireLock (lock, leaseTime);
//	}
//
//	public boolean releaseLock(String lock) {
//		LockServer temp = getRemoteObject();
//		return temp.releaseLock(lock);
//	}
//
//	public Lock renewLock(String lock, int renewalTime) {
//		LockServer temp = getRemoteObject();
//		return temp.renewLock (lock, renewalTime);
//	}
//
//	public boolean isValid(String lock) {	/* non RMI */
//		return valid_locks.contains(lock);
//	}
//}		// class LockClient
//
