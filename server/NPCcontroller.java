import java.util.Random;
import tage.ai.behaviortrees.*;
import org.joml.*;

public class NPCcontroller {
    private NPC npc;
    Random rn = new Random();
    BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
    long thinkStartTime, tickStartTime;
    long lastThinkUpdateTime, lastTickUpdateTime;
    GameServerUDP server;
    double criteria;

    private boolean didOneSecPass;

    public void updateNPCs() {
        npc.updateLocation();
    }

    public void start(GameServerUDP s) {
        thinkStartTime = System.nanoTime();
        tickStartTime = System.nanoTime();
        lastThinkUpdateTime = thinkStartTime;
        lastTickUpdateTime = tickStartTime;
        server = s;
        didOneSecPass = false;
        setupNPCs();
        setupBehaviorTree();
        npcLoop();
    }

    public void setupNPCs() {
        npc = new NPC();
        npc.randomizeLocation(rn.nextInt(40), rn.nextInt(40));
    }

    public void npcLoop() {
        while (true) {
            long currentTime = System.nanoTime();
            float elapsedThinkMilliSecs = (currentTime - lastThinkUpdateTime)/(1000000.0f);
            float elapsedTickMilliSecs = (currentTime - lastTickUpdateTime)/(1000000.0f);

            if (elapsedTickMilliSecs >= 25.0f) {
                lastTickUpdateTime = currentTime;
                npc.updateLocation();
                server.sendNPCinfo();
            }

            if (elapsedThinkMilliSecs >= 250.0f) {
                lastThinkUpdateTime = currentTime;
                didOneSecPass = true;
                bt.update(elapsedThinkMilliSecs);
            } else {
                didOneSecPass = false;
            }

            Thread.yield();
        }
    }

    public void setupBehaviorTree() {
        bt.insertAtRoot(new BTSequence(10));
        bt.insert(10, new OneSecPassed(server, this, npc, false));
        bt.insert(10, new ChaseTageman(server, npc));
    }

    public NPC getNPC() { return npc; }
    public boolean getDidOneSecPass() { return true; }

    public void setTarget(String[] p) {
        Vector3f position = new Vector3f(
            Float.parseFloat(p[0]),
            Float.parseFloat(p[1]),
            Float.parseFloat(p[2])
        );
        npc.setTarget(position);
    }
}