package roboy.dialog.personality.states;

import java.util.*;

import roboy.linguistics.Linguistics;
import roboy.linguistics.Triple;
import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.memory.PersistentKnowledge;
import roboy.memory.RoboyMind;
import roboy.util.Lists;
import roboy.util.Concept;


public class QuestionAskingState implements State
{

//	private boolean first = true;
	//is used to change the name
    //smallTalkPersonality.setName();
//    private SmallTalkPersonality smallTalkPersonality;
//    private State top;
	private Concept objectOfFocus;
	private String currentIntent;
	private static final int TOASK = 3;
	private int questionsCount;
	private Map<String, List<String>> questions;
	private Random generator;



	public QuestionAskingState(Map<String, List<String>> questions)
	{
//        this.smallTalkPersonality = smallTalkPersonality;
		this.questions = questions;
		this.objectOfFocus = new Concept();
		this.questionsCount = 0;
		this.generator = new Random();


	}

	@Override
	public List<Interpretation> act() 
	{
		List<Interpretation> action = Lists.interpretationList();
		if(questionsCount==0)
		{
			//fist question is always name
			if (questions.containsKey("name"))
			{
				currentIntent = "name";
				List<String> questionsOptions = questions.get(currentIntent);
				String questionToAsk = questionsOptions.get(generator.nextInt(questionsOptions.size()));
				action = Lists.interpretationList(new Interpretation(questionToAsk));
				questionsCount++;
			}

		}
		else
		{
			// pick intent randomly from the list
			List<String> intents = new ArrayList (questions.keySet());
			currentIntent = intents.get(generator.nextInt(intents.size()));

			if (!objectOfFocus.hasAttribute(currentIntent))
			{
				// pick question formulation randomly
				List<String> questionsToAsk = questions.get(currentIntent);
				String questionToAsk = questionsToAsk.get(generator.nextInt(questionsToAsk.size()));
				action = Lists.interpretationList(new Interpretation(questionToAsk));
				questionsCount++;
			}
		}

		return action;

	}

	@Override
	public Reaction react(Interpretation input) 
	{

		String sentence = (String) input.getFeatures().get(Linguistics.SENTENCE);
		if("".equals(sentence))
		{
			return new Reaction(this,Lists.interpretationList()); // new Interpretation(SENTENCE_TYPE.GREETING)
		}

		// add to memory what was understood
		Triple triple = (Triple) input.getFeatures().get(Linguistics.TRIPLE); //TODO: improve triples formation

		if (triple.patiens!="")
		{
			objectOfFocus.addAttribute(this.currentIntent, triple.patiens);
			objectOfFocus.updateInMemory();
		}
		// call DBpedia or memory (about himself or person) and put the answers to input
		List<Interpretation> comments = checkOwnMemory(input);
		if (!comments.isEmpty())
		{
			return new Reaction(determineNextState(input), comments);
		}
		comments.add(checkRoboyMind());
		comments.add(checkDBpedia(input));
		return new Reaction(determineNextState(input), comments);

	}

	protected State determineNextState(Interpretation input)
	{
		String sentence = (String) input.getFeatures().get(Linguistics.SENTENCE);

		if (TOASK==questionsCount)
		{
			return new WildTalkState();
		}
		else if (sentence.isEmpty())
		{
			return new IdleState();
		}
		return this;

	}

	private Interpretation checkRoboyMind()
	{
		Map<String, List<Concept>> matches = RoboyMind.getInstance().match(objectOfFocus);

		if (!matches.isEmpty() && matches.containsKey(currentIntent))
		{
			return new Interpretation("I know " + matches.get(currentIntent).size() + " people with the same " + currentIntent);
		}

		return new Interpretation("");
	}

	private Interpretation checkDBpedia(Interpretation input)
	{
		return new Interpretation("fun fact from DBpedia");
	}

	private List<Interpretation> checkOwnMemory(Interpretation input)
	{
		Triple triple = (Triple) input.getFeatures().get(Linguistics.TRIPLE);

		// reverse you <-> I
		if(triple.agens!=null && "you".equals(triple.agens.toLowerCase())) triple.agens = "i";
		if(triple.patiens!=null && "you".equals(triple.patiens.toLowerCase())) triple.patiens = "i";
		if(triple.predicate!=null && "are".equals(triple.predicate.toLowerCase())) triple.predicate = "am";

		//TODO remove this ugly parsing!
		List<Interpretation> result = new ArrayList<>();
		if(input.getSentenceType() == Linguistics.SENTENCE_TYPE.DOES_IT || input.getSentenceType() == Linguistics.SENTENCE_TYPE.IS_IT){
			List<Triple> t = PersistentKnowledge.getInstance().retrieve(new Triple(triple.predicate, triple.agens, null));
			if(!t.isEmpty()){
				result.add(new Interpretation("Yes. "));
				for(int i=0; i<t.size(); i++){
					String prefix = (i>0 && i==t.size()-1) ? "also, " : "";
					result.add(new Interpretation(prefix+t.get(i).agens+" "+t.get(i).predicate+" "+t.get(i).patiens));
				}
			}
		} else if(input.getSentenceType() == Linguistics.SENTENCE_TYPE.WHO){
			List<Triple> t = PersistentKnowledge.getInstance().retrieve(new Triple(triple.predicate, triple.agens, triple.patiens));
			if(!t.isEmpty()){
				for(int i=0; i<t.size(); i++){
					String prefix = (i>0 && i==t.size()-1) ? "also, " : "";
					result.add(new Interpretation(prefix+t.get(i).agens+" "+t.get(i).predicate+" "+t.get(i).patiens));
				}
			}
		} else if(input.getSentenceType() == Linguistics.SENTENCE_TYPE.WHAT){
			List<Triple> t = PersistentKnowledge.getInstance().retrieve(new Triple(triple.predicate, triple.agens, triple.patiens));
			if(!t.isEmpty()){

				for(int i=0; i<t.size(); i++){
					String prefix = (i>0 && i==t.size()-1) ? "also, " : "";
					result.add(new Interpretation(prefix+t.get(i).agens+" "+t.get(i).predicate+" "+t.get(i).patiens));
				}
			}
		} else if(input.getSentenceType() == Linguistics.SENTENCE_TYPE.HOW_DO){
			List<Triple> t = PersistentKnowledge.getInstance().retrieve(new Triple(triple.predicate, triple.agens, null));
			if(!t.isEmpty())
			{
				for(int i=0; i<t.size(); i++){
					String prefix = (i>0 && i==t.size()-1) ? "also, " : "";
					result.add(new Interpretation(prefix+t.get(i).agens+" "+t.get(i).predicate+" "+t.get(i).patiens));
				}
			}
		}
		return result;
	}




}