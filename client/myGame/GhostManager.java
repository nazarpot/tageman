package myGame;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, String name, Vector3f position) throws IOException
	{	System.out.println("adding ghost with ID --> " + id);
		ObjShape s = game.getGhostShape(name);
		TextureImage t = game.getGhostTexture(name);
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position, name);
		Matrix4f initialScale = (new Matrix4f()).scaling(0.5f);
		newAvatar.setLocalScale(initialScale);
		ghostAvatars.add(newAvatar);
		//game.initializeAvatarPhysics(newAvatar, 10f);
		System.out.println("added ghost");
	}
	
	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	public GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{	return ghostAvatar;
			}
		}		
		return null;
	}
	
	public void updateGhostAvatar(UUID id, Vector3f position, float rotateBy)
	{	GhostAvatar ghostAvatar = findAvatar(id);

		if (ghostAvatar != null)
		{	
			ghostAvatar.setPosition(position);
			ghostAvatar.setRotation(rotateBy);
		}
		else
		{	System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
		
	}

	public int getGhostsNum() { return ghostAvatars.size(); }

	public GameObject getAvatar(int i) { return ghostAvatars.elementAt(i); }
}
