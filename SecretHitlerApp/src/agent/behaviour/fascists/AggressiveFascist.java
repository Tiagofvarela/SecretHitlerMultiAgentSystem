package agent.behaviour.fascists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import agent.behaviour.FascistBehaviour;
import app.Policy;
import jade.core.AID;

public class AggressiveFascist extends FascistBehaviour {
    public AggressiveFascist(List<AID> fascists, AID hitler, AID id, List<AID> players, boolean verbose) {
        super(fascists, hitler, id, players, verbose);
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
                increaseOpinion(current, LARGE_CHANGE);
                print(current.getLocalName()+" voted in favour of liberal government, increase "+SMALL_CHANGE);
            } else {
                decreaseOpinion(current, LARGE_CHANGE);
                print(current.getLocalName()+" voted against liberal government, decrease "+SMALL_CHANGE);
            }
        }
    }

    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> chancellorVotes) {

//        print("As president, I will choose who to nominate as Chancellor.");
//        List<AID> fascists = getFascists();
//        if(fascists.size() == 1){
//            return findMostFrequent(chancellorVotes.values());
//        }else{
//            for(AID fascist : fascists){
//                if(!fascist.equals(id)){
//                    print("I choose another fascist.");
//                    return fascist;
//                }
//            }
//        }
//        return findMostFrequent(chancellorVotes.values());
    	print("I will choose as chancellor the other fascist");
    	for (AID fascist : fascists) {
			if(fascist != id)
				return fascist;
		}
		return hitler;
    	
    }

    @Override
    protected AID presidentChooseDead(Map<AID, AID> executionVotes) {
        print("As president, I will choose who to execute.");
        print("I will execute the most suspicious liberal.");
        return getMostSuspiciousLiberal();
    }

    @Override
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
//        print("As president, I will choose who to investigate.");
//        return getMostSuspiciousLiberal();
    	
    	LinkedHashMap<AID, Integer> interest = new LinkedHashMap<>();
    	myOpinionOfOthers.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByValue())
        .forEachOrdered(x -> interest.put(x.getKey(), x.getValue()));
    	
    	for (Entry<AID, Integer> e : interest.entrySet()) {
    		if(e.equals(liberals))
    			return e.getKey();
			
		}
		return interest.entrySet().iterator().next().getKey(); // devolve menos confiavel
    }

//    @Override
//    protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {
//
//        print("I will choose fascist policy if possible.");
//        policies.remove(Policy.Liberal);
//        if (policies.size() > 1) {
//            print("There were two fascist policies.");
//            policies.remove(Policy.Fascist);
//        }
//        return policies;
//    }

    /*@Override
    protected List<Policy> explainCardsPresident(List<Policy> policies) {

        print("I will always tell the opposite of the received policies.");
        List<Policy> result = new ArrayList<>();

        int facists = Collections.frequency(policies, Policy.Fascist);

        if (facists == 3) {
            result.add(Policy.Liberal);
            result.add(Policy.Liberal);
            result.add(Policy.Liberal);
        }else if(facists == 2){
            result.add(Policy.Liberal);
            result.add(Policy.Liberal);
            result.add(Policy.Fascist);
        }else if (facists == 1){
            result.add(Policy.Liberal);
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
        }else{
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
        }

        return result;
    }*/

    @Override
    protected List<Policy> explainCardsChancellor(List<Policy> policies) {

//        print("I will always tell the opposite of the received policies.");
//        List<Policy> result = new ArrayList<>();
//
//        int facists = Collections.frequency(policies, Policy.Fascist);
//
//        if (facists == 2) {
//            result.add(Policy.Liberal);
//            result.add(Policy.Liberal);
//        }else{
//            result.add(Policy.Fascist);
//            result.add(Policy.Fascist);
//        }
//
//        return result;
    	if(peek != null)
			print("President peaked so i must tell the truth");
    	else if(fascists.contains(currentPresident)) {
    		print("I'll just tell the truth");
    	}else{
    		if(policies.contains(Policy.Fascist) && policies.contains(Policy.Liberal))
    			print("I chose fascist, so i must lie");
    		else if(!policies.contains(Policy.Fascist))
    			print("I'll just tell the truth");
    		else if(!policies.contains(Policy.Liberal))
    			print("I'll just tell the truth");	
    	}
    	return policies;
    }

    @Override
    protected List<Policy> explainCardsPeek(List<Policy> policies) {
    	List<AID> fascists = getFascists();
    	int fascistPoliciesCounter = 0;
		for (Policy policy : policies) {
			if(policy.equals(Policy.Fascist))
				fascistPoliciesCounter++;
		}
    	if(fascists.contains(getNextPresident()) && fascistPoliciesCounter > 1) {
    		print("Since the next president is fascist, I'll say the cards are all fascist "
    				+ "when they aren't.");
    		return Arrays.asList(Policy.Fascist, Policy.Fascist, Policy.Fascist);
    	}
    	print("Since the next President is Liberal, I may as well tell the truth about what I peeked.");
        return policies;
    }

	@Override
    public boolean explainInvestigation(AID investigated, boolean fascist) {
        print("I'll just say the opposite of what it is. Liberal -> Fascist | Fascist -> Liberal");
        return !fascist;
    }

    @Override
    protected AID selectChancellor() {

        print("I will always choose fascist as chancellor if possible.");
        List<AID> fascists = getFascists();

        for(AID fascist : fascists){
            if(!fascist.equals(currentPresident) || !fascist.equals(currentChancellor)){
                return fascist;
            }
        }
        print("There are not more fascists players to choose from, choose lowest suspicious liberal.");

        return getLowestSuspiciousLiberal();
    }

    @Override
    protected AID selectExecute() {
        print("I will execute the most suspicious liberal.");
        return getMostSuspiciousLiberal();
    }
    
    @Override
	protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
    	policies.remove(Policy.Liberal);
    	print("As president, i will try to remove a liberal");
    	if(policies.size() == 3) {
    		print("I couldnt remove a liberal, so i remove a fascist");
    		policies.remove(0);
    	}
    	return policies;
	}
    
    @Override
	protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {

		//        //Hitler agir como inocente
		//        if (isHitler) {
		//            print("I'm Hitler, so I'll just remove the fascist policy if exists or a liberal if not. Trying to act innocent.");
		//            policies.remove(Policy.Fascist);
		//            if (policies.size() > 1)
		//                policies.remove((Policy.Liberal));
		//
		//            return policies;
		//
		//        }
		//
		//        print("I'll just remove the liberal policy if exists or a fascist if not.");
		//        policies.remove(Policy.Liberal);
		//        if (policies.size() > 1)
		//            policies.remove((Policy.Fascist));

		if(fascists.contains(currentPresident)) {

			if(policies.contains(Policy.Liberal) && policies.contains(Policy.Fascist)) {
				policies.remove(Policy.Liberal);
				print("President is fascist and i received a liberal and fascist policy, so i will just enact the fascist policy.");
			}else {
				policies.remove(0); 
				print("President is fascist and both cards are the same, so I'll just discard one.");
			}

		}else { // if president is liberal
			if(policies.contains(Policy.Liberal) && policies.contains(Policy.Fascist)) {
				policies.remove(Policy.Liberal);
				print("President is liberal and i received a liberal and fascist policy, so i will just enact the fascist policy.");
			}else {
				policies.remove(0); 
				print("President is liberal and both cards are the same, so I'll just discard one.");
			}
		}

		return policies;
	}

	@Override
    protected AID selectInvestigate() {
		print("I want to investigate the liberal I most dislike.");
		List<AID> fascists = getFascists();
		List<AID> except_list = new ArrayList<AID>(Arrays.asList(currentPresident));
		for(AID fascist : fascists) {
			except_list.add(fascist);
		}
		AID aid = getMostDislikedAID(myOpinionOfOthers, except_list);		
		return aid;
	}

	@Override
    public boolean vote() {
    	List<AID> fascists = getFascists();
    	boolean chancellor_is_fascist = fascists.contains(currentChancellor);
    	boolean president_is_fascist = fascists.contains(currentPresident);
    	
    	if(chancellor_is_fascist || president_is_fascist) {
    		print("Either the President or the Chancellor is fascist, so i'll vote yes.");
    		return true;
    	}
    	print("Neither the President or the Chancellor is fascist, so i'll vote no.");
    	return false;
    }

}
