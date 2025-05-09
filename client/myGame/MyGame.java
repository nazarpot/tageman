package myGame;

import tage.*;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;
import tage.physics.*;
import tage.physics.JBullet.JBulletPhysicsEngine;
import tage.physics.JBullet.JBulletPhysicsObject;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.*;
import java.util.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import net.java.games.input.Event;
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
	private Vector<GameObject> avatarSelection = new Vector<GameObject>();
	private String[] characterNames = {"tageman", "blinky", "pinky", "inky", "clyde"};
	private String characterName;
	private int selection = 0;

	private GameObject avatar, terrain;
	private GameObject tageman, dol, blinky, pinky, inky, clyde, pellet;
	private Vector<GameObject> powerPellets = new Vector<>();
	private ObjShape dolS, ghostS, pacmanGhostS, terrainS, pelletS;
	private TextureImage tageTX, ghostT, terrainT, mazeTx;
	private TextureImage blinkyT, pinkyT, inkyT, clydeT, casperT, scaredGhostT;
	private AnimatedShape tageS;
	private Light light1;
	private PhysicsEngine physicsEngine;
	private PhysicsObject pelletP, tageP, blinkyP, pinkyP, inkyP, clydeP, terrainP;
	private PhysicsObject wall1, wall2, wall3, wall4, wall5, wall6, wall7, wall8, wall9, wall10, wall11, wall12, wall13, wall14, wall15, wall16, wall17, wall18, wall19, wall20, wall21, wall22, wall23, wall24, wall25, wall26, wall27, wall28, wall29, wall30, wall31, wall32, wall33, wall34, wall35, wall36, wall37, wall38, wall39, wall40, wall41, wall42, gate;
	private float vals[] = new float[16];

	private ObjShape npcShape;
	private TextureImage npcTex;

	private IAudioManager audioMgr;
	private Sound hereSound, oceanSound;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false, alreadyMoving = false, isMovingForward = false, isMovingBackward = false;
	private boolean isGameOngoing = false, isGateOpen = false;
	private boolean gameStarted = false;

	private int mapWidth, mapHeight;

	private String dispStr2 = new String("choose character");
	private Vector3f hud2Color = new Vector3f(0, 1, 0);

	private boolean poweredUp = false;
	private double powerTime;

	private double prevCountdown, countdown;

	private int remainingLives = 3;
	private int remainingPellets;
	private boolean powerup = false;

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
	{	tageS = new AnimatedShape("tagemanMESH.rkm", "tagemanSKEL.rks");
		tageS.loadAnimation("Action.027", "tagemanCHOMP.rka");
		pelletS = new Sphere();
		terrainS = new TerrainPlane(1000);
		pacmanGhostS = new ImportedModel("ghost.obj");

	}

	@Override
	public void loadTextures()
	{	tageTX = new TextureImage("tagemanTex.png");
		mazeTx = new TextureImage("background.png");
		terrainT = new TextureImage("rigidpacmanmaze2.jpg");

		blinkyT = new TextureImage("blinky.png");
		pinkyT = new TextureImage("pinky.png");
		inkyT = new TextureImage("inky.png");
		clydeT = new TextureImage("clyde.png");
		casperT = new TextureImage("casper.png");
		scaredGhostT = new TextureImage("scared.png");

		npcTex = casperT;
	}

	@Override
	public void loadSounds(){ 
		AudioResource resource1, resource2;
		audioMgr = engine.getAudioManager();
		resource1 = audioMgr.createAudioResource("start.wav", AudioResourceType.AUDIO_SAMPLE);
		hereSound = new Sound(resource1, SoundType.SOUND_EFFECT, 10, false);
		hereSound.initialize(audioMgr);
		hereSound.setMaxDistance(10.0f);
		hereSound.setMinDistance(0.5f);
		hereSound.setRollOff(5.0f);
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		tageman = new GameObject(GameObject.root(), tageS, tageTX);
		initialScale = (new Matrix4f()).scaling(.5f);
		tageman.setLocalScale(initialScale);
		avatar = tageman;
		avatarSelection.add(tageman);

		blinky = new GameObject(GameObject.root(), pacmanGhostS, blinkyT);
		initialScale = (new Matrix4f()).scaling(0.4f);
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

		pellet = new GameObject(GameObject.root(), pelletS);
		initialTranslation = (new Matrix4f()).translation(0, .5f, 0);
		pellet.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(.1f);
		pellet.setLocalScale(initialScale);
		
		// Set ghost shape for NPC instantiation
		npcShape = pacmanGhostS;
	}


	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.2f, 0.2f, 0.5f);
		light1 = new Light();
		light1.setDiffuse(0.9f, 0.85f, 0.10f);	//yellow for pacman
		light1.setLocation(new Vector3f(0.0f, 1.0f, 0.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void loadSkyBoxes()
	{ 	tronSky = (engine.getSceneGraph()).loadCubeMap("tronSky");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(tronSky);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame()
	{
		im = engine.getInputManager();

		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsedTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900, 1000);

		// initial sound settings
		hereSound.setLocation(tageman.getWorldLocation());
		setEarParameters();
		hereSound.play();

		// --- initialize physics system ---
		float[] gravity = {0f, -5f, 0f};
		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		// --- only add terrain and pellet physics now ---
		float[] upVector = {0, 1f, 0};
		double[] tempTransform;
		float planeConstant = 0f;
		float pelletRadius = 0.1f;

		Matrix4f translation = new Matrix4f(pellet.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		pelletP = (engine.getSceneGraph()).addPhysicsSphere(0f, tempTransform, pelletRadius);
		pelletP.setBounciness(0.8f);
		pellet.setPhysicsObject(pelletP);

		translation = new Matrix4f(terrain.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		terrainP = (engine.getSceneGraph()).addPhysicsStaticPlane(tempTransform, upVector, planeConstant);
		terrain.setPhysicsObject(terrainP);

		//initializeAvatarPhysics(blinky, 10f);

		initilializeWallPhysics();

		// ------------- camera setup -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0, 0, 5));
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		String keyboardName = im.getKeyboardName();
		Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);

		//setupNetworking();

		//engine.disableGraphicsWorldRender();
		//engine.enablePhysicsWorldRender();
	}


	public void setEarParameters(){
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(avatar.getWorldLocation());
		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
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

		if (orbitController != null) {
			orbitController.updateCameraPosition();
		}

		usePowerPellet(false);
		countdownToStart();

		// build and set HUD
		int elapsedTimeSec = Math.round((float)elapsedTime);
		String elapsedTimeStr = Integer.toString(elapsedTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "";
		if (isGameOngoing) {
			dispStr1 = "Lives: " + remainingLives;
		}
		Vector3f hud1Color = new Vector3f(1,0,0);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		im.update((float) elapsedTime);
		processNetworking((float) elapsedTime);

		Vector3f avatarLoc = avatar.getWorldLocation();
		light1.setLocation(avatarLoc.add(0.0f, 1.0f, 0.0f));

		tageS.updateAnimation();

		// update sound
		hereSound.setLocation(tageman.getWorldLocation());
		setEarParameters();

		if (joined) {
			float radiusOffset = 0.5f;
			float stepSize = 0.05f;
			float moveSpeed = 2f;
		
			if (isMovingForward || isMovingBackward) {
				Vector4f moveDirection = isMovingForward ?
					new Vector4f(0f, 0f, 1f, 0f) :
					new Vector4f(0f, 0f, -1f, 0f);
		
				moveDirection.mul(avatar.getWorldRotation());
		
				Vector3f currPos = avatar.getWorldLocation();
				Vector3f nextPos = new Vector3f(
					currPos.x() + moveDirection.x() * stepSize,
					currPos.y(),
					currPos.z() + moveDirection.z() * stepSize
				);
		
				Matrix4f newTransform = new Matrix4f().translation(
					nextPos.x(), currPos.y(), nextPos.z()
				);
		
				avatar.getPhysicsObject().setTransform(toDoubleArray(newTransform.get(vals)));
				avatar.getPhysicsObject().setLinearVelocity(new float[] {
					moveDirection.x() * moveSpeed,
					0f,
					moveDirection.z() * moveSpeed
				});
				
				if (protClient != null) {
					protClient.sendMoveMessage(avatar.getWorldLocation(), 0.0f);
				}
			} else {
				avatar.getPhysicsObject().setLinearVelocity(new float[] { 0f, 0f, 0f });
			}
		
			// Physics and visual sync
			AxisAngle4f aa = new AxisAngle4f();
			Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			Matrix4f mat3 = new Matrix4f().identity();
		
			checkForCollisions();
			physicsEngine.update((float)elapsedTime);
		
			for (GameObject go : engine.getSceneGraph().getGameObjects()) {
				if (go.getPhysicsObject() != null) {
					// Get transform from physics
					mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
		
					// Set translation visually
					mat2.set(3, 0, mat.m30());
					mat2.set(3, 1, mat.m31());
					mat2.set(3, 2, mat.m32());
					go.setLocalTranslation(mat2);
		
					// Set rotation if not player avatar
					if (go != avatar) {
						mat.getRotation(aa);
						mat3.rotation(aa);
						go.setLocalRotation(mat3);
					}
				}
			}
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
				if (!joined) {
					characterName = characterNames[selection];
				joinGame(characterName);

				im = engine.getInputManager();
				String gpName = im.getFirstGamepadName();
				String keyboardName = im.getKeyboardName();
				Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
				orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);
				}
				
				break;
			case KeyEvent.VK_BACK_SLASH:
				if (!joined) {
				characterName = characterNames[selection];
				String gpName = im.getFirstGamepadName();
				String keyboardName = im.getKeyboardName();
				Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
				orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);

				joined = true;
				dispStr2 = "";

				setUpGame();
				}
				
				break;
			case KeyEvent.VK_SPACE:		//to start game. pacman starts the game
				if (joined == true && characterName.equals("tageman") && !isGameOngoing) {
					startGame();
				}
			case KeyEvent.VK_G:
				if (isGateOpen) {
					closeGate();
				} else {
					openGate();
				}
				break;
		}
		super.keyPressed(e);
	}

	private void joinGame(String character) {
		setupNetworking(character);

		avatar.setLocalTranslation(new Matrix4f().translation(0f, .5f, -5f));

		setUpGame();
	}

	private void setUpGame() {
		initializeAvatarPhysics(avatar, 10f);

		//------------- INPUTS SECTION--------------------------
		MoveAction fwdAction = new MoveAction(this, protClient, 'F');
		MoveAction bkwdAction = new MoveAction(this, protClient, 'B');

		TurnAction turnAction = new TurnAction(this, protClient);
		TurnAction turnLeftAction = new TurnAction(this, protClient, 1);
		TurnAction turnRightAction = new TurnAction(this, protClient, -1);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._1, 
											fwdAction, 
											InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, 
											fwdAction, 
											InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Button._3, 
											bkwdAction, 
											InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, 
											bkwdAction, 
											InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, 
											turnLeftAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.X, 
											turnAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, 
											turnRightAction, 
											InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	
		if (characterName == "tageman") {
			//create power pellets
			for (int i = -1; i < 2; i = i + 2) {
				for (int j = -1; j < 2; j = j + 2) {
					GameObject powerPellet = new GameObject(GameObject.root(), pelletS);
					Matrix4f initialTranslation = (new Matrix4f()).translation((i * 44), 1, (j * 22));
					powerPellet.setLocalTranslation(initialTranslation);
					powerPellets.add(powerPellet);
				}
			}

			for (int i = 0; i < 4; i++) {
				GameObject powerPellet = powerPellets.elementAt(i);
				Matrix4f translation = new Matrix4f(powerPellet.getLocalTranslation());
				double[] tempTransform = toDoubleArray(translation.get(vals));
				PhysicsObject powerP = (engine.getSceneGraph()).addPhysicsSphere(0f, tempTransform, 1.0f);
				powerP.setBounciness(0.8f);
				powerPellet.setPhysicsObject(powerP);
			}
		}
	}

	public void confirmJoin() {
		joined = true;
		avatar.setLocalTranslation((new Matrix4f()).translation(0, 1, -5));
		ghostS = avatar.getShape();
		ghostT = avatar.getTextureImage();

		dispStr2 = "";
	}

	public void startGame() {
		System.out.println("starting game");
		if (characterName == "tageman") {
			countdownToStart();
		}

		isGameOngoing = true;
	}

	private void countdownToStart() {
		if (isGameOngoing) {
			if (countdown <= 0 && !gameStarted) {
				gameStarted = true;
				protClient.sendStartGame(0);

				Vector3f position = new Vector3f(0.0f, 1.0f, 5.0f);
				System.out.println("creating npc ghost");
				protClient.sendCreateNPCmessage(position);
			} else {
				countdown = countdown - (currFrameTime - lastFrameTime);
				if ((prevCountdown - countdown) >= 0) {
					protClient.sendStartGame(Math.round((float)countdown));
					prevCountdown = Math.round((float)countdown);
					dispStr2 = "Starting game in " + countdown;
				} 
			}
		} else {
			countdown = 10000;
			prevCountdown = 10000;
		}
	}

	public void setTagemanChomp(boolean moving) {
		if (moving && !alreadyMoving) {
			tageS.playAnimation("Action.027", 0.25f, AnimatedShape.EndType.LOOP, 0);
		} else if (!moving && alreadyMoving) {
			tageS.stopAnimation();
		}
		alreadyMoving = moving;
	}

	public void setMovingForward(boolean value) {
		isMovingForward = value;
	}

	public void setMovingBackward(boolean value) {
		isMovingBackward = value;
	}

	// ------------------ UTILITY FUNCTIONS used by physics
	public void initilializeWallPhysics() {
		float[] wall1S = {26.5f, 3.5f, 1.75f};//{width (X), height (Y), depth (Z)}
		float[] wall2S = {1.75f, 3.5f, 12f};
		float[] wall3S = {1.75f, 3.5f, 12f};
		float[] wall4S = {9.5f, 3.5f, 1.5f};
		float[] wall5S = {9f, 3.5f, 1.5f};
		float[] wall6S = {5.25f, 3.5f, 10.5f};
		float[] wall7S = {27f, 3.5f, 5f};
		float[] wall8S = {16.25f, 3.5f, 4.75f};
		float[] wall9S = {16.25f, 3.5f, 4.75f};
		float[] wall10S = {5.35f, 3.5f, 24.25f};
		float[] wall11S = {5.35f, 3.5f, 24.25f};
		float[] wall12S = {16.25f, 3.5f, 8.125f};
		float[] wall13S = {16.25f, 3.5f, 8.125f};
		float[] wall14S = {12.5f, 3.5f, 8.125f};
		float[] wall15S = {12.5f, 3.5f, 8.125f};
		float[] wall16S = {5.5f, 3.5f, 14.75f};
		float[] wall17S = {12.5f, 3.5f, 5f};
		float[] wall18S = {12.5f, 3.5f, 5f};
		float[] wall19S = {5.35f, 3.5f, 14.65f};
		float[] wall20S = {5.35f, 3.5f, 14.65f};
		float[] wall21S = {5.5f, 3.5f, 10.5f};
		float[] wall22S = {27f, 3.5f, 5f};
		float[] wall23S = {5.5f, 3.5f, 10.5f};
		float[] wall24S = {27f, 3.5f, 5f};
		float[] wall25S = {16.25f, 3.5f, 4.875f};
		float[] wall26S = {16.25f, 3.5f, 4.875f};
		float[] wall27S = {5.75f, 3.5f, 14.65f};
		float[] wall28S = {12.5f, 3.5f, 5f};
		float[] wall29S = {5.5f, 3.5f, 14.65f};
		float[] wall30S = {12.5f, 3.5f, 5f};
		float[] wall31S = {5.5f, 3.5f, 14.65f};
		float[] wall32S = {5.5f, 3.5f, 14.65f};
		float[] wall33S = {34.25f, 3.5f, 5f};
		float[] wall34S = {34.25f, 3.5f, 5f};
		float[] wall35S = {8f, 3.5f, 5f};
		float[] wall36S = {8f, 3.5f, 5f};
		float[] wall37S = {96f, 3.5f, 2f};
		float[] wall38S = {96f, 3.5f, 2f};
		float[] wall39S = {2f, 3.5f, 98f};
		float[] wall40S = {2f, 3.5f, 98f};
		float[] wall41S = {25f, 3.5f, 34f};
		float[] wall42S = {25f, 3.5f, 34f};
		float[] gateS = {9f, 3.5f, 1.5f};
		Matrix4f wall1T = new Matrix4f().translation(0f, 2f, 3.25f);//position
		Matrix4f wall2T = new Matrix4f().translation(12.5f, 2f, -3f);
		Matrix4f wall3T = new Matrix4f().translation(-12.5f, 2f, -3f);
		Matrix4f wall4T = new Matrix4f().translation(8.75f, 2f, -9.75f);
		Matrix4f wall5T = new Matrix4f().translation(-8.75f, 2f, -9.75f);
		Matrix4f wall6T = new Matrix4f().translation(0f, 2f, -20.25f);
		Matrix4f wall7T = new Matrix4f().translation(0f, 2f, -27.375f);
		Matrix4f wall8T = new Matrix4f().translation(-16f, 2f, -17.75f);
		Matrix4f wall9T = new Matrix4f().translation(16f, 2f, -17.75f);
		Matrix4f wall10T = new Matrix4f().translation(-21.45f, 2f, -17.75f);
		Matrix4f wall11T = new Matrix4f().translation(21.45f, 2f, -17.75f);
		Matrix4f wall12T = new Matrix4f().translation(-16f, 2f, -38.7f);
		Matrix4f wall13T = new Matrix4f().translation(16f, 2f, -38.7f);
		Matrix4f wall14T = new Matrix4f().translation(-35.75f, 2f, -38.7f);
		Matrix4f wall15T = new Matrix4f().translation(35.75f, 2f, -38.7f);
		Matrix4f wall16T = new Matrix4f().translation(0f, 2f, -42f);
		Matrix4f wall17T = new Matrix4f().translation(-35.75f, 2f, -27.375f);
		Matrix4f wall18T = new Matrix4f().translation(35.75f, 2f, -27.375f);
		Matrix4f wall19T = new Matrix4f().translation(-21.425f, 2f, 6.45f);
		Matrix4f wall20T = new Matrix4f().translation(21.425f, 2f, 6.45f);
		Matrix4f wall21T = new Matrix4f().translation(0f, 2f, 18.3f);
		Matrix4f wall22T = new Matrix4f().translation(0f, 2f, 11.25f);
		Matrix4f wall23T = new Matrix4f().translation(0f, 2f, 37.675f);
		Matrix4f wall24T = new Matrix4f().translation(0f, 2f, 30.675f);
		Matrix4f wall25T = new Matrix4f().translation(-16f, 2f, 20.95f);
		Matrix4f wall26T = new Matrix4f().translation(16f, 2f, 20.95f);
		Matrix4f wall27T = new Matrix4f().translation(-32.25f, 2f, 25.875f);
		Matrix4f wall28T = new Matrix4f().translation(-35.75f, 2f, 21f);
		Matrix4f wall29T = new Matrix4f().translation(32.125f, 2f, 25.875f);
		Matrix4f wall30T = new Matrix4f().translation(35.75f, 2f, 21f);
		Matrix4f wall31T = new Matrix4f().translation(-21.425f, 2f, 35.5f);
		Matrix4f wall32T = new Matrix4f().translation(21.425f, 2f, 35.4f);
		Matrix4f wall33T = new Matrix4f().translation(-25f, 2f, 40.3f);
		Matrix4f wall34T = new Matrix4f().translation(25f, 2f, 40.3f);
		Matrix4f wall35T = new Matrix4f().translation(-44f, 2f, 30.6f);
		Matrix4f wall36T = new Matrix4f().translation(44f, 2f, 30.6f);
		Matrix4f wall37T = new Matrix4f().translation(0f, 2f, 48.5f);
		Matrix4f wall38T = new Matrix4f().translation(0f, 2f, -48.5f);
		Matrix4f wall39T = new Matrix4f().translation(-48.25f, 2f, 0f);
		Matrix4f wall40T = new Matrix4f().translation(48.25f, 2f, 0f);
		Matrix4f wall41T = new Matrix4f().translation(-41.875f, 2f, -3.25f);
		Matrix4f wall42T = new Matrix4f().translation(41.875f, 2f, -3.25f);
		Matrix4f gateT = new Matrix4f().translation(0f, 2f, -9.75f);
		double[] transformArray = toDoubleArray(wall1T.get(vals));
		wall1 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall1S);
		transformArray = toDoubleArray(wall2T.get(vals));
		wall2 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall2S);
		transformArray = toDoubleArray(wall3T.get(vals));
		wall3 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall3S);
		transformArray = toDoubleArray(wall4T.get(vals));
		wall4 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall4S);
		transformArray = toDoubleArray(wall5T.get(vals));
		wall5 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall5S);
		transformArray = toDoubleArray(wall6T.get(vals));
		wall6 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall6S);
		transformArray = toDoubleArray(wall7T.get(vals));
		wall7 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall7S);
		transformArray = toDoubleArray(wall8T.get(vals));
		wall8 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall8S);
		transformArray = toDoubleArray(wall9T.get(vals));
		wall9 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall9S);
		transformArray = toDoubleArray(wall10T.get(vals));
		wall10 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall10S);
		transformArray = toDoubleArray(wall11T.get(vals));
		wall11 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall11S);
		transformArray = toDoubleArray(wall12T.get(vals));
		wall12 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall12S);
		transformArray = toDoubleArray(wall13T.get(vals));
		wall13 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall13S);
		transformArray = toDoubleArray(wall14T.get(vals));
		wall14 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall14S);
		transformArray = toDoubleArray(wall15T.get(vals));
		wall15 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall15S);
		transformArray = toDoubleArray(wall16T.get(vals));
		wall16 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall16S);
		transformArray = toDoubleArray(wall17T.get(vals));
		wall17 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall17S);
		transformArray = toDoubleArray(wall18T.get(vals));
		wall18 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall18S);
		transformArray = toDoubleArray(wall19T.get(vals));
		wall19 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall19S);
		transformArray = toDoubleArray(wall20T.get(vals));
		wall20 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall20S);
		transformArray = toDoubleArray(wall21T.get(vals));
		wall21 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall21S);
		transformArray = toDoubleArray(wall22T.get(vals));
		wall22 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall22S);
		transformArray = toDoubleArray(wall23T.get(vals));
		wall23 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall23S);
		transformArray = toDoubleArray(wall24T.get(vals));
		wall24 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall24S);
		transformArray = toDoubleArray(wall25T.get(vals));
		wall25 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall25S);
		transformArray = toDoubleArray(wall26T.get(vals));
		wall26 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall26S);
		transformArray = toDoubleArray(wall27T.get(vals));
		wall27 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall27S);
		transformArray = toDoubleArray(wall28T.get(vals));
		wall28 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall28S);
		transformArray = toDoubleArray(wall29T.get(vals));
		wall29 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall29S);
		transformArray = toDoubleArray(wall30T.get(vals));
		wall30 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall30S);
		transformArray = toDoubleArray(wall31T.get(vals));
		wall31 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall31S);
		transformArray = toDoubleArray(wall32T.get(vals));
		wall32 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall32S);
		transformArray = toDoubleArray(wall33T.get(vals));
		wall33 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall33S);
		transformArray = toDoubleArray(wall34T.get(vals));
		wall34 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall34S);
		transformArray = toDoubleArray(wall35T.get(vals));
		wall35 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall35S);
		transformArray = toDoubleArray(wall36T.get(vals));
		wall36 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall36S);
		transformArray = toDoubleArray(wall37T.get(vals));
		wall37 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall37S);
		transformArray = toDoubleArray(wall38T.get(vals));
		wall38 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall38S);
		transformArray = toDoubleArray(wall39T.get(vals));
		wall39 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall39S);
		transformArray = toDoubleArray(wall40T.get(vals));
		wall40 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall40S);
		transformArray = toDoubleArray(wall41T.get(vals));
		wall41 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall41S);
		transformArray = toDoubleArray(wall42T.get(vals));
		wall42 = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, wall42S);
		transformArray = toDoubleArray(gateT.get(vals));
		gate = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, gateS);
	}

	public void openGate() {
		if (gate != null) {
			physicsEngine.removeObject(gate.getUID());
			gate = null;
			System.out.println("Gate opened");
		}
		isGateOpen = true;
	}

	public void closeGate() {
		if (gate == null) {
			Matrix4f gateT = new Matrix4f().translation(0f, 2f, -9.75f);
			float[] gateSize = new float[] {9f, 3.5f, 1.5f};
			double[] transformArray = toDoubleArray(gateT.get(vals));
			gate = (engine.getSceneGraph()).addPhysicsBox(0f, transformArray, gateSize);
			System.out.println("Gate closed");
		}
		isGateOpen = false;
	}

	private float[] toFloatArray(double[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float)arr[i];
		}
		return ret;
	}

	private double[] toDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}

	private void checkForCollisions() {
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;
		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++) {
			manifold = dispatcher.getManifoldByIndexInternal(i);
			if (manifold != null) {
				object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

			for (int k = 0; k < powerPellets.size(); k++) {
				GameObject powerPellet = powerPellets.elementAt(k);
				int avatarUID = avatar.getPhysicsObject().getUID();
				int pelletUID = powerPellet.getPhysicsObject().getUID();

				boolean tagemanHitPellet = (
				(obj1.getUID() == avatarUID && obj2.getUID() == pelletUID) ||
				(obj2.getUID() == avatarUID && obj1.getUID() == pelletUID)
				);

				for (int j = 0; j < manifold.getNumContacts(); j++) {
					contactPoint = manifold.getContactPoint(j);
					if (contactPoint.getDistance() < 0.0f && tagemanHitPellet) {
						System.out.println("Pellet eaten!");
						powerPellet.getRenderStates().disableRendering();
						(engine.getSceneGraph()).removeGameObject(powerPellet);
						physicsEngine.removeObject(powerPellet.getPhysicsObject().getUID());
						usePowerPellet(true);
						break;
					}
					if (contactPoint.getDistance() < 0.0f && ((obj1 == tageP && obj2 == blinkyP) || (obj1 == blinkyP && obj2 == tageP))) {
						// Apply bounce forces
						// Get positions
						javax.vecmath.Vector3f posTageman = new javax.vecmath.Vector3f();
						javax.vecmath.Vector3f posBlinky = new javax.vecmath.Vector3f();
						((JBulletPhysicsObject) tageP).getRigidBody().getCenterOfMassPosition(posTageman);
						((JBulletPhysicsObject) blinkyP).getRigidBody().getCenterOfMassPosition(posBlinky);


						// Direction from Blinky to Tageman
						javax.vecmath.Vector3f bounceDirection = new javax.vecmath.Vector3f();
						bounceDirection.sub(posTageman, posBlinky); // Tageman - Blinky
						bounceDirection.normalize();

						// Now apply impulse to both
						javax.vecmath.Vector3f impulseToTageman = new javax.vecmath.Vector3f(bounceDirection);
						impulseToTageman.scale(.5f); //force

						javax.vecmath.Vector3f impulseToBlinky = new javax.vecmath.Vector3f(bounceDirection);
						impulseToBlinky.scale(-.5f); //opposite direction

						((JBulletPhysicsObject) tageP).getRigidBody().applyCentralImpulse(impulseToTageman);
						((JBulletPhysicsObject) blinkyP).getRigidBody().applyCentralImpulse(impulseToBlinky);

						System.out.println("Tageman and Blinky bounced!");
					}
				}
			}
			
			
			}
			
		}
	}

	public void initializeAvatarPhysics(GameObject character, float mass) {
		float radius = 0.5f;
		float height = 0.15f;

    	//Adjust shape if it's a ghost
		if (character.getShape() == pacmanGhostS) {
			radius = 0.4f;
			height = 0.2f;
		}

		Matrix4f translation = new Matrix4f(character.getLocalTranslation());
		double[] tempTransform = toDoubleArray(translation.get(vals));
		PhysicsObject po = engine.getSceneGraph().addPhysicsCapsule(mass, tempTransform, radius, height);
		po.setBounciness(0.8f);
		character.setPhysicsObject(po);
		((JBulletPhysicsObject) po).getRigidBody().setAngularFactor(0f);

		// Assign named reference based on character
		if (character == tageman) {
			tageP = po;
		} else if (character == blinky) {
			blinkyP = po;
		} else if (character == pinky) {
			pinkyP = po;
		} else if (character == inky) {
			inkyP = po;
		} else if (character == clyde) {
			clydeP = po;
		}
	}

	public  void usePowerPellet(Boolean activate) {
		if (poweredUp == true) {
			if (powerTime < 0) {
				Matrix4f scale = (new Matrix4f().scaling(0.5f));
				avatar.setLocalScale(scale);
				if (protClient != null) {
					protClient.sendPowerMessage(false);
				}

				int num = gm.getGhostsNum();
				for (int i = 0; i < num; i++) {
					GhostAvatar go = (GhostAvatar) gm.getAvatar(i);
					if (go.getName() != "tageman") {
						go.setTextureImage(getGhostTexture(go.getName()));
					}
				}
				
				poweredUp = false;
			} else {
				powerTime = powerTime - (currFrameTime - lastFrameTime);
			}
		} else {
			if (activate) {
				poweredUp = true;
				powerTime = 20000;
				Matrix4f scale = (new Matrix4f()).scaling(2.0f);
				avatar.setLocalScale(scale);
				if (protClient != null) {
					protClient.sendPowerMessage(true);
				}

				int num = gm.getGhostsNum();
				for (int i = 0; i < num; i++) {
					GhostAvatar go = (GhostAvatar) gm.getAvatar(i);
					if (go.getName() != "tageman") {
						go.setTextureImage(scaredGhostT);
					}
				}
			}
		}
	}

	// ---------- NETWORKING SECTION ----------------

	public GameObject getAvatar() {return avatar;}
	public ObjShape getGhostShape(String name) {
		if (name.equals("tageman")) {
			return tageS;
		} else if (name.equals("blinky") || name.equals("pinky") || name.equals("inky") || name.equals("clyde")) {
			return pacmanGhostS;
		} else {
			return null;
		}
	}
	public TextureImage getGhostTexture(String name) {
		if (name.equals("tageman")) {
			return tageTX;
		} else if (name.equals("blinky")) {
			return blinkyT;
		} else if (name.equals("pinky")) {
			return pinkyT;
		} else if (name.equals("inky")) {
			return inkyT;
		} else if (name.equals("clyde")) {
			return clydeT;
		} else {
			return null;
		}
	}
	public GhostManager getGhostManager() {return gm;}
	public Engine getEngine() {return engine;}

	private void setupNetworking(String character)
	{	isClientConnected = false;	
		try {	
			if (protClient == null) {
				protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
			}
		} 	catch (UnknownHostException e) {	
			e.printStackTrace();
		}	catch (IOException e) {	
			e.printStackTrace();
		} 
		if (protClient == null) {	
			System.out.println("missing protocol host");
		} else {	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage(character);
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null) {
			protClient.processPackets();
		}
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public String getCharacterName() { return characterName; }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }

	@Override
	public void shutdown() {
		super.shutdown();
		if (protClient != null) {
			protClient.sendByeMessage(characterName);
		}
	}
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	
				protClient.sendByeMessage(characterName);
			}
		}
	}

	public ObjShape getNPCshape() { return npcShape; }
	public TextureImage getNPCtexture() { return npcTex; }

	public void setHUD2string(String s) {dispStr2 = s;}

	public void setLives(int lives) { remainingLives = lives; }


	public void setPowerup(UUID ghostID, boolean powerup) { 
		this.powerup = powerup; 
		GameObject pacman = gm.findAvatar(ghostID);

		if (powerup) {
			Matrix4f scale = (new Matrix4f().scaling(2.f));
			pacman.setLocalScale(scale);

			int num = gm.getGhostsNum();
			for (int i = 0; i < num; i++) {
				GhostAvatar go = (GhostAvatar) gm.getAvatar(i);
				if (go.getName() != "tageman") {
					go.setTextureImage(scaredGhostT);
				}
			}

			if (characterName != "tageman") {
				avatar.setTextureImage(scaredGhostT);
			}
			
		} else {
			Matrix4f scale = (new Matrix4f()).scaling(0.5f);
			pacman.setLocalScale(scale);

			int num = gm.getGhostsNum();
			for (int i = 0; i < num; i++) {
				GhostAvatar go = (GhostAvatar) gm.getAvatar(i);
				if (go.getName() != "tageman") {
					go.setTextureImage(getGhostTexture(go.getName()));
				}
			}

			avatar.setTextureImage(getGhostTexture(characterName));
		}
	}
}