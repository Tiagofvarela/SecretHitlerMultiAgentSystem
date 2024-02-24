package agent.behaviour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import app.Policy;
import jade.core.AID;

public class FascistBehaviour extends DefaultLiberalBehaviour {
	private final boolean isHitler;
	protected final AID hitler;
	protected final List<AID> fascists;
	protected final List<AID> liberals;

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
		AID fascist = getLowestSuspiciousFacist(exceptGovernment());
		AID result = getMostLikedAID(myOpinionOfOthers, exceptGovernment());
		if (isHitler) {
			if(round <= 3 || (fascist != null && myOpinionOfOthers.get(fascist) > DISLIKE_THRESHOLD)) {
				result = fascist;
			}else {
				result = getMostLikedAID(myOpinionOfOthers, exceptGovernment());
			}			
		} else {
			if (fascistTracker >= 3 && !tooSuspicious(id) && currentChancellor != null 
					&& !currentChancellor.equals(hitler) && !currentPresident.equals(hitler)) {
				print("The tracker is at " + fascistTracker + " and I'm not too suspicious. I'll try to make hitler chancellor");
				result = hitler;
			} else if (tooSuspicious(id)) {
				print("I'm too suspicious. I'll make the chancellor a liberal");
				List<AID> fascists = getFascists();
				List<AID> except_list = exceptGovernment();
				AID aid = getMostLikedAID(myOpinionOfOthers, except_list);
				while(fascists.contains(aid)) {
					except_list.add(aid);
					aid = getMostLikedAID(myOpinionOfOthers, except_list);
				}
				result = aid;
			} else {
				print("I'm not too suspicious. I'll try to make a fascist chancellor");
				if(fascist != null)
					result = fascist;
			}
		}
		return result;
	}

	@Override
	protected AID selectExecute() {

		AID liberal = getMostSuspiciousLiberal();

		//If Im hitler I can throw my facists under the bus if they are too suspicious
		if (isHitler) {
			print("I'm hitler! I can throw my fascists under the bus if they are too suspicious");
			AID facist = getMostSuspiciousFacist(new ArrayList<>());

			if(!facist.equals(hitler))
				return (this.myOpinionOfOthers.get(liberal) - this.myOpinionOfOthers.get(facist)) > 15 ? liberal : facist;
		}
		print("I'll just nominate the most suspicious liberal to gain others trust");
		return liberal;
	}

	@Override
	protected AID selectInvestigate() {

		//As facist I know who is who, I need to hear other opinions and who they think is suspicious
		//If Im hitler I can throw my facists under the bus if they are too suspicious

		//Does not investigate himself or president.
		print("I'll just select the one I dislike the most except hitler or the president");
		return getMostDislikedAID(myOpinionOfOthers, Arrays.asList(id, currentPresident, hitler));
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
			if (fascistTracker >= 3 && !tooSuspicious(hitler) && !currentChancellor.equals(hitler) && !currentPresident.equals(hitler) && !tooSuspicious(this.id)) {
				print("If Hitler is not too suspicious and the tracker is at " + fascistTracker + " I'll try nominate him as chancellor");
				result = hitler;

			} else if (!tooSuspicious(id)) {
				print("I'm not too suspicious so I'll just try to nominate the most frequent nominated");
				return findMostFrequent(chancellorVotes.values());
			}

		}
		return result;
	}


	@Override
	protected AID presidentChooseDead(Map<AID, AID> executionVotes) {

		//		AID liberal = getMostSuspiciousLiberal();
		//
		//		//If Im hitler I can throw my facists under the bus if they are too suspicious
		//		if (isHitler) {
		//			print("I'm hitler! I can throw my fascists under the bus if they are to suspicious. " +
		//					"I'll just choose the most frequent of the nominations");
		//			return findMostFrequent(executionVotes.values());
		//		}
		//		print("I'll just nominate the most suspicious liberal to gain others trust");
		//		return liberal;

		Map<AID, Integer> freq = createFrequencyMap(executionVotes.values());
		for (Entry<AID, Integer> entry : freq.entrySet()) {
			if(entry.getKey().equals(liberals) && entry.getValue()>2) {
				return entry.getKey();
			}

		}
		return findMostFrequent(executionVotes.values());

	}

	@Override
	protected AID presidentChooseInvestigate(Map<AID, AID> votes) {
		print("I'll just nominate the most voted player");
		return findMostFrequent(votes.values());
	}


	@Override
	protected List<Policy> choosePoliciesPresident(List<Policy> policies) {
		int meSuspicious = myOpinionOfOthers.get(id);
		int chancellorSuspicious = myOpinionOfOthers.get(currentChancellor);
		boolean iAmSuspicious =  meSuspicious< DISLIKE_THRESHOLD;
		boolean moreSuspect = chancellorSuspicious > meSuspicious;

		int fascistPoliciesCounter = Collections.frequency(policies, Policy.Fascist);
		if(!iAmSuspicious || moreSuspect) {
			policies.remove(Policy.Liberal);
			print("As president, i will try to remove a liberal");
			if(policies.size() == 3) {
				print("I couldnt remove a liberal, so i remove a fascist");
				policies.remove(0);
			}
		}else { // if chancellor is liberal
			if(fascistPoliciesCounter == 2) {
				policies.remove(Policy.Fascist); // Deliver 2 liberal policies
				print("As president i will deliver a fascist and liberal policy");
			}else if(fascistPoliciesCounter == 1) {
				policies.remove(Policy.Liberal); // Deliver 2 liberal policies
				print("As president i will deliver a fascist and liberal policy");
			}if(fascistPoliciesCounter == 3) {
				policies.remove(Policy.Fascist); // Deliver 2 liberal policies
				print("As president i have to deliver 2 fascist policies");
			}else if(fascistPoliciesCounter == 0) {
				policies.remove(Policy.Liberal); // Deliver 2 liberal policies
				print("As president i have to deliver 2 liberal policies");
			}
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
				policies.remove(Policy.Fascist);
				print("President is liberal and i received a liberal and fascist policy, so i will just enact the liberal policy.");
			}else {
				policies.remove(0); 
				print("President is liberal and both cards are the same, so I'll just discard one.");
			}
		}

		return policies;
	}


	@Override
	protected List<Policy> explainCardsChancellor(List<Policy> policies) {

		//		List<Policy> result = new ArrayList<>();
		//
		//		if (isHitler) {
		//			print("I'm Hitler, I want to seem good, so I'll just tell the truth");
		//			return policies;
		//		}
		//
		//		if (tooSuspecious(id)) {
		//			print("I'm too suspicious so I'll just tell the truth");
		//			return policies;
		//		}
		//
		//		//Quero passar uma facista se o presidente nao for hitler
		//		if (policies.contains(Policy.Fascist) && !currentPresident.equals(hitler)) {
		//			print("The cards had a fascist card and Hitler is not president. I'll say I had 2 fascists.");
		//			result.add(Policy.Fascist);
		//			result.add(Policy.Fascist);
		//			return result;
		//		}
		//
		//		print("I'll just tell the truth");
		//		return policies;
		List<Policy> result = new ArrayList<>();
		Policy finalPolicy = choosePoliciesChancellor(policies).get(0);
		if(peek != null) {
			print("President peeked so I must tell the truth.");
			return policies;
		}else if(finalPolicy.equals(Policy.Fascist)) {
			print("I passed a fascist policy so I must say I had no choice.");
			result.add(Policy.Fascist);
			result.add(Policy.Fascist);
			return result;
		}
		print("I passed a Liberal so I'll tell the truth.");
		return policies;
	}


	@Override
	protected List<Policy> explainCardsPresident(List<Policy> policiesPresidentReceived) {
		List<Policy> result = new ArrayList<>();
		List<Policy> policiesChancellorReceived = choosePoliciesPresident(policiesPresidentReceived);

		if(!policiesChancellorReceived.contains(Policy.Liberal)){
			print("I sent no liberal policies so I must claim I had three fascist cards.");
			result.add(Policy.Fascist);
			result.add(Policy.Fascist);
			result.add(Policy.Fascist);
			return result;
		}else if (policiesChancellorReceived.contains(Policy.Liberal) 
				&& policiesChancellorReceived.contains(Policy.Fascist)) {
			print("I only had 1 fascist card. I'll say I had 2 fascists and 1 liberal.");
			result.add(Policy.Fascist);
			result.add(Policy.Fascist);
			result.add(Policy.Liberal);
			return result;
		}else { 
			print("I only received Liberals, but I'll lie and say I discarded a fascist card.");
			result.add(Policy.Fascist);
			result.add(Policy.Liberal);
			result.add(Policy.Liberal);
			return result;
		}
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
		List<AID> fascists = getFascists();
		boolean investigated_is_fascist = fascists.contains(investigated);

		if(investigated_is_fascist && myOpinionOfOthers.get(investigated) > 30) {
			print("Investigated player is fascist and its suspicious threshold is above 30, i'll say it is Liberal ");
			return false;
		}

		if(!investigated_is_fascist && myOpinionOfOthers.get(investigated) < 50) {
			print("Investigated player is liberal and its suspicious threshold is below 50, i'll say it is Liberal ");
			return true;
		}

		print("I'll just say the it is Liberal");
		return false;
	}

	//see if other liberals think am too suspicious
	public boolean tooSuspicious(AID id) {
		return myOpinionOfOthers.get(id) < DISLIKE_THRESHOLD;
	}

	public AID getMostSuspiciousFacist(List<AID> except) {
		for(AID player : liberals) {
			if(!except.contains(player)) {
				except.add(player);
			}
		}
        return getMostDislikedAID(myOpinionOfOthers, except);
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

	/**
	 * Gets the most liked AID except for the fascists.
	 * @return
	 */
	public AID getLowestSuspiciousFacist(List<AID> except) {
		for(AID player : liberals) {
			if(!except.contains(player)) {
				except.add(player);
			}
		}
        return getMostLikedAID(myOpinionOfOthers, except);
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

	@Override
	public boolean vote() {
		List<AID> fascists = getFascists();
		boolean chancellor_is_fascist = fascists.contains(currentChancellor);
		boolean president_is_fascist = fascists.contains(currentPresident);

		if (isHitler) {
			return super.vote();
		} else {
			if(fascistTracker >= 3 && currentChancellor == hitler) {
				print("3 fascist policies have passed and Hitler is being voted for Chancellor, i'll vote yes.");
				return true;
			}
			if(chancellor_is_fascist || president_is_fascist) {
				print("Either the President or the Chancellor is fascist, so i'll vote yes.");
				return true;
			}
		}
		return super.vote();
	}

	/**
	 * @return Returns a list with the current government.
	 */
	protected List<AID> exceptGovernment() {
		List<AID> list = new ArrayList<>();
		list.add(currentChancellor);
		list.add(currentPresident);
		return list;
	}
}
