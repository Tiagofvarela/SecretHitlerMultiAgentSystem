package agent.behaviour.liberals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import agent.behaviour.DefaultLiberalBehaviour;
import app.Policy;
import jade.core.AID;

public class AggressiveLiberal extends DefaultLiberalBehaviour {
    public AggressiveLiberal(AID id, List<AID> players, boolean verbose) {
        super(id, players, verbose);
        //addBias(-SMALL_CHANGE);
        
        name = id.getLocalName()+" (aggressive)";
    }

	@Override
	protected void processNewLiberal(Map<AID, Boolean> votes) {
        increaseOpinion(currentPresident, LARGEST_CHANGE);
        increaseOpinion(currentChancellor, LARGEST_CHANGE);
        print("Government passed liberal, increase "+LARGE_CHANGE);

        for (Entry<AID, Integer> pair : myOpinionOfOthers.entrySet()) {
            AID current = pair.getKey();
            boolean yes = votes.get(current);

            if (yes) {
                increaseOpinion(current, NORMAL_CHANGE);
                print(current.getLocalName()+" voted in favour of liberal government, increase "+SMALL_CHANGE);
            } else {
                decreaseOpinion(current, NORMAL_CHANGE);
                print(current.getLocalName()+" voted against liberal government, decrease "+SMALL_CHANGE);
            }
        }
    }
    
    /**
     * First checks the peek, and reduces the trust damage for those they suspect were betrayed.
     */
    @Override
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
        
        if(myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD 
        		&& myOpinionOfOthers.get(currentChancellor) < DISLIKE_THRESHOLD) {
        	print("I don't trust the entire government, so no further deliberations are necessary.");
        	return;
        }
        
        if(myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD) {
        	print("I dislike the president, "
        			+ "so I'll increase the chancellor "+SMALL_CHANGE);
        	increaseOpinion(currentChancellor, SMALL_CHANGE);
        }
        
        if(myOpinionOfOthers.get(currentChancellor) < DISLIKE_THRESHOLD) {
        	print("I dislike the chancellor, "
        			+ "so I'll increase the president "+SMALL_CHANGE);
        	increaseOpinion(currentPresident, SMALL_CHANGE);
        }
    }
    
    /**
     * Chooses least liked unless they're most voted.
     */
    @Override    
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {  	
//    	print("As president, I will choose who to investigate.");
//    	return aggressiveNegativeChoose(votes);
    	LinkedHashMap<AID, Integer> interest = new LinkedHashMap<>();
    	myOpinionOfOthers.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue())
        .forEachOrdered(x -> interest.put(x.getKey(), x.getValue()));
    	
    	return interest.entrySet().iterator().next().getKey();
	}

	/**
     * Chooses least liked unless they're most voted.
     */
    @Override
    protected AID presidentChooseDead(Map<AID, AID> votes) { // Nominates the least trustworthy player that was voted
//    	print("As president, I will choose who to execute.");
//    	return aggressiveNegativeChoose(votes);
    	AID playerLeastTrustworthy = null;
    	int minTrust = 100;
    	for (Entry<AID,AID> e: votes.entrySet()) {
    		int trustOther = myOpinionOfOthers.get(e.getValue());
    		if(trustOther < minTrust){
    			playerLeastTrustworthy = e.getKey();
    			minTrust = trustOther;
    			
    		}
    	}
    	print("As president, i will choose the least trustworthy player that was voted");
    	return playerLeastTrustworthy;
			
		}

    /**
     * Chooses least liked unless they're most voted.
     */
    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> votes) {
    	print("I will execute the person I most like.");
    	return getMostLikedAID(myOpinionOfOthers, Arrays.asList(id, currentChancellor));
	}
    
    /**
     * Believes the president if their claim is in agreement with their own suspicions.
     */
    @Override
    public void processInvestigation(AID investigated, boolean fascist) {
    	int value;
    	boolean myOpinion;
    	String claim;
    	if (fascist) {
    		value = 0;
    		myOpinion = myOpinionOfOthers.get(investigated) < DISLIKE_THRESHOLD;
    		claim = "fascist.";
    	}else {
    		value = 100;
        	myOpinion = myOpinionOfOthers.get(investigated) > LIKE_THRESHOLD;
    		claim = "liberal.";
    	}
    	if(myOpinion) {
    		print("I believe the claim because I suspected the same. "
    				+ "The other guy is "+claim);
    		myOpinionOfOthers.put(investigated, value);
    	}else {
    		print("I don't believe the claim because I didn't suspect the same. "
    				+ "Therefore, the president lied and is fascist for sure.");
    		myOpinionOfOthers.put(currentPresident, 0);
    	}
    }
    
    /**
     * If they love the president they may choose to activate a Power, given that it isn't
     * too dangerous to do so. Otherwise they enact Liberal if they can.
     */
//    @Override
//    protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {
//    	if(!policies.contains(Policy.Liberal) || !policies.contains(Policy.Fascist)) {
//    		print("Both cards are the same, so I'll just discard one.");
//    		policies.remove(0); 
//    		return policies;
//    	}
//		if(myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD) {
//			print("I trust the current president, so I'll see if I can give him a power.");
//			if(fascistTracker+1 == 2) {
//				policies.remove(Policy.Liberal);
//				print("This will activate investigation but not Hitler.");
//			}else if(fascistTracker+1 == 4) {
//				policies.remove(Policy.Liberal);
//				print("This will activate execution but isn't too close to loss.");
//			}else {
//				print("No, it's too dangerous.");
//			}
//			return policies;
//		}
//		return super.choosePoliciesChancellor(policies);
//	}
    /**
     * If they love the Chancellor they remove a fascist a let them do what they will.
     * If they don't, and have two liberal cards, they may choose to test the Chancellor if they're 
     * unsure about the Chancellor's affiliation.
     * If they have two fascist cards, they may choose to give themselves a power, if not too dangerous.
     * Otherwise they'll discard a fascist card.
     */
    @Override
	protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
//    	if(!policies.contains(Policy.Liberal) || !policies.contains(Policy.Fascist)) {
//    		print("All cards are the same, so I'll just discard one.");
//    		policies.remove(0); 
//    		return policies;
//    	}
//		if(myOpinionOfOthers.get(currentChancellor) > LIKE_THRESHOLD) {
//			print("I trust the current Chancellor so I'll just remove a fascist.");
//			return super.choosePoliciesPresident(policies);
//		}else if(myOpinionOfOthers.get(currentChancellor) > DISLIKE_THRESHOLD
//				&& findMostFrequentPolicy(policies).equals(Policy.Liberal)) {
//			print("I want to test the current Chancellor so I'll just remove a liberal.");
//			policies.remove(Policy.Liberal);
//			return policies;
//		}
//		print("I have two fascist cards, so let me see if I should unlock a power for myself.");
//		if(fascistTracker+1 == 2) {
//			policies.remove(Policy.Liberal);
//			print("This will activate investigation but not Hitler.");
//		}else if(fascistTracker+1 == 4) {
//			policies.remove(Policy.Liberal);
//			print("This will activate execution but isn't too close to loss.");
//		}else {
//			policies.remove(Policy.Fascist);
//			print("No, it's too dangerous.");
//		}
//		return policies;
    	int fascistPoliciesCounter = 0;
		for (Policy policy : policies) {
			if(policy.equals(Policy.Fascist))
				fascistPoliciesCounter++;
		}
		if(fascistPoliciesCounter == 0) {
			policies.remove(Policy.Liberal); // Deliver 2 liberal policies
			print("As president i have to deliver 2 liberal policies");
		}else if(fascistPoliciesCounter == 2) {
			policies.remove(Policy.Fascist); // Deliver 2 liberal policies
			print("As president i will deliver a fascist and liberal policy");
		}else if(fascistPoliciesCounter == 1) {
			policies.remove(Policy.Liberal); // Deliver 2 liberal policies
			print("As president i will deliver a fascist and liberal policy");
		}else if(fascistPoliciesCounter == 3) {
			policies.remove(Policy.Liberal); // Deliver 2 liberal policies
			print("As president i have to deliver 2 fascist policies");
		}
		return policies;
	}
    /**
     * If they hate the President they'll try to lie to increase suspicion on them.
     * Otherwise tells the truth.
     */
    @Override
    protected List<Policy> explainCardsChancellor(List<Policy> policies) {
    	if(myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD && newPolicy == Policy.Liberal) {
    		print("I hate the president, so I'll say they gave me a fascist card.");
    		return Arrays.asList(Policy.Liberal, Policy.Fascist);
    	}
    	print("Since I don't hate the President, I may as well tell the truth about what I received from them.");
        return policies;
    }
    /**
     * If they hate the Chancellor they'll try to lie to increase suspicion on them.
     * Otherwise tells the truth. 
     */
    @Override
    protected List<Policy> explainCardsPresident(List<Policy> policies) {
    	if(myOpinionOfOthers.get(currentChancellor) < DISLIKE_THRESHOLD ) {
    		print("I hate the Chancellor, so I'll say they had no choice.");
    		if(newPolicy == Policy.Liberal) {
    			return Arrays.asList(Policy.Liberal, Policy.Liberal, Policy.Liberal);
    		}
    		return Arrays.asList(Policy.Fascist, Policy.Fascist, Policy.Fascist);
    	}
    	print("Since I don't hate the Chancellor, "
    			+ "I may as well tell the truth about what I received from the deck.");
        return policies;
    }
    /**
     * If they hate the next president, they may claim the deck has a liberal card when it does not.
     * Otherwise tell the truth.
     */
    @Override
    protected List<Policy> explainCardsPeek(List<Policy> policies) {
    	if(myOpinionOfOthers.get(getNextPresident()) < DISLIKE_THRESHOLD && !policies.contains(Policy.Liberal)) {
    		print("Since I hate the next president, I'll say the cards have a liberal "
    				+ "when they don't.");
    		return Arrays.asList(Policy.Liberal, Policy.Fascist, Policy.Fascist);
    	}
    	print("Since I don't hate the President, I may as well tell the truth about what I peeked.");
        return policies;
    }
    
    /**
     * Aggressive checks if the president killed the same person the aggressive would have killed.
     */
    @Override
    protected void processDead(AID victim) {
		if(victim.equals(getMostDislikedAID(myOpinionOfOthers, new ArrayList() ))) {
			print("The president killed who I'd have killed. Increase "+LARGE_CHANGE);
			increaseOpinion(currentPresident, LARGE_CHANGE);
		}else {
			print("The president didn't kill who I'd have killed. Decrease "+LARGE_CHANGE);
			decreaseOpinion(currentPresident, LARGE_CHANGE);
		}
		//Ignores votes because they've already been processed.
	}  
	
	/**
	 * Believes the president if they trust the president.
	 */
	protected void processPeek(List<Policy> data) {
		peek.clear();
		//If I trust president, remember peek.
		if(myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD) {
			print("I trust the president, so I'll believe their peek.");
			for(Policy p : data) {
				peek.add(p);
			}
		}
    }
    
    @Override
    public void processGovernmentVotes(Map<AID, Boolean> governmentVotes) {
    	boolean likeGovernment = vote();    	
        for(Entry<AID, Boolean> pair: governmentVotes.entrySet()) {
        	print(pair.getKey().getLocalName()+" ");
        	if(pair.getValue() == likeGovernment) {
        		increaseOpinion(pair.getKey(), NORMAL_CHANGE);
        		print("Voted in favour of a government I liked."
        				+ " Increase "+NORMAL_CHANGE);
        	}else {
        		decreaseOpinion(pair.getKey(), NORMAL_CHANGE);
        		print("Voted against a government I liked."
        				+ " Decrease "+NORMAL_CHANGE);
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

	/**
	 * Checks if they voted like me, targeted a suspicious person or voted for me.
	 */
	protected void processVotesInvestigate(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey();
		    AID other = votes.get(current);
		    print(current.getLocalName()+" voted to investigate "+other.getLocalName());		    
		    if(votes.get(id).equals(other)) {
		    	print("They voted the same as me. "
		    			+ "Increase "+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);		    	
		    }else {
		    	print("They didn't vote the same as me. "
		    			+ "Decrease "+SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);	
		    }
		    if(myOpinionOfOthers.get(other) < DISLIKE_THRESHOLD){
		    	print("I suspect the target so this is a good choice. "
		    			+ "Increase voter and decrease target "+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    	decreaseOpinion(other, SMALL_CHANGE);
		    }
		    if(other.equals(id)) {
		    	print("They voted for me! "
		    			+ "Decrease "+NORMAL_CHANGE);
		    	decreaseOpinion(current, NORMAL_CHANGE);
		    }
		}
	}

	/**
	 * Checks if they voted like me, how much I trust both and whether they voted for me.
	 */
	@Override
	protected void processVotesExecute(Map<AID, AID> votes) {
		
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey();
		    AID target = votes.get(current);
		    print(current.getLocalName()+" voted to investigate "+target.getLocalName());		    
		    if(votes.get(id).equals(target)) {
		    	print("They voted the same as me. "
		    			+ "Increase "+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);		    	
		    }else {
		    	print("They didn't vote the same as me. "
		    			+ "Decrease "+SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);	
		    }
		    if(myOpinionOfOthers.get(target) > myOpinionOfOthers.get(current)) {
		    	print("I like the target more, so I'll increase the target and decrease the voter "
		    			+ "for having voted to kill someone I trust more.");
		    	increaseOpinion(target, SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);
		    }else {
		    	print("I like the voter more, so I'll decrease the target and increase the voter "
		    			+ "for having voted to kill someone I don't trust as much.");
		    	increaseOpinion(current, SMALL_CHANGE);
		    	decreaseOpinion(target, SMALL_CHANGE);
		    }
		    if(target.equals(id)) {
		    	print("They voted to kill me! "
		    			+ "Increase "+NORMAL_CHANGE);
		    	decreaseOpinion(current, NORMAL_CHANGE);
		    }
		}
	}

	/**
	 * Checks if they voted like me, how much I trust both and whether they voted for me.
	 */
	@Override
	protected void processVotesChancellor(Map<AID, AID> votes) {
		
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey();
		    AID target = votes.get(current);
		    print(current.getLocalName()+" voted to investigate "+target.getLocalName());		    
		    if(votes.get(id).equals(target)) {
		    	print("They voted the same as me. "
		    			+ "Increase "+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);		    	
		    }else {
		    	print("They didn't vote the same as me. "
		    			+ "Decrease "+SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);	
		    }
		    if(myOpinionOfOthers.get(target) > myOpinionOfOthers.get(current)) {
		    	print("I like the target more, so I'll increase the voter and decrease the target "
		    			+ "for being voted by someone I don't trust as much.");
		    	increaseOpinion(current, SMALL_CHANGE);
		    	decreaseOpinion(target, SMALL_CHANGE);
		    }else {
		    	print("I like the voter more, so I'll increase the target and decrease the voter "
		    			+ "for having voted on someone I don't trust as much.");
		    	increaseOpinion(target, SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);
		    }
		    if(target.equals(id)) {
		    	print("They voted for me! "
		    			+ "Increase "+NORMAL_CHANGE);
		    	decreaseOpinion(current, NORMAL_CHANGE);
		    }
		}
	}
    
    /**
     * Aggressive behaviour for negative choice. Chooses least liked that isn't majority.
     */
    private AID aggressiveNegativeChoose(Map<AID, AID> votes) {
    	List<AID> exceptions = new ArrayList<>();  
		exceptions.add(currentPresident);  	
    	Map<AID, Integer> frequencies = createFrequencyMap(votes.values());
    	int mostVoted = frequencies.get(getMostLikedAID(frequencies, new ArrayList<>()));
    	
    	AID myChoice; 
    	int myChoiceVotes;
    	do {
    		myChoice = getMostDislikedAID(myOpinionOfOthers, exceptions);
    		print(myChoice.getLocalName()+" is my current choice.");
    		print("I'll try to choose someone that isn't the majority.");
        	myChoiceVotes = frequencies.getOrDefault(myChoice, 0);
        	exceptions.add(myChoice);
    	}while(myChoiceVotes >= mostVoted);
    	print(myChoice.getLocalName()+" is my final choice.");
		return myChoice;
	}
    /**
     * SAME AS aggressiveNegativeChoose(); getMostDisliked -> getMostLiked
     */
    private AID aggressivePositiveChoose(Map<AID, AID> votes) {
		List<AID> exceptions = new ArrayList<>();  
		exceptions.add(currentPresident);
    	Map<AID, Integer> frequencies = createFrequencyMap(votes.values());
    	int mostVoted = frequencies.get(getMostLikedAID(frequencies, new ArrayList<>()));
    	
    	AID myChoice; 
    	int myChoiceVotes;
    	do {
    		myChoice = getMostLikedAID(myOpinionOfOthers, exceptions);
    		print(myChoice.getLocalName()+" is my current choice.");
    		print("I'll try to choose someone that isn't the majority.");
        	myChoiceVotes = frequencies.getOrDefault(myChoice, 0);
        	exceptions.add(myChoice);
    	}while(myChoiceVotes >= mostVoted);
    	print(myChoice.getLocalName()+" is my final choice.");
		return myChoice;
	}

}
