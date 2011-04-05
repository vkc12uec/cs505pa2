import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

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


