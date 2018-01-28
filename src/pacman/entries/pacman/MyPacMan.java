package pacman.entries.pacman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fuzzylite.Engine;
import com.fuzzylite.defuzzifier.Centroid;
import com.fuzzylite.norm.s.Maximum;
import com.fuzzylite.norm.t.Minimum;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Constant;
import com.fuzzylite.term.Trapezoid;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

import pacman.game.Game;
import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class MyPacMan extends Controller<MOVE>{
	private MOVE myMove = MOVE.NEUTRAL;
	Engine engine = new Engine();	// Our Fuzzy Logic engine
	private EnumMap<GHOST, MOVE> myMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
	
	double gene[];	// For testing
	
	/***
	 * The Fuzzy Logic engine is set up in the constructor
	 * Establece los parámetros que le manda el genético
	 * @param range el intervalo en el que se pueden mover los valores
	 * @param array los valores a fijar
	 */
	public MyPacMan() {
		gene = new double[] {0.000, 0.000, 25.000, 50.000, 25.000, 50.000, 75.000, 150.000, 0.000, 25.000, 50.000, 50.000, 75.000, 150.000};
		/////// System.out.println("\nConstructor MyFuzzyPacMan");
		engine.setName("Fuzzy-PacMan");
		
		InputVariable inputVariable = new InputVariable();
		inputVariable.setEnabled(true);
		// The fuzzy input variable
		inputVariable.setName("Ghost");
		// The maximum input range (line 61 DataTuple.java)?)
		inputVariable.setRange(0.0, 150.0);
		// The fuzzy input values of the fuzzy input variable
		inputVariable.addTerm(new Trapezoid("NEAR", gene[0], gene[1], gene[2], gene[3]));	// Cada uno de estos valores forma parte del array o individuo
		inputVariable.addTerm(new Trapezoid("FAR", gene[4],gene[5], gene[6], gene[7]));	// Estos valores tambiém forman parte del array o individuo
		engine.addInputVariable(inputVariable);
		
		
		OutputVariable outputVariable = new OutputVariable();
		outputVariable.setEnabled(true);
		// The fuzzy output variable (line 61 DataTuple.java)?)
		outputVariable.setName("Action");
		// The maximum output range
		outputVariable.setRange(0.0, 150.0);	// En este caso, los rangos coinciden, pero podría no ser así
		outputVariable.fuzzyOutput().setAggregation(new Maximum());
		outputVariable.setDefuzzifier(new Centroid()); // <---
		outputVariable.setDefaultValue(Double.NaN);
		outputVariable.setLockValueInRange(false);
		outputVariable.setLockPreviousValue(false);
		// The fuzzy output value of the fuzzy output variable
		outputVariable.addTerm(new Triangle("RUN", gene[8], gene[9], gene[10]));	// Este valor también forma parte del array o individuo
		outputVariable.addTerm(new Triangle("EATPILLS", gene[11], gene[12], gene[13]));	// Este valor también forma parte del array o individuo
		engine.addOutputVariable(outputVariable);
		
		
		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setEnabled(true);
		ruleBlock.setName("");
		ruleBlock.setConjunction(null);
		ruleBlock.setDisjunction(null);
		ruleBlock.setImplication(new Minimum());
		// Rules!!
		ruleBlock.addRule(Rule.parse("if Ghost is NEAR then Action is RUN", engine));
		ruleBlock.addRule(Rule.parse("if Ghost is FAR then Action is EATPILLS", engine));
		engine.addRuleBlock(ruleBlock);
	}
	
	Map<String, Double> finalAction = new HashMap<String, Double>();
	Pattern oPattern = Pattern.compile("[A-Z]+");
	Pattern mPattern = Pattern.compile("\\d+\\.\\d+");
	Matcher oMatcher, mMatcher;
	public MOVE getMove(Game game, long timeDue) 
	{
		/////// System.out.println("getMove MyFuzzyPacMan");
		//Place your game logic here to play the game as Ms Pac-Man
		
		// Save the distance to each ghost in a GHOST/DISTANCE dictionary
		// We need this in order to know who is the closest ghost (to know his name and
		// the distance to him at the same time, two birds in one shot)
		Map<GHOST, Double> ghostsDistanceDictionary = new HashMap<GHOST, Double>();
		for(int i = 0; i < GHOST.values().length; i++) {
			ghostsDistanceDictionary.put(GHOST.values()[i], game.getEuclideanDistance(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(GHOST.values()[i])));
		}
		//System.out.println("Distances: " + ghostsDistanceDictionary.entrySet());
		
		// Choose the closest ghost distance and pass it to the fuzzy logic engine
		Entry<GHOST, Double> closestGhostDistancePair = getMinimumGhostDistancePair(ghostsDistanceDictionary);
		/////// System.out.println("Closest ghost: " + getMinimumGhostDistancePair(ghostsDistanceDictionary));
		engine.setInputValue("Ghost", closestGhostDistancePair.getValue());
		
		// Engine, work!!
		engine.process();
		
		// But the engine give as the result fuzzified, so we need to defuzzify it
		OutputVariable runOutput = engine.getOutputVariable("Action");	
		/////// System.out.println("All: " + runOutput.fuzzyOutputValue()); // This returns a string with each action and its membership value
		
		// Choose the action with the highest membership value
		String s = getHighestActivatedTerm(runOutput.fuzzyOutputValue());
		
		// Last, map the the action to do to a PacMan MOVE
		int pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
		if(s.equals("RUN")) {
			/////// System.out.println("The ghost is near. RUUUUUUN!");
			int closestGhostNodeIndex = game.getGhostCurrentNodeIndex(closestGhostDistancePair.getKey());
			MOVE nextMoveAwayFromTarget = game.getNextMoveAwayFromTarget(pacmanCurrentNodeIndex, closestGhostNodeIndex, DM.EUCLID);
			/////// System.out.println("Next move away: " + nextMoveAwayFromTarget);
			return nextMoveAwayFromTarget;
		}
		else if(s.equals("EATPILLS")) {
			/////// System.out.println("The ghost is far. EAAAAAAT!");
			MOVE nextMoveTowardsTarget = MOVE.NEUTRAL;
			int[] pillCurrentNodeIndex = game.getActivePillsIndices();
			// Check if there are pills available in the level
			if(pillCurrentNodeIndex.length != 0) {
				int closestPillNodeIndex = game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), pillCurrentNodeIndex, DM.EUCLID);
				nextMoveTowardsTarget = game.getNextMoveTowardsTarget(pacmanCurrentNodeIndex, closestPillNodeIndex, DM.EUCLID);	
			}else {
				// System.out.println("There are not more pills available in this level!");
			}
			/////// System.out.println("Next move towards: " + nextMoveTowardsTarget);
			return nextMoveTowardsTarget;
		}
		
		/////// System.out.println("\n");
		
		return myMove;
	}

	public String getHighestActivatedTerm(String fuzzyOutputValue) {
		Map<String, Double> dictionary = convertToDictionary(fuzzyOutputValue);
		Entry<String, Double> s = getMaximumActionValuePair(dictionary);
		dictionary.clear();
		
		return s.getKey();
	}
	
	public Map<String, Double> convertToDictionary(String fuzzyOutputValue) {
		Map<String, Double> dictionary = new HashMap<String, Double>();
		
		// Extraemos los valores de pertenencia de la cadena de texto
		Pattern p = Pattern.compile("\\d+\\.\\d+/[A-Z]+");
		Matcher m = p.matcher(fuzzyOutputValue);
		
		// Separa los números de las letras, y los guarda en un diccionario, cada variable con su valor de pertenencia
		String s = null;
		Double d = null;
		while (m.find()) {
			mMatcher = mPattern.matcher(m.group());
			while (mMatcher.find()) {
				//System.out.println("Pertenencia: " + mMatcher.group());
				d = Double.parseDouble(mMatcher.group());
			}
			oMatcher = oPattern.matcher(m.group());
			while (oMatcher.find()) {
				//System.out.println("Acción: " + oMatcher.group());
				s = oMatcher.group();
			}
			dictionary.put(s, d);
			}
		
		return dictionary;
	}
	
	public Entry<String, Double> getMaximumActionValuePair(Map<String, Double> dictionary) {
		Entry<String, Double> s = null;
		Object[] a = dictionary.entrySet().toArray();
		Arrays.sort(a, new Comparator() {
		    public int compare(Object o1, Object o2) {
		        return ((Map.Entry<String, Double>) o2).getValue()
		                   .compareTo(((Map.Entry<String, Double>) o1).getValue());
		    }
		});
		
		// No hace falta imprimir el set ordenado, ya nos vamos a quedar con el valor
		// de la primera posición antes. a es un array de Entries
//		for (Object e : a) {
//		    System.out.println(((Map.Entry<String, Double>) e).getKey() + " : "
//		            + ((Map.Entry<String, Double>) e).getValue());
//		}
		
		s = (Map.Entry<String, Double>)a[0];
		//System.out.println("Final action/membership pair: " + s);
		
		return s;
	}
	
	public Entry<GHOST, Double> getMinimumGhostDistancePair(Map<GHOST, Double> dictionary) {
		Entry<GHOST, Double> s = null;
		Object[] a = dictionary.entrySet().toArray();
		Arrays.sort(a, new Comparator() {
		    public int compare(Object o1, Object o2) {
		        return ((Map.Entry<GHOST, Double>) o1).getValue()
		                   .compareTo(((Map.Entry<GHOST, Double>) o2).getValue());
		    }
		});
		
		// No hace falta imprimir el set ordenado, ya nos vamos a quedar con el valor
		// de la primera posición antes
//		for (Object e : a) {
//		    System.out.println(((Map.Entry<GHOST, Double>) e).getKey() + " : "
//		            + ((Map.Entry<GHOST, Double>) e).getValue());
//		}
		
		//System.out.println("Final pair: " + (Map.Entry<GHOST, Double>)a[0]);
		
		s = (Map.Entry<GHOST, Double>)a[0];
		
		return s;
	}
}
