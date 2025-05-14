package myGame;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private ProtocolClient protClient;
    private GameObject avatar;
    private Matrix4f oldRotation, newRotation, rotAroundAvatarUp; 
    private Vector4f oldUp;
    private int direction;
    private boolean isAxisController;

    public TurnAction(MyGame g, ProtocolClient p) {
        game = g;
        protClient = p;
        isAxisController = true;
    }

    public TurnAction(MyGame g, ProtocolClient p, int direction) {
        game = g;
        protClient = p;
        this.direction = direction;
        isAxisController = false;
    }

    @Override
    public void performAction(float time, Event e)
    {
        avatar = game.getAvatar();

        if (isAxisController) {
            float keyValue = e.getValue();
            if (keyValue > -0.2 && keyValue <0.2) return; //deadzone
        
            avatar.yaw(-keyValue);
        } else {
            avatar.yaw(5.0f * direction);
        }
        if (protClient != null) {
            protClient.sendMoveMessage(avatar.getWorldLocation(), 1.66f * direction);
        }
    }
}