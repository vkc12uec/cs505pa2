import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

class LockClient { /* applications can use this class to acquire and release locks */

	public List<String> valid_locks;
	LockServer lockInterface;
	Registry registry;
	public static String serverAdd = "sslab08.cs.purdue.edu";		// rmi server
	public static int rmiport = 3232;

	public LockClient () {
		valid_locks = new ArrayList<String> ();

		try {
			registry=LocateRegistry.getRegistry(serverAdd, rmiport);
			lockInterface = (LockServer) registry.lookup("Lock");
		} catch (Exception e) {
			System.out.println ("LockClient exception: ");
			e.printStackTrace();
		}
	}

	public void debug (String msg) {
		System.out.println (msg);
	}

	public boolean createLock(String lock) {
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
		List<String> list= null;

		try {
			list = temp.getAllLocks();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public Lock acquireLock(String lock, int leaseTime) {
		LockServer temp = lockInterface;
		Lock t = null;

		try {
			t = temp.acquireLock (lock, leaseTime);
			if(t.leaseTime == -1)
			{
				System.out.println("lock cannot be aquired");
			}
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
		Lock t = null;

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
		client.createLock("ramu");
		/*client.createLock("kanu");
		client.createLock("samu");
		Lock l1 = client.acquireLock("ramu", 100);
		Lock l2 = client.acquireLock("kanu", 100);
		Lock l3 = client.acquireLock("samu", 100);*/

		while (true) {
		}
	}
}
