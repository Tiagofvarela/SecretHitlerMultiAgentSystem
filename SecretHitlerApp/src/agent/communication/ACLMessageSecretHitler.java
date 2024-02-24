package agent.communication;

import jade.lang.acl.ACLMessage;

@SuppressWarnings({"deprecation"})
public class ACLMessageSecretHitler extends ACLMessage {

    // ACLMessage performatives go from -1 to 19;
    public static final int START = 20;
    public static final int ROLE = 21;
    public static final int PRESIDENT_CANDIDATE = 22;
    public static final int CHANCELLOR_CANDIDATE = 23;
    public static final int VOTE = 24;
    public static final int CHOOSE_POLICY = 25;
    public static final int FINAL_POLICY = 26;
    public static final int CHANCELLOR_CARDS = 27;
    public static final int PRESIDENT_CARDS = 28;
    public static final int POWER = 29;
    public static final int PEEK = 30;
    public static final int EXECUTE = 31;
    public static final int INVESTIGATE = 32;
    public static final int DEAD = 33;
    public static final int END_TURN = 34;
    public static final int END_GAME = 35;
}

