package de.roboy.dialog.personality.states;

import java.util.List;

import de.roboy.linguistics.Linguistics;
import de.roboy.linguistics.sentenceanalysis.Interpretation;
import de.roboy.logic.StatementInterpreter;
import de.roboy.util.Lists;

public class InquiryState extends AbstractBooleanState{

	private String inquiry;
	private List<String> successTerms;
	private String failureText;
	
	public InquiryState(String inquiry, List<String> successTerms, String failureText){
		this.inquiry = inquiry;
		this.successTerms = successTerms;
		this.failureText = failureText;
	}
	
	@Override
	public List<Interpretation> act() {
		return Lists.interpretationList(new Interpretation(inquiry));
	}

	@Override
	public Reaction react(Interpretation input) {
		String sentence = (String) input.getFeatures().get(Linguistics.SENTENCE);
		boolean successful = StatementInterpreter.isFromList(sentence, successTerms);
		if(successful){
			return new Reaction(success);
		} else {
			return new Reaction(failure,Lists.interpretationList(new Interpretation(failureText)));
		}
	}
	

}