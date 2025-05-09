package myGame;

import tage.*;
import org.joml.*;

public class GhostNPC extends GameObject {
    private int id;
    private double x, y, z;

    public GhostNPC(int id, ObjShape s, TextureImage t, Vector3f p) {
        super(GameObject.root(), s, t);
        Matrix4f initialScale = (new Matrix4f()).scaling(0.5f);
		this.setLocalScale(initialScale);
        this.id = id;
        setPosition(p);
    }
    
    public void setPosition(Vector3f p) {
        x = p.x();
        y = p.y();
        z = p.z();
        setLocalLocation(p);
    }

    public void lookAtTageman(Vector3f tp) {
        this.lookAt(tp);
    }
}
