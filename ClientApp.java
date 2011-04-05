import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

class LockClient { /* applications can use this class to acquire and release locks */
	
	public List<String> valid_locks;
	LockServer lockInterface;

	public LockClient () {
		valid_locks = new ArrayList<String> ();
		
			try {
			  lockInterface = 
				(LockServer) Naming.lookup ("sac07.cs.purdue.edu/Lock");
			  //System.out.println (hello.say());
			} catch (Exception e) {
			  System.out.println ("LockClient exception: " + e.toString());
			}
	}

	public void debug (String msg) {
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
		boolean b  = false;
		try {
			b = temp.createLock(lock);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		if( b ) {
			System.out.println ("createLock: for " + lock + " success");
			valid_locks.add(lock);		// add
		}
		else {
			System.out.println ("createLock: for " + lock + " fail");
		}
		return b;
	}

	public List<String> getAllLocks() {
		LockServer temp = lockInterface;
		List<String> list;

		try {
		list = temp.getAllLocks();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public Lock acquireLock(String lock, int leaseTime) {
		LockServer temp = lockInterface;
		Lock t;
		
		try {
			t = temp.acquireLock (lock, leaseTime);
		} catch (Exception e) {
				e.printStackTrace();
			}
		return t;
	}

	public boolean releaseLock(String lock) {
		LockServer temp = lockInterface;
		boolean b=false;

		try {
			b = temp.releaseLock(lock);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	public Lock renewLock(String lock, int renewalTime) {
		LockServer temp = lockInterface;
		Lock t;

		try {
			t = temp.renewLock (lock, renewalTime);
		} catch (Exception e) {
				e.printStackTrace();
			}
		return t;
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
