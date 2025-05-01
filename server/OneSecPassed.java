import tage.ai.behaviortrees.BTCondition;

public class OneSecPassed extends BTCondition {
    NPC npc;
    NPCcontroller npcc;
    GameServerUDP server;

    public OneSecPassed(GameServerUDP s, NPCcontroller c, NPC n, boolean toNegate) {
        super(toNegate);
        server = s;
        npcc = c;
        npc = n;
    }

    protected boolean check() {
        return npcc.getDidOneSecPass();
    }
}