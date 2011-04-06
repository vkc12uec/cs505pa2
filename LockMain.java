import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

public class LockMain {
	public static void main (String[] argv) {
		String [] hosts = {"sslab08.cs.purdue.edu", "sslab07.cs.purdue.edu", "sslab06.cs.purdue.edu" };
		try {
			new Locks(hosts);
			System.out.println ("Lock Server is ready.");
		} catch (Exception e) {
			System.out.println ("Lock Server failed: " + e);
		}
	}

}	// CLockMain
