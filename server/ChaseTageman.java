import tage.ai.behaviortrees.*;

public class ChaseTageman extends BTAction {
    GameServerUDP server;
    NPC npc;

    public ChaseTageman(GameServerUDP s, NPC n) {
        server = s;
        npc = n;
    }

    @Override
    public BTStatus update(float elapsedTime) {
        server.requestTarget();
        server.lookAtTarget();
        return getStatus();
    }
}