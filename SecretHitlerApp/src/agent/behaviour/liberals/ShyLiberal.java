package agent.behaviour.liberals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import agent.behaviour.DefaultLiberalBehaviour;
import app.Policy;
import jade.core.AID;

public class ShyLiberal extends DefaultLiberalBehaviour {
	
	private Map<AID, AID> positiveVotes = new HashMap<>();
	private Map<AID, AID> negativeVotes = new HashMap<>();
	
    public ShyLiberal(AID id, List<AID> players, boolean verbose) {
        super(id, players, verbose);
        //addBias(SMALL_CHANGE);
        name = id.getLocalName()+" (shy)";
    }
    
    @Override
    protected void processNewLiberal(Map<AID, Boolean> votes) {
        increaseOpinion(currentPresident, LARGE_CHANGE);
        increaseOpinion(currentChancellor, LARGE_CHANGE);
        print("Government passed liberal, increase "+LARGE_CHANGE);
    }
    
    /**
     * First checks the peek, and reduces the trust damage on those they trust.
     */
    protected void processNewFascist(Map<AID, Boolean> votes) {       	
        if(!peek.isEmpty()) {
        	print("I trust the President's previous peek.");
        	if(peek.contains(Policy.Liberal)) {
        		decreaseOpinion(currentPresident, LARGEST_CHANGE);
        		decreaseOpinion(currentChancellor, LARGEST_CHANGE);
        		print("Government could have passed liberal! Decrease both "+LARGEST_CHANGE);
        	}else {
        		print("Government had no choice, so no opinion change.");
        	}
            peek.clear();
        }else {
        	print("The president didn't peek or I didn't trust him.");
            decreaseOpinion(currentPresident, LARGE_CHANGE);
            decreaseOpinion(currentChancellor, LARGE_CHANGE);
            print("Government passed fascist, decrease "+LARGE_CHANGE);
        }
        
        if(myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD) {
        	print("I trust the president, "
        			+ "so I'll increase the president "+NORMAL_CHANGE);
        	increaseOpinion(currentPresident, NORMAL_CHANGE);
        }
        
        if(myOpinionOfOthers.get(currentChancellor) > LIKE_THRESHOLD) {
        	print("I trust the chancellor, "
        			+ "so I'll increase the chancellor "+NORMAL_CHANGE);
        	increaseOpinion(currentChancellor, NORMAL_CHANGE);
        }
    }
    
    @Override
    public void removePlayer(AID player) {
    	super.removePlayer(player);
    	positiveVotes.entrySet().removeIf(entry -> entry.getValue().equals(player));
    	negativeVotes.entrySet().removeIf(entry -> entry.getValue().equals(player));
    	/*chancellorVotes.remove(player);
    	executeVotes.remove(player);
    	peekVotes.remove(player);*/
    }
    
    /**
     * Chooses the person with most votes.
     */
    @Override    
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
    	print("I will investigate whoever got most votes (or randomly if there's a tie.)");
		return findMostFrequent(votes.values());
	}
    
    /**
     * Chooses the person with most votes.
     */
    @Override
    protected AID presidentChooseDead(Map<AID, AID> votes) {
    	print("I will execute whoever got most votes (or randomly if there's a tie.)");
    	return findMostFrequent(votes.values());
	}

    /**
     * Chooses the person with most votes.
     */
    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> votes) {
    	print("I will nominate whoever got most votes (or randomly if there's a tie.)");
    	return findMostFrequent(votes.values());
	}
    
    @Override
    protected void processVotesExecute(Map<AID, AID> votes) {
    	for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey(); 
		    AID target = votes.get(current);
		    print(current.getLocalName()+" voted to execute "+target.getLocalName());		    
		    if(myOpinionOfOthers.get(target) > myOpinionOfOthers.get(current)) {
		    	print("I like who they voted for, so I'll decrease the voter. "
		    			+ "Increase "+SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);
		    }else {
		    	print("I like the voter, so I'll decrease the target. "
		    			+ "Increase "+SMALL_CHANGE);
		    	decreaseOpinion(target, SMALL_CHANGE);
		    }
    	}
	}

	/**
	 * Checks if they targeted a suspicious person.
	 */
    @Override
	protected void processVotesInvestigate(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey();
		    AID other = votes.get(current);
		    print(current.getLocalName()+" voted to investigate "+other.getLocalName());		    
		    if(myOpinionOfOthers.get(other) < DISLIKE_THRESHOLD){
		    	print("I suspect the target so this is a good choice. "
		    			+ "Increase"+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    }
		}
	}

    /**
     * Increases the opinion of the one that is lowest.
     */
    @Override
	protected void processVotesChancellor(Map<AID, AID> votes) {
    	for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey(); 
		    AID target = votes.get(current);
		    print(current.getLocalName()+" voted to nominate "+target.getLocalName());
		    if(myOpinionOfOthers.get(target) > myOpinionOfOthers.get(current)) {
		    	print("I like who they voted for, so I'll increase the voter. "
		    			+ "Increase "+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    }else {
		    	print("I like the voter, so I'll increase the target. "
		    			+ "Increase "+SMALL_CHANGE);
		    	increaseOpinion(target, SMALL_CHANGE);
		    }
    	}
	}
    
    /**
     * Selects who to vote for based on votes last time.
     */
    @Override
    protected AID selectExecute() {
        if(negativeVotes.isEmpty())
            return super.selectExecute();
        print("I'll vote what everyone voted for last time, as long as it's not me...");
        AID lastTime = findMostFrequent(negativeVotes.values());
        return lastTime.equals(currentPresident) ? super.selectExecute() : lastTime;
    }

    //INVESTIGATE only occurs once per game.

    /**
     * Selects who to vote for based on votes last time.
     */
    /*@Override
	protected AID selectChancellor() {
    	if(positiveVotes.isEmpty())
    		return super.selectChancellor();
    	print("I'll vote what everyone voted for last time, as long as it's valid...");
    	AID lastTime = findMostFrequent(positiveVotes.values());
		return lastTime.equals(currentPresident) || lastTime.equals(currentChancellor) ? 
				super.selectExecute() : lastTime;
	}*/
    
    /**
     * Shy checks if they liked the victim.
     */
    @Override
    protected void processDead(AID victim) {
		int opinionOfVictim = myOpinionOfOthers.get(victim);		
		if (opinionOfVictim > LIKE_THRESHOLD) {
		    decreaseOpinion(currentPresident, LARGE_CHANGE);
		    print("President executed someone I liked. Decrease opinion of president "+LARGE_CHANGE);
		} else {
		    increaseOpinion(currentPresident, LARGE_CHANGE);
		    print("President executed someone I disliked. Increase opinion of president "+LARGE_CHANGE);
		}
		//Ignores votes because they've already been processed.
	}
    
    /**
     * Believes the president if they don't distrust them.
     */
    @Override
    protected void processPeek(List<Policy> data) {
		peek.clear();
		//If I trust president, remember peek.
		if(myOpinionOfOthers.get(currentPresident) > DISLIKE_THRESHOLD) {
			print("I don't hate the president, so I'll believe their peek.");
			for(Policy p : data) {
				peek.add(p);
			}
		}
    }
    
    /**
     * First checks if voted the same, and the updates opinion on government based on the voter.
     */
    @Override
    public void processGovernmentVotes(Map<AID, Boolean> governmentVotes) {
    	boolean likeGovernment = vote();    	
        for(Entry<AID, Boolean> pair: governmentVotes.entrySet()) {
        	print(pair.getKey().getLocalName()+" ");
        	if(pair.getValue() == likeGovernment) {
        		increaseOpinion(pair.getKey(), SMALL_CHANGE);
        		print("Voted in favour of a government I liked."
        				+ " Increase "+SMALL_CHANGE);
        	}else {
        		decreaseOpinion(pair.getKey(), SMALL_CHANGE);
        		print("Voted against a government I liked."
        				+ " Decrease "+SMALL_CHANGE);
        	}  
        	if(myOpinionOfOthers.get(pair.getKey()) > LIKE_THRESHOLD) {
            	if(pair.getValue()) { 
            		//Voted in favour
            		print("I trust the voter, so I'll trust the government a bit too. "
        					+ "Increase both members "+SMALL_CHANGE);
        			increaseOpinion(currentPresident, SMALL_CHANGE);
        			increaseOpinion(currentChancellor, SMALL_CHANGE);     		
            	}else { 
            		//Voted against
            		print("I trust the voter, so I'll distrust the government a bit too. "
        					+ "Decrease both members "+SMALL_CHANGE);
        			decreaseOpinion(currentPresident, SMALL_CHANGE);
        			decreaseOpinion(currentChancellor, SMALL_CHANGE);
            	}        		
        	}
        }
    }
    
    @Override
    public void processInvestigation(AID investigated, boolean fascist) {
    	int value;
    	int unlikeliness;
    	String claim;
    	if (fascist) {
    		value = 0;
        	unlikeliness = myOpinionOfOthers.get(investigated);
    		claim = "fascist.";
    	}else {
    		value = 100;
        	unlikeliness = 100-myOpinionOfOthers.get(investigated);
    		claim = "liberal.";
    	}
    	if(myOpinionOfOthers.get(currentPresident) > unlikeliness) {
    		print("I believe the current president more than the other person. I'll trust them. "
    				+ "The other guy is "+claim);
    		myOpinionOfOthers.put(investigated, value);
    	}
    }
}
