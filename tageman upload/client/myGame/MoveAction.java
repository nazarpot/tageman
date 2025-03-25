package myGame;

import tage.*;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import static java.lang.Math.*;

public class MoveAction extends AbstractInputAction
{
    private MyGame game;
    private GameObject avatar;
    private Vector3f oldPosition, newPosition;
    private Vector4f moveDirection;
    private char direction;

    public MoveAction(MyGame g, char direction) {
        game = g;
        this.direction = direction;
        //F for forward
        //B for backward
    }

    @Override
    public void performAction(float time, Event e)
    {
        Vector3f forwardVec, dolLocation;
        avatar = game.getAvatar();
            
        oldPosition = avatar.getWorldLocation();

        switch(direction) {
            case 'F':
                moveDirection = new Vector4f(0f, 0f, 1f, 1f);
                break;
            case 'B':
                moveDirection = new Vector4f(0f, 0f, -1f, 1f);
                break;
        }
        moveDirection.mul(avatar.getWorldRotation());
        moveDirection.mul(0.05f);
        newPosition = oldPosition.add(moveDirection.x(), moveDirection.y(), moveDirection.z());
        avatar.setLocalLocation(newPosition);
    }
}