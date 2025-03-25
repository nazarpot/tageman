package myGame;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;
	private CameraOrbitController orbitController;

	private int counter=0;
	private double lastFrameTime, currFrameTime, elapsedTime;

	private GameObject dol, avatar, terrain;
	private ObjShape dolS, ghostS, terrainS;
	private TextureImage doltx, ghostT, terrainT;
	private Light light1;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	public MyGame(String serverAddress, int serverPort, String protocol) { 
		super(); 
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	dolS = new ImportedModel("dolphinHighPoly.obj");
		//ghostS = new ImportedModel("dolphinHighPoly.obj");
		ghostS = new Sphere();
		terrainS = new TerrainPlane(1000);
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.png");
		ghostT = new TextureImage("redDolphin.jpg");
		//terrainT = new TextureImage("Originalpacmaze.jpg");
		terrainT = new TextureImage("pacmanmaze.jpg");

	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		initialScale = (new Matrix4f()).scaling(3.0f);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);
		avatar = dol;

		terrain = new GameObject(GameObject.root(), terrainS);
		initialTranslation = (new Matrix4f()).translation(0, 0, 0);
		terrain.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scale(50.0f, 4.0f, 50.0f);
		terrain.setLocalScale(initialScale);
		terrain.setHeightMap(terrainT);
		terrain.getRenderStates().setTiling(1);
		terrain.getRenderStates().setTileFactor(10);
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsedTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ------------- positioning the camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		String keyboardName = im.getKeyboardName();
		
		Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);

		//------------- INPUTS SECTION--------------------------
		MoveAction fwdAction = new MoveAction(this, 'F');
		MoveAction bkwdAction = new MoveAction(this, 'B');

		TurnAction turnAction = new TurnAction(this);
		TurnAction turnLeftAction = new TurnAction(this, 1);
		TurnAction turnRightAction = new TurnAction(this, -1);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._1, 
											fwdAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, 
											fwdAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._3, 
											bkwdAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, 
											bkwdAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, 
											turnLeftAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.X, 
											turnAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, 
											turnRightAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		setupNetworking();

	}

	@Override
	public void update()
	{	
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsedTime += (currFrameTime - lastFrameTime) / 1000.0;

		orbitController.updateCameraPosition();

		// build and set HUD
		int elapsedTimeSec = Math.round((float)elapsedTime);
		String elapsedTimeStr = Integer.toString(elapsedTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsedTimeStr;
		String dispStr2 = "Keyboard hits = " + counterStr;
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(0,0,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		im.update((float) elapsedTime);
		processNetworking((float) elapsedTime);

		
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{	case KeyEvent.VK_C:
				counter++;
				break;
			case KeyEvent.VK_2:
				dol.getRenderStates().setWireframe(true);
				break;
			case KeyEvent.VK_3:
				dol.getRenderStates().setWireframe(false);
				break;
			case KeyEvent.VK_4:
				(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,0));
				break;
		}
		super.keyPressed(e);
	}

	// ---------- NETWORKING SECTION ----------------

	public GameObject getAvatar() {return avatar;}
	public ObjShape getGhostShape() {return ghostS;}
	public TextureImage getGhostTexture() {return ghostT;}
	public GhostManager getGhostManager() {return gm;}
	public Engine getEngine() {return engine;}

	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	
				protClient.sendByeMessage();
			}
		}
	}
}
