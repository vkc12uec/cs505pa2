import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;

class LockClient() { /* applications can use this class to acquire and release locks */
	
	public List<String> valid_locks;
	LockServer lockInterface;

	public LockClient () {
		valid_locks = new List<String> ();
		
			try {
			  lockInterface = 
				(LockServer) Naming.lookup ("sac07.cs.purdue.edu/Lock");
			  //System.out.println (hello.say());
			} catch (Exception e) {
			  System.out.println ("LockClient exception: " + e.toString());
			}
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
		//LockServer temp = getRemoteObject();
		LockServer temp = lockInterface;


		if( temp.createLock(lock) ) {
			System.out.println ("createLock: for " + lock + " success");
			valid_locks.add(lock);		// add
		}
		else {
			System.out.println ("createLock: for " + lock + " fail");
		}
	}

	public List<String> getAllLocks() {
		//LockServer temp = getRemoteObject();
		LockServer temp = lockInterface;
		List<String> list;
		list = temp.getAllLocks();
		return list;
	}

	public Lock acquireLock(String lock, int leaseTime) {
		//LockServer temp = getRemoteObject();
		LockServer temp = lockInterface;
		return temp.acquireLock (lock, leaseTime);
	}

	public boolean releaseLock(String lock) {
		//LockServer temp = getRemoteObject();
		LockServer temp = lockInterface;
		return temp.releaseLock(lock);
	}

	public Lock renewLock(String lock, int renewalTime) {
		//LockServer temp = getRemoteObject();
		LockServer temp = lockInterface;
		return temp.renewLock (lock, renewalTime);
	}

	public boolean isValid(String lock) {	/* non RMI */
		return valid_locks.contains(lock);
	}
}		// class LockClient

public class ClientApp
{
	public static void main (String args[])
	{
		LockClient client = new LockClient();
		//give the list of actions you wish to do with the client 
		client.createLock("Ramu");
	}
}
