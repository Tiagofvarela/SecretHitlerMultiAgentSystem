package agent.behaviour.liberals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import agent.behaviour.DefaultLiberalBehaviour;
import app.Policy;
import jade.core.AID;

public class AggressiveLiberal extends DefaultLiberalBehaviour {
    public AggressiveLiberal(AID id, List<AID> players, boolean verbose) {
        super(id, players, verbose);
        addBias(-SMALL_CHANGE);
        
        name = id.getLocalName()+" (aggressive)";
    }
    
    /**
     * Chooses least liked unless they're most voted.
     */
    @Override    
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {  	
    	print("As president, I will choose who to investigate.");
    	return aggressiveNegativeChoose(votes);
	}

	/**
     * Chooses least liked unless they're most voted.
     */
    @Override
    protected AID presidentChooseDead(Map<AID, AID> votes) {
    	print("As president, I will choose who to execute.");
    	return aggressiveNegativeChoose(votes);
	}	

    /**
     * Chooses least liked unless they're most voted.
     */
    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> votes) {
    	print("As president, I will choose who to nominate as Chancellor.");
    	return aggressivePositiveChoose(votes);
	}
    /**
     * Believes the president if they love him, otherwise ignores.
     * If they hate the president, they assume he said the opposite instead.
     */
    @Override
    public void processInvestigation(AID investigated, boolean fascist) {    	
    	if(myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD) {
    		print("I hate the current president, so I'll assume the opposite is true.");
    		fascist = !fascist;
    	}else if(myOpinionOfOthers.get(currentPresident) < LIKE_THRESHOLD) {
    		print("I don't really believe the current president, so I'll ignore this.");
    		return;
    	}
        if (fascist) {
            myOpinionOfOthers.put(investigated, 0);
            print("I'll remember "+investigated.getLocalName()+" is fascist.");
        } else {
            myOpinionOfOthers.put(investigated, 100);
            print("I'll remember "+investigated.getLocalName()+" is liberal.");
        }
    }
    /**
     * If they love the president they may choose to activate a Power, given that it isn't
     * too dangerous to do so. Otherwise they enact Liberal if they can.
     */
    @Override
    protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {
    	if(!policies.contains(Policy.Liberal) || !policies.contains(Policy.Fascist)) {
    		print("Both cards are the same, so I'll just discard one.");
    		policies.remove(0); 
    		return policies;
    	}
		if(myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD) {
			print("I trust the current president, so I'll see if I can give him a power.");
			if(fascistTracker+1 == 2) {
				policies.remove(Policy.Liberal);
				print("This will activate investigation but not Hitler.");
			}else if(fascistTracker+1 == 4) {
				policies.remove(Policy.Liberal);
				print("This will activate execution but isn't too close to loss.");
			}else {
				print("No, it's too dangerous.");
			}
			return policies;
		}
		return super.choosePoliciesChancellor(policies);
	}
    /**
     * If they love the Chancellor they remove a fascist a let them do what they will.
     * If they don't, and have two liberal cards, they may choose to test the Chancellor if they're 
     * unsure about the Chancellor's affiliation.
     * If they have two fascist cards, they may choose to give themselves a power, if not too dangerous.
     * Otherwise they'll discard a fascist card.
     */
    @Override
	protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
    	if(!policies.contains(Policy.Liberal) || !policies.contains(Policy.Fascist)) {
    		print("All cards are the same, so I'll just discard one.");
    		policies.remove(0); 
    		return policies;
    	}
		if(myOpinionOfOthers.get(currentChancellor) > LIKE_THRESHOLD) {
			print("I trust the current Chancellor so I'll just remove a fascist.");
			return super.choosePoliciesPresident(policies);
		}else if(myOpinionOfOthers.get(currentChancellor) > DISLIKE_THRESHOLD
				&& findMostFrequentPolicy(policies).equals(Policy.Liberal)) {
			print("I want to test the current Chancellor so I'll just remove a liberal.");
			policies.remove(Policy.Liberal);
			return policies;
		}
		print("I have two fascist cards, so let me see if I should unlock a power for myself.");
		if(fascistTracker+1 == 2) {
			policies.remove(Policy.Liberal);
			print("This will activate investigation but not Hitler.");
		}else if(fascistTracker+1 == 4) {
			policies.remove(Policy.Liberal);
			print("This will activate execution but isn't too close to loss.");
		}else {
			policies.remove(Policy.Fascist);
			print("No, it's too dangerous.");
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
