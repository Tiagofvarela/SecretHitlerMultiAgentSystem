package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import jade.core.AID;

public class Game {

    private static final int NUMBER_FASCISTS = 2;
    private static final int POLICIES_DECK_SIZE = 17;
    //Decks
    private final List<Policy> policiesDeck = generatePolicyList();
    private final List<Policy> discardedPile = new LinkedList<>();
    private final Random rnd = new Random();
    //Trackers
    private int fascistTracker = 0;
    private int liberalTracker = 0;
    private int failTracker = 0;

    /**
     * Adds a new policy to the game.
     *
     * @param finalPolicy Final and chosen policy for the round
     */
    public void newPolicy(Policy finalPolicy) {
        if (finalPolicy == Policy.Fascist) {
            fascistTracker++;
        } else {
            liberalTracker++;
        }
    }

    /**
     * Gets the current value of the fail tracker
     *
     * @return The value of fail tracker
     */
    public int getFailTracker() {
        return failTracker;
    }

    /**
     * Sets the new value for the fail tracker
     *
     * @param value Value to set
     */
    public void setFailTracker(int value) {
        failTracker = value;
    }

    /**
     * Gets the next 3 policies from the deck of policies
     *
     * @return The next 3 policies as an array
     */
    public List<Policy> getNext3Policies() {
        List<Policy> next3Policies = new LinkedList<>();
        next3Policies.add(policiesDeck.remove(0));
        next3Policies.add(policiesDeck.remove(0));
        next3Policies.add(policiesDeck.remove(0));
        if (policiesDeck.size() < 3)
            reshuffle();
        return next3Policies;
    }

    /**
     * Gets the next policy from the deck of policies
     *
     * @return The next policy
     */
    public Policy getNextPolicy() {
        Policy policy = policiesDeck.remove(0);
        if (policiesDeck.size() < 3)
            reshuffle();
        return policy;
    }

    /**
     * Peek next 3 cards
     *
     * @return The next 3 cards
     */
    public List<Policy> peekNext3Policies() {
    	List<Policy> newList = new ArrayList<>();
    	for(Policy p : policiesDeck.subList(0, 3))
    		newList.add(p);
        return newList;
    }

    /**
     * Obtains the number of fascists cards currently in th board
     *
     * @return The number of fascists cards
     */
    public int getNumberOfFascistCards() {
        return fascistTracker;
    }
    
    public int getNumberOfLiberalCards() {
        return liberalTracker;
    }

    /**
     * Adds policies to discard pile.
     *
     * @param discarded
     */
    public void discard(List<Policy> discarded) {
        discardedPile.addAll(discarded);
    }

    /**
     * Generates and returns a random list with the AID's of the fascist players
     *
     * @param secretHitlerPlayers Players in the game
     * @return A List<PlayerAID> of the fascists
     */
    public List<AID> generateFascists(List<AID> secretHitlerPlayers) {
        List<AID> shp2 = new ArrayList<>(secretHitlerPlayers);
        List<AID> fascistsPlayers = new ArrayList<>();
        for (int i = 0; i < NUMBER_FASCISTS; i++) {
            int nextFascist = rnd.nextInt(shp2.size());
            fascistsPlayers.add(shp2.remove(nextFascist));
        }
        return fascistsPlayers;
    }

    /**
     * Checks if the current game as ended
     * Winning conditions: fascistCards = 5 || liberalCards = 6 || Hitler is killed
     *
     * @return true if game ended | false if not
     */
    public boolean isDone() {
        return fascistTracker == 6 || liberalTracker == 5;
    }

    /**
     * Reshuffles discard pile into policies pile.
     */
    private void reshuffle() {
        policiesDeck.addAll(discardedPile);
        discardedPile.clear();
        Collections.shuffle(policiesDeck);
    }

    /**
     * Generates a list of policies and shuffles it.
     *
     * @return
     */
    private List<Policy> generatePolicyList() {
        List<Policy> policies = new ArrayList<>();
        for (int i = 0; i < POLICIES_DECK_SIZE; i++) {
            if (i <= 6)
                policies.add(Policy.Liberal);
            else
                policies.add(Policy.Fascist);
        }
        Collections.shuffle(policies);
        return policies;
    }

	public int discardSize() {
		return discardedPile.size();		
	}

	public int deckSize() {
		return policiesDeck.size();
	}
}
