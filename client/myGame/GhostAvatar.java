package myGame;

import java.util.UUID;

import tage.*;
import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject
{
	UUID uuid;
	String name;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p, String n) {
		super(GameObject.root(), s, t);
		uuid = id;
		name = n;
		setPosition(p);
	}
	
	public UUID getID() { return uuid; }
	public void setPosition(Vector3f m) { setLocalLocation(m); }
	public Vector3f getPosition() { return getWorldLocation(); }
	public void setRotation(float m) { yaw(3.0f * m); }
	public String getName() { return name; }
}
