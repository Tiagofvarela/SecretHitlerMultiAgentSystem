package agent.behaviour.fascists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import agent.behaviour.FascistBehaviour;
import app.Policy;
import jade.core.AID;

public class ShyFascist extends FascistBehaviour {
    public ShyFascist(List<AID> fascists, AID hitler, AID id, List<AID> players, boolean verbose) {
        super(fascists, hitler, id, players, verbose);
        addBias(SMALL_CHANGE);
        
        name = id.getLocalName()+" (shy)";
    }

    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> chancellorVotes) {
        print("As president, I will choose who to nominate as Chancellor.");
        return (fascistTracker >= 3 && !hitlerAID().equals(id)) ? hitlerAID() : findMostFrequent(chancellorVotes.values());
    }

    @Override
    protected AID presidentChooseDead(Map<AID, AID> executionVotes) {
        print("As president, I will choose who to execute.");
        AID aux = findMostFrequent(executionVotes.values());
        return aux.equals(hitlerAID()) ? getMostSuspiciousLiberal() : aux;

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
    protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
        print("I will try to make one fascist and one liberal policies.");
        int facists = Collections.frequency(policies, Policy.Fascist);

        if(facists >= 2) {
            print("There are three fascist cards, remove one.");
            policies.remove(Policy.Fascist);
        }else{
            print("Remove one liberal policy.");
            policies.remove(Policy.Liberal);
        }

        return policies;
    }

    @Override
    protected List<Policy> explainCardsPresident(List<Policy> policies) {

        print("I will tell 2 fascist 1 liberal if there is 1 fascist, otherwise tell truth.");
        List<Policy> result = new ArrayList<>();

        int facists = Collections.frequency(policies, Policy.Fascist);

        if (facists == 1) {
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
            result.add(Policy.Liberal);
            return result;
        }

        return policies;
    }

    @Override
    public boolean explainInvestigation(AID investigated, boolean fascist) {
        print("I will always say it is liberal.");
        return false;
    }

    @Override
    protected AID selectExecute() {
        print("I will execute always the most suspicious liberal.");
        return getMostSuspiciousLiberal();
    }

}
