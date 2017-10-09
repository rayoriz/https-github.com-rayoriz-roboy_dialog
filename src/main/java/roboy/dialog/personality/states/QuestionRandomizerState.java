package roboy.dialog.personality.states;

import java.util.*;

import roboy.linguistics.sentenceanalysis.Interpretation;
import roboy.memory.Neo4jRelations;
import roboy.memory.nodes.Interlocutor;
import roboy.util.JsonUtils;

import static roboy.memory.Neo4jRelations.*;

/**
 * Manages the questions that can be asked from a person.
 * Coupled with Neo4j information about the person to prevent duplicates.
 */
public class QuestionRandomizerState implements State{
	// Favorite movie currently not represented in memory.
	//private static final String MOVIE = "likes the movie";
	// TODO ask memory to add relation.
	// TODO There are new predicates from memory, need integrating.
	
	private ArrayList<PersonalQAState> questionStates = new ArrayList<>();
	private PersonalQAState locationQuestion;
	private HashMap<Neo4jRelations, Boolean> alreadyAsked;
	private State inner;
	private State chosenState;
	private Interlocutor person;

	// All spoken phrases for asking questions are stored in these JSON files.
	String questionsFile = "questions.json";
	String successAnswersFile = "successAnswers.json";
	String failureAnswersFile = "failureAnswers.json";
	Map<String, List<String>> questions;
	Map<String, List<String[]>> successAnswers;
	Map<String, List<String>> failureAnswers;
	
	public QuestionRandomizerState(State inner, Interlocutor person, String questionsFolder) {
		this.inner = inner;
		this.person = person;
		questions = JsonUtils.getSentencesFromJsonFile(questionsFolder+questionsFile);
		successAnswers = JsonUtils.getSentenceArraysFromJsonFile(questionsFolder+successAnswersFile);
		failureAnswers = JsonUtils.getSentencesFromJsonFile(questionsFolder+failureAnswersFile);
		// alreadyAsked is filled automatically by the initializeQuestion method,
		// then updated to match already existing information with checkForAskedQuestions()
		alreadyAsked = new HashMap<>();

		locationQuestion = initializeQuestion(FROM);
		LocationDBpedia locationDBpedia = new LocationDBpedia();
		locationDBpedia.setSuccess(this);
		locationDBpedia.setFailure(this);
		locationQuestion.setSuccess(locationDBpedia);
//		questionStates = new PersonalQAState[]{
//				locationQuestion,
//				initializeQuestion(HAS_HOBBY),
//				initializeQuestion(WORK_FOR),
//				initializeQuestion(STUDY_AT),
//				initializeQuestion(MOVIE),
//// TODO request support for Occupation and Movie data in the database.
//			 initializeQuestion(OCCUPATION)
////			 initializeQuestion(LIKES_MOVIE),
//		};
//		ArrayList qs = new ArrayList<PersonalQAState>();
		questionStates.add(locationQuestion);
		Iterator it = questions.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry pair = (Map.Entry) it.next();
			if (!pair.getKey().equals("FROM"))
			{
				Neo4jRelations r = Neo4jRelations.valueOf((String) pair.getKey());
				questionStates.add(initializeQuestion(r));
			}
		}
	}

	@Override
	public List<Interpretation> act() {
		checkForAskedQuestions();
		// TODO Remove old code after successfully switching to Neo4j memory
		//WorkingMemory memory = WorkingMemory.getInstance();

		// chosenState refers to the question to be asked.
		chosenState = null;
		// List<Triple> nameTriples = memory.retrieve(new Triple("is","name",null));
		// if(nameTriples.isEmpty()) return inner.act();
		// String name = nameTriples.get(0).patiens;
		//List<Triple> infos = memory.retrieve(new Triple(null,name,null));
		//infos.addAll(memory.retrieve(new Triple(null,null,name)));
		//TODO why this if statement?
		if(Math.random()<1){

			int index = (int) (Math.random()*questionStates.size());
			if(!alreadyAsked.get(questionStates.get(index).predicate)){
				alreadyAsked.put(questionStates.get(index).predicate, true);
				chosenState = questionStates.get(index);
				return chosenState.act();
			}
		}
		return inner.act();
	}

	@Override
	public Reaction react(Interpretation input) {
		if(chosenState==null){
			return inner.react(input);
		}
		return chosenState.react(input);
	}

	public void setTop(State top){
		for(int i=1; i<questionStates.size(); i++){
			questionStates.get(i).setNextState(top);
		}
	}

	private PersonalQAState initializeQuestion(Neo4jRelations relation) {
		alreadyAsked.put(relation, false);
		return new PersonalQAState(
				questions.get(relation.type),
				failureAnswers.get(relation.type),
				successAnswers.get(relation.type),
				relation, person);

	}

	private void checkForAskedQuestions() {
		for(Neo4jRelations relation : alreadyAsked.keySet()) {
			if(person.hasRelation(relation)) alreadyAsked.put(relation, true);
		}
	}
	
}
