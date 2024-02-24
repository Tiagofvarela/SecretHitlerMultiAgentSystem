package agent.communication;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import agent.behaviour.DefaultLiberalBehaviour;
import agent.behaviour.FascistBehaviour;
import agent.behaviour.PlayerBehaviour;
import agent.behaviour.fascists.AggressiveFascist;
import agent.behaviour.fascists.ShyFascist;
import agent.behaviour.humans.ManualFascistBehaviour;
import agent.behaviour.humans.ManualLiberalBehaviour;
import agent.behaviour.liberals.AggressiveLiberal;
import agent.behaviour.liberals.ShyLiberal;
import app.PlayerPersonality;
import app.PlayerType;
import app.Policy;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLCodec.CodecException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.StringACLCodec;
import jade.lang.acl.UnreadableException;

public class PlayerAgent extends Agent {

    public static final String ENGLISH = "English";
    public static final String SECRET_HITLER_ONTOLOGY = "Secret-Hitler-ontology";
    private static final String SECRET_HITLER_GAMERS = "SecretHitlerGamers";

    private PlayerType role;
    private PlayerPersonality playerPersonality;
    private PlayerBehaviour playerBehaviour;
    private AID gameManager;

    @Override
    protected void setup() {

        // Obtains player personality to create respective behaviour
        Object[] args = getArguments();

        try {
            playerPersonality = PlayerPersonality.valueOf(((String) args[0]).toUpperCase());
        } catch (IllegalArgumentException e) {
            playerPersonality = PlayerPersonality.MODERATE;
        }
        boolean verbose = false;
        if(args.length > 1) {
        	try {
	            verbose = ((String) args[1]).equals("true");
	            //System.out.println(getLocalName()+": I got a boolean. It's "+verbose+" which I got from "+args[1]);
	        } catch (IllegalArgumentException e) {
	        }
        }        

        // Creates the agent description and service to collect all gamers
        DFAgentDescription dfAgentDescription = new DFAgentDescription();
        dfAgentDescription.setName(getAID());
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType(SECRET_HITLER_GAMERS);
        serviceDescription.setName(getLocalName() + "-" + SECRET_HITLER_GAMERS);
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

        // Before the game starts receives the role
        try {
            receiveRole(verbose);
        } catch (UnreadableException | InterruptedException e) {
            e.printStackTrace();
        }

        // Adds a simple behaviour that repeats until game finishes
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {

                try {
                    // Receives the president candidate
                    playerBehaviour.updatePresident((AID) receiveObject(ACLMessageSecretHitler.PRESIDENT_CANDIDATE));

                    // Vote for the chancellor
                    sendObject(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE,
                            playerBehaviour.selectPerson(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE), gameManager);

                    // Receives everyone votes for chancellor
                    Map<AID, AID> receivedVotes = (Map<AID, AID>) receiveObject(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE);
                    playerBehaviour.process(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE, receivedVotes);

                    // If president choose chancellor
                    if (playerBehaviour.isPresident()) {
                        sendObject(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE,
                                playerBehaviour.presidentChoose(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE, receivedVotes),
                                gameManager);
                    }

                    // Receives current chancellor
                    playerBehaviour.updateChancellor((AID) receiveObject(ACLMessageSecretHitler.CHANCELLOR_CANDIDATE));

                    // Votes for the government
                    sendObject(ACLMessageSecretHitler.VOTE, playerBehaviour.vote(), gameManager);

                    // Obtains votes from all
                    Map<AID, Boolean> governmentVotes = (Map<AID, Boolean>) receiveObject(ACLMessageSecretHitler.VOTE);
                    playerBehaviour.processGovernmentVotes(governmentVotes);
                    int numberOfApprovals = Collections.frequency(governmentVotes.values(), true);
                    int numberOfDisapproval = Collections.frequency(governmentVotes.values(), false);

                    // If government is approved
                    if (numberOfApprovals > numberOfDisapproval) {
                        // Checks if the game ends
                        if ((Boolean) receiveObject(ACLMessageSecretHitler.END_GAME)) {
                            endGame();
                            return;
                        }
                        governmentApproved(governmentVotes);

                    } else {

                        // If 3 governments failed in a row the policy on top is enacted
                        Object o = receiveObject(ACLMessageSecretHitler.FINAL_POLICY);
                        if (o instanceof Policy) {
                            playerBehaviour.processNewPolicy((Policy) o, new HashMap<>());
                            System.out.println("New policy received due to 3 failed governments");
                            // Checks if game ends
                            if (playerBehaviour.isGameEnd()) {
                                endGame();
                            }
                        }else {
                        	playerBehaviour.failGovernment();
                        }
                    }

                } catch (UnreadableException | IOException | CodecException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Deals with the government approval
             * @param governmentVotes Votes of the government
             * @throws UnreadableException Problem reading content object
             * @throws IOException Problem receiving data
             * @throws CodecException Problem converting to AID
             * @throws InterruptedException Problem with time
             */
            private void governmentApproved(Map<AID, Boolean> governmentVotes)
                    throws UnreadableException, IOException, CodecException, InterruptedException {

                // If president -> receives policies from manager and passes 2 to chancellor
                if (playerBehaviour.isPresident()) {
                    List<Policy> receivedPolicies = (List<Policy>) receiveObject(ACLMessageSecretHitler.CHOOSE_POLICY);
                    sendObject(ACLMessageSecretHitler.CHOOSE_POLICY,
                            (Serializable) playerBehaviour.choosePolicies(receivedPolicies),
                            playerBehaviour.getCurrentChancellor());
                    sendObject(ACLMessageSecretHitler.PRESIDENT_CARDS,
                            (Serializable) playerBehaviour.explainCards(ACLMessageSecretHitler.PRESIDENT_CARDS, receivedPolicies),
                            gameManager);
                }

                // If chancellor -> receives policies from president and informs manager from chosen policy
                if (playerBehaviour.isChancellor()) {
                    List<Policy> receivedPolicies = (List<Policy>) receiveObject(ACLMessageSecretHitler.CHOOSE_POLICY);
                    sendObject(ACLMessageSecretHitler.FINAL_POLICY,
                            (Serializable) playerBehaviour.choosePolicies(receivedPolicies),
                            gameManager);
                    sendObject(ACLMessageSecretHitler.CHANCELLOR_CARDS,
                            (Serializable) playerBehaviour.explainCards(ACLMessageSecretHitler.CHANCELLOR_CARDS, receivedPolicies),
                            gameManager);
                }

                // Receives final policy
                Policy newPolicy = (Policy) receiveObject(ACLMessageSecretHitler.FINAL_POLICY);
                playerBehaviour.processNewPolicy(newPolicy, governmentVotes);

                // Checks if game ends
                if (playerBehaviour.isGameEnd()) {
                    endGame();
                    return;
                }

                // Process policies
                playerBehaviour.processPolicyJustification(newPolicy,
                        (List<Policy>) receiveObject(ACLMessageSecretHitler.CHANCELLOR_CARDS),
                        (List<Policy>) receiveObject(ACLMessageSecretHitler.PRESIDENT_CARDS));

                // If the new policy is fascist then check if a power is activated
                if (newPolicy == Policy.Fascist) {

                    int nextStep = playerBehaviour.checkTracker();
                    if (playerBehaviour.isPresident()) {
                        // Informs next step
                        sendObject(ACLMessageSecretHitler.INFORM, nextStep, gameManager);
                    }

                    switch (nextStep) {
                        case ACLMessageSecretHitler.EXECUTE:
                            executePower();
                            break;
                        case ACLMessageSecretHitler.PEEK:
                            peekPower();
                            break;
                        case ACLMessageSecretHitler.INVESTIGATE:
                            investigatePower();
                            break;
                    }

                } else if (playerBehaviour.isPresident()) {
                    //  If no power then ends
                    sendObject(ACLMessageSecretHitler.INFORM, ACLMessageSecretHitler.END_TURN, gameManager);
                }
            }

            /**
             * Add execute power behaviour
             * @throws UnreadableException Problem reading content object
             * @throws IOException Problem receiving data
             * @throws InterruptedException Problem with time
             */
            private void executePower() throws IOException, UnreadableException, InterruptedException {

                // Informs the preferred player to execute
                sendObject(ACLMessageSecretHitler.EXECUTE,
                        playerBehaviour.selectPerson(ACLMessageSecretHitler.EXECUTE), gameManager);

                // Receive all votes of execution
                Map<AID, AID> executionVotes = (Map<AID, AID>) receiveObject(ACLMessageSecretHitler.EXECUTE);

                // Process the execution votes of the players
                playerBehaviour.process(ACLMessageSecretHitler.EXECUTE, executionVotes);

                //  President chooses target, then receives message.
                if (playerBehaviour.isPresident()) {
                    // Execute the chosen player
                    AID chosenPlayer = playerBehaviour.presidentChoose(ACLMessageSecretHitler.DEAD, executionVotes);
                    System.out.println(getLocalName() + ": As president, I choose to execute " + chosenPlayer.getLocalName());
                    sendObject(ACLMessageSecretHitler.DEAD, ACLMessageSecretHitler.DEAD,
                            chosenPlayer);
                }

                // Everyone may receive a message. It's either from the president or from the manager.
                MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessageSecretHitler.DEAD);
                ACLMessage message;
                while (true) {
                    message = receive(messageTemplate);
                    if (message != null) {
                        if (message.getSender().equals(playerBehaviour.getCurrentPresident())) {
                            sendObject(ACLMessageSecretHitler.DEAD, getAID(), gameManager);
                            endGame();
                            return;
                        }
                        break;
                    }
                }

                // If received from president, I'm already dead.
                // If received from manager, proceed to process contents.
                AID executedPlayer = (AID) message.getContentObject();
                playerBehaviour.process(ACLMessageSecretHitler.DEAD, executedPlayer);
                playerBehaviour.removePlayer(executedPlayer);

                // Receives if the killed player is hitler
                if ((Boolean) receiveObject(ACLMessageSecretHitler.DEAD)) {
                    endGame();
                }
            }

            /**
             * Add peek power behaviour
             * @throws UnreadableException Problem reading content object
             * @throws IOException Problem receiving data
             * @throws InterruptedException Problem with time
             */
            private void peekPower() throws IOException, UnreadableException, InterruptedException {

                if (playerBehaviour.isPresident()) {
                    // Receives next 3 policies
                    List<Policy> next3 = (List<Policy>) receiveObject(ACLMessageSecretHitler.PEEK);

                    // Informs which policies it has peeked
                    sendObject(ACLMessageSecretHitler.PEEK,
                            (Serializable) playerBehaviour.explainCards(ACLMessageSecretHitler.PEEK, next3),
                            gameManager);
                }

                // Receives claimed policies from president
                playerBehaviour.process(ACLMessageSecretHitler.PEEK, receiveObject(ACLMessageSecretHitler.PEEK));
            }

            /**
             * Add investigated power behaviour
             * @throws UnreadableException Problem reading content object
             * @throws IOException Problem receiving data
             * @throws InterruptedException Problem with time
             */
            private void investigatePower() throws IOException, UnreadableException, CodecException, InterruptedException {

                // Sends preferred investigated player
                sendObject(ACLMessageSecretHitler.INVESTIGATE,
                        playerBehaviour.selectPerson(ACLMessageSecretHitler.INVESTIGATE), gameManager);

                // Receive all votes to investigate
                Map<AID, AID> investigateVotes = (Map<AID, AID>) receiveObject(ACLMessageSecretHitler.INVESTIGATE);
                playerBehaviour.process(ACLMessageSecretHitler.INVESTIGATE, investigateVotes);

                // Deals with the investigation for both president and player
                String playerInvestigated;
                if (playerBehaviour.isPresident()) {
                    playerInvestigated = presidentInvestigation(investigateVotes);
                } else {
                    playerInvestigated = playerInvestigation();
                }

                // Process investigation
                String[] info = playerInvestigated.split(";");
                //StringACLCodec codec = new StringACLCodec(new StringReader(info[0]), null);
                //AID aid_rec = codec.decodeAID();                
                playerBehaviour.processInvestigation(playerBehaviour.getPlayer(info[0]), info[1].equals("true"));
            }

            /**
             * Add investigated power behaviour of player
             * @throws UnreadableException Problem reading content object
             * @throws IOException Problem receiving data
             * @throws InterruptedException Problem with time
             */
            private String playerInvestigation() throws IOException, UnreadableException, InterruptedException {
                MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessageSecretHitler.INVESTIGATE);
                ACLMessage message;
                String playerInvestigated;
                while (true) {
                    message = receive(messageTemplate);
                    if (message != null) {
                        if (message.getSender().equals(playerBehaviour.getCurrentPresident())) {
                            sendObject(ACLMessageSecretHitler.INVESTIGATE,
                                    role == PlayerType.FASCIST || role == PlayerType.HITLER,
                                    playerBehaviour.getCurrentPresident());
                            playerInvestigated = (String) receiveObject(ACLMessageSecretHitler.INVESTIGATE);
                        } else {
                            playerInvestigated = (String) message.getContentObject();
                        }
                        break;
                    }
                }
                return playerInvestigated;
            }

            /**
             * Add investigated power behaviour of president
             * @throws UnreadableException Problem reading content object
             * @throws IOException Problem receiving data
             * @throws InterruptedException Problem with time
             */
            private String presidentInvestigation(Map<AID, AID> investigateVotes)
                    throws IOException, UnreadableException, InterruptedException {

                String playerInvestigated;

                // Investigate the chosen player
                AID chosenPlayer = playerBehaviour.presidentChoose(ACLMessageSecretHitler.INVESTIGATE, investigateVotes);
                sendObject(ACLMessageSecretHitler.INVESTIGATE, ACLMessageSecretHitler.INVESTIGATE, chosenPlayer);

                // Receives info from the investigated player
                Boolean investigationResults = (Boolean) receiveObject(ACLMessageSecretHitler.INVESTIGATE);

                // Informs the manager of the investigation data
                String playerInfo = chosenPlayer.getLocalName() + ";" + playerBehaviour.explainInvestigation(chosenPlayer, investigationResults);
                sendObject(ACLMessageSecretHitler.INVESTIGATE, playerInfo, gameManager);

                // Receives the investigated player
                playerInvestigated = (String) receiveObject(ACLMessageSecretHitler.INVESTIGATE);
                return playerInvestigated;
            }
        });
    }

    /**
     * The game has ended -> Reveals info and kills itself
     */
    private void endGame() {
        System.out.println("Agent " + getLocalName() + " : My role was " + role);
        doDelete();
    }

    /**
     * Sends object to receiver
     *
     * @param type     - Type of the action.
     * @param object   - Target of the action.
     * @param receiver - Receiver of the message.
     * @throws IOException Problems doing setContentObject
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
     * @param msgType - Type of message to receive.
     * @return The object associated with the message.
     * @throws UnreadableException Problems doing getContentObject
     */
    private Object receiveObject(int msgType) throws UnreadableException, InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100);
        MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(msgType);
        while (true) {
            ACLMessage message = receive(messageTemplate);
            if (message != null)
                return message.getContentObject();
        }
    }

    /**
     * Receives the respective role for the game
     * @param verbose 
     *
     * @throws UnreadableException  Problem reading content
     * @throws InterruptedException Problem with sleep
     */
    private void receiveRole(boolean verbose) throws UnreadableException, InterruptedException {
        List<AID> secretHitlerPlayers = (List<AID>) receiveObject(ACLMessageSecretHitler.ROLE);
        MessageTemplate messageTemplate = MessageTemplate.MatchPerformative(ACLMessageSecretHitler.ROLE);

        while (true) {
            ACLMessage message = receive(messageTemplate);
            if (message != null) {
                gameManager = message.getSender();

                if (message.getContentObject() instanceof PlayerType) {
                    role = (PlayerType) message.getContentObject();
                    createRespectiveBehaviour(null, null, getAID(), secretHitlerPlayers, verbose);
                } else if (message.getContentObject() instanceof List) {
                    List<AID> fascists = (List<AID>) message.getContentObject();
                    if (fascists.get(0).equals(getAID())) {
                        role = PlayerType.HITLER;
                    } else {
                        role = PlayerType.FASCIST;
                    }
                    createRespectiveBehaviour(fascists, fascists.get(0), getAID(), secretHitlerPlayers, verbose);
                }
                TimeUnit.MILLISECONDS.sleep(100);
                System.out.println(getLocalName() + " role is " + role);
                return;
            }
        }
    }

    /**
     * Creates the behaviour for the respective personality
     *
     * @param fascists            The AID's of the fascists players
     * @param hitler              The AID of hitler
     * @param aid                 The player AID
     * @param secretHitlerPlayers AID's of players
     * @param verbose
     */
    private void createRespectiveBehaviour(List<AID> fascists, AID hitler, AID aid, List<AID> secretHitlerPlayers, boolean verbose) {
        if (fascists != null) {
            switch (playerPersonality) {
                case SHY:
                    playerBehaviour = new ShyFascist(fascists, hitler, aid, secretHitlerPlayers, verbose);
                    break;
                case AGGRESSIVE:
                    playerBehaviour = new AggressiveFascist(fascists, hitler, aid, secretHitlerPlayers, verbose);
                    break;
                case CALM:
                    playerBehaviour = new ManualFascistBehaviour(fascists, hitler, aid, secretHitlerPlayers);
                    break;
                default:
                    playerBehaviour = new FascistBehaviour(fascists, hitler, aid, secretHitlerPlayers, verbose);
                    break;
            }
        } else {
            switch (playerPersonality) {
                case SHY:
                    playerBehaviour = new ShyLiberal(aid, secretHitlerPlayers, verbose);
                    break;
                case AGGRESSIVE:
                    playerBehaviour = new AggressiveLiberal(aid, secretHitlerPlayers, verbose);
                    break;
                case CALM:
                    playerBehaviour = new ManualLiberalBehaviour(aid, secretHitlerPlayers);
                    break;
                default:
                    playerBehaviour = new DefaultLiberalBehaviour(aid, secretHitlerPlayers, verbose);
                    break;
            }
        }
    }
}
