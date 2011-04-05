import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.*;

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
