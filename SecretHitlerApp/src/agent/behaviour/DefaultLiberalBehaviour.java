package agent.behaviour;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import app.Policy;
import jade.core.AID;

public class DefaultLiberalBehaviour extends PlayerBehaviour {

    //Opinion Changes
	protected int LARGEST_CHANGE = 20;
    protected int LARGE_CHANGE = 15;
    protected int NORMAL_CHANGE = 10;
    protected int SMALL_CHANGE = 5;

    //Thresholds
    protected int DISLIKE_THRESHOLD = 40;
    protected int LIKE_THRESHOLD = 60;

    public DefaultLiberalBehaviour(AID id, List<AID> players, boolean verbose) {
        super(id, players, verbose);
        name = name+" (default)";
    }
    
    protected void addBias(int bias) {
    	//Opinion Changes
        LARGEST_CHANGE += bias;
        LARGE_CHANGE += bias;
        NORMAL_CHANGE += bias;
        SMALL_CHANGE += bias;

        //Thresholds
        DISLIKE_THRESHOLD -= bias*2;	// bias > 0 means it's harder to dislike; bias < 0 means it's easier.
        LIKE_THRESHOLD -= bias*2;
    }

    @Override
    public boolean isHitler() {
        return false;
    }

    @Override
    public boolean isFascist() {
        return false;
    }
    
    protected void processNewLiberal(Map<AID, Boolean> votes) {
        increaseOpinion(currentPresident, LARGE_CHANGE);
        increaseOpinion(currentChancellor, LARGE_CHANGE);
        print("Government passed liberal, increase "+LARGE_CHANGE);

        for (Entry<AID, Integer> pair : myOpinionOfOthers.entrySet()) {
            AID current = pair.getKey();
            boolean yes = votes.get(current);

            if (yes) {
                increaseOpinion(current, SMALL_CHANGE);
                print(current.getLocalName()+" voted in favour of liberal government, increase "+SMALL_CHANGE);
            } else {
                decreaseOpinion(current, SMALL_CHANGE);
                print(current.getLocalName()+" voted against liberal government, decrease "+SMALL_CHANGE);
            }
        }
    }

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

        for (Entry<AID, Integer> pair : myOpinionOfOthers.entrySet()) {
            AID current = pair.getKey();
            boolean yes = votes.get(current);

            if (yes) {
                decreaseOpinion(current, SMALL_CHANGE);
                print(current.getLocalName()+" voted in favour of fascist government, decrease "+SMALL_CHANGE);
            } else {
                increaseOpinion(current, SMALL_CHANGE);
                print(current.getLocalName()+" voted against fascist government, increase "+SMALL_CHANGE);
            }
        }
    }
    
    public void removePlayer(AID player) {
    	myOpinionOfOthers.remove(player);
    	players.remove(player);
    }
    
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
    	print("I will investigate the person I most dislike.");
		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id));
	}
    
    protected AID presidentChooseDead(Map<AID, AID> votes) {
    	print("I will execute the person I most dislike.");
		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id));
	}

    protected AID presidentChooseChancellor(Map<AID, AID> votes) {
    	print("I will nominate the person I most like (but not previous Chancellor).");
		return getMostLikedAID(myOpinionOfOthers, Arrays.asList(id, currentChancellor));
	}

    protected List<Policy> choosePoliciesChancellor(List<Policy> modify) {
		modify.remove(Policy.Fascist);
		print("As Chancellor, I'll discard one fascist card and enact the other.");
		return modify;	//Returns either one or two cards.
	}

	protected List<Policy> choosePoliciesPresident(List<Policy> modify) {
		modify.remove(Policy.Fascist);
		print("As President, I'll discard one fascist card and send the Chancellor the others.");
		if (modify.size() > 2) {
			modify.remove(0);
			print("But since I got three liberals, I'll discard one of those.");
		}
		return modify;
	}
    
    protected List<Policy> explainCardsChancellor(List<Policy> policies) {
    	print("As Chancellor, I'll tell the truth about the policies the President gave to me.");
        return policies;
    }
    
    protected List<Policy> explainCardsPresident(List<Policy> policies) {
    	print("As President, I'll tell the truth about the policies I got from the deck.");
        return policies;
    }
    
    protected List<Policy> explainCardsPeek(List<Policy> policies) {
    	print("As President, I'll tell the truth about the policies I peeked.");
        return policies;
    }
    
    public boolean explainInvestigation(AID investigated, boolean fascist) {
    	print("As President, I'll tell the truth on whether "+investigated.getLocalName()+" is a fascist.");
        return fascist;
    }
    
    protected AID selectExecute() {
    	print("I want to execute the person I most dislike.");
		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id, currentPresident));
	}

	protected AID selectInvestigate() {
		print("I want to investigate the person I most dislike (or myself if I'm suspicious).");
		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(currentPresident));
	}

	protected AID selectChancellor() {
		print("I want to nominate the person I most like (but not previous Chancellor).");
		return getMostLikedAID(myOpinionOfOthers, Arrays.asList(currentChancellor, currentPresident));
	}
	
	protected void processDead(AID data) {
		int opinionOfVictim = myOpinionOfOthers.get(data);
		if (opinionOfVictim > LIKE_THRESHOLD) {
		    decreaseOpinion(currentPresident, LARGE_CHANGE);
		    print("President executed someone I liked. Decrease opinion of president "+LARGE_CHANGE);
		} else if (opinionOfVictim < DISLIKE_THRESHOLD) {
		    increaseOpinion(currentPresident, LARGE_CHANGE);
		    print("President executed someone I disliked. Increase opinion of president "+LARGE_CHANGE);
		}
		//Ignores votes because they've already been processed.
	}
	
	protected void processPeek(List<Policy> data) {
		peek.clear();
		//If I trust president, remember peek.
		if(myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD-NORMAL_CHANGE) { //- bias*3
			peek = data;
			print("I trust the President enough to believe the peeked cards he claims.");
		}
    }

    public void processGovernmentVotes(Map<AID, Boolean> governmentVotes) {
    	boolean likeGovernment = vote();
        for(Entry<AID, Boolean> pair: governmentVotes.entrySet()) {
        	if(pair.getValue() == likeGovernment) {
        		increaseOpinion(pair.getKey(), SMALL_CHANGE);
        		print(pair.getKey().getLocalName()+" Voted in favour of a government I liked."
        				+ " Increase "+SMALL_CHANGE);
        	}else {
        		decreaseOpinion(pair.getKey(), SMALL_CHANGE);
        		print(pair.getKey().getLocalName()+" Voted against a government I liked."
        				+ " Decrease "+SMALL_CHANGE);
        	}
        }
    }
	
    /**
     * Votes in favour if both members are okay, or if one is great even if the other
     * isn't that good.
     */
	public boolean vote() {
        boolean bothOkay = myOpinionOfOthers.get(currentPresident) > DISLIKE_THRESHOLD
                && myOpinionOfOthers.get(currentChancellor) > DISLIKE_THRESHOLD;

        boolean chancellorGreat = myOpinionOfOthers.get(currentChancellor) > LIKE_THRESHOLD
                && myOpinionOfOthers.get(currentPresident) > DISLIKE_THRESHOLD - LARGEST_CHANGE;

        boolean presidentGreat = myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD
                && myOpinionOfOthers.get(currentChancellor) > DISLIKE_THRESHOLD - LARGEST_CHANGE;
        if(verbose) {
        	if(bothOkay) {
            	print("Both candidates are okay on my book.");
            }else if(chancellorGreat){
            	print("The Chancellor is great, even if the President isn't.");
            }else if(presidentGreat){
            	print("The President is great, even if the Chancellor isn't.");
            }else {
            	print("I can't trust this Government.");
            }
        }        
        return bothOkay || chancellorGreat || presidentGreat;
        //Belligerent has harsh bias. They must like government.
        //After 3rd round or after two fascists the AI kicks in.
    }

	/**
	 * Depending on opinion of person voted for, likes or dislikes voter more.
	 */
	protected void processVotesExecute(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : myOpinionOfOthers.entrySet()) {
		    AID current = pair.getKey();
		    AID other = votes.get(current);
		    print(current.getLocalName()+" voted to execute "+other.getLocalName());
		    processNegativeVote(current, other);
		}
	}

	/**
	 * Depending on opinion of person voted for, likes or dislikes voter more.
	 */
	protected void processVotesInvestigate(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : myOpinionOfOthers.entrySet()) {
		    AID current = pair.getKey();
		    AID other = votes.get(current);
		    print(current.getLocalName()+" voted to investigate "+other.getLocalName());
		    processNegativeVote(current, other);
		}
	}

	/**
	 * Depending on opinion of person voted for, likes or dislikes voter more.
	 */
	protected void processVotesChancellor(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : myOpinionOfOthers.entrySet()) {
		    AID current = pair.getKey(); 
		    AID other = votes.get(current);
		    print(current.getLocalName()+" voted to nominate "+other.getLocalName());
		    processPositiveVote(current, other);
		}
	}
	/**
     * We like current and they voted for other. Adjust opinion of both.
     *
     * @param current - Agent that voted.
     * @param other   - Agent who current voted for.
     */
    private void processPositiveVote(AID current, AID other) {        
        int currentOpinion = myOpinionOfOthers.get(current);
        int otherOpinion = myOpinionOfOthers.get(other);

        if (other.equals(id)) {            
            increaseOpinion(current, LARGE_CHANGE);
            print("They voted for me! Increase "+LARGE_CHANGE);
        }
        //TODO: Consider that not voting for trustworthy person may be betrayal.
        if (current.equals(other)) {
        	print("They voted for themselves. I don't care either way.");
        	return;
        }            

        //LIKE CURRENT
        if (currentOpinion > LIKE_THRESHOLD) {
            print("I like "+current.getLocalName()+" so I'll increase "
            		+other.getLocalName()+" by "+SMALL_CHANGE);
            
            if (otherOpinion > LIKE_THRESHOLD) { 
            	increaseOpinion(current, NORMAL_CHANGE);
            	print("And I also like "+other.getLocalName()+" so I'll increase the voter "+NORMAL_CHANGE);
            }else if (otherOpinion < DISLIKE_THRESHOLD) { 
            	decreaseOpinion(current, NORMAL_CHANGE);
            	print("But I don't like "+other.getLocalName()+" so I'll decrease the voter "+NORMAL_CHANGE);
            }
            increaseOpinion(other, SMALL_CHANGE);
            return;
        }
        //DISLIKE CURRENT
        if (currentOpinion < DISLIKE_THRESHOLD) {
        	print("I dislike "+current.getLocalName()+" so I'll decrease "
            		+other.getLocalName()+" by "+SMALL_CHANGE);
        	
            if (otherOpinion > LIKE_THRESHOLD) { 
            	increaseOpinion(current, NORMAL_CHANGE);
            	print("But I like "+other.getLocalName()+" so I'll increase the voter "+NORMAL_CHANGE);
            }
            else if (otherOpinion < DISLIKE_THRESHOLD) { 
            	decreaseOpinion(current, NORMAL_CHANGE);
            	print("And I also dislike "+other.getLocalName()+" so I'll decrease the voter "+NORMAL_CHANGE);
            }
            decreaseOpinion(other, SMALL_CHANGE);
            return;
        }
        print("I have no particular opinion on "+current.getLocalName());
        //INDIFERENT CURRENT
        if (otherOpinion > LIKE_THRESHOLD) { 
        	increaseOpinion(current, SMALL_CHANGE);
        	print("I like "+other.getLocalName()+" so I'll increase the voter "+SMALL_CHANGE);
        }
        else if (otherOpinion < DISLIKE_THRESHOLD) { 
        	decreaseOpinion(current, SMALL_CHANGE);
        	print("I dislike "+other.getLocalName()+" so I'll decrease the voter "+SMALL_CHANGE);
        }
    }

    /**
     * We dislike current and they voted for other. Adjust opinion of both.
     *
     * @param current - Agent that voted.
     * @param other   - Agent who current voted for.
     */
    private void processNegativeVote(AID current, AID other) {      
        int currentOpinion = myOpinionOfOthers.get(current);
        int otherOpinion = myOpinionOfOthers.get(other);

        if (other.equals(id)) {            
            decreaseOpinion(current, LARGE_CHANGE);
            print("They voted for me! Decrease "+LARGE_CHANGE);
        }           

        //LIKE CURRENT
        if (currentOpinion > LIKE_THRESHOLD) {
            print("I like "+current.getLocalName()+" so I'll decrease "
            		+other.getLocalName()+" by "+SMALL_CHANGE);
            
            if (otherOpinion > LIKE_THRESHOLD) { 
            	decreaseOpinion(current, NORMAL_CHANGE);
            	print("And I also like "+other.getLocalName()+" so I'll decrease the voter "+NORMAL_CHANGE);
            }else if (otherOpinion < DISLIKE_THRESHOLD) { 
            	increaseOpinion(current, NORMAL_CHANGE);
            	print("But I don't like "+other.getLocalName()+" so I'll increase the voter "+NORMAL_CHANGE);
            }
            decreaseOpinion(other, SMALL_CHANGE);
            return;
        }
        //DISLIKE CURRENT
        if (currentOpinion < DISLIKE_THRESHOLD) {
        	print("I dislike "+current.getLocalName()+" so I'll increase "
            		+other.getLocalName()+" by "+SMALL_CHANGE);
        	
            if (otherOpinion > LIKE_THRESHOLD) { 
            	decreaseOpinion(current, NORMAL_CHANGE);
            	print("But I like "+other.getLocalName()+" so I'll decrease the voter "+NORMAL_CHANGE);
            }
            else if (otherOpinion < DISLIKE_THRESHOLD) { 
            	increaseOpinion(current, NORMAL_CHANGE);
            	print("And I also dislike "+other.getLocalName()+" so I'll increase the voter "+NORMAL_CHANGE);
            }
            increaseOpinion(other, SMALL_CHANGE);
            return;
        }
        print("I have no particular opinion on "+current.getLocalName());
        //INDIFERENT CURRENT
        if (otherOpinion > LIKE_THRESHOLD) { 
        	decreaseOpinion(current, SMALL_CHANGE);
        	print("I like "+other.getLocalName()+" so I'll decrease the voter "+SMALL_CHANGE);
        }
        else if (otherOpinion < DISLIKE_THRESHOLD) { 
        	increaseOpinion(current, SMALL_CHANGE);
        	print("I dislike "+other.getLocalName()+" so I'll increase the voter "+SMALL_CHANGE);
        }
    }
    public void processInvestigation(AID investigated, boolean fascist) {
    	if(myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD) {
    		print("I hate the current president, so I'll assume the opposite is true.");
    		fascist = !fascist;
    	}else if(myOpinionOfOthers.get(currentPresident) < LIKE_THRESHOLD-NORMAL_CHANGE) {
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

    public void processPolicyJustification(Policy newPolicy,
                                    List<Policy> chancellorCards, List<Policy> presidentCards) {

        boolean fascist = newPolicy == Policy.Fascist;
        //President is for sure honest
        if (currentPresident.equals(id)) {
        	print("I was President.");
            //Chancellor passed fascist when he could've passed Liberal.
            if (fascist && choosePolicies(presidentCards).contains(Policy.Liberal)) {
            	print("And I gave him a liberal policy!");
                voluntarilyChoseFascist();
            }
            return;
        }
        //Someone's lying, and only fascists lie.
        if (!consistentPolicies(chancellorCards, presidentCards)) {
            decreaseOpinion(currentPresident, 50);
            decreaseOpinion(currentChancellor, 50);
            print("Either the President or the Chancellor are lying so I'll decrease both 50");
        }

        if (fascist) {
        	print("They passed a fascist policy.");
            if (chancellorCards.contains(Policy.Liberal)) {
            	print("The Chancellor admitted he had a Liberal policy.");
                voluntarilyChoseFascist();
            }else if (consistentPolicies(chancellorCards, presidentCards)) {
                increaseOpinion(currentPresident, SMALL_CHANGE);
                increaseOpinion(currentChancellor, SMALL_CHANGE);
                print("The President and Chancellor may not be lying, so maybe they couldn't "
                		+ "have passed a liberal policy. Increase both "+SMALL_CHANGE);
            }
        }else {
        	print("Well, they passed a liberal policy, so I have nothing further to comment.");
        }
    }
    
    protected void voluntarilyChoseFascist() {   
    	print("The Chancellor must have chosen fascist on purpose.");
        //Too dangerous
        if (fascistTracker == 1 || fascistTracker == 5) {
        	decreaseOpinion(currentChancellor, 50);
        	print("Passing a fascist card now only helps the fascists. "
        			+ "There's already "+fascistTracker+" in play now."
        					+ " Decrease 50");
        	return;
        }            
        
        int baseReduction = 10;
    	if(isPresident()) {      
            baseReduction += 5;
            print("Since I'm president, he's actually helping me with a power.");
    	}

    	if (fascistTracker == 3) {
    		//Unlocks power and Hitler
    		decreaseOpinion(currentChancellor, 50-baseReduction);  
    		print("This unlocks a power, but will allow Hitler to win as Chancellor! "
    				+ "Decrease "+(50-baseReduction));
    	}else if (fascistTracker == 4 || fascistTracker == 2) {
    		//Unlocks power
    		decreaseOpinion(currentChancellor, 50-baseReduction*2);
    		print("This unlocks a power, but still... Decrease "+(50-baseReduction*2));
    	} 
    }

    /**
     * Checks if chancellor's claim is consistent with the president's.
     *
     * @param chancellorCards
     * @param presidentCards
     * @return Whether chancellorCards is contained in presidentCards.
     */
    private boolean consistentPolicies(List<Policy> chancellorCards, List<Policy> presidentCards) {
        List<Policy> pCards = cloneList(presidentCards);
        for (Policy policy : chancellorCards) {
            if (pCards.contains(policy))
                pCards.remove(policy);
            else
                return false;
        }
        return true;
    }
}
