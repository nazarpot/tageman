import org.joml.*;

public class NPC {
    double locationX, locationY, locationZ;
    double dirX = 0.1;
    double dirZ = 0.1;
    Vector3f target;
    double size = 1.0;

    public NPC() {
        locationX = 0.0;
        locationY = 1.0;
        locationZ = 0.0;
        target = new Vector3f(0.0f, 0.0f, 0.0f);
    }

    public void randomizeLocation(int seedX, int seedZ) {
        locationX = ((double) seedX)/4.0-5.0;
        locationY = 1.0;
        locationZ = -2;
    }

    public double getX() {return locationX;}
    public double getY() {return locationY;}
    public double getZ() {return locationZ;}

    public void setTarget(Vector3f t) {target = t;}

    public void updateLocation() {
        if (locationX > target.x()) {
            dirX = -0.1;
        } else if (locationX < target.x()) {
            dirX = 0.1;
        }

        if (locationZ > target.z()) {
            dirZ = -0.1;
        } else if (locationZ < target.z()) {
            dirZ = 0.1;
        }

        locationX += dirX;
        locationZ += dirZ;
    }
}