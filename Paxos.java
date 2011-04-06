
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.*;
import java.math.*;

class Paxos extends Thread
{
	public List<String> server_List;
	public static String leader;
	public static String self_ID;

	public class ConsensusDecision 		// whr is this class used ?
	{
		public int instance;
		public String proposedValue;
		public String decidedValue;
	}

	public static void debug (String s) {
		System.out.println ("\n");
		System.out.println (s);
		System.out.println ("\n");
	}

	public Paxos(List<String> Members)
	{
		debug ("me: ctor:Paxos");
		global_Proposal_Num = 0;
		global_Instance_Num = 0;
		// Geting localhostname
		try 
		{
			java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
			self_ID = localMachine.getHostName();
		}
		catch (java.net.UnknownHostException uhe) 
		{
			System.out.println("Problem in getting local host name");
		}

		server_List = new ArrayList<String>();

		for(int i= 0; i <Members.size() ; i++) {
			server_List.add(Members.get(i));
		}

		Collections.sort(Members, Collections.reverseOrder());
		if(self_ID.equals(Members.get(0)))
		{
			am_I_Leader = true;
			global_Proposal_Num++;			// this is done just for leader not acceptors, TODO
			//TODO Prepare and Propose
		}

		myFD = new FailureDetector(Members);
		myFD.start();		// start thread
	}

	public int getNextInstance()
	{
		return ++global_Instance_Num;
	}

	public int getProposalNumber()
	{
		return global_Proposal_Num;
	}
	//	public ConsensusDecision propose(String proposal, int instance)


	//Message types
	String getInstance_Tag = "GetInstance";
	String request_Tag = "Request";
	String prepare_Tag = "Prepare";
	String promise_Tag = "Promise";
	String accept_Tag = "Accept";
	String accepted_Tag = "Accepted";
	String response_Tag = "Response";

	String alive_Tag = "Alive";

	public static boolean am_I_Leader;
	public static int global_Proposal_Num;
	public int global_Instance_Num;
	public int [][]num_Promise;
	public int [][]num_Accepted;
	public int [][]num_Rejected;
	public String [][]clients_Info;
	public String [][]response_info;
	public String [][]lock_Info;
	public int [][]lock_Action_Info;

	public boolean  return_response_info(int inst_num,int prop_num)
	{
		if(response_info[prop_num][inst_num].equals("yes"))
			return true;
		else
			return false;
	}

	public void start_Accept(String msg)
	{
		String new_Msg = accept_Tag + msgDelimiter + Integer.toString(global_Proposal_Num) + msgDelimiter +  msg;

		for( String host : server_List)
		{
			if(!host.equals(self_ID))
			{
				sendToHost_tmp(new_Msg, host);
			}
		}
	}

	public String prepare_Response(String msg)
	{
		String[] words = msg.split(msgDelimiter);
		int proposal_Number = Integer.parseInt(words[1]);
		int instance_Number = Integer.parseInt(words[2]);
		String lock_Name = words[3];
		String lock_Action = words[4];
		int lock_act = Integer.parseInt(lock_Action);
		int leaseTime;
		String response = "";

		if(global_Proposal_Num <= proposal_Number)
			global_Proposal_Num = proposal_Number;
		else
			response = "no";
		//return (accepted_Tag + msgDelimiter + "no");
		//TODO if the response is NO ensure that it is being send to the correct node... not necessarily the leader

		switch(lock_act)
		{
			case 0: 
				leaseTime = Integer.parseInt(words[5]);
				Lock t = new Lock (lock_Name, leaseTime);
				Locks.valid_locks.add(lock_Name);	
				Locks.lock_map.put (lock_Name, t);		// new lock in map with timer = 0
				response = "yes";
				break;
			case 1:
				leaseTime = Integer.parseInt(words[5]);
				Locks.lock_map.get(lock_Name).birthTime = (int)System.currentTimeMillis(); 
				Locks.lock_map.get(lock_Name).leaseTime = leaseTime;

				response = "yes";	
				break;
			case 2:
				Locks.lock_map.get(lock_Name).birthTime =0;
				//Locks.lock_map.get(lock_Name).leaseTime = leaseTime;
				response = "yes";	
				break;

				//case 3:
			default: break;
		}

		String final_response = accepted_Tag + msgDelimiter + proposal_Number + msgDelimiter + instance_Number + 
			msgDelimiter + response + msgDelimiter + Integer.toString(lock_act);
		return final_response;
	}

	// ####################################### Paxos thread runs this function ##############################################

	String msgDelimiter = "#";
	ServerSocket 		mysock;
	Socket 				outsock;
	private static int	listenport = 6789;

	public void run() 
	{
		try
		{
			mysock = new ServerSocket (listenport);
			String clientMsg;
			String msg, reply, reply1, reply2;		// mite cause PROBLEM 

			while(true)
			{
				Socket connectionSocket = mysock.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

				clientMsg = inFromClient.readLine();
				//debug("\n\nMsg Recevied: \n\t" + clientMsg+ "\n");
				if(clientMsg.indexOf(msgDelimiter) == -1)
				{
					continue;
				}
				//System.out.println(clientMsg);

				// Message sent will have different words/token separated by msgDelimiter defined above
				String[] words = clientMsg.split(msgDelimiter);
				String msg_tag = words[0];
				//String src = words[1];				// host who wants to join the ring
				//System.out.println("Tag = " + msg_tag + " src = " + src);


				//######################################  ###############################################
				if(msg_tag.equals(alive_Tag)) 
				{
					outToClient.writeBytes("yes\n");
				}                    
				//######################################  ###############################################
				if(msg_tag.equals(getInstance_Tag)) 
				{
					global_Instance_Num++;
					msg = Integer.toString(global_Instance_Num);
					outToClient.writeBytes(msg+"\n");
				}                    
				//######################################  ###############################################

				if(msg_tag.equals(request_Tag)) 
				{
					// Assume that the message format is request_Tag##client_ID##Instance_Num##lock_Num##Some_Number
					// Some_Number= 2 -release lock, 1 - aquire lock, 0 - create lock, 3 - renew lock
					String client_ID = words[1];
					int instance_Number = Integer.parseInt(words[2]);
					String lock_Name = words[3];
					String lock_Action = words[4];
					//TODO parser for lock Action
					int some_Number = Integer.parseInt(lock_Action);

					num_Accepted[global_Proposal_Num][instance_Number] = 0;
					num_Promise[global_Proposal_Num][instance_Number] = 0;
					clients_Info[global_Proposal_Num][instance_Number] = client_ID;
					lock_Info[global_Proposal_Num][instance_Number] = lock_Name;
					lock_Action_Info[global_Proposal_Num][instance_Number] = some_Number;

					msg = joinit(words);

					// This is similar to the propose function in the assignment
					start_Accept(msg);	
				}                    
				//######################################  ###############################################

				if(msg_tag.equals(prepare_Tag)) 
				{
					outToClient.writeBytes("yes\n");
				}                    
				//######################################  ###############################################

				if(msg_tag.equals(promise_Tag)) 
				{
					outToClient.writeBytes("yes\n");
				}                    

				//######################################  ###############################################
				if(msg_tag.equals(accept_Tag)) 
				{
					//outToClient.writeBytes("yes\n");
					msg = joinit(words);
					String response = prepare_Response(msg);
					//TODO response  = accepted_Tag + Proposal number + Instance Number + Yes/No
					sendToHost_tmp(response, leader);
				}                    

				//######################################  ###############################################
				if(msg_tag.equals(accepted_Tag)) 
				{
					//outToClient.writeBytes("yes\n");
					int proposal_Num = Integer.parseInt(words[1]);
					int instance_Number = Integer.parseInt(words[2]);
					reply = words[3];
					int lock_act = Integer.parseInt(words[4]);

					if(reply.equals("yes"))
						num_Accepted[proposal_Num][instance_Number]++;

					if(reply.equals("no"))
						num_Rejected[proposal_Num][instance_Number]++;

					if(num_Accepted[proposal_Num][instance_Number] >= (alive_Host.size()/2))
					{
						response_info[proposal_Num][instance_Number] = "yes";
						// Response to client
						// Tag + Instance number + reply (yes/no)
						//msg = response_Tag + msgDelimiter + words[2] + msgDelimiter + "yes" ; 
						//sendToHost_tmp(msg, clients_Info[proposal_Num][instance_Number]);
						//TODO make sure that this send is done only once
					}

					if(num_Rejected[proposal_Num][instance_Number] >= (alive_Host.size()/2))
					{
						// Response to client
						// Tag + Instance number + reply (yes/no)
						//msg = response_Tag + msgDelimiter + words[2] + msgDelimiter + "no" ; 
						//sendToHost_tmp(msg, clients_Info[proposal_Num][instance_Number]);
						//TODO make sure that this send is done only once
						response_info[proposal_Num][instance_Number] = "no";
					}

					Object lock_ref = Locks.lock2d[proposal_Num][instance_Number];

					switch (lock_act) {			// we need lock_act to do such a switch case
						case 0:
							synchronized (lock_ref) {
								Locks.createLock_run = false;  
								lock_ref.notifyAll();
							}
							break;

						case 1:
							synchronized (lock_ref) {
								Locks.acquireLock_run = false;  
								lock_ref.notifyAll();
							}
							break;

						case 2:
							break;

						case 3:
							break;
					}

				}                    

				//######################################  ###############################################
				if(msg_tag.equals(response_Tag)) 
				{
					outToClient.writeBytes("yes\n");
				}                    
			}                   
		}        
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}                   

	public static String sendToHost(String msg, String dest)
	{
		String whoami = "sendToHost";
		try
		{
			Socket clientSocket = new Socket(dest, listenport);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			outToServer.writeBytes(msg + '\n');
			String reply = inFromServer.readLine();

			//debug(reply + " from " + dest);
			clientSocket.close();
			return reply;
			//return "yes";
		}
		catch(Exception e)
		{
			if (e.toString().equals ("java.net.ConnectException: Connection refused"))
				return "no";
			//return "java.net.ConnectException: Connection refused";
			//e.printStackTrace();
		}
		System.out.println (whoami + ": returns null ");
		return null;
	}

	public  String joinit (String[] args) {
		//String delim = "#";
		String delim = msgDelimiter;
		String reply="";
		for (int i=1; i<args.length; i++) {
			reply += args[i];
			if (i == args.length-1)
				break;
			reply += delim;
		}
		return reply;
	}  		// check return OK

	public static String sendToHost_tmp(String msg, String dest)
	{
		String whoami = "sendToHost_tmp";
		try
		{
			Socket clientSocket = new Socket(dest, listenport);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			//BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			outToServer.writeBytes(msg + '\n');
			//String reply = inFromServer.readLine();

			//debug(reply + " from " + dest);
			clientSocket.close();
			//return reply;
			return "yes";
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.out.println (whoami + ": returns null ");
		return null;
	}

	public static List<String> alive_Host = new ArrayList<String> ();
	public static List<String> failed_Host = new ArrayList<String> ();

	public class FailureDetector extends Thread
	{
		public List<String> list_Host;

		public FailureDetector(List<String> Members)		// static assigned list , cant change in future
		{
			list_Host = new ArrayList<String> ();
			for(int i= 0; i <Members.size() ; i++) {
				list_Host.add(Members.get(i));
				Paxos.alive_Host.add(Members.get(i));
			}
		}

		public List<String> getAlive() {
			return Paxos.alive_Host;
		}

		public List<String> getFailed() {
			return Paxos.failed_Host;
		}

		public boolean isAlive(String hostname) {

			if(Paxos.alive_Host.contains(hostname))
				return true;
			else
				return false;
		}

		public String getLeader() {
			Collections.sort(Paxos.alive_Host, Collections.reverseOrder());
			if (Paxos.alive_Host.size() == 0){
				debug ("me: FD::getLeader , list size = 0");
				return "null";
				}
			else if(self_ID.equals(Paxos.alive_Host.get(0)))
			{
				Paxos.am_I_Leader = true;
				Paxos.global_Proposal_Num++;
				//TODO Prepare and Propose
				//Paxos temp = new Paxos();		// if I m leader, i shud start my Paxos thread
				//Locks temp = new Locks();		// if I m leader, i shud start my Paxos thread
				//temp.start();
			}
			return Paxos.alive_Host.get(0);
		}

		public void run() {
			debug ("me: FD::run, starting FD thread for host = "+Paxos.self_ID);

			while (true) {
				try {
					sleep (30000); 	// 30 sec sleep
					} 
				catch (Exception e){
					}
				for(int i = 0 ; i < list_Host.size() ; i++) {
					String msg = alive_Tag + msgDelimiter;
					String reply = sendToHost(msg, list_Host.get(i));

					if(reply.equals("yes")) {
						Paxos.alive_Host.add(list_Host.get(i));
					}
					else {
						Paxos.failed_Host.add(list_Host.get(i));
					}
				}
				Paxos.leader = getLeader();			// CHANGE THIS, APPEARS ICKY
				debug ("me: FD:run, leader of system = "+Paxos.leader);

			}	// while
		}	// run

	}		// CFailDetector

	public FailureDetector myFD;		// this will be spawn in Paxos construtor

	/*public static void main (String [] args)
	  {
	  System.out.println ("hel");
	  }*/
}
