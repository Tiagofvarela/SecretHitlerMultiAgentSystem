package agent.behaviour.fascists;

import agent.behaviour.FascistBehaviour;
import app.Policy;
import jade.core.AID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AggressiveFascist extends FascistBehaviour {
    public AggressiveFascist(List<AID> fascists, AID hitler, AID id, List<AID> players, boolean verbose) {
        super(fascists, hitler, id, players, verbose);
        addBias(-SMALL_CHANGE);
        
        name = id.getLocalName()+" (aggressive)";
    }

    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> chancellorVotes) {

        print("As president, I will choose who to nominate as Chancellor.");
        List<AID> fascists = getFascists();
        if(fascists.size() == 1){
            return findMostFrequent(chancellorVotes.values());
        }else{
            for(AID fascist : fascists){
                if(!fascist.equals(id)){
                    print("I choose another fascist.");
                    return fascist;
                }
            }
        }
        return findMostFrequent(chancellorVotes.values());
    }

    @Override
    protected AID presidentChooseDead(Map<AID, AID> executionVotes) {
        print("As president, I will choose who to execute.");
        print("I will execute the most suspicious liberal.");
        return getMostSuspiciousLiberal();
    }

    @Override
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
        print("As president, I will choose who to investigate.");
        return getMostSuspiciousLiberal();
    }

    @Override
    protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {

        print("I will choose fascist policy if possible.");
        policies.remove(Policy.Liberal);
        if (policies.size() > 1) {
            print("There were two fascist policies.");
            policies.remove(Policy.Fascist);
        }
        return policies;
    }

    @Override
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
    }

    @Override
    protected List<Policy> explainCardsChancellor(List<Policy> policies) {

        print("I will always tell the opposite of the received policies.");
        List<Policy> result = new ArrayList<>();

        int facists = Collections.frequency(policies, Policy.Fascist);

        if (facists == 2) {
            result.add(Policy.Liberal);
            result.add(Policy.Liberal);
        }else{
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
        }

        return result;
    }

    @Override
    protected List<Policy> explainCardsPeek(List<Policy> policies) {
        print("I will always tell the opposite of the seen policies.");
        return explainCardsPresident(policies);
    }

    @Override
    protected AID selectChancellor() {

        print("I will always choose fascist as chancellor if possible.");
        List<AID> fascists = getFascists();

        for(AID fascist : fascists){
            if(!fascist.equals(id)){
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

}
