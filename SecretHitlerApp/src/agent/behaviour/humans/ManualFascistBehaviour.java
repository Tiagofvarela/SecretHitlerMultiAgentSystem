package agent.behaviour.humans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import agent.behaviour.PlayerBehaviour;
import app.Policy;
import jade.core.AID;

public class ManualFascistBehaviour extends PlayerBehaviour {

    private final Map<AID, String> convertToString = new HashMap<>();
    private final Map<String, AID> convertToAID = new HashMap<>();
    protected List<Policy> peek = new ArrayList<>();
    Scanner scn = new Scanner(System.in);
    private final AID hitler;

    public ManualFascistBehaviour(List<AID> fascists, AID hitler, AID id, List<AID> players) {
        super(id, players, false);
        //Requires players have no repeat names.
        for (AID p : players) {
            convertToString.put(p, p.getLocalName());
            convertToAID.put(p.getLocalName(), p);
        }

        this.hitler = hitler;
        name = name + " (human)";
    }

    @Override
    public boolean isHitler() {
        return id.equals(hitler);
    }

    @Override
    public boolean isFascist() {
        return true;
    }

    public void removePlayer(AID player) {
        myOpinionOfOthers.remove(player);
        players.remove(player);
        convertToAID.remove(player.getLocalName());
        convertToString.remove(player);
    }

    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
        System.out.println(name + ": Please type in the name of the person you want to Investigate:");
        return getPersonInput(votes, Arrays.asList(id));
    }

    protected AID presidentChooseDead(Map<AID, AID> votes) {
        System.out.println(name + ": Please type in the name of the person you want to Execute:");
        return getPersonInput(votes, Arrays.asList(id));
    }

    protected AID presidentChooseChancellor(Map<AID, AID> votes) {
        System.out.println(name + ": Please type in the name of the person you want as Chancellor:");
        return getPersonInput(votes, Arrays.asList(id, currentChancellor));
    }

    protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
        System.out.println(name + ": The policies you received were " + policies);
        System.out.println(name + ": Please choose 2 policies to keep, President.");
        return getPolicyInput(policies, 2);
    }

    protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {
        System.out.println(name + ": The policies you received were " + policies);
        System.out.println(name + ": Please choose a policy to enact, Chancellor.");
        return getPolicyInput(policies, 1);
    }

    protected List<Policy> explainCardsChancellor(List<Policy> policies) {
        System.out.println(name + ": The policies you received were " + policies);
        System.out.println(name + ": Please tell everyone which policies you received, Chancellor.");
        return getPolicyInput(new ArrayList<>(), 2);
    }

    protected List<Policy> explainCardsPresident(List<Policy> policies) {
        System.out.println(name + ": The policies you received were " + policies);
        System.out.println(name + ": Please tell everyone which policies you received, President.");
        return getPolicyInput(new ArrayList<>(), 3);
    }

    protected List<Policy> explainCardsPeek(List<Policy> policies) {
        System.out.println(name + ": The policies you peeked at were " + policies);
        return getPolicyInput(new ArrayList<>(), 3);
    }

    public boolean explainInvestigation(AID investigated, boolean fascist) {
        System.out.println(name + ": " + investigated.getLocalName() + " is a fascist? " + fascist);
        return getBooleanInput();
    }

    protected AID selectExecute() {
        System.out.println(name + ": Vote for who you'd like to see executed.");
        return getPersonInput(null, Arrays.asList(currentPresident));
    }

    protected AID selectInvestigate() {
        System.out.println(name + ": Vote for who you'd like to see investigated.");
        return getPersonInput(null, Arrays.asList(currentPresident));
    }

    protected AID selectChancellor() {
        System.out.println(name + ": Vote for who you'd like to see as Chancellor.");
        return getPersonInput(null, Arrays.asList(currentPresident, currentChancellor));
    }

    /**
     * Votes in favour if both members are okay, or if one is great even if the other
     * isn't that good.
     */
    public boolean vote() {
        return getVoteInput();
    }

    /**
     * Depending on opinion of person voted for, likes or dislikes voter more.
     */
    protected void processVotesExecute(Map<AID, AID> votes) {
        System.out.println(name + " PROCESS: \n"
                + "Votes on who to execute:");
        printMap(votes);
    }

    /**
     * Depending on opinion of person voted for, likes or dislikes voter more.
     */
    protected void processVotesInvestigate(Map<AID, AID> votes) {
        System.out.println(name + " PROCESS: \n"
                + "Votes on who to investigate:");
        printMap(votes);
    }

    /**
     * Depending on opinion of person voted for, likes or dislikes voter more.
     */
    protected void processVotesChancellor(Map<AID, AID> votes) {
        System.out.println(name + " PROCESS: \n"
                + "Votes on who to nominate as Chancellor:");
        printMap(votes);
    }


    public void processInvestigation(AID investigated, boolean fascist) {
        System.out.println(name + " PROCESS: \n"
                + "President on whether " + investigated.getLocalName() + " is fascist: " + fascist);
    }

    public void processPolicyJustification(Policy newPolicy,
                                           List<Policy> chancellorCards, List<Policy> presidentCards) {
        System.out.println(name + " PROCESS: \n"
                + "Chancellor claims he had received: " + chancellorCards
                + "\nPresident claims he had received: " + presidentCards);
    }

    protected void processDead(AID deadPlayer) {
        System.out.println(name + " PROCESS: \n"
                + "The following player has died: " + deadPlayer.getLocalName());
    }

    protected void processPeek(List<Policy> data) {
        System.out.println(name + " PROCESS: \n"
                + "The president says the next three policies are " + data);
    }

    public void processGovernmentVotes(Map<AID, Boolean> governmentVotes) {
        System.out.println(name + " PROCESS: \n"
                + "The votes for government are as follows.");
        printMap(governmentVotes);
    }

    protected void processNewLiberal(Map<AID, Boolean> votes) {
        System.out.println(name + " PROCESS: \n"
                + "The government passed a LIBERAL policy.");
    }

    protected void processNewFascist(Map<AID, Boolean> votes) {
        System.out.println(name + " PROCESS: \n"
                + "The government passed a FASCIST policy.");
    }
    //////////////////////////////////////////////INPUT PROCESSING//////////////////////////////////////////////

    /**
     * Scans a human player's vote and converts it to a valid AID not contained in except.
     *
     * @param except - Valid names that are invalid for the current event.
     * @return Chosen AID scanned from human player input.
     */
    private AID getPersonInput(Map<AID, AID> votes, List<AID> except) {
        //printMap(votes);
        String person = scn.nextLine();
        while (convertToAID.get(person) == null
                || except.contains(convertToAID.get(person))
                || !myOpinionOfOthers.containsKey(convertToAID.get(person))) {

            System.out.println("The name was invalid. Please try again.");
            person = scn.nextLine();
        }
        return convertToAID.get(person);
    }

    /**
     * Scans a human player's vote and converts it to boolean. Players vote with "yay" or "nay".
     *
     * @return true if "yay" or false if "nay".
     */
    private boolean getVoteInput() {
        System.out.println(name + ": The president is " + currentPresident.getLocalName()
                + " and the chancellor is " + currentChancellor.getLocalName());
        System.out.println(name + ": Write 'yay' or 'nay' to vote for or against the government.");
        String vote = scn.nextLine();
        boolean yay = vote.equals("yay");
        boolean nay = vote.equals("nay");
        while (!yay && !nay) {
            System.out.println("The vote was invalid. Please try again.");
            vote = scn.nextLine();
            yay = vote.equals("yay");
            nay = vote.equals("nay");
        }
        return yay;
    }

    /**
     * Scans a human player's input and converts it to boolean. Players vote with "t" or "f".
     *
     * @return true if "t" or false if "f".
     */
    private boolean getBooleanInput() {
        System.out.println(name + ": Type 't' for true or 'f' for false.");
        String vote = scn.nextLine();
        boolean isTrue = vote.equals("t");
        boolean isFalse = vote.equals("f");
        while (!isTrue && !isFalse) {

            System.out.println("The boolean was invalid. Please try again.");
            vote = scn.nextLine();
            isTrue = vote.equals("t");
            isFalse = vote.equals("f");
        }
        return isTrue;
    }

    /**
     * Scans a human player's input and converts it to a list of policies.
     * Players create a list with "l" and "f" separated by commas and no spaces.
     *
     * @param policies     - Policies from which to choose. Player list must be contained in policies.
     * @param selectNumber - Number of policies to write for the list.
     * @return List created by interpreting human player input.
     */
    private List<Policy> getPolicyInput(List<Policy> policies, int selectNumber) {
        System.out.println(name + ": Type 'l' for liberal or 'f' for fascist. "
                + "Separate each with a comma and no spaces. You must input " + selectNumber + ".");

        String[] cards = scn.nextLine().split(",");
        List<Policy> newList = makeCardList(cards);
        boolean valid = listContainsSublist(policies, newList);

        while (!valid || newList.size() != selectNumber) {
            System.out.println("The policy list was invalid. Please try again.");

            cards = scn.nextLine().split(",");
            newList = makeCardList(cards);
            valid = listContainsSublist(policies, newList);
        }
        return newList;
    }

    /**
     * @param cardInput
     * @return New list made by interpreting input.
     */
    private List<Policy> makeCardList(String[] cardInput) {
        List<Policy> newList = new ArrayList<>();
        for (int i = 0; i < cardInput.length; i++) {
            if (cardInput[i].equals("f"))
                newList.add(Policy.Fascist);
            else if (cardInput[i].equals("l"))
                newList.add(Policy.Liberal);
        }
        return newList;
    }

    /**
     * @return Whether subList is contained in list, or true if list is empty.
     */
    private boolean listContainsSublist(List<Policy> list, List<Policy> subList) {
        if (list.isEmpty())
            return true;
        List<Policy> elementsToMatch = cloneList(list);
        for (Policy p : subList) {
            if (elementsToMatch.contains(p))
                elementsToMatch.remove(p);
            else
                return false;
        }
        return true;
    }
}
