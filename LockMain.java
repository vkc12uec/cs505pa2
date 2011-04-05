
import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;

public class Lock {
	public String lockName; //name of the lock
	public int leaseTime; //the lease time is decided by the DLM
	public int birthTime;

	public Lock ( String name, int ltime) {
		lockName = name;
		leaseTime = ltime;
		birthTime = 0;
	}
}

public class Locks extends UnicastRemoteObject implements LockServer {

	public static List <String> valid_locks;
	public static HashMap<String, int> acquired_locks;		// Lock name and 0/1 acquired / release
	public static HashMap<String, Lock> lock_map ;			// lock name / lock object
	// these 3 states are replicas	, shud they be in Paxos class ?

	
	public static Object lock1;
	public static Object lock2;
	public static Object lock3;
	public static boolean  createLock_run;

	public Paxos myPaxos;
	String msgDelimiter = "#";

	public Locks () {
		valid_locks = new ArrayList<String> ();
		acquired_locks = new HashMap();
		lock_map = new HashMap();
		myPaxos = new Paxos();
		lock1 = new Object();
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

		int leaseTime = int(Math.random() * 10);  // It returns a lease time between 1 - 10 secs
		Lock t = new Lock (lock, leaseTime);
		if (t != null)
			{ 
				// Send a message to the Paxos node to be forwarded to other servers
				int instance_Num = myPaxos.getNextInstance();
				int prop_num = myPaxos.getProposalNumber();
				// TODO how to find out the client ID here.
				String msg = Integer.toString(instance_Num) + msgDelimiter + lock + msgDelimiter + Integer.toString(0) + msgDelimiter + Integer.toString(leaseTime); // 0 - create lock

				myPaxos.start_Accept(msg);
				createLock_run = true;
				synchronized(lock1){
						while (createLock_run) {
								try {
										lock1.wait();           
								} catch (Exception e) {
										e.printStackTrace();
								}
						}	// whiel
				}

				valid_locks.add(lock);	
				lock_map.put (lock, t);		// new lock in map with timer = 0

				//return true; 
				return myPaxos.return_response_info(instance_Num, prop_num);
				//.TODO create 2D locks to avoid overwriting of locks from another clients
			}
		else 
			return false;
	}

	public List<String> getAllLocks() throws RemoteException {
		return valid_locks;
	}

	public Lock acquireLock(String lock, int minTime) throws RemoteException {
		// is Paxos consensus algo. needed here ?:

		int lTime = lock_map.get(lock).leaseTime;
		int bTime = lock_map.get(lock).birthTime;

		long curTime = System.currentTimeMillis();

		if(! ((bTime == 0) || ((bTime + (lTime  * 1000)) <= curTime ))) {
		//if ((acquired_locks.get(lock) == 1) || (!lock_map.contains(lock))) {
			Exception e = new Exception ("acquireLock: lock already taken by someone");
			throw e;
		}
		else {
				int instance_Num = myPaxos.getNextInstance();
				int prop_num = myPaxos.getProposalNumber();
				String msg = Integer.toString(instance_Num) + msgDelimiter + lock + msgDelimiter + Integer.toString(1) + msgDelimiter + Integer.toString(minTime); // 1 - aquire lock

				myPaxos.start_Accept(msg);
			//TODO Create 2D lock... create a function which is common across all lock actions
				
			LockMain.lock_map.get(lock_Name).birthTime = System.currentTimeMillis(); 
			LockMain.lock_map.get(lock_Name).leaseTime = leaseTime;
			//lock_map.get(lock).leaseTime = minTime;		// start a timer here
			//acquired_locks.put (lock, 1);
			//TODO Create 2D lock... create a function which is common across all lock actions
			return lock_map.get(lock);
		}
	}

	public boolean releaseLock(String lock) throws RemoteException {

		int instance_Num = myPaxos.getNextInstance();
		int prop_num = myPaxos.getProposalNumber();
		String msg = Integer.toString(instance_Num) + msgDelimiter + lock + msgDelimiter + Integer.toString(2); // 2 - release lock

		myPaxos.start_Accept(msg);

			//TODO Create 2D lock... create a function which is common across all lock actions
				
			LockMain.lock_map.get(lock_Name).birthTime = 0; 
		//String msg = prepare_msg ("releaseLock", lock);
		//boolean res = myPaxos.start_Accept (msg);		// this shud be blocking ?

		//if (res == true) {
		//	acquired_locks.remove(lock);
		//	acquired_locks.put(lock, 0);
		//}
		//return res;
	}

	public Lock renewLock(String Lock, int renewalTime) throws RemoteException {
		// is Paxos consensus needed her e?
		String msg = prepare_msg("renewLock" , Lock, renewalTime);
		myPaxos.start_accept (msg);
		// if not needed, then renew by probabiltiy half
	}

}	// end class Locks

public interface LockServer extends Remote {
	public boolean createLock(String lock) throws RemoteException;
	//This method is used by the "resource" to define locks
	//For example, a database service may use this method to define its locks
	public List<String> getAllLocks() throws RemoteException;
	//Returns a list of currently defined locks
	public Lock acquireLock(String lock, int minTime) throws RemoteException;
	// This is used to request a lock for a minimum lease time
	// The server grants the lock only if it can lease the lock atleast for minTime
	// The actual time for which the server leases the lock is returned in the leaseTime variable of Lock
	// Your code should throw a RemoteException if the call fails for any reason
	// Your code should set the Message field of RemoteException properly
	// The server may choose to queue acquireLock requests from clients
	// If lock l was last held by client c1, and both c1 and c2 request it, the server may choose to grant it to c2.
	// The server has to run an instance of the Paxos protocol before returning an acquired lock.
	public boolean releaseLock(String lock) throws RemoteException;
	// self explanatory
	// The server has to run an instance of the Paxos protocol before returning true.
	public Lock renewLock(String Lock, int renewalTime) throws RemoteException;
	// This method is similar to acquiring the lock again
	// It is upto the server to grant this request
}

public class LockMain {
  /**
   * Server program for the "Hello, world!" example.
   * @param argv The command line arguments which are ignored.
   */
  public static void main (String[] argv) {
    try {
      Naming.rebind ("Lock", new Locks ());
      System.out.println ("Lock Server is ready.");
    } catch (Exception e) {
      System.out.println ("Lock Server failed: " + e);
    }
  }

}

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
