package myGame;

import tage.*;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.input.*;
import tage.input.action.*;

import java.lang.Math;

public class CameraOrbitController {
    private Engine engine;
    private Camera camera;
    private GameObject avatar;
    private float cameraAzimuth;
    private float cameraElevation;
    private float cameraRadius;

    public CameraOrbitController(Camera cam, GameObject av, String gpName, String kbName, Engine e) {
        engine = e;
        camera = cam;
        avatar = av;
        cameraAzimuth = 0.0f;
        cameraElevation = 10.0f;
        cameraRadius = 2.75f;
        setupInputs(gpName, kbName);
        updateCameraPosition();
    }

    private void setupInputs(String gp, String kb) {
        OrbitAzimuthAction azmAction = new OrbitAzimuthAction();
        OrbitAzimuthAction azmLeftAction = new OrbitAzimuthAction(-1);  //left arrow key
        OrbitAzimuthAction azmRightAction = new OrbitAzimuthAction(1);  //right arrow key
        OrbitAzimuthAction azmCenterAction = new OrbitAzimuthAction('C');

        OrbitElevationAction elevationAction = new OrbitElevationAction();
        OrbitElevationAction elevationUpAction = new OrbitElevationAction(1);   //up arrow key
        OrbitElevationAction elevationDownAction = new OrbitElevationAction(-1);//down arrow key


        InputManager im = engine.getInputManager();
        if (gp != null) {
            im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.RX, 
                                                azmAction, 
                                                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN); 
            im.associateAction(gp, net.java.games.input.Component.Identifier.Axis.RY, 
                                                elevationAction, 
                                                InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        }

        im.associateAction(kb, net.java.games.input.Component.Identifier.Key.LEFT,
                                            azmLeftAction,
                                            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(kb, net.java.games.input.Component.Identifier.Key.RIGHT,
                                            azmRightAction,
                                            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(kb, net.java.games.input.Component.Identifier.Key.C,
                                            azmCenterAction,
                                            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

        im.associateAction(kb, net.java.games.input.Component.Identifier.Key.UP,
                                            elevationUpAction,
                                            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateAction(kb, net.java.games.input.Component.Identifier.Key.DOWN,
                                            elevationDownAction,
                                            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        

    }

    public void updateCameraPosition() {

        Vector3f avatarRot = avatar.getWorldForwardVector();
        double avatarAngle = Math.toDegrees((double) 
                                                avatarRot.angleSigned(new Vector3f(0, 0, -1), 
                                                                        new Vector3f(0, 1, 0)));
        float totalAz = cameraAzimuth - (float) avatarAngle;
        double theta = Math.toRadians(totalAz);
        double phi = Math.toRadians(cameraElevation);
        float x = cameraRadius * (float)(Math.cos(phi) * Math.sin(theta));
        float y = cameraRadius * (float)(Math.sin(phi));
        float z = cameraRadius * (float)(Math.cos(phi) * Math.cos(theta));
        camera.setLocation(new Vector3f(x, y, z).add(avatar.getWorldLocation()));
        camera.lookAt(avatar);
        /*System.out.println("updating: \n" + 
                            "azimuth: " + cameraAzimuth + "\n" + 
                            "elevation: " + cameraElevation + "\n" + 
                            "radius: " + cameraRadius);*/
        cameraAzimuth = 0;
        cameraElevation = 10;
    }

    private class OrbitAzimuthAction extends AbstractInputAction {
        private boolean isController;
        private int direction;
        private char action;

        public OrbitAzimuthAction() {
            isController = true;
            action = 'D';
        }

        public OrbitAzimuthAction(int d) {
            isController = false;
            direction = d;
            action = 'D';
        }

        public OrbitAzimuthAction(char c) {
            action = c;
        }


        public void performAction(float time, Event event) {
            float rotAmount;
            switch(action) {
                case 'C':
                    cameraAzimuth = 0.0f;
                    break;
                default:
                    if (isController) {
                        cameraAzimuth += -90 * event.getValue();
                    } else {
                        cameraAzimuth = -90 * direction;
                    }     
            }

            cameraAzimuth = cameraAzimuth % 360;
            updateCameraPosition();
        }
    }

    private class OrbitRadiusAction extends AbstractInputAction {
        private boolean isController;
        private int direction;

        public OrbitRadiusAction() {
            isController = true;
        }

        public OrbitRadiusAction(int d) {
            isController = false;
            direction = d;
        }


        public void performAction(float time, Event event) {
            float radiusChange;

            if (isController) {
                if (event.getValue() < -0.2) {radiusChange = -0.5f;}
            else if (event.getValue() > 0.2) {radiusChange = 0.5f;}
            else {radiusChange = 0.0f;}
            } else {
                radiusChange = 0.5f * direction;
            }
            
            cameraRadius += radiusChange;
            if (cameraRadius < 1) {
                cameraRadius = 1.0f;
            }
        }
    }

    private class OrbitElevationAction extends AbstractInputAction {
        private boolean isController;
        private int direction;

        public OrbitElevationAction() {
            isController = true;
        }

        public OrbitElevationAction(int d) {
            isController = false;
            direction = d;
        }

        public void performAction(float time, Event event) {
            float elevChange;

            if (isController) {
                if (event.getValue() < -0.2) {elevChange = -0.5f;}
                else if (event.getValue() > 0.2) {elevChange = 0.5f;}
                else {elevChange = 0.0f;}
            } else {
                elevChange = 155;
            }
            
            cameraElevation += elevChange;
            if (cameraElevation < 0) {
                cameraElevation = 0.0f;
            }
            if (cameraElevation > 180) {
                cameraElevation = 180.0f;
            }
        }
    }
}