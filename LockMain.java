import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

public class LockMain {
	public static void main (String[] argv) {
		try {
			Naming.rebind ("Lock", new Locks ());
			System.out.println ("Lock Server is ready.");
		} catch (Exception e) {
			System.out.println ("Lock Server failed: " + e);
		}
	}

}	// CLockMain


