package agent.behaviour.liberals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import agent.behaviour.DefaultLiberalBehaviour;
import app.Policy;
import jade.core.AID;

public class ShyLiberal extends DefaultLiberalBehaviour {
	
	private Map<AID, AID> positiveVotes = new HashMap<>();
	private Map<AID, AID> negativeVotes = new HashMap<>();
	
    public ShyLiberal(AID id, List<AID> players, boolean verbose) {
        super(id, players, verbose);
        addBias(SMALL_CHANGE);
        name = id.getLocalName()+" (shy)";
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
    	super.processVotesExecute(votes);
    	negativeVotes = votes;
    	print("I'll remember these votes for next time we need to vote on this...");
	}
    
    @Override
    protected void processVotesInvestigate(Map<AID, AID> votes) {
    	super.processVotesInvestigate(votes);
    	negativeVotes = votes;
    	print("I'll remember these votes for next time we need to vote on this...");
	}

    @Override
	protected void processVotesChancellor(Map<AID, AID> votes) {
		super.processVotesChancellor(votes);
		positiveVotes = votes;
		print("I'll remember these votes for next time we need to vote on this...");
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
    @Override
	protected AID selectChancellor() {
    	if(positiveVotes.isEmpty())
    		return super.selectChancellor();
    	print("I'll vote what everyone voted for last time, as long as it's valid...");
    	AID lastTime = findMostFrequent(positiveVotes.values());
		return lastTime.equals(currentPresident) || lastTime.equals(currentChancellor) ? 
				super.selectExecute() : lastTime;
	}
    
    @Override
    protected void processPeek(List<Policy> data) {
		peek.clear();
		//If I trust president, remember peek.
		if(myOpinionOfOthers.get(currentPresident) > DISLIKE_THRESHOLD) { //- bias*3
			print("I don't hate the president, so I'll believe his peek.");
			peek = data;
		}
    }
    
    /**
     * Votes for government if the current Chancellor was the one the majority voted for.
     * Else uses default behaviour.
     */
    @Override
    public boolean vote() {    	
    	if(currentChancellor.equals(findMostFrequent(positiveVotes.values()))) {
    		print("The Chancellor got the majority, so I'll accept the government.");
    		return true;
    	}
    	print("If we just started or we've failed two governments I guess I have to choose. "
    			+ "Otherwise I'll reject since they didn't get majority.");
        return round <= 3 || failedGovernments == 2 ? super.vote() : false;
    }    
}
