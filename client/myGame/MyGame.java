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

	private boolean joined = false;
	private int counter=0;
	private double lastFrameTime, currFrameTime, elapsedTime;

	private int tronSky;
	private Vector<GameObject> avatarSelection = new Vector<GameObject>(5);
	private int selection = 0;

	private GameObject avatar, terrain;
	private GameObject tageman, dol, blinky, pinky, inky, clyde;
	private ObjShape dolS, ghostS, pacmanGhostS, terrainS, tageS;
	private TextureImage tageTX, ghostT, terrainT, mazeTx;
	private TextureImage blinkyT, pinkyT, inkyT, clydeT, scaredGhostT;
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
	{	tageS = new ImportedModel("tageman.obj");
		terrainS = new TerrainPlane(1000);
		//ghostS = new ImportedModel("dolphinHighPoly.obj");
		pacmanGhostS = new ImportedModel("ghost.obj");

	}

	@Override
	public void loadTextures()
	{	tageTX = new TextureImage("tageman.png");
		//ghostT = new TextureImage("redDolphin.jpg");
		mazeTx = new TextureImage("background.png");
		terrainT = new TextureImage("rigidpacmanmaze2.jpg");

		blinkyT = new TextureImage("blinky.png");
		pinkyT = new TextureImage("pinky.png");
		inkyT = new TextureImage("inky.png");
		clydeT = new TextureImage("clyde.png");
		scaredGhostT = new TextureImage("scared.png");

	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build dolphin in the center of the window
		tageman = new GameObject(GameObject.root(), tageS, tageTX);
		//initialTranslation = (new Matrix4f()).translation(0,1,0);
		initialScale = (new Matrix4f()).scaling(.5f);
		//tageman.setLocalTranslation(initialTranslation);
		tageman.setLocalScale(initialScale);
		avatar = tageman;
		avatarSelection.add(tageman);

		blinky = new GameObject(GameObject.root(), pacmanGhostS, blinkyT);
		initialScale = (new Matrix4f()).scaling(0.5f);
		blinky.setLocalScale(initialScale);
		blinky.getRenderStates().disableRendering();
		avatarSelection.add(blinky);

		pinky = new GameObject(GameObject.root(), pacmanGhostS, pinkyT);
		initialScale = (new Matrix4f()).scaling(0.5f);
		pinky.setLocalScale(initialScale);
		pinky.getRenderStates().disableRendering();
		avatarSelection.add(pinky);

		inky = new GameObject(GameObject.root(), pacmanGhostS, inkyT);
		initialScale = (new Matrix4f()).scaling(0.5f);
		inky.setLocalScale(initialScale);
		inky.getRenderStates().disableRendering();
		avatarSelection.add(inky);

		clyde = new GameObject(GameObject.root(), pacmanGhostS, clydeT);
		initialScale = (new Matrix4f()).scaling(0.5f);
		clyde.setLocalScale(initialScale);
		clyde.getRenderStates().disableRendering();
		avatarSelection.add(clyde);

		terrain = new GameObject(GameObject.root(), terrainS, mazeTx);
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
	{	Light.setGlobalAmbient(0.7f, 0.7f, 0.7f);
		light1 = new Light();
		light1.setDiffuse(1.0f, 1.0f, 1.0f);
		light1.setLocation(new Vector3f(0.0f, 1.0f, 0.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsedTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		//setupNetworking();

		// ------------- positioning the camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		String keyboardName = im.getKeyboardName();
		
		Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);

		//------------- INPUTS SECTION--------------------------
		MoveAction fwdAction = new MoveAction(this, protClient, 'F');
		MoveAction bkwdAction = new MoveAction(this, protClient, 'B');

		TurnAction turnAction = new TurnAction(this, protClient);
		TurnAction turnLeftAction = new TurnAction(this, protClient, 1);
		TurnAction turnRightAction = new TurnAction(this, protClient, -1);

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
	

	}

	@Override
	public void update()
	{	
		if (!joined) {
			avatar.setLocalTranslation((new Matrix4f()).translation(0, -15, 0));
		}

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

		Vector3f avatarLoc = avatar.getWorldLocation();
		light1.setLocation(avatarLoc);

		float height = terrain.getHeight(avatarLoc.x(), avatarLoc.z());
		if (joined) {
			avatar.setLocalLocation(new Vector3f(avatarLoc.x(), height+1, avatarLoc.z()));
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{
			case KeyEvent.VK_PERIOD:
				if (!joined) {
					selection += 1;
					selection %= 5;
					avatar.getRenderStates().disableRendering();
					avatar = avatarSelection.elementAt(selection);
					avatar.getRenderStates().enableRendering();
				}
				break;
			case KeyEvent.VK_COMMA:
				if (!joined) {
					if (selection == 0) {
						selection = 4;
					} else {
						selection -= 1;
					}
					avatar.getRenderStates().disableRendering();
					avatar = avatarSelection.elementAt(selection);
					avatar.getRenderStates().enableRendering();
				}
				break;
			case KeyEvent.VK_ENTER:
				joined = true;
				avatar.setLocalTranslation((new Matrix4f()).translation(0, 1, 0));

				im = engine.getInputManager();
				String gpName = im.getFirstGamepadName();
				String keyboardName = im.getKeyboardName();
		
				Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
				orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);

				ghostS = avatar.getShape();
				ghostT = avatar.getTextureImage();

				joinGame();
				break;
		}
		super.keyPressed(e);
	}

	private void joinGame() {
		setupNetworking();

		//------------- INPUTS SECTION--------------------------
		MoveAction fwdAction = new MoveAction(this, protClient, 'F');
		MoveAction bkwdAction = new MoveAction(this, protClient, 'B');

		TurnAction turnAction = new TurnAction(this, protClient);
		TurnAction turnLeftAction = new TurnAction(this, protClient, 1);
		TurnAction turnRightAction = new TurnAction(this, protClient, -1);

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
