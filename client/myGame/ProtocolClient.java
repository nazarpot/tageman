package myGame;

import java.lang.Math;
import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.shapes.*;
import tage.networking.IGameConnection.ProtocolType;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	private GhostNPC ghostNPC;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	
	@Override
	protected void processPacket(Object message)
	{	String strMessage = (String)message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");
		
		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	
					System.out.println("join success confirmed");
					game.setIsConnected(true);
					game.confirmJoin();
					String character = messageTokens[2];
					sendCreateMessage(game.getPlayerPosition(), character, game.getAvatar().getLocalForwardVector());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}

			//if a character is taken
			if (messageTokens[0].compareTo("taken") == 0) {
				System.out.println("character is taken");
				game.setHUD2string("character is taken");
			}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				String character = messageTokens[5];

				Vector3f forwardVector = new Vector3f (
					Float.parseFloat(messageTokens[6]),
					Float.parseFloat(messageTokens[7]), 
					Float.parseFloat(messageTokens[8]));

				try
				{	ghostManager.createGhostAvatar(ghostID, character, ghostPosition, forwardVector);
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}
			
			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition(), game.getCharacterName(), game.getAvatar().getLocalForwardVector());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z,r)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4])
				);

				Float ghostRotation = Float.parseFloat(messageTokens[5]);
				
				ghostManager.updateGhostAvatar(ghostID, ghostPosition, ghostRotation);
			}

			//to chomp or not to chomp
			if (messageTokens[0].compareTo("chomp") == 0) {
				Boolean toChomp = Boolean.parseBoolean(messageTokens[2]);
				game.setTagemanChomp(toChomp);
			}

			// START GAME
			if (messageTokens[0].compareTo("start") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				float time = Float.parseFloat(messageTokens[2]);
				game.setGameOngoing(true);
				game.updateCountdown(time);
			}

			// POWER UP
			if (messageTokens[0].compareTo("power") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				Boolean powerup = Boolean.parseBoolean(messageTokens[2]);
				game.setPowerup(ghostID, powerup);
				if (powerup) {
					ghostNPC.setTextureImage(game.getScaredTexture());
				} else {
					ghostNPC.setTextureImage(game.getNPCtexture());
				}
			}

			// EATEN message
			if (messageTokens[0].compareTo("eaten") == 0) {
				game.eaten();
			}

			// CAUGHT message 
			if (messageTokens[0].compareTo("caught") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				int lives = Integer.parseInt(messageTokens[2]);
				game.reset(lives);
			}

			// VICTORY MESSAGE
			// 0 - tageman won
			// 1 - ghosts win
			if (messageTokens[0].compareTo("victory") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				int who = Integer.parseInt(messageTokens[2]);
				game.victory(who);
			}

			// create ghost npc
			if (messageTokens[0].compareTo("createNPC") == 0) {
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4])
				);
				try {
					createGhostNPC(ghostPosition);
				} catch (IOException e) {
					System.out.println("error creating npc");
				}
				System.out.println("NPC created");
			}

			//NPC INFO
			//update NPC location
			if (messageTokens[0].compareTo("npcInfo") == 0) {
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3])
				);

				//check if tageman collides with NPC
				if (game.getCharacterName() == "tageman") {
					Vector3f tagemanPos = game.getAvatar().getWorldLocation();
					if (Math.abs(tagemanPos.x() - ghostPosition.x()) <= 1 && Math.abs(tagemanPos.z() - ghostPosition.z()) <= 1) {
						game.setLives(game.getLives() - 1);
						sendCaughtMessage(game.getLives());
						
					} else if (Math.abs(tagemanPos.x() - ghostPosition.x()) <= 2 && Math.abs(tagemanPos.z() - ghostPosition.z()) <= 2) {
						if (game.getPoweredUp()) {
							sendEatNPCMessage();
						}		
					} else {
						ghostNPC.setPosition(ghostPosition);
					}
				} else {
					ghostNPC.setPosition(ghostPosition);
				}
			}

			//NPC to look at tageman
			if (messageTokens[0].compareTo("lookAt") == 0) {
				Vector3f tagemanPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3])
				);
				ghostNPC.lookAtTageman(tagemanPosition);
			}

			if (messageTokens[0].compareTo("pelletCount") == 0) {
				int count = Integer.parseInt(messageTokens[1]);
				game.setPelletCount(count);
			}
		}
	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage(String character)
	{	try 
		{	sendPacket(new String("join," + id.toString() + "," + character));
			System.out.println("client requesting to join server");
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage(String character)
	{	try 
		{	sendPacket(new String("bye," + id.toString() + "," + character));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the clients Avatars position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position, String character, Vector3f forward)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + character;
			message += "," + forward.x();
			message += "," + forward.y();
			message += "," + forward.z();
			
			sendPacket(message);

			System.out.println(character + " character created");
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position, String character, Vector3f forward)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + character;
			message += "," + forward.x();
			message += "," + forward.y();
			message += "," + forward.z();
			
			sendPacket(message);

			System.out.println(character + " gave details");
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessage(Vector3f position, float rotateBy)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + rotateBy;
			
			sendPacket(message);
		} catch (IOException e) 
		{	
			e.printStackTrace();
		}	
	}

	public void sendChompMessage(boolean toChomp) {
		try {
			String message = new String("chomp," + id.toString());
			message += "," + Boolean.toString(toChomp);
			sendPacket(message);
		} catch (IOException e) {
			System.out.println("failure to update chomp");
			e.printStackTrace();
		}
	}

	public void sendStartGame(int time) {
		try {
			String message = new String("start," + id.toString());
			message += "," + time;
			sendPacket(message);
		} catch (IOException e) {
			System.out.println("failure to start game");
			e.printStackTrace();
		}
	}

	public void sendPowerMessage(boolean isPowered) {
		//true if power up is in play
		//false if powerup is no longer in play
		try {
			String message = new String("power," + id.toString());
			message += "," + Boolean.toString(isPowered);
			sendPacket(message);
		} catch (IOException e) {
			System.out.println("power message not sent");
			e.printStackTrace();
		}
	}

	public void sendEatenMessage(UUID recipientID) {
		try {
			String message = new String("eaten," + id.toString());
			message += "," + recipientID;
			sendPacket(message);
		} catch (IOException e) {
			System.out.println("eaten message not sent");
			e.printStackTrace();
		}
	}

	public void sendCaughtMessage(int lives) {
		try {
			String message = new String("caught," + id.toString());
			message += "," + lives;
			sendPacket(message);
		} catch (IOException e) {
			System.out.println("tageman caught not sent to server");
			e.printStackTrace();
		}
	}

	// ---------------- Ghost section -------------------
	public void sendCreateNPCmessage(Vector3f position) {
		try {
			createGhostNPC(position);
			String message = new String("createNPC," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();

			sendPacket(message);

			System.out.println("created NPC");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void createGhostNPC(Vector3f position) throws IOException {
		if (ghostNPC == null) {
			ghostNPC = new GhostNPC(0, game.getNPCshape(), game.getNPCtexture(), position);
		}
	}

	public void sendEatNPCMessage() {
		try {
			String message = new String("eatNPC");
			sendPacket(message);
		} catch (IOException e) {
			System.out.println("failure to eat NPC");
			e.printStackTrace();
		}
	}

	public void sendDeleteNPC() {
		try {
			String message = new String("deleteNPC," + id.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void deleteNPC() {
		(game.getEngine().getSceneGraph()).removeGameObject(ghostNPC);
		ghostNPC = null;
	}

	public void sendPelletCount(int count) {
		try {
			String message = new String("pelletCount," + count);
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
