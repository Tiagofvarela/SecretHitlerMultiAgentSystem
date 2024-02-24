package agent.behaviour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import agent.communication.ACLMessageSecretHitler;
import app.Policy;
import jade.core.AID;

public abstract class PlayerBehaviour {
    
    //Important IDs to keep track of.
    protected AID id;
    protected AID currentPresident;
    protected AID currentChancellor;
    //Trackers and latest policy.
    protected int fascistTracker = 0;
    protected int liberalTracker = 0;
    protected int failedGovernments = 0;
    protected Policy newPolicy;    
	protected List<Policy> peek = new ArrayList<>();
    //List of players and map of opinions for each.
    protected List<AID> players;
    protected Map<AID, Integer> myOpinionOfOthers = new HashMap<>();
    //Number of rounds since start.
    protected int round;
    //Whether this agent should print all their thoughts or not.
	protected boolean verbose;  
	protected String name;
	
    Random rnd = new Random();

    protected PlayerBehaviour(AID id, List<AID> players, boolean verbose) {
        this.id = id;        
    	this.players = players;
    	Collections.shuffle(this.players);
    	this.verbose = verbose;
        for (AID player :
                this.players) {
            myOpinionOfOthers.put(player, 50);
        }
        name = id.getLocalName(); 
    }

    /**
     * Checks if the game ends.
     * @return true if the game ends | false if not
     */
    public boolean isGameEnd() {
        return fascistTracker == 6 || liberalTracker == 5;
    }

    /**
     * Checks whether player is Hitler.
     * @return Whether I am Hitler or not.
     */
    public abstract boolean isHitler();

    /**
     * When the president investigates me, I must reply with my role.
     * @return Whether I am fascist or not.
     */
    public abstract boolean isFascist();

    /**
     * Checks if this agent is the President.
     * @return If this agent is President.
     */
    public boolean isPresident() {
        return id.equals(currentPresident);
    }

    /**
     * Checks if this agent is the Chancellor.
     * @return If this agent is Chancellor.
     */
    public boolean isChancellor() {
        return id.equals(currentChancellor);
    }

    /**
     * Obtains the current chancellor
     * @return The AID of the current chancellor | null if none currently
     */
    public AID getCurrentChancellor() {
        return currentChancellor;
    }

    /**
     * Obtains the current president
     * @return The AID of the current president | null if none currently
     */
    public AID getCurrentPresident() {
        return currentPresident;
    }

    /**
     * Updates who is current President and keeps track of the round.
     */
    public void updatePresident(AID newPresident) {
    	printMap(myOpinionOfOthers);
        currentPresident = newPresident;
        round++;
    }

	/**
     * Updates who is current Chancellor.
     */
    public void updateChancellor(AID newChancellor) {
        currentChancellor = newChancellor;
    }

    /**
     * Keeps track that a government failed.
     */
	public void failGovernment() {
		updateChancellor(null);
		failedGovernments += (failedGovernments+1) % 3;		
	}

    /**
     * Logic for the player to update values based on passed policy.
     * Keeps track of whether liberal or fascist.
     * @param newPolicy - The enacted policy
     * @param votes     - Votes for or against the government
     */
    public void processNewPolicy(Policy newPolicy, Map<AID, Boolean> votes) {
        this.newPolicy = newPolicy;
        if(votes.isEmpty()) {
        	failGovernment();
        	print("This was the third failed government in a row, so it's not the government's fault.");
        }else {
        	failedGovernments = 0;
        }
        if (newPolicy == Policy.Fascist) {
            fascistTracker++;
            if(!votes.isEmpty())
            	processNewFascist(votes);
        } else {
            liberalTracker++;
            if(!votes.isEmpty())
            	processNewLiberal(votes);
        }
    }
    /**
     * Logic for processing that a Fascist policy was enacted.
     * @param votes - Who voted for or against current government.
     */
    protected abstract void processNewLiberal(Map<AID, Boolean> votes);
    /**
     * Logic for processing that a Fascist policy was enacted.
     * @param votes - Who voted for or against current government.
     */
    protected abstract void processNewFascist(Map<AID, Boolean> votes);

    /**
     * Checks the election tracker to see if any power was activated.
     * @return The activated power or ACLMessage.END_TURN
     */
    public int checkTracker() {
        if (fascistTracker == 2) {
            return ACLMessageSecretHitler.INVESTIGATE;
        }
        if (fascistTracker == 3) {
            return ACLMessageSecretHitler.PEEK;
        }
        if (fascistTracker == 4 || fascistTracker == 5) {
            return ACLMessageSecretHitler.EXECUTE;
        }
        return ACLMessageSecretHitler.END_TURN;
    }
    
    public abstract void removePlayer(AID player);

    /**
     * Logic for the President to make the final choice in a type of event.
     * @param type  - Type of event to make a choice on.
     * @param votes - Players' opinions on who to choose.
     * @return Chosen player
     */
    public AID presidentChoose(int type, Map<AID, AID> votes) {
        if (type == ACLMessageSecretHitler.CHANCELLOR_CANDIDATE) {
            return presidentChooseChancellor(votes);
        } else if (type == ACLMessageSecretHitler.DEAD) {
            return presidentChooseDead(votes);
        } else if (type == ACLMessageSecretHitler.INVESTIGATE) {
            return presidentChooseInvestigate(votes);
        }
        return null;
    }
    /**
     * As President, make final decision on who to investigate.
     * @param votes - Other players' opinion on the matter.
     * @return Player to investigate
     */
    protected abstract AID presidentChooseInvestigate(Map<AID, AID> votes);
    /**
     * As President, make final decision on who to execute.
     * @param votes - Other players' opinion on the matter.
     * @return Player to execute
     */
    protected abstract AID presidentChooseDead(Map<AID, AID> votes);
    /**
     * As President, make final decision on who to nominate as Chancellor.
     * @param votes - Other players' opinion on the matter.
     * @return Nominated Chancellor
     */
    protected abstract AID presidentChooseChancellor(Map<AID, AID> votes);

    /**
     * From a list of Policies remove one and return the rest.
     * Choose 2 of 3 as President or 1 of 2 as Chancellor.
     * @param policies - Received policies.
     * @return Chosen policies.
     */
    public List<Policy> choosePolicies(List<Policy> policies) {    	
    	List<Policy> modify = cloneList(policies);
        if (id.equals(currentPresident)) {
        	return choosePoliciesPresident(modify);
        } else if (id.equals(currentChancellor)) {
        	return choosePoliciesChancellor(modify);
        }
        return null;
    }
    /**
     * Logic for the Chancellor to choose a policy to enact.
     * @param policies - Policies received from the President.
     * @return Chosen policy (first element in list is chosen).
     */
    protected abstract List<Policy> choosePoliciesChancellor(List<Policy> policies);
    /**
     * Logic for the President to choose 2 policies to send the Chancellor.
     * @param policies - Policies received from deck.
     * @return Chosen 2 policies.
     */
	protected abstract List<Policy> choosePoliciesPresident(List<Policy> policies);

    /**
     * Explains card choice for Chancellor, President or Peek.
     * @param type     - Type of explanation requested.
     * @param policies - Received policies for the event in question.
     * @return A list of policies the person in question claims they saw.
     */
    public List<Policy> explainCards(int type, List<Policy> policies) {
    	List<Policy> modify = cloneList(policies);
        if (type == ACLMessageSecretHitler.CHANCELLOR_CARDS) {
            return explainCardsChancellor(modify);
        } else if (type == ACLMessageSecretHitler.PRESIDENT_CARDS) {
            return explainCardsPresident(modify);
        } else if (type == ACLMessageSecretHitler.PEEK) {
        	peek = policies;
            return explainCardsPeek(modify);
        }
        return null;
    }   
    /**
     * Logic for the Chancellor to share the policies he received with the others.
     * @param policies - Policies received from the President.
     * @return What they tell the other players they saw.
     */
    protected abstract List<Policy> explainCardsChancellor(List<Policy> policies);
    /**
     * Logic for the President to share the policies he received with the others.
     * @param policies - Policies received from deck.
     * @return What they tell the other players they saw.
     */
    protected abstract List<Policy> explainCardsPresident(List<Policy> policies);    
    /**
     * Logic for the President to share the policies he peeked with the others.
     * The president must process the peek for themselves here.
     * @param policies - Policies peeked.
     * @return What they tell the other players they saw.
     */
    protected abstract List<Policy> explainCardsPeek(List<Policy> policies);
    /**
     * As President, share investigation results with other players.
     * @param investigated - Investigated player.
     * @param fascist      - Whether they are fascist or not.
     * @return What they tell other players. Whether investigated is fascist or not.
     */
    public abstract boolean explainInvestigation(AID investigated, boolean fascist);
    /**
     * As a player, vote for a person.
     * @param type - Type of event to vote for.
     * @return The preferred player.
     */
    public AID selectPerson(int type) {
        if (type == ACLMessageSecretHitler.CHANCELLOR_CANDIDATE) {  
        	return selectChancellor();
        } else if (type == ACLMessageSecretHitler.EXECUTE) {
            return selectExecute();
        } else if (type == ACLMessageSecretHitler.INVESTIGATE) {
        	return selectInvestigate();
        }
        return null;
    }
    /**
     * Logic for selecting who they prefer as Chancellor.
     * @return Who they prefer as Chancellor.
     */
	protected abstract AID selectChancellor();
    /**
     * Logic for selecting who they prefer to have executed.
     * @return Who they prefer to have executed.
     */
	protected abstract AID selectExecute();
    /**
     * Logic for selecting who they prefer to have investigated.
     * @return Who they prefer to have investigated.
     */
	protected abstract AID selectInvestigate();
	/**
	 * Logic for voting for or against current government (currentPresident + currentChancellor).
	 * @return Vote in favour or against.
	 */
    public abstract boolean vote();
    /**
     * Logic for processing different types of events.
     * @param type - Type of event to process.
     * @param data - Relevant information for the event processing.
     */
    public void process(int type, Object data) {
        if (type == ACLMessageSecretHitler.EXECUTE || type == ACLMessageSecretHitler.INVESTIGATE
                || type == ACLMessageSecretHitler.CHANCELLOR_CANDIDATE) {
            processVotes(type, (Map<AID, AID>) data);
        } else if (type == ACLMessageSecretHitler.VOTE) {
            processGovernmentVotes((Map<AID, Boolean>) data);
        } else if (type == ACLMessageSecretHitler.PEEK && !isPresident()) {
            processPeek((List<Policy>) data);
        } else if (type == ACLMessageSecretHitler.DEAD) {
            processDead((AID) data);
        }
    }
    /**
     * Logic for processing the death of a player.
     * @param deadPlayer - The player who has died.
     */
	protected abstract void processDead(AID deadPlayer);
	/**
	 * Logic for a processing peek. Not for the president.
	 */
	protected abstract void processPeek(List<Policy> data);
	/**
	 * Logic for processing votes for or against current government (currentPresident + currentChancellor).
	 * @param governmentVotes - Whether a player voted for or against.
	 */
    public abstract void processGovernmentVotes(Map<AID, Boolean> governmentVotes);
    /**
     * Logic for processing votes for players in different types of events.
     * @param type  - Type of event in question.
     * @param votes - Who voted for who.
     */
	private void processVotes(int type, Map<AID, AID> votes) {
        if (type == ACLMessageSecretHitler.EXECUTE) {
            processVotesExecute(votes);
        } else if (type == ACLMessageSecretHitler.INVESTIGATE) {
            processVotesInvestigate(votes);
        } else if (type == ACLMessageSecretHitler.CHANCELLOR_CANDIDATE) {
            processVotesChancellor(votes);
        }
    }
	/**
	 * Logic for processing votes on who to execute.
	 * @param votes - Who voted for who.
	 */
	protected abstract void processVotesExecute(Map<AID, AID> votes);
	/**
	 * Logic for processing votes on who to investigate.
	 * @param votes - Who voted for who.
	 */
	protected abstract void processVotesInvestigate(Map<AID, AID> votes);
	/**
	 * Logic for processing votes on who to chose as Chancellor.
	 * @param votes - Who voted for who.
	 */
	protected abstract void processVotesChancellor(Map<AID, AID> votes);  
	/**
	 * Logic for processing the President's investigation results.
	 * @param investigated - Who was investigated.
	 * @param fascist      - Whether they are fascist or not, according to the President.
	 */
    public abstract void processInvestigation(AID investigated, boolean fascist);
    /**
     * Logic for processing the President and Chancellor's claims as to what policies they received.
     * @param newPolicy       - The policy that was enacted.
     * @param chancellorCards - What the President claims they received from the deck.
     * @param presidentCards  - What the Chancellor claims they received from the President.
     */
    public abstract void processPolicyJustification(Policy newPolicy,
                                    List<Policy> chancellorCards, List<Policy> presidentCards);   
    /**
     * @param opinions - Map of AID and corresponding opinion of them. Higher values are positive.
     * @param except   - List of AID that cannot be chosen.     
     * @return AID of agent I most dislike that is not in except or null if except.contains(opinions.keySet()).
     */
    protected AID getMostDislikedAID(Map<AID, Integer> opinions, List<AID> except) {
        int minValue = 101;
        List<Entry<AID, Integer>> list = shuffleMap(opinions);
        List<AID> topChoices = new ArrayList<>();
        for (Entry<AID, Integer> pair : list) {
        	if (except.contains(pair.getKey()))
            	continue;
            if(pair.getValue() == minValue) {
            	topChoices.add(pair.getKey());
            }else if (pair.getValue() < minValue) {   
            	topChoices.clear();
            	topChoices.add(pair.getKey());
            	minValue = pair.getValue();
            }            
        }
        return topChoices.get(rnd.nextInt(topChoices.size()));
    }
    /**
     * @param opinions - Map of AID and corresponding opinion of them. Higher values are positive.
     * @param except   - List of AID that cannot be chosen.     
     * @return AID of agent I most like that is not in except or null if except.contains(opinions.keySet()).
     */
    protected AID getMostLikedAID(Map<AID, Integer> opinions, List<AID> except) {
        int maxValue = -1;
        List<Entry<AID, Integer>> list = shuffleMap(opinions);
        List<AID> topChoices = new ArrayList<>();
        for (Entry<AID, Integer> pair : list) {
        	if (except.contains(pair.getKey()))
            	continue;
            if(pair.getValue() == maxValue) {
            	topChoices.add(pair.getKey());
            }else if (pair.getValue() > maxValue) {            	
                //preferred = pair.getKey();
            	topChoices.clear();
            	topChoices.add(pair.getKey());
                maxValue = pair.getValue();
            }
        }
        if(topChoices.isEmpty())
        	return null;
        return topChoices.get(rnd.nextInt(topChoices.size()));
    }

	/**
	 * @param opinions
	 * @return
	 */
	protected List<Entry<AID, Integer>> shuffleMap(Map<AID, Integer> opinions) {
		List<Entry<AID, Integer>> list = new ArrayList<Entry<AID, Integer>>(opinions.entrySet());
        Collections.shuffle(list);
		return list;
	}

    /**
     * Increase my opinion of agentID by val.
     */
    protected void increaseOpinion(AID agentID, int val) {
        myOpinionOfOthers.put(agentID, Math.min(100, myOpinionOfOthers.get(agentID) + val));
    }

    /**
     * Decrease my opinion of agentID by val.
     */
    protected void decreaseOpinion(AID agentID, int val) {
        myOpinionOfOthers.put(agentID, Math.max(0, myOpinionOfOthers.get(agentID) - val));
    }
    /**
     * Gets the next President.
     * @return Who will be the next president.
     */
    protected AID getNextPresident() {
    	return players.get(
    			(players.indexOf(currentPresident)+1) % players.size());
    }

    ////////////////////////////////////////////HELP FUNCTIONS/////////////////////////////////////////
    
    /**
     * Prints a map of votes. Votes for an AID are converted to a string with the Local Name.
     * @param map - Map of votes.
     */
    protected void printMap(Map<AID, ?> map) {
    	if(map == null)
    		return;
    	StringBuilder sb = new StringBuilder("\n-------------\n");
    	for (Entry<AID, ?> pair : map.entrySet()) {
    		if(pair.getValue() instanceof AID) {
    			AID aid = (AID) pair.getValue();
    			sb.append(pair.getKey().getLocalName() + ":" + aid.getLocalName());
    		}else {
    			sb.append(pair.getKey().getLocalName() + ":" + pair.getValue());
    		}    			
    		sb.append("\n");
        }
    	print(sb.toString());
	}
    /**
     * Clones a list without cloning the objects.
     * (This does not make much sense as new ArrayList<>(list) would achieve the same result.)
     */
    protected List<Policy> cloneList(List<Policy> list) {
    	List <Policy> newList = new ArrayList<>();
    	for(Policy o: list)
    		newList.add(o);
		return newList;    	
    }
    /**
     * Clones a set into a list without cloning the objects.
     */
    protected List<Entry<AID, Integer>> cloneSet(Set<Entry<AID, Integer>> set) {
    	List<Entry<AID, Integer>> newList = new ArrayList<>();
    	for(Entry<AID, Integer> entry : set) {
    		newList.add(entry);
    	}
		return newList;    	
    }
    /**
     * Find the most frequent value in a collection of AID.
     */
    protected AID findMostFrequent(Collection<AID> elements) {
        Map<AID, Integer> frequencies = createFrequencyMap(elements);
        return getMostLikedAID(frequencies, new ArrayList<>());
    }
    
    /**
     * Find the most frequent value in a collection of Policy.
     */
    protected Policy findMostFrequentPolicy(List<Policy> policies) {
    	Map<Policy, Integer> frequencies = new HashMap<>();
        for (Policy p : policies) {
            frequencies.put(p, frequencies.getOrDefault(p, 0) + 1);
        }
        int max = 0;
        Policy preferred = null;
        for(Entry<Policy, Integer> pair : frequencies.entrySet()) {
        	if(pair.getValue() > max) {
        		max = pair.getValue();
        		preferred = pair.getKey();
        	}
        }
        return preferred;
    }

	/**
	 * @param elements
	 * @return
	 */
	protected Map<AID, Integer> createFrequencyMap(Collection<AID> elements) {
		Map<AID, Integer> frequencies = new HashMap<>();
        for (AID el : elements) {
            frequencies.put(el, frequencies.getOrDefault(el, 0) + 1);
        }
		return frequencies;
	}
    /**
     * Prints messages if verbose mode is enabled.
     * @param text - Text to print.
     */
    protected void print(String text) {
    	if(verbose) {
    		System.out.println(name+": "+text);
    	}
    		
    }

	public AID getPlayer(String string) {
		for(AID player : players) {
			if(player.getLocalName().equals(string))
				return player;
		}
		return null;
	}
}
