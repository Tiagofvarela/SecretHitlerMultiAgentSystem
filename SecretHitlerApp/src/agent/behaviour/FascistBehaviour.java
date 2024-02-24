package agent.behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.Policy;
import jade.core.AID;

public class FascistBehaviour extends DefaultLiberalBehaviour {
    private final boolean isHitler;
    private final AID hitler;
    private final List<AID> fascists;
    private final List<AID> liberals;

    public FascistBehaviour(List<AID> fascists, AID hitler, AID id, List<AID> players, boolean verbose) {
        super(id, players, verbose);
        this.isHitler = this.id.equals(hitler);
        this.fascists = fascists;
        this.hitler = hitler;

        liberals = new ArrayList<>();

        for (AID key : this.myOpinionOfOthers.keySet()) {
            if (!fascists.contains(key)) {
                liberals.add(key);
            }
        }
    }

    @Override
    public boolean isHitler() {
        return isHitler;
    }

    public AID hitlerAID(){ return hitler;}

    public List<AID> getFascists(){ return fascists;}

    public List<AID> getLiberals(){ return liberals;}

    @Override
    public boolean isFascist() {
        return true;
    }
    
    @Override
    public void removePlayer(AID player) {
    	super.removePlayer(player);
    	fascists.remove(player);
    	liberals.remove(player);
    }

    @Override
    protected AID selectChancellor() {
        print("I want to nominate the person I most like (but not previous Chancellor).");
        //highest number = lowest suspicious

        AID facist = getLowestSuspiciousFacist();
        AID liberal = getLowestSuspiciousLiberal();


        AID result = getMostLikedAID(myOpinionOfOthers, Arrays.asList(currentChancellor, currentPresident));

        //If Im hitler I do not want to mess around to much behaviour similar to liberal

        if(currentChancellor != null){
            if(!currentChancellor.equals(facist) && !currentPresident.equals(facist) &&
                    !currentChancellor.equals(liberal) && !currentPresident.equals(liberal)) {

                if (isHitler) {
                    if (!tooSuspecious(id)) {
                        print("Im hitler and I'm not too suspicious so I can decide normally");
                        result = (this.myOpinionOfOthers.get(liberal) - 10 > this.myOpinionOfOthers.get(facist)) ? liberal : facist;
                    } else {
                        print("Im hitler and I'm too suspicous I don't want to mess around too much. I have to behave similar to a liberal");
                        result = (this.myOpinionOfOthers.get(liberal) > this.myOpinionOfOthers.get(facist)) ? liberal : facist;
                    }


                } else {//If Im just facist try to make hitler chancellor if tracker is over or equal a 3, otherwise make other
                    //facist chancellor or act normal and elate anyone

                    if (fascistTracker >= 3 && !tooSuspecious(id) && !currentChancellor.equals(hitler) && !currentPresident.equals(hitler)) {
                        print("The tracker is at " + fascistTracker + " and I'm not too suspicious. I'll try to make hitler chancellor");
                        result = hitler;

                    } else if (!tooSuspecious(id)) {
                        print("I'm not too suspicious. I'll try to make a fascist chancellor so he can advance in the powers");
                        //If Im not too suspicious I still prefer a facist with power CAN CHANGE THE VALUE 20
                        result = (this.myOpinionOfOthers.get(liberal) - 20 > this.myOpinionOfOthers.get(facist)) ? liberal : facist;
                    } else {
                        print("I'm too suspicious. I'll try to make a low-profile move");
                        //If Im suspicious just choose someone who I think has been not suspicious or do I make that person suspicious?
                        result = (this.myOpinionOfOthers.get(liberal) > this.myOpinionOfOthers.get(facist)) ? liberal : facist;
                    }

                }
            }
        }
        return result;
    }

    @Override
    protected AID selectExecute() {

        AID liberal = getMostSuspiciousLiberal();

        //If Im hitler I can throw my facists under the bus if they are too suspicious
        if (isHitler) {
            print("I'm hitler! I can throw my fascists under the bus if they are to suspicious");
            AID facist = getMostSuspiciousFacist();

            if(!facist.equals(hitler))
                return (this.myOpinionOfOthers.get(liberal) <= this.myOpinionOfOthers.get(facist)) ? liberal : facist;
        }
        print("I'll just nominate the most suspicious liberal to gain others trust");
        return liberal;

    }

    @Override
    protected AID selectInvestigate() {

        //As facist I know who is who, I need to hear other opinions and who they think is suspicious
        //If Im hitler I can throw my facists under the bus if they are too suspicious
    	
    	//Does not investigate himself or president.
        print("I'll just select the one I dislike the most except me or the president");
        return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id, currentPresident));
    }


    @Override
    protected AID presidentChooseChancellor(Map<AID, AID> chancellorVotes) {

        AID result = null;

        //If Im hitler I do not want to mess around to much behaviour similar to liberal

        if (isHitler) {
            print("I'm Hitler, and I'll just nominate the most frequent and liked player so I don't get too suspicious");
            return findMostFrequent(chancellorVotes.values());

        } else {//If Im just facist try to make hitler chancellor if tracker is over or equal a 3, otherwise make other
            //facist chancellor or act normal and elate anyone

            //verificar se hitler estah suspeito
            if (fascistTracker >= 3 && !tooSuspecious(hitler) && !currentChancellor.equals(hitler) && !currentPresident.equals(hitler)) {
                print("If Hitler is not too suspicious and the tracker is at " + fascistTracker + " I'll try nominate him as chancellor");
                result = hitler;

            } else if (!tooSuspecious(id)) {
                print("I'm not too suspicious so I'll just try to nominate the most frequent nominated");
                return findMostFrequent(chancellorVotes.values());
            }

        }
        return result;
    }


    @Override
    protected AID presidentChooseDead(Map<AID, AID> executionVotes) {

        AID liberal = getMostSuspiciousLiberal();

        //If Im hitler I can throw my facists under the bus if they are too suspicious
        if (isHitler) {
            print("I'm hitler! I can throw my fascists under the bus if they are to suspicious. " +
                    "I'll just choose the most frequent of the nominations");
            return findMostFrequent(executionVotes.values());
        }
        print("I'll just nominate the most suspicious liberal to gain others trust");
        return liberal;

    }

    @Override
    protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
        print("I'll just nominate the most voted player");
        return findMostFrequent(votes.values());
    }


    @Override
    protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
    	//System.out.println("I, fascist, just received: "+policies);
        print("I'll just remove the liberal policy if exists or the first one if not");
        policies.remove(Policy.Liberal);
        if (policies.size() > 2)
            policies.remove(0);
        //System.out.println("I, fascist, just removed: "+policies);
        return policies;
    }

    @Override
    protected List<Policy> choosePoliciesChancellor(List<Policy> policies) {

        //Hitler agir como inocente
        if (isHitler) {
            print("I'm Hitler, so I'll just remove the fascist policy if exists or a liberal if not. Trying to act innocent.");
            policies.remove(Policy.Fascist);
            if (policies.size() > 1)
                policies.remove((Policy.Liberal));

            return policies;

        }

        print("I'll just remove the liberal policy if exists or a fascist if not.");
        policies.remove(Policy.Liberal);
        if (policies.size() > 1)
            policies.remove((Policy.Fascist));

        return policies;
    }


    @Override
    protected List<Policy> explainCardsChancellor(List<Policy> policies) {

        List<Policy> result = new ArrayList<>();

        if (isHitler) {
            print("I'm Hitler, I want to seem good, so I'll just tell the truth");
            return policies;
        }

        if (tooSuspecious(id)) {
            print("I'm too suspicious so I'll just tell the truth");
            return policies;
        }

        //Quero passar uma facista se o presidente nao for hitler
        if (policies.contains(Policy.Fascist) && !currentPresident.equals(hitler)) {
            print("The cards had a fascist card and Hitler is not president. I'll say I had 2 fascists.");
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
            return result;
        }

        print("I'll just tell the truth");
        return policies;
    }


    @Override
    protected List<Policy> explainCardsPresident(List<Policy> policies) {

        List<Policy> result = new ArrayList<>();

        int facists = Collections.frequency(policies, Policy.Fascist);

        if (isHitler && facists == 1) {
            print("I'm Hitler, and I only had 1 fascist card. I'll say I had 2 fascists and 1 liberal");
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
            result.add(Policy.Liberal);
            return result;
        }

        if (tooSuspecious(id)) {
            print("I'm too suspicious so I'll just tell the truth");
            return policies;
        }

        //se o chancellor for um facista quero passar apenas cartas facistas
        boolean facistChancellor = false;
        for (AID facist : fascists) {

            if (policies.contains(Policy.Fascist) && currentChancellor.equals(facist) && !tooSuspecious(id)) {
                facistChancellor = true;
            }
        }

        if (facistChancellor) {
            print("The chancellor is fascist so I'll just say I had 3 fascist cards");
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
            result.add(Policy.Fascist);
            return result;
        }

        print("I'll just tell the truth");
        return policies;
    }

    /**
     * Logic for deciding what to tell the other players you saw when you peeked
     * the next 3 policies in the deck.
     *
     * @param policies
     * @return
     */
    @Override
    protected List<Policy> explainCardsPeek(List<Policy> policies) {
        print("I'll just tell the truth");
        return policies;
    }


    @Override
    public boolean explainInvestigation(AID investigated, boolean fascist) {
        print("I'll just say the opposite of what it is. Liberal -> Fascist | Fascist -> Liberal");
        //TODO: Quick implementation I did. Calls fascists liberal and liberals fascists. -Tiago
        return !fascist;
    }

    //see if other liberals think am too suspicious
    public boolean tooSuspecious(AID id) {
        return myOpinionOfOthers.get(id) < 30;
    }

    public AID getMostSuspiciousFacist() {
        int lower = 101;
        int aux;
        AID result = null;


        for (AID facist : this.fascists) {
            aux = this.myOpinionOfOthers.get(facist);

            if (aux <= lower) {
                lower = aux;
                result = facist;
            }
        }
        return result;
    }

    public AID getMostSuspiciousLiberal() {

        int lower = 101;
        int aux;
        AID result = null;

        //Choose someone who is suspicious and is not facist
        for (AID liberal : this.liberals) {
            aux = this.myOpinionOfOthers.get(liberal);

            if (aux <= lower) {
                lower = aux;
                result = liberal;
            }
        }
        return result;
    }

    public AID getLowestSuspiciousFacist() {
        int max = -1;
        int aux;
        AID result = null;

        //printList(this.myOpinionOfOthers.keySet());
        for (AID facist : this.fascists) {
            if (!fascists.equals(id)) {
                aux = this.myOpinionOfOthers.get(facist);

                if (aux >= max) {
                    max = aux;
                    result = facist;
                }
            }
        }
        return result;
    }

    private void printList(Set<AID> fascists) {
        System.out.print("FASCISTS LIST\n[");
        for (AID fascist :
                fascists) {
            System.out.print(fascist + ", ");
        }
        System.out.print("]");
    }

    public AID getLowestSuspiciousLiberal() {

        int max = -1;
        int aux;
        AID result = null;

        //Choose someone who is suspicious and is not facist
        for (AID liberal : this.liberals) {
            aux = this.myOpinionOfOthers.get(liberal);

            if (aux >= max) {
                max = aux;
                result = liberal;
            }
        }
        return result;
    }
}
