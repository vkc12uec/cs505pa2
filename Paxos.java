
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.*;
import java.math.*;

class Paxos
{
    //int num_Servers;
	public List<String> server_List;
	public static String leader;
	public String self_ID;

    public class ConsensusDecision 
	{
		public int instance;
		public String proposedValue;
		public String decidedValue;
	}

    public class ClientRequest 
	{
		public String subTag;
	}

	public Paxos(List<String> Members)
	{
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
			global_Proposal_Num++;
			//TODO Prepare and Propose
		}
	}

//	public int getNextInstance()

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
	public int [][]lock_Info;
	public int [][]lock_Action_Info;


	public void start_Accept(String msg)
	{
		String new_Msg = accept_Tag + msgDelimiter + Integer.toString(global_Proposal_Num) + msgDelimiter +  msg;

		for( String host : server_List)
		{
			if(!host.equals(self_ID))
			{
				sendToHost_tmp(new_Msg, host);
			}
			//TODO do we need to start a timer
		}
	}

	public String prepare_Response(String msg)
	{
			String[] words = msg.split(msgDelimiter);
			int proposal_Number = Integer.parseInt(words[1]);
			String client_ID = words[2];
			int instance_Number = Integer.parseInt(words[3]);
			int lock_Num = Integer.parseInt(words[4]);
			String lock_Action = words[5];
			//TODO parser for lock Action
			int some_Number = Integer.parseInt(lock_Action);

			if(global_Proposal_Num <= proposal_Number)
				global_Proposal_Num = proposal_Number;
			else
				return (accepted_Tag + msgDelimiter + "no");
			//TODO if the response is NO ensure that it is being send to the correct node... not necessarily the leader

			if(Math.random() > 0.5)
				return (accepted_Tag + msgDelimiter + "yes");
			else
				return (accepted_Tag + msgDelimiter + "no");
	}

// ####################################### Ring substrate thread runs this function ##############################################

	String msgDelimiter = "#";
	ServerSocket 		mysock;
	Socket 				outsock;
	private static int	listenport = 6789;

	public void run() //throws /*RingException*/ InterruptedException
	{
		try
		{
			mysock = new ServerSocket (listenport);
			String clientMsg;
			String msg, reply, reply1, reply2;		// mite cause PROBLEM 

			while(true)
			{
				//hostsJoinedlist = ("HJL in run thread");
				//debug (hostsJoinedlist);
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
				

//###################################### handler for election msg listen A ###############################################
				if(msg_tag.equals(alive_Tag)) 
                {
                	outToClient.writeBytes("yes\n");
                }                    
//###################################### handler for election msg listen A ###############################################
				if(msg_tag.equals(getInstance_Tag)) 
                {
					global_Instance_Num++;
					msg = Integer.toString(global_Instance_Num);
                	outToClient.writeBytes(msg+"\n");
                }                    
//###################################### handler for election msg listen A ###############################################

				if(msg_tag.equals(request_Tag)) 
                {
					// Assume that the message format is request_Tag##client_ID##Instance_Num##lock_Num##Some_Number
					// Some_Number= 0 -release lock, 1 - aquire lock, 2 - create lock, 3 - renew lock
					String client_ID = words[1];
					int instance_Number = Integer.parseInt(words[2]);
					int lock_Num = Integer.parseInt(words[3]);
					String lock_Action = words[4];
					//TODO parser for lock Action
					int some_Number = Integer.parseInt(lock_Action);

					num_Accepted[global_Proposal_Num][instance_Number] = 0;
					num_Promise[global_Proposal_Num][instance_Number] = 0;
					clients_Info[global_Proposal_Num][instance_Number] = client_ID;
					lock_Info[global_Proposal_Num][instance_Number] = lock_Num;
					lock_Action_Info[global_Proposal_Num][instance_Number] = some_Number;
					
					msg = joinit(words);

					// This is similar to the propose function in the assignment
					start_Accept(msg);	
                }                    
//###################################### handler for election msg listen A ###############################################

				if(msg_tag.equals(prepare_Tag)) 
                {
                	outToClient.writeBytes("yes\n");
                }                    
//###################################### handler for election msg listen A ###############################################

				if(msg_tag.equals(promise_Tag)) 
                {
                	outToClient.writeBytes("yes\n");
                }                    

//###################################### handler for election msg listen A ###############################################
				if(msg_tag.equals(accept_Tag)) 
                {
                	//outToClient.writeBytes("yes\n");
					msg = joinit(words);
					String response = prepare_Response(msg);
					//TODO response  = accepted_Tag + Proposal number + Instance Number + Yes/No
					sendToHost_tmp(response, leader);
                }                    

//###################################### handler for election msg listen A ###############################################
				if(msg_tag.equals(accepted_Tag)) 
                {
                	//outToClient.writeBytes("yes\n");
					int proposal_Num = Integer.parseInt(words[1]);
					int instance_Number = Integer.parseInt(words[2]);
					reply = words[3];
					
					if(reply.equals("yes"))
						num_Accepted[proposal_Num][instance_Number]++;

					if(reply.equals("no"))
						num_Rejected[proposal_Num][instance_Number]++;

					if(num_Accepted[proposal_Num][instance_Number] >= (alive_Host.size()/2))
					{
						// Response to client
						// Tag + Instance number + reply (yes/no)
						msg = response_Tag + msgDelimiter + words[2] + msgDelimiter + "yes" ; 
						sendToHost_tmp(msg, clients_Info[proposal_Num][instance_Number]);
						//TODO make sure that this send is done only once
					}

					if(num_Rejected[proposal_Num][instance_Number] >= (alive_Host.size()/2))
					{
						// Response to client
						// Tag + Instance number + reply (yes/no)
						msg = response_Tag + msgDelimiter + words[2] + msgDelimiter + "no" ; 
						sendToHost_tmp(msg, clients_Info[proposal_Num][instance_Number]);
						//TODO make sure that this send is done only once
					}

                }                    

//###################################### handler for election msg listen A ###############################################
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
		public static List<String> alive_Host;
		public static List<String> failed_Host;
	public class FailureDetector
	{
		public List<String> list_Host;

		public FailureDetector(List<String> Members)
		{
			list_Host = new ArrayList<String> ();
			for(int i= 0; i <Members.size() ; i++) {
				list_Host.add(Members.get(i));
			}

		}

		public List<String> getAlive() {
			return alive_Host;
		}
		//return the list of processes that are alive
		//
		public List<String> getFailed() {
			return failed_Host;
		}
		//return the list of processes that have failed
		//
		public boolean isAlive(String hostname) {

			if(alive_Host.contains(hostname))
				return true;
			else
				return false;

		}
		//return true if hostname is alive

		public String getLeader() {
			Collections.sort(alive_Host, Collections.reverseOrder());
			if(self_ID.equals(alive_Host.get(0)))
			{
				Paxos.am_I_Leader = true;
				Paxos.global_Proposal_Num++;
				//TODO Prepare and Propose
			}
			return alive_Host.get(0);
		}
		//return current Leader

		public void run() {
			alive_Host = new ArrayList<String> ();
			failed_Host = new ArrayList<String> ();

			for(int i = 0 ; i < list_Host.size() ; i++) {
				String msg = alive_Tag + msgDelimiter;
				String reply = sendToHost(msg, list_Host.get(i));
				if(reply.equals("yes")) {
					alive_Host.add(list_Host.get(i));
				}
				else {
					failed_Host.add(list_Host.get(i));
					if(list_Host.get(i).equals(Paxos.leader))
					{
						Paxos.leader = getLeader();
					}
				}

			}
		}
	}

	public static void main (String args[])
	{
		
	}
}
