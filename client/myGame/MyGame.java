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
	private ObjShape dolS, ghostS, pacmanGhostS, terrainS, pelletS;
	private TextureImage tageTX, ghostT, terrainT, mazeTx;
	private TextureImage blinkyT, pinkyT, inkyT, clydeT, scaredGhostT;
	private AnimatedShape tageS;
	private Light light1;
	private PhysicsEngine physicsEngine;
	private PhysicsObject pelletP, tageP, blinkyP, pinkyP, inkyP, clydeP, terrainP;
	private float vals[] = new float[16], bounceCooldown;

	private ObjShape npcShape;
	private TextureImage npcTex;

	private IAudioManager audioMgr;
	private Sound hereSound, oceanSound;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false, alreadyMoving = false, isMovingForward = false, isMovingBackward = false;
	private boolean isGameOngoing = false;

	private int mapWidth, mapHeight;

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
		scaredGhostT = new TextureImage("scared.png");

		npcTex = scaredGhostT;
	}

	@Override
	public void loadSounds(){ 
		AudioResource resource1, resource2;
		audioMgr = engine.getAudioManager();
		resource1 = audioMgr.createAudioResource("here.wav", AudioResourceType.AUDIO_SAMPLE);
		hereSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, true);
		hereSound.initialize(audioMgr);
		hereSound.setMaxDistance(10.0f);
		hereSound.setMinDistance(0.5f);
		hereSound.setRollOff(5.0f);
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build dolphin in the center of the window
		tageman = new GameObject(GameObject.root(), tageS, tageTX);
		initialScale = (new Matrix4f()).scaling(.5f);
		tageman.setLocalScale(initialScale);
		//tageman.getRenderStates().disableRendering();
		avatar = tageman;
		avatarSelection.add(tageman);

		blinky = new GameObject(GameObject.root(), pacmanGhostS, blinkyT);
		//initialTranslation = new Matrix4f().translation(5f, 1f, -5f);
		initialScale = (new Matrix4f()).scaling(0.4f);
		//blinky.setLocalTranslation(initialTranslation);
		blinky.setLocalScale(initialScale);
		//blinky.getRenderStates().disableRendering();
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

		npcShape = pacmanGhostS;

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
	public void loadSkyBoxes()
	{ 	tronSky = (engine.getSceneGraph()).loadCubeMap("tronSky");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(tronSky);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsedTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// initial sound settings
		hereSound.setLocation(tageman.getWorldLocation());
		setEarParameters();
		hereSound.play();
		
		// --- initialize physics system ---
		float[] gravity = {0f, -5f, 0f};
		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(gravity);

		// --- create physics world ---
		float mass = 10.0f;
		float[] upVector = {0,1f,0};
		float pelletRadius = 0.1f;
		float tagemanRadius = .5f;
		float tagemanHeight = .15f;
		float ghostRadius = .4f;
		float ghostHeight = .5f;
		//float height = 2.0f;
		double[ ] tempTransform;
		float planeConstant = 0f;
		
		Matrix4f translation = new Matrix4f(pellet.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		pelletP = (engine.getSceneGraph()).addPhysicsSphere(0f, tempTransform, pelletRadius);
		pelletP.setBounciness(0.8f);
		pellet.setPhysicsObject(pelletP);

		translation = new Matrix4f(tageman.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		tageP = (engine.getSceneGraph()).addPhysicsCapsule(mass, tempTransform, tagemanRadius, tagemanHeight);
		tageP.setBounciness(0.8f);
		tageman.setPhysicsObject(tageP);
		((JBulletPhysicsObject) tageP).getRigidBody().setAngularFactor(0f);

		translation = new Matrix4f(blinky.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		blinkyP = (engine.getSceneGraph()).addPhysicsCapsule(mass, tempTransform, ghostRadius, ghostHeight);
		blinkyP.setBounciness(0.8f);
		blinky.setPhysicsObject(blinkyP);
		((JBulletPhysicsObject) blinkyP).getRigidBody().setAngularFactor(0f);

		translation = new Matrix4f(pinky.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		pinkyP = (engine.getSceneGraph()).addPhysicsCapsule(mass, tempTransform, ghostRadius, ghostHeight);
		pinkyP.setBounciness(0.8f);
		pinky.setPhysicsObject(pinkyP);
		((JBulletPhysicsObject) pinkyP).getRigidBody().setAngularFactor(0f);

		translation = new Matrix4f(inky.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		inkyP = (engine.getSceneGraph()).addPhysicsCapsule(mass, tempTransform, ghostRadius, ghostHeight);
		inkyP.setBounciness(0.8f);
		inky.setPhysicsObject(inkyP);
		((JBulletPhysicsObject) inkyP).getRigidBody().setAngularFactor(0f);

		translation = new Matrix4f(clyde.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		clydeP = (engine.getSceneGraph()).addPhysicsCapsule(mass, tempTransform, ghostRadius, ghostHeight);
		clydeP.setBounciness(0.8f);
		clyde.setPhysicsObject(clydeP);
		((JBulletPhysicsObject) clydeP).getRigidBody().setAngularFactor(0f);

		translation = new Matrix4f(terrain.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		terrainP = (engine.getSceneGraph()).addPhysicsStaticPlane(tempTransform, upVector, planeConstant);
		//caps1P.setBounciness(0.8f);
		terrain.setPhysicsObject(terrainP);


		//setupNetworking();

		//engine.disableGraphicsWorldRender();
		engine.enablePhysicsWorldRender();

		// ------------- positioning the camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		String keyboardName = im.getKeyboardName();
		
		Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);
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

		tageS.updateAnimation();

		// update sound
		hereSound.setLocation(tageman.getWorldLocation());
		setEarParameters();

		if (joined) {

			float wallThreshold = 1.0f;
			float radiusOffset = 0.5f;
			float stepSize = 0.05f;
			float moveSpeed = 2f;

			if (isMovingForward || isMovingBackward) {
            	Vector4f moveDirection = isMovingForward ?
                	new Vector4f(0f, 0f, 1f, 0f) :
                	new Vector4f(0f, 0f, -1f, 0f);

				moveDirection.mul(avatar.getWorldRotation());

				Vector3f currPos = avatar.getWorldLocation();
				Vector3f nextPos = new Vector3f(currPos.x() + moveDirection.x() * stepSize, 0, currPos.z() + moveDirection.z() * stepSize);

				float nextHeight = terrain.getHeight(nextPos.x(), nextPos.z());

				if (nextHeight < wallThreshold) {
					Matrix4f newTransform = new Matrix4f().translation(nextPos.x(), nextHeight + radiusOffset, nextPos.z());

					avatar.getPhysicsObject().setTransform(toDoubleArray(newTransform.get(vals)));

					avatar.getPhysicsObject().setLinearVelocity(new float[] { moveDirection.x() * moveSpeed, 0f, moveDirection.z() * moveSpeed });
				} else {
					avatar.getPhysicsObject().setLinearVelocity(new float[] { 0f, 0f, 0f });
				}
				protClient.sendMoveMessage(avatar.getWorldLocation(), 0.0f);
			} else {
				avatar.getPhysicsObject().setLinearVelocity(new float[] { 0f, 0f, 0f });
			}

			

			// Tranlation of movements into physics
			AxisAngle4f aa = new AxisAngle4f();
			Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			Matrix4f mat3 = new Matrix4f().identity();
			checkForCollisions();
			physicsEngine.update((float)elapsedTime);
			for (GameObject go : engine.getSceneGraph().getGameObjects()){
				if (go.getPhysicsObject() != null) { 
					// set translation
					mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
					mat2.set(3,0,mat.m30());
					mat2.set(3,1,mat.m31());
					mat2.set(3,2,mat.m32());

					// 🚨 ADD THIS WALL CHECK:
					float goX = mat.m30();
					float goZ = mat.m32();
					float wallHeight = terrain.getHeight(goX, goZ);

					if (wallHeight >= wallThreshold) {
						float correctedY = wallHeight + radiusOffset;

						mat2.set(3, 1, correctedY); // visually move the mesh
						go.getPhysicsObject().setLinearVelocity(new float[] {0f, 0f, 0f}); // stop motion

						// keep the physics body aligned!
						Matrix4f physicsTransform = new Matrix4f().translation(goX, correctedY, goZ);
						go.getPhysicsObject().setTransform(toDoubleArray(physicsTransform.get(vals)));
					}

					go.setLocalTranslation(mat2);

					// set rotation
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
				joined = true;
				avatar.setLocalTranslation((new Matrix4f()).translation(0, 1, 0));

				im = engine.getInputManager();
				String gpName = im.getFirstGamepadName();
				String keyboardName = im.getKeyboardName();
		
				Camera c = (engine.getRenderSystem().getViewport("MAIN").getCamera());
				orbitController = new CameraOrbitController(c, avatar, gpName, keyboardName, engine);

				ghostS = avatar.getShape();
				ghostT = avatar.getTextureImage();

				characterName = characterNames[selection];
				joinGame(characterName);
				break;
			case KeyEvent.VK_SPACE:		//to start game. pacman starts the game
				if (joined == true && characterName.equals("tageman") && isGameOngoing) {
					startGame();
				}
		}
		super.keyPressed(e);
	}

	private void joinGame(String character) {
		setupNetworking(character);

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
	}

	public void startGame() {
		System.out.println("starting game");
		Vector3f position = new Vector3f(0.0f, 0.0f, 5.0f);
		System.out.println("creating npc ghost");
		protClient.sendCreateNPCmessage(position);
		isGameOngoing = true;
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
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

			boolean tagemanHitPellet = (obj1 == tageP && obj2 == pelletP) || (obj1 == pelletP && obj2 == tageP);

			for (int j = 0; j < manifold.getNumContacts(); j++) {
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f && tagemanHitPellet) {
					System.out.println("Pellet eaten!");
					pellet.getRenderStates().disableRendering();
					(engine.getSceneGraph()).removeGameObject(pellet);
					physicsEngine.removeObject(pellet.getPhysicsObject().getUID());
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
					bounceDirection.normalize(); // unit vector

					// Now apply impulse to both
					javax.vecmath.Vector3f impulseToTageman = new javax.vecmath.Vector3f(bounceDirection);
					impulseToTageman.scale(.5f); // moderate force

					javax.vecmath.Vector3f impulseToBlinky = new javax.vecmath.Vector3f(bounceDirection);
					impulseToBlinky.scale(-.5f); // opposite direction

					((JBulletPhysicsObject) tageP).getRigidBody().applyCentralImpulse(impulseToTageman);
					((JBulletPhysicsObject) blinkyP).getRigidBody().applyCentralImpulse(impulseToBlinky);

					System.out.println("Tageman and Blinky bounced!");
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
			protClient.sendJoinMessage(character);
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public String getCharacterName() { return characterName; }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }

	@Override
	public void shutdown() {
		super.shutdown();
		protClient.sendByeMessage();
	}
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	
				protClient.sendByeMessage();
			}
		}
	}

	public ObjShape getNPCshape() { return npcShape; }
	public TextureImage getNPCtexture() { return npcTex; }
}
