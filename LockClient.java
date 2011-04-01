import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;

public class Lock {
	public String lockName; //name of the lock
	public int leaseTime; //the lease time is decided by the DLM

	public Lock ( String name, int ltime) {
		lockName = name;
		leaseTime = ltime;
	}
}

public class Locks extends UnicastRemoteObject implements LockServer {

	public List <String> valid_locks;
	public HashMap<String, int> acquired_locks;		// Lock name and 0/1 acquired / release
	public HashMap<String, Lock> lock_map ;			// lock name / lock object
	// these 3 states are replicas	, shud they be in Paxos class ?

	public Paxos myPaxos;

	public Locks () {
		valid_locks = new ArrayList<String> ();
		acquired_locks = new HashMap();
		lock_map = new HashMap();
		myPaxos = new Paxos();
	
		try {
		Naming.rebind("Locks", new Locks());	// correct ?	see RMI example
		}
		catch (Exception e) {
			System.out.println ("Locks: unable to rebind " + e.printStackTrace());
		}

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

		Lock t = new Lock (lock,0);
		if (t != null)
			{ 
				valid_locks.add(lock);	
				lock_map.put (lock, t);		// new lock in map with timer = 0
				return true; 
			}
		else 
			return false;
	}

	public List<String> getAllLocks() throws RemoteException {
		return valid_locks;
	}

	public Lock acquireLock(String lock, int minTime) throws RemoteException {
		// is Paxos consensus algo. needed here ?:

		if (acquired_locks.get(lock) == 1) {
			Exception e = new Exception ("acquireLock: lock already taken by someone");
			throw e;
		}
		else {
			lock_map.get(lock).leaseTime = minTime;		// start a timer here
			acquired_locks.put (lock, 1);
			return lock_map.get(lock);
		}
	}

	public boolean releaseLock(String lock) throws RemoteException {
		String msg = prepare_msg ("releaseLock", lock);
		boolean res = myPaxos.start_Accept (msg);		// this shud be blocking ?

		if (res == true) {
			acquired_locks.remove(lock);
			acquired_locks.put(lock, 0);
		}
		return res;
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

class LockClient() { /* applications can use this class to acquire and release locks */
	
	public List<String> valid_locks;

	public LockClient () {
		valid_locks = new List<String> ();
	}

	public static debug (String msg) {
		System.out.println (msg);
	}

	public LockServer getRemoteObject () {	// static ?
		String DLM_server = "sac07.cs.purdue.edu";
		try {
		LockServer ls = (LockServer) Naming.lookup (DLM_server+"/Lock");
		return ls;
		}
		catch (Exception e) {
			System.out.println ("getRemoteObject: exception "+ e);
		}
		return null;
	}

	public boolean createLock(String lock) {
		LockServer temp = getRemoteObject();

		if( temp.createLock(lock) ) {
			System.out.println ("createLock: for " + lock + " success");
			valid_locks.add(lock);		// add
		}
		else {
			System.out.println ("createLock: for " + lock + " fail");
		}
	}

	public List<String> getAllLocks() {
		LockServer temp = getRemoteObject();
		List<String> list;
		list = temp.getAllLocks();
		return list;
	}

	public Lock acquireLock(String lock, int leaseTime) {
		LockServer temp = getRemoteObject();
		return temp.acquireLock (lock, leaseTime);
	}

	public boolean releaseLock(String lock) {
		LockServer temp = getRemoteObject();
		return temp.releaseLock(lock);
	}

	public Lock renewLock(String lock, int renewalTime) {
		LockServer temp = getRemoteObject();
		return temp.renewLock (lock, renewalTime);
	}

	public boolean isValid(String lock) {	/* non RMI */
		return valid_locks.contains(lock);
	}
}		// class LockClient

