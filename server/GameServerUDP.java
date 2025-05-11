import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.IGameConnection.ProtocolType;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> 
{
	private NPCcontroller npcCtrl;
	private UUID tagemanID;
	private String[] tagemanLoc = new String[3];
	private boolean tageman, pinky, blinky, inky, clyde;

	public GameServerUDP(int localPort, NPCcontroller npc) throws IOException 
	{	super(localPort, ProtocolType.UDP);
		npcCtrl = npc;

		tageman = false;
		pinky = false;
		blinky = false;
		inky = false;
		clyde = false;
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		String message = (String)o;
		String[] messageTokens = message.split(",");
		
		if(messageTokens.length > 0)
		{	// JOIN -- Case where client just joined the server
			// Received Message Format: (join,localId)
			if(messageTokens[0].compareTo("join") == 0)
			{	try 
				{	IClientInfo ci;					
					ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);
					String character = messageTokens[2];
					addClient(ci, clientID);
					
					if (checkAvailability(character)) {
						System.out.println("Join request received from - " + clientID.toString());
						confirmCharacter(character);
						sendJoinedMessage(clientID, character, true);

						if (character.equals("tageman")) {
							tagemanID = clientID;
						}
					} else {
						System.out.println("character taken, " + clientID.toString() + " not able to join");
						sendJoinedMessage(clientID, character, false);
						sendTakenMessage(clientID);
					}
				} 
				catch (IOException e) {	
					e.printStackTrace();
				}		
			}
		
			// BYE -- Case where clients leaves the server
			// Received Message Format: (bye,localId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String character = messageTokens[2];
				System.out.println("Exit request received from - " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
				freeCharacter(character);

				if (clientID.equals(tagemanID)) {
					tagemanID = null;
				}
			}
			
			// CREATE -- Case where server receives a create message (to specify avatar location)
			// Received Message Format: (create,localId,x,y,z)
			if(messageTokens[0].compareTo("create") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				String character = messageTokens[5];
				String[] forward = {messageTokens[6], messageTokens[7], messageTokens[8]};
				sendCreateMessages(clientID, character, pos, forward);
				sendWantsDetailsMessages(clientID);

				if (character.equals("tageman")) {
					tagemanID = clientID;
					tagemanLoc[0] = pos[0];
					tagemanLoc[1] = pos[1];
					tagemanLoc[2] = pos[2];
				}
			}
			
			// DETAILS-FOR --- Case where server receives a details for message
			// Received Message Format: (dsfr,remoteId,localId,x,y,z)
			if(messageTokens[0].compareTo("dsfr") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
				String character = messageTokens[6];
				String[] forward = {messageTokens[7], messageTokens[8], messageTokens[9]};
				sendDetailsForMessage(clientID, remoteID, pos, character, forward);
			}
			
			// MOVE --- Case where server receives a move message
			// Received Message Format: (move,localId,x,y,z)
			if(messageTokens[0].compareTo("move") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				System.out.println(message);
				String rotate = messageTokens[5];
				sendMoveMessages(clientID, pos, rotate);

				if (clientID.equals(tagemanID)) {
					System.out.println("updated tagemanLocation");
					tagemanLoc[0] = pos[0];
					tagemanLoc[1] = pos[1];
					tagemanLoc[2] = pos[2];
				}
			}	

			//TAGEMAN is or is not CHOMPING
			if (messageTokens[0].compareTo("chomp") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String toChomp = messageTokens[2];
				sendChompMessages(clientID, toChomp);
			}

			//START GAME
			if (messageTokens[0].compareTo("start") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String time = messageTokens[2];
				sendStartGameMessages(clientID, time);
			}

			// POWERUP message
			if (messageTokens[0].compareTo("power") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String powerUp = messageTokens[2];
				sendPowerMessages(clientID, powerUp);
			}

			// EATEN message
			if (messageTokens[0].compareTo("eaten") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				UUID recipientID = UUID.fromString(messageTokens[2]);
				sendEatenMessage(recipientID);
			}

			// CAUGHT message
			if (messageTokens[0].compareTo("caught") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String lives = messageTokens[2];
				sendCaughtMessage(clientID, lives);
			}

			if (messageTokens[0].compareTo("eatNPC") == 0) {
				npcCtrl.start(this);
			}

			//CREATE NPC
			if (messageTokens[0].compareTo("createNPC") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				String[] ghostPosition = { messageTokens[2], messageTokens[3], messageTokens[4]};
				try {
					sendCreateNPCmsg(clientID, ghostPosition);
				} catch (IOException e) {
					System.out.println("error creating npc");
				}
			}

			//NEED NPC
			if(messageTokens[0].compareTo("needNPC") == 0) {
				System.out.println("server got a needNPC message");
				UUID clientID = UUID.fromString(messageTokens[1]);
				//sendNPCstart(clientID);
			}

			// DELETE NPC
			if (messageTokens[0].compareTo("deleteNPC") == 0) {
				UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("deleting NPC");
				sendDeleteNPCMessage(clientID);
			}

			if (messageTokens[0].compareTo("pelletCount") == 0) {
				String count = messageTokens[1];
				sendPelletCountMessage(count);
			}


		}
	}

	private boolean checkAvailability(String character) {
		if (character.equals("tageman")) {
			return !tageman;
		} else if (character.equals("blinky")) {
			return !blinky;
		} else if (character.equals("pinky")) {
			return !pinky;
		} else if (character.equals("inky")) {
			return !inky;
		} else if (character.equals("clyde")) {
			return !clyde;
		}
		return false;
	}

	private void confirmCharacter(String character) {
		if (character.equals("tageman")) {
			tageman = true;
		} else if (character.equals("blinky")) {
			blinky = true;
		} else if (character.equals("pinky")) {
			pinky = true;
		} else if (character.equals("inky")) {
			inky = true;
		} else if (character.equals("clyde")) {
			clyde = true;
		}
	}

	private void freeCharacter(String character) {
		if (character.equals("tageman")) {
			tageman = false;
		} else if (character.equals("blinky")) {
			blinky = false;
		} else if (character.equals("pinky")) {
			pinky = false;
		} else if (character.equals("inky")) {
			inky = false;
		} else if (character.equals("clyde")) {
			clyde = false;
		}
	}

	private void sendTakenMessage(UUID clientID) {
		try {
			String message = new String("taken");
			message += "," + clientID.toString();
			sendPacket(message, clientID);
			System.out.println("sent taken message");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// Informs the client who just requested to join the server if their if their 
	// request was able to be granted. 
	// Message Format: (join,success) or (join,failure)
	
	public void sendJoinedMessage(UUID clientID, String character, boolean success)
	{	try 
		{	System.out.println("trying to confirm join");
			String message = new String("join,");
			if(success) {
				message += "success";
				message += "," + character;
			}
			else
				message += "failure";
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that the avatar with the identifier remoteId has left the server. 
	// This message is meant to be sent to all client currently connected to the server 
	// when a client leaves the server.
	// Message Format: (bye,remoteId)
	
	public void sendByeMessages(UUID clientID)
	{	try 
		{	String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a new avatar has joined the server with the unique identifier 
	// remoteId. This message is intended to be send to all clients currently connected to 
	// the server when a new client has joined the server and sent a create message to the 
	// server. This message also triggers WANTS_DETAILS messages to be sent to all client 
	// connected to the server. 
	// Message Format: (create,remoteId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessages(UUID clientID, String character, String[] position, String[] forward)
	{	try 
		{	String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + character;
			message += "," + forward[0];
			message += "," + forward[1];
			message += "," + forward[2];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client of the details for a remote client�s avatar. This message is in response 
	// to the server receiving a DETAILS_FOR message from a remote client. That remote client�s 
	// message�s localId becomes the remoteId for this message, and the remote client�s message�s 
	// remoteId is used to send this message to the proper client. 
	// Message Format: (dsfr,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position, String character, String[] forward)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];	
			message += "," + character;
			message += "," + forward[0];
			message += "," + forward[1];
			message += "," + forward[2];
			sendPacket(message, clientID);
			System.out.println(character + " gave details");
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a local client that a remote client wants the local client�s avatar�s information. 
	// This message is meant to be sent to all clients connected to the server when a new client 
	// joins the server. 
	// Message Format: (wsds,remoteId)
	
	public void sendWantsDetailsMessages(UUID clientID)
	{	try 
		{	String message = new String("wsds," + clientID.toString());	
			forwardPacketToAll(message, clientID);
			System.out.println(clientID + " wants details");
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a remote client�s avatar has changed position. x, y, and z represent 
	// the new position of the remote avatar. This message is meant to be forwarded to all clients
	// connected to the server when it receives a MOVE message from the remote client.   
	// Message Format: (move,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessages(UUID clientID, String[] position, String rotateBy)
	{	try 
		{	String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + rotateBy;
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	public void sendChompMessages(UUID clientID, String toChomp) {
		try {
			String message = new String("chomp," + clientID.toString());
			message += "," + toChomp;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendStartGameMessages(UUID clientID, String time) {
		try {
			String message = new String("start," + clientID.toString());
			message += "," + time;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			System.out.println("failure to start game");
			e.printStackTrace();
		}
	}

	public void sendPowerMessages(UUID clientID, String powerup) {
		try {
			String message = new String("power," + clientID.toString());
			message += "," + powerup;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			System.out.println("failure to send power message");
			e.printStackTrace();
		}
	}

	public void sendEatenMessage(UUID recipientID) {
		try {
			String message = new String("eaten," + recipientID.toString());
			sendPacket(message, recipientID);
		} catch (IOException e) {
			System.out.println("failure to send eaten message");
			e.printStackTrace();
		}
	}

	public void sendCaughtMessage(UUID clientID, String l) {
		int lives = Integer.parseInt(l);

		if (lives < 0) {
			sendVictoryMessage(1);
		} else {
			try {
				String message = new String("caught," + clientID.toString());
				message += "," + l;
				forwardPacketToAll(message, clientID);
				sendPacket(message, clientID);
			} catch (IOException e) {
				System.out.println("failure to send caught message");
				e.printStackTrace();
			}
		}
	}

	public void sendVictoryMessage(int who) {
		try {
			String message = new String("victory," );
			message += "," + who;
			sendPacketToAll(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ---------- protocols for NPC's ---------------------------

	public void sendCheckForAvatarNear() {
		try {
			String message = new String("isnr");
			message += "," + (npcCtrl.getNPC()).getX();
			message += "," + (npcCtrl.getNPC()).getY();
			message += "," + (npcCtrl.getNPC()).getZ();
			sendPacketToAll(message);
		} catch (IOException e) {
			System.out.println("couldn't send msg"); 
			e.printStackTrace();
		}
	}

	public void sendNPCinfo() {
		try {
			String message = new String("npcInfo");
			message += "," + (npcCtrl.getNPC()).getX();
			message += "," + (npcCtrl.getNPC()).getY();
			message += "," + (npcCtrl.getNPC()).getZ();
			sendPacketToAll(message);
		} catch (IOException e) {
			System.out.println("couldn't send info about npc");
			e.printStackTrace();
		}
	}

	public void requestTarget() {
		npcCtrl.setTarget(tagemanLoc);
	}

	public void lookAtTarget() {
		try {
			String message = new String("lookAt");
			message += "," + tagemanLoc[0];
			message += "," + tagemanLoc[1];
			message += "," + tagemanLoc[2];
		sendPacketToAll(message);
		} catch (IOException e) {
			System.out.println("Unable to send command for NPC to look at tageman");
			e.printStackTrace();
		}
		
	}

	// -------- Sending NPC messages ------------------------
	//infomrs clients about the whereabouts of the NPCs
	public void sendCreateNPCmsg(UUID clientID, String[] position) throws IOException {
		try {
			System.out.println("server telling clients about an NPC");
			String message = new String("createNPC," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
		npcCtrl.start(this);
	}

	public void sendDeleteNPCMessage(UUID clientID) {
		try {
			System.out.println("server telling clients to delete NPC");
			String message = new String("deleteNPC," + clientID.toString());
			forwardPacketToAll(message, clientID);
			sendPacket(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
		npcCtrl.setActive(false);
	}

	public void sendPelletCountMessage(String count) {
		try {
			if (count == "0") {
				sendVictoryMessage(0);
			} else {
				String message = "pelletCount," + count;
				sendPacketToAll(message);
			}
		} catch (IOException e) {
			System.out.println("Failed to send pellet count update");
			e.printStackTrace();
		}
	}
}
