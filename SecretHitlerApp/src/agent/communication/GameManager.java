package agent.communication;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import app.Game;
import app.PlayerType;
import app.Policy;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class GameManager extends Agent {

    public static final String ENGLISH = "English";
    public static final String SECRET_HITLER_ONTOLOGY = "Secret-Hitler-ontology";
    private static final String SECRET_HITLER_GAMERS = "SecretHitlerGamers";
    private static final String SECRET_HITLER_MANAGER = "SecretHitlerManager";

    private final List<AID> secretHitlerPlayers = new ArrayList<>();
    private Game currentGame;
    private AID currentPresident;
    private AID currentChancellor;
    private AID hitler;
    private int currentPresidentPointer = -1;

    @Override
    protected void setup() {

        // Creates the agent description and service to collect all gamers
        DFAgentDescription dfAgentDescription = new DFAgentDescription();
        dfAgentDescription.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(SECRET_HITLER_MANAGER);
        serviceDescription.setName(getLocalName() + "-" + SECRET_HITLER_MANAGER);
        dfAgentDescription.addServices(serviceDescription);

        // Register the service
        try {
            DFService.register(this, dfAgentDescription);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Updates the list of secret hitler gamers
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SECRET_HITLER_GAMERS);
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            secretHitlerPlayers.clear();
            for (DFAgentDescription agentDescription : result) {
                secretHitlerPlayers.add(agentDescription.getName());
            }

        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Starts the game
        try {
            startGame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends object to receiver with a certain type and object
     *
     * @param type     - Type of the action.
     * @param object   - Object of the action.
     * @param receiver - Receiver of the message.
     * @throws IOException Exception setting object content
     */
    private void sendObject(int type, Serializable object, AID receiver) throws IOException {
        ACLMessage voteInformation = new ACLMessage(type);
        voteInformation.setLanguage(ENGLISH);
        voteInformation.setOntology(SECRET_HITLER_ONTOLOGY);
        voteInformation.setContentObject(object);
        voteInformation.addReceiver(receiver);
        send(voteInformation);
    }

    /**
     * Sends the given object with the given performative to all players
     *
     * @param performative Performative of the message
     * @param data         Data to send to all
     */
    private void sendObjectToAll(int performative, Serializable data) throws IOException {
        for (AID player :
                secretHitlerPlayers) {
            sendObject(performative, data, player);
        }
    }

    /**
     * Receives object from the communication
     *
     * @param msgType - Type of message to receive.
     * @param sender  - AID of the sender or null if from anyone
     * @return The object associated with the message.
     * @throws UnreadableException Exception setting object content
     */
    private Object receiveObjectFrom(int msgType, AID sender) throws UnreadableException, InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
        MessageTemplate messageTemplate;
        if (sender != null) {
            messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(msgType), MessageTemplate.MatchSender(sender));
        } else {
            messageTemplate = MessageTemplate.MatchPerformative(msgType);
        }

        // Consumes to much processing -> Try to use better alternative
        while (true) {
            ACLMessage message = receive(messageTemplate);
            if (message != null)
                return message.getContentObject();
        }
    }

    /**
     * Receives approval from all
     *
     * @return Returns a Map<PlayerAID, Choice (true | false)>
     */
    private Map<AID, Boolean> receiveApprovalFromAll() throws UnreadableException, InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
        Map<AID, Boolean> votes = new HashMap<>();
        int nResponses = 0;
        MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessageSecretHitler.VOTE);

        while (nResponses < secretHitlerPlayers.size()) {
            ACLMessage message = receive(messageTemplate);
            if (message != null) {
                votes.put(message.getSender(), (Boolean) message.getContentObject());
                nResponses++;
            }
        }

        return votes;
    }

    /**
     * Receives votes from all players
     *
     * @param messageType Type of message it will receive
     * @return A Map <PlayerAID, ChosenPlayerAID>
     * @throws UnreadableException Exception setting object content
     */
    private Map<AID, AID> receiveVotesFromAll(int messageType) throws UnreadableException, InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
        Map<AID, AID> votes = new HashMap<>();
        int nResponses = 0;
        MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(messageType);

        while (nResponses < secretHitlerPlayers.size()) {
            ACLMessage message = receive(messageTemplate);
            if (message != null) {
                votes.put(message.getSender(), (AID) message.getContentObject());
                nResponses++;
            }
        }
        return votes;
    }

    /**
     * Ends the current game
     */
    private void endGame() {
        System.out.println("Game Manager: The game is about to end");
        doDelete();
    }

    /**
     * Indicates to the game manager to start the game
     * Attributes the first president and starts
     */
    public void startGame() throws IOException {
        currentGame = new Game();
        System.out.println("Game Manager: The game is about to start");

        // Generates list of fascists
        List<AID> fascists = currentGame.generateFascists(secretHitlerPlayers);
        hitler = fascists.get(0);

        for (AID shp : secretHitlerPlayers) {
            sendObject(ACLMessageSecretHitler.ROLE, (Serializable) secretHitlerPlayers, shp);
            if (fascists.contains(shp)) {
                sendObject(ACLMessageSecretHitler.ROLE, (Serializable) fascists, shp);
            } else {
                sendObject(ACLMessageSecretHitler.ROLE, PlayerType.LIBERAL, shp);
            }
        }
        startsBehaviour();
    }

    /**
     * Starts the normal behaviour
     */
    private void startsBehaviour() {
        // Adds a simple behaviour that repeats until game finishes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                try {
                    // Gets the next president
                    currentPresidentPointer = (currentPresidentPointer + 1) % secretHitlerPlayers.size();
                    currentPresident = secretHitlerPlayers.get(currentPresidentPointer);

                    // Informs about game stats
                    System.out.println("\n                     FascistTracker: " + currentGame.getNumberOfFascistCards() +
                            "\n                     LiberalTracker: " + currentGame.getNumberOfLiberalCards() +
                            "\n                     Deck size: " + currentGame.deckSize() +
                            "\n                     Discard pile: " + currentGame.discardSize()+"\n");

                    // Informs current president
                    sendObjectToAll(ACLMessageSecretHitler.PRESIDENT_CANDIDATE, currentPresident);
                    System.out.println("Game Manager: The PRESIDENT is: " + currentPresident.getLocalName());

                    // Receive players chancellor votes
                    Map<AID, AID> choices = receiveVotesFromAll(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE);
                    //System.out.println("Game Manager: Process Chancellor Votes");
                    printMap(choices);

                    // Informs all players from other players choices
                    sendObjectToAll(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE, (Serializable) choices);
                    //System.out.println("Game Manager: Informing other players choices");

                    // Receives chancellor nomination from president
                    currentChancellor = (AID) receiveObjectFrom(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE, currentPresident);
                    System.out.println("Game Manager: President nominates CHANCELLOR: " + currentChancellor.getLocalName());

                    // Informs about new chancellor
                    sendObjectToAll(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE, currentChancellor);
                    //System.out.println("Game Manager: Informing new chancellor");

                    // Receives the players votes for government approval
                    Map<AID, Boolean> governmentVotes = receiveApprovalFromAll();
                    int numberOfApprovals = Collections.frequency(governmentVotes.values(), true);
                    int numberOfDisapproval = Collections.frequency(governmentVotes.values(), false);
                    //System.out.println("Game Manager: Received government approval votes");
                    printMap(governmentVotes);

                    // Informs all players about others votes to government approval
                    sendObjectToAll(ACLMessageSecretHitler.VOTE, (Serializable) governmentVotes);
                    //System.out.println("Game Manager: Informs government votes");

                    // If government is approved then pass to policies
                    if (numberOfApprovals > numberOfDisapproval) {
                        governmentApproved();

                    } else {
                        currentGame.setFailTracker(currentGame.getFailTracker() + 1);

                        // If 3 governments failed sequentially implement next policy
                        if (currentGame.getFailTracker() == 3) {
                            currentGame.setFailTracker(0);
                            Policy nextPolicy = currentGame.getNextPolicy();

                            // Broadcasts the final policy to all players
                            sendObjectToAll(ACLMessageSecretHitler.FINAL_POLICY, nextPolicy);
                            System.out.println("3rd failed government. Enact NEW POLICY: " + nextPolicy);
                            currentGame.newPolicy(nextPolicy);
                            if (currentGame.isDone()) {
                                if (currentGame.getNumberOfFascistCards() >= 6) {
                                    System.out.println("Game Manager: Game Ends => Number of Fascist Cards >= 6");
                                } else {
                                    System.out.println("Game Manager: Game Ends => Number of Liberal Cards >= 5");
                                }
                                endGame();
                                return;
                            }

                        } else {

                            // Informs about turn end, and that no policy was enacted
                            sendObjectToAll(ACLMessageSecretHitler.FINAL_POLICY, "");
                            System.out.println("Game Manager: Informs all no policy was enacted.");
                        }
                        System.out.println("Game Manager: Government was not approved");
                    }
                    System.out.println("Game Manager: Turn Ends\n");

                } catch (IOException | UnreadableException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Deals with the government approval
             * @throws IOException Exception of input
             * @throws UnreadableException Problems reading objects
             * @throws InterruptedException Problem with sleeps
             */
            private void governmentApproved() throws IOException, UnreadableException, InterruptedException {
                System.out.println("Game Manager: Government was approved");

                // Checks if the game ends
                if (currentGame.getNumberOfFascistCards() >= 3 && hitler.equals(currentChancellor)) {
                    sendObjectToAll(ACLMessageSecretHitler.END_GAME, true);
                    System.out.println("Game Manager: End Condition => FascistTracker >= 3 && Hitler is chancellor");
                    endGame();
                    return;
                }

                // Informs that the game hasn't end yet
                sendObjectToAll(ACLMessageSecretHitler.END_GAME, false);

                // Sets the fail tracker to 0
                currentGame.setFailTracker(0);

                // Obtains the next policies to send
                List<Policy> next3Policies = currentGame.getNext3Policies();
                System.out.println("Next three policies: " + next3Policies);

                // Passes next 3 policies to the president
                sendObject(ACLMessageSecretHitler.CHOOSE_POLICY, (Serializable) next3Policies, currentPresident);
                //System.out.println("Game Manager: Sends next 3 policies to president");

                // Receives cards from president
                List<Policy> presidentPolicies = (List<Policy>) receiveObjectFrom(ACLMessageSecretHitler.PRESIDENT_CARDS, currentPresident);
                System.out.println("Game Manager: Receives president card claims: " + presidentPolicies);

                // Sends president cards
                sendObjectToAll(ACLMessageSecretHitler.PRESIDENT_CARDS, (Serializable) presidentPolicies);
                //System.out.println("Game Manager: Informing about president card claims");

                // Receives final policy from chancellor
                Policy finalPolicy =
                        ((List<Policy>) receiveObjectFrom(ACLMessageSecretHitler.FINAL_POLICY, currentChancellor)).get(0);
                next3Policies.remove(finalPolicy);
                currentGame.discard(next3Policies);
                currentGame.newPolicy(finalPolicy);
                System.out.println("Game Manager: Receives final policy: " + finalPolicy);

                // Receives cards from chancellor
                List<Policy> chancellorPolicies = (List<Policy>) receiveObjectFrom(ACLMessageSecretHitler.CHANCELLOR_CARDS, currentChancellor);
                System.out.println("Game Manager: Receives Chancellor card claims: " + chancellorPolicies);

                // Broadcasts the final policy to all players
                sendObjectToAll(ACLMessageSecretHitler.FINAL_POLICY, finalPolicy);
                //System.out.println("Game Manager: Informs all about final policy");

                // Checks if the game ends
                if (currentGame.isDone()) {
                    if (currentGame.getNumberOfFascistCards() >= 6) {
                        System.out.println("Game Manager: Game Ends => Number of Fascist Cards >= 6");
                    } else {
                        System.out.println("Game Manager: Game Ends => Number of Liberal Cards >= 5");
                    }
                    endGame();
                    return;
                }

                // Sends chancellor cards
                sendObjectToAll(ACLMessageSecretHitler.CHANCELLOR_CARDS, (Serializable) chancellorPolicies);
                //System.out.println("Game Manager: Informing about chancellor card claims");

                // Receives message for next step
                int nextStep = (int) receiveObjectFrom(ACLMessageSecretHitler.INFORM, currentPresident);

                if (nextStep == ACLMessageSecretHitler.EXECUTE) {
                    // Execute someone
                    executePower();
                } else if (nextStep == ACLMessageSecretHitler.PEEK) {
                    // Peek next cards
                    peekPower();

                } else if (nextStep == ACLMessageSecretHitler.INVESTIGATE) {
                    // Investigate other player
                    investigatePower();
                }
            }

            /**
             * Adds investigate power behaviour
             * @throws IOException Exception of input
             * @throws UnreadableException Problems reading objects
             * @throws InterruptedException Problem with sleeps
             */
            private void investigatePower() throws UnreadableException, IOException, InterruptedException {
                System.out.println("Game Manager: President will investigate");

                // Receive players investigation choices
                Map<AID, AID> investigationChoices = receiveVotesFromAll(ACLMessageSecretHitler.INVESTIGATE);
                //System.out.println("Game Manager: Receives investigation choices");
                printMap(investigationChoices);

                // Inform players of others choices
                sendObjectToAll(ACLMessageSecretHitler.INVESTIGATE, (Serializable) investigationChoices);
                //System.out.println("Game Manager: Informing about investigation choices");

                // Receives investigated player status from president
                String investigatedPlayer = (String) receiveObjectFrom(ACLMessageSecretHitler.INVESTIGATE, currentPresident);
                System.out.println("Game Manager: Receives investigation status from the president: " + investigatedPlayer);

                // Informs all other players about the investigated player
                sendObjectToAll(ACLMessageSecretHitler.INVESTIGATE, investigatedPlayer);
                //System.out.println("Game Manager: Informing investigated player");
            }

            /**
             * Adds peek power behaviour
             * @throws IOException Exception of input
             * @throws UnreadableException Problems reading objects
             * @throws InterruptedException Problem with sleeps
             */
            private void peekPower() throws IOException, UnreadableException, InterruptedException {
                System.out.println("Game Manager: President will peek");

                // Manager obtains next 3 cards for the peek action
                List<Policy> next3 = currentGame.peekNext3Policies();

                // Informs the president about next 3 cards
                sendObject(ACLMessageSecretHitler.PEEK, (Serializable) next3, currentPresident);
                System.out.println("Game Manager: Informing current president about the next 3 policies in the deck: " + next3);

                // Receives policies observed by president
                List<Policy> observedPolicies = (List<Policy>) receiveObjectFrom(ACLMessageSecretHitler.PEEK, currentPresident);
                System.out.println("Game Manager: Receives policies observed by the president: " + observedPolicies);

                // Informs the cards the president claims it has observed
                sendObjectToAll(ACLMessageSecretHitler.PEEK, (Serializable) observedPolicies);
                //System.out.println("Game Manager: Informing about what president observed");
            }

            /**
             * Add execute power behaviour
             * @throws IOException Exception of input
             * @throws UnreadableException Problems reading objects
             * @throws InterruptedException Problem with sleeps
             */
            private void executePower() throws UnreadableException, IOException, InterruptedException {
                System.out.println("Game Manager: President will execute someone");

                // Receive players execution choices
                Map<AID, AID> executionChoices = receiveVotesFromAll(ACLMessageSecretHitler.EXECUTE);
                //System.out.println("Game Manager: Receives execution choices");
                printMap(executionChoices);

                // Inform players of others choices
                sendObjectToAll(ACLMessageSecretHitler.EXECUTE, (Serializable) executionChoices);
                //System.out.println("Game Manager: Informing about execution choices");

                // Receives message of executed player
                AID deadPlayer = (AID) receiveObjectFrom(ACLMessageSecretHitler.DEAD, null);
                secretHitlerPlayers.remove(deadPlayer);

                // Informs all other players about the killed player
                sendObjectToAll(ACLMessageSecretHitler.DEAD, deadPlayer);
                System.out.println("Game Manager: Informing about executed player: "+deadPlayer.getLocalName());

                // Checks if the killed player is hitler
                boolean hitlerKilled = deadPlayer.equals(hitler);

                // Informs all other players about the killed player
                sendObjectToAll(ACLMessageSecretHitler.DEAD, hitlerKilled);
                System.out.println("Game Manager: Killed player is Hitler: " + hitlerKilled);

                // If hitler dies the game ends otherwise ends turn
                if (hitlerKilled) {
                    System.out.println("Game Manager: Game Ends => Hitler killed");
                    endGame();
                }
            }
        });
    }

    /**
     * Prints map to the user
     *
     * @param map Map to print
     */
    private void printMap(Map<AID, ?> map) {
        StringBuilder sb = new StringBuilder(getAID().getLocalName() + ":\n");
        for (Entry<AID, ?> pair : map.entrySet()) {
            if (pair.getValue() instanceof AID) {
                AID id = (AID) pair.getValue();
                sb.append(pair.getKey().getLocalName() + ":" + id.getLocalName());
            } else {
                sb.append(pair.getKey().getLocalName() + ":" + pair.getValue());
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
}
