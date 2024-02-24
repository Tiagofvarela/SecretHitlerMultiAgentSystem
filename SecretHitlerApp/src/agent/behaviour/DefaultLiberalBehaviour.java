package agent.behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import app.Policy;
import jade.core.AID;

public class DefaultLiberalBehaviour extends PlayerBehaviour {

    //Opinion Changes
	protected int LARGEST_CHANGE = 10;
    protected int LARGE_CHANGE = 7;
    protected int NORMAL_CHANGE = 3;
    protected int SMALL_CHANGE = 1;

    //Thresholds
    protected int DISLIKE_THRESHOLD = 40;
    protected int FASCIST_THRESHOLD = 20;
    protected int LIKE_THRESHOLD = 60;
    protected int LIBERAL_THRESHOLD = 80;
    protected int ROUND_THRESHOLD = 3;
    protected int VOTE_THRESHOLD = 4;

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
        for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
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
        
        if(myOpinionOfOthers.get(currentPresident) < FASCIST_THRESHOLD 
        		&& myOpinionOfOthers.get(currentChancellor) < FASCIST_THRESHOLD) {
        	print("The government is clearly fascist, so no further deliberations are necessary.");
        	return;
        }
        
        if((myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD 
        		&& myOpinionOfOthers.get(currentChancellor) < DISLIKE_THRESHOLD)
        		|| myOpinionOfOthers.get(currentChancellor) < FASCIST_THRESHOLD) {
        	print("I trust the president but not the chancellor, "
        			+ "so I'll increase the president "+NORMAL_CHANGE);
        	increaseOpinion(currentPresident, NORMAL_CHANGE);
        }
        
        if((myOpinionOfOthers.get(currentChancellor) > LIKE_THRESHOLD 
        		&& myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD)
        		|| myOpinionOfOthers.get(currentPresident) < FASCIST_THRESHOLD) {
        	print("I trust the chancellor but not the president, "
        			+ "so I'll increase the chancellor "+NORMAL_CHANGE);
        	increaseOpinion(currentChancellor, NORMAL_CHANGE);
        }
        
        if(myOpinionOfOthers.get(currentPresident) > LIKE_THRESHOLD 
        		&& myOpinionOfOthers.get(currentChancellor) > LIKE_THRESHOLD) {
        	print("However, government may not have had a choice, and I do like both of them. "
        			+ "Increase both "+SMALL_CHANGE);
        	increaseOpinion(currentPresident, SMALL_CHANGE);
            increaseOpinion(currentChancellor, SMALL_CHANGE);
        }
    }
    
    public void removePlayer(AID player) {
    	myOpinionOfOthers.remove(player);
    	players.remove(player);
    }
    
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
    	print("I will investigate the person second most i dislike.");
		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id,getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id))));
	}
    
    protected AID presidentChooseDead(Map<AID, AID> votes) {
//    	print("I will execute the person I most dislike.");
//    	return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id));
    	
    	Map<AID, Integer> freq = createFrequencyMap(votes.values());
    	int maxVotes = -1;
    	AID playerMostVoted = null;
    	for (Entry<AID, Integer> entry : freq.entrySet()) {
    		if(entry.getValue()> maxVotes) {
    			maxVotes = entry.getValue();
    			playerMostVoted = entry.getKey();
    		}
			
    	}
    	if (maxVotes == VOTE_THRESHOLD) {
    		return playerMostVoted;
    	}else
    		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id, currentChancellor));
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
	
	protected void processDead(AID victim) {
		int opinionOfVictim = myOpinionOfOthers.get(victim);
		if(opinionOfVictim > FASCIST_THRESHOLD 
				&& myOpinionOfOthers.get(getMostDislikedAID(myOpinionOfOthers, new ArrayList())) < FASCIST_THRESHOLD) {
			print("The president didn't kill an obvious fascist. Decrease "+NORMAL_CHANGE);
			decreaseOpinion(currentPresident, NORMAL_CHANGE);
		}else if(opinionOfVictim > LIBERAL_THRESHOLD){
			print("The president killed an obvious liberal. Decrease "+LARGEST_CHANGE);
			decreaseOpinion(currentPresident, LARGEST_CHANGE);
		}
		
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
	 * Believes the president if the claim isn't too unlikely.
	 */
	protected void processPeek(List<Policy> data) {
		peek.clear();
		int count = 0;
		for(Policy p : data) {
			if(p == Policy.Liberal)
				count++;
		}
		int unlikeliness = 0;
		if(count == 0) {		//Three fascist
			unlikeliness = fascistTracker*10;
		}else if(count == 1) {	//Two fascist
			unlikeliness = fascistTracker*5;
		}else if(count == 2) {	//Two liberal
			unlikeliness = liberalTracker*5;
		}else {					//Three liberal
			unlikeliness = liberalTracker*10;
		}		
		
		//If I trust president, remember peek.
		if(myOpinionOfOthers.get(currentPresident) - unlikeliness > 50) {
			for(Policy p : data) {
				peek.add(p);
			}
			print("I trust the President enough to believe the peeked cards they claim. "
					+ "Their claim was "+unlikeliness+" unlikely.");
		}
    }

	/**
	 * Process government votes, first by comparing them with their own, then by checking if
	 * they like the people involved in government and the voter.
	 */
    public void processGovernmentVotes(Map<AID, Boolean> governmentVotes) {
    	boolean likeGovernment = vote();    	
        for(Entry<AID, Boolean> pair: governmentVotes.entrySet()) {
        	print(pair.getKey().getLocalName()+" ");
        	if(pair.getValue() == likeGovernment) {
        		increaseOpinion(pair.getKey(), SMALL_CHANGE);
        		print("Voted much like me."
        				+ " Increase "+SMALL_CHANGE);
        	}else {
        		decreaseOpinion(pair.getKey(), SMALL_CHANGE);
        		print("Voted differently from me."
        				+ " Decrease "+SMALL_CHANGE);
        	}        	
        	if(pair.getValue()) { 
        		//Voted in favour        		
        		if(myOpinionOfOthers.get(currentPresident) < FASCIST_THRESHOLD ||
        				myOpinionOfOthers.get(currentChancellor) < FASCIST_THRESHOLD) {
        			print("Voted in favour when someone in government is obviously fascist."
        				+" Decrease "+SMALL_CHANGE);
        			decreaseOpinion(pair.getKey(), SMALL_CHANGE);
        		}
        		
        		if(myOpinionOfOthers.get(currentPresident) > LIBERAL_THRESHOLD ||
        				myOpinionOfOthers.get(currentChancellor) > LIBERAL_THRESHOLD) {
        			print("Voted in favour when someone in government is obviously liberal."
        					+" Increase "+SMALL_CHANGE);
        			increaseOpinion(pair.getKey(), SMALL_CHANGE);
        		}
        		
        		if(myOpinionOfOthers.get(pair.getKey()) > LIKE_THRESHOLD) {
        			print("I trust the voter, so I'll trust the government a bit too. "
        					+ "Increase both members "+SMALL_CHANGE);
        			increaseOpinion(currentPresident, SMALL_CHANGE);
        			increaseOpinion(currentChancellor, SMALL_CHANGE);
        		}      		
        	}else { 
        		//Voted against
        		if(myOpinionOfOthers.get(currentPresident) < FASCIST_THRESHOLD ||
        				myOpinionOfOthers.get(currentChancellor) < FASCIST_THRESHOLD) {
        			print("Voted against when someone in government is obviously fascist."
        				+" Increase "+SMALL_CHANGE);
        			increaseOpinion(pair.getKey(), SMALL_CHANGE);
        		}
        		
        		if(myOpinionOfOthers.get(currentPresident) > LIBERAL_THRESHOLD ||
        				myOpinionOfOthers.get(currentChancellor) > LIBERAL_THRESHOLD) {
        			print("Voted in favour when someone in government is obviously liberal."
        					+" Decrease "+SMALL_CHANGE);
        			decreaseOpinion(pair.getKey(), SMALL_CHANGE);
        		}
        		
        		if(myOpinionOfOthers.get(pair.getKey()) > LIKE_THRESHOLD) {
        			print("I trust the voter, so I'll distrust the government a bit too. "
        					+ "Decrease both members "+SMALL_CHANGE);
        			decreaseOpinion(currentPresident, SMALL_CHANGE);
        			decreaseOpinion(currentChancellor, SMALL_CHANGE);
        		}
        	}
        }
    }
	
    public boolean vote() {
    	if(round > ROUND_THRESHOLD)
    		print("It's not early in the game anymore, so I'll be strict with trusting the government.");
    	else
    		print("Since it's still early in the game, I'll trust the government more easily.");
		boolean president_okay = myOpinionOfOthers.get(currentPresident) > (round > ROUND_THRESHOLD ? LIKE_THRESHOLD : 49);
		boolean president_bad = myOpinionOfOthers.get(currentPresident) < DISLIKE_THRESHOLD;
		boolean chancellor_okay = myOpinionOfOthers.get(currentChancellor) > (round > ROUND_THRESHOLD ? LIKE_THRESHOLD : 49);
		boolean chancellor_bad = myOpinionOfOthers.get(currentChancellor) < DISLIKE_THRESHOLD;
		
		if((president_okay || currentPresident == id) && (!chancellor_bad || currentChancellor == id)) {
			print("I have a good opinion on the President, and a not so bad one on the Chancellor, so i vote Yes.");
			return true;
		}
		if((chancellor_okay || currentChancellor == id) && (!president_bad || currentPresident == id)) {
			print("I have a good opinion on the Chancellor, and a not so bad one on the President, so i vote Yes.");
			return true;
		}
		print("I don't have a good opinion on both President and Chancellor.");
		return false;
	}

	/**
	 * Depending on opinion of person voted for, likes or dislikes voter more.
	 */
	protected void processVotesExecute(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey(); 
		    AID target = votes.get(current);
		    print(current.getLocalName()+" voted to execute "+target.getLocalName());
		    
		    AID mostDisliked = getMostDislikedAID(myOpinionOfOthers, Arrays.asList(currentPresident));
		    //There was someone more suspicious.
		    if(myOpinionOfOthers.get(mostDisliked) < FASCIST_THRESHOLD 
		    		&& myOpinionOfOthers.get(target) > FASCIST_THRESHOLD) {
		    	print("There was an obvious fascist they could have voted to kill but they did not. "
		    			+ "Decrease "+NORMAL_CHANGE);
		    	decreaseOpinion(current, NORMAL_CHANGE);
		    }		
		    //Target is liberal.
		    if(myOpinionOfOthers.get(target) > LIBERAL_THRESHOLD) {
		    	print("They voted to kill somone trustworthy. "
		    			+ "Decrease "+LARGE_CHANGE);
		    	decreaseOpinion(current, LARGE_CHANGE);
		    }		
		    //Voter is liberal.
		    if(myOpinionOfOthers.get(current) > LIBERAL_THRESHOLD) {
		    	print("They're trustworthy, so I'll trust the one they voted for is fascist. "
		    			+ "Decrease target "+NORMAL_CHANGE);
		    	increaseOpinion(target, NORMAL_CHANGE);
		    }
		    //Relationship between the two.
		    if(myOpinionOfOthers.get(target) > LIKE_THRESHOLD 
		    		&& myOpinionOfOthers.get(current) > LIKE_THRESHOLD) {
		    	print("I trust both, so something is off. I'll decrease both slightly.");
		    	decreaseOpinion(target, SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);
		    }else if(myOpinionOfOthers.get(target) > LIKE_THRESHOLD 
		    		&& myOpinionOfOthers.get(current) < DISLIKE_THRESHOLD) {
		    	print("I like the target but not the voter, so the voter is probably lying. "
		    			+ "I'll trust the target more and the voter less.");
		    	decreaseOpinion(current, SMALL_CHANGE);
		    	increaseOpinion(target, SMALL_CHANGE);
		    }else if(myOpinionOfOthers.get(target) < DISLIKE_THRESHOLD 
		    		&& myOpinionOfOthers.get(current) > LIKE_THRESHOLD) {
		    	print("I like the voter but not the target, so I agree with this vote. "
		    			+ "I'll trust the voter more and the target less.");
		    	increaseOpinion(current, SMALL_CHANGE);
		    	decreaseOpinion(target, SMALL_CHANGE);
		    }else {
		    	print("I have no particularly contradictory opinion on either of them.");
		    }
		}
	}

	/**
	 * Checks if they wasted a vote or else targeted a suspicious person.
	 */
	protected void processVotesInvestigate(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey();
		    AID other = votes.get(current);
		    print(current.getLocalName()+" voted to investigate "+other.getLocalName());		    
		    if(myOpinionOfOthers.get(other) > LIBERAL_THRESHOLD) {
		    	print("The target is obviously liberal. This is a wasted vote. "
		    			+ "Decrease "+SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);		    	
		    }else if(myOpinionOfOthers.get(other) < DISLIKE_THRESHOLD){
		    	print("I suspect the target so this is a good choice. "
		    			+ "Increase"+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    }
		}
	}

	/**
	 * Depending on opinion of person voted for, likes or dislikes voter more.
	 */
	protected void processVotesChancellor(Map<AID, AID> votes) {
		for (Entry<AID, Integer> pair : cloneSet(myOpinionOfOthers.entrySet())) {
		    AID current = pair.getKey(); 
		    AID target = votes.get(current);
		    print(current.getLocalName()+" voted to nominate "+target.getLocalName());
		    
		    AID mostLiked = getMostLikedAID(myOpinionOfOthers, Arrays.asList(currentChancellor, currentPresident));
		    if(myOpinionOfOthers.get(target) > LIBERAL_THRESHOLD) {
		    	print("They voted for the trustworthy chancellor. "
		    			+ "Increase "+SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    }else if(myOpinionOfOthers.get(mostLiked) > LIBERAL_THRESHOLD) {
		    	print("There was a trustworthy individual they could have voted for but they did not. "
		    			+ "Decrease "+NORMAL_CHANGE);
		    	decreaseOpinion(current, NORMAL_CHANGE);
		    }
		    
		    if(myOpinionOfOthers.get(current) > LIBERAL_THRESHOLD) {
		    	print("They're trustworthy, so I'll trust the one they voted for. "
		    			+ "Increase target "+SMALL_CHANGE);
		    	increaseOpinion(target, SMALL_CHANGE);
		    }
		    
		    if(myOpinionOfOthers.get(target) > LIKE_THRESHOLD 
		    		&& myOpinionOfOthers.get(current) > LIKE_THRESHOLD) {
		    	print("I trust both, so I'll increase both slightly.");
		    	increaseOpinion(target, SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    }else if(myOpinionOfOthers.get(target) > LIKE_THRESHOLD 
		    		&& myOpinionOfOthers.get(current) < DISLIKE_THRESHOLD) {
		    	print("I like the target but not the voter, I'll be more neutral on both.");
		    	decreaseOpinion(target, SMALL_CHANGE);
		    	increaseOpinion(current, SMALL_CHANGE);
		    }else if(myOpinionOfOthers.get(target) < DISLIKE_THRESHOLD 
		    		&& myOpinionOfOthers.get(current) > LIKE_THRESHOLD) {
		    	print("I like the voter but not the target, I'll be more neutral on both.");
		    	increaseOpinion(target, SMALL_CHANGE);
		    	decreaseOpinion(current, SMALL_CHANGE);
		    }else {
		    	print("I have no particularly contradictory opinion on either of them.");
		    }
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
    
    /**
     * Examines if the president is obviously fascist or liberal, and else examines how likely
     * the claim is with an unlikeliness variable.
     */
    public void processInvestigation(AID investigated, boolean fascist) {    	
    	if(myOpinionOfOthers.get(currentPresident) < FASCIST_THRESHOLD) {
    		print("Can't trust anything the president says because he's obviously a fascist. "
    				+ "Could be a trick.");
    		return;
    	}
    	int value;
		//The higher the unlikeness the more unlikely it is that they're fascist.
    	int unlikeliness;
    	String claim;
    	if (fascist) {
        	unlikeliness = myOpinionOfOthers.get(investigated)/2;
    		value = 0;
    		claim = "fascist.";
    	}else {
        	unlikeliness = (100 - myOpinionOfOthers.get(investigated))/2;
    		value = 100;
    		claim = "liberal.";
    	}
    	if(myOpinionOfOthers.get(currentPresident) > LIBERAL_THRESHOLD) {
    		print("President is obviously liberal. I can trust them. "
    					+investigated.getLocalName()+" is "+claim);
    		myOpinionOfOthers.put(investigated, value);
    	}else if(myOpinionOfOthers.get(currentPresident) - unlikeliness > 50) {
    		print("The claim is believable, I'll trust the president."
    				+investigated.getLocalName()+" is "+claim);
    		myOpinionOfOthers.put(investigated, value);
    	}else {
    		print("I can't trust this claim, it's not believable. "
    				+ "I'll decrease the president "+NORMAL_CHANGE);
    		decreaseOpinion(currentPresident, NORMAL_CHANGE);
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
