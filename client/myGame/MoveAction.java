package myGame;

import tage.*;

import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
//import static java.lang.Math.*;

public class MoveAction extends AbstractInputAction
{
    private MyGame game;
    private ProtocolClient protClient;
    private GameObject avatar;
    private Vector3f oldPosition, newPosition;
    private Vector4f moveDirection;
    private char direction;

    public MoveAction(MyGame g, ProtocolClient p, char direction) {
        game = g;
        protClient = p;
        avatar = game.getAvatar();
        this.direction = direction;
        //F for forward
        //B for backward
    }

    @Override
    public void performAction(float time, Event e) {

        float keyVal = e.getValue();
        System.out.println("W Key state: " + keyVal);
        switch (direction) {
            case 'F':
                if (keyVal == 1.0f) {
                    game.setMovingForward(true);
                    if (game.getCharacterName().equals("tageman")) {
                        game.setTagemanChomp(true);
                        if (protClient != null) {
                           protClient.sendChompMessage(true); 
                        }   
                    }
                    
                } else {
                    game.setMovingForward(false);
                    if (game.getCharacterName().equals("tageman")) {
                        game.setTagemanChomp(false);    
                        if (protClient != null) {
                            protClient.sendChompMessage(false);
                        }
                    }
                }
                break;
            case 'B':
                if (keyVal == 1.0f) {
                    game.setMovingBackward(true);
                    //game.setTagemanChomp(true);
                } else {
                    game.setMovingBackward(false);
                    //game.setTagemanChomp(false);
                }
                break;
        }
    }
}