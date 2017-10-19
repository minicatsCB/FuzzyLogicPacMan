package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import com.fuzzylite.term.Trapezoid;
import com.fuzzylite.term.Triangle;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class MyPacMan extends Controller<MOVE>
{
	private MOVE myMove = MOVE.NEUTRAL;
	Engine engine = new Engine();	// Our Fuzzy Logic engine
	private EnumMap<GHOST, MOVE> myMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
	
	/*
	 * The Fuzzy Logic engine is set up in the constructor
	 * */
	public MyPacMan() {
		engine.setName("Fuzzy-PacMan");
		
		InputVariable inputVariable = new InputVariable();
		inputVariable.setEnabled(true);
		inputVariable.setName("Ghost");	// Distancia a Ghost (próximamente a CUALQUIER fantasma). Esta es la variable borrosa
		inputVariable.setRange(0.000, 150.000);	// Aqui hay que poner la maxima distancia posible a un fantasma (si está en el lado opuesto del laberinto (linea 61 DataTuple.java)?)
		inputVariable.addTerm(new Trapezoid("NEAR", 0.000, 0.000, 25.000, 50.000));	// Estos son los valores borrosos del conjunto borroso Distancia a Ghost
		inputVariable.addTerm(new Trapezoid("FAR", 25.000,50.000, 75.000, 150.000));
		engine.addInputVariable(inputVariable);
		
		
		OutputVariable outputVariable = new OutputVariable();
		outputVariable.setEnabled(true);
		outputVariable.setName("Action");
		outputVariable.setRange(0.000, 150.000); // Repasar esto
		outputVariable.fuzzyOutput().setAggregation(new Maximum());
		outputVariable.setDefuzzifier(new Centroid());	// Esto sí sé de lo que es
		outputVariable.setDefaultValue(Double.NaN);
		outputVariable.setLockValueInRange(false);
		outputVariable.setLockPreviousValue(false);
		outputVariable.addTerm(new Triangle("RUN", 0.000, 25.000, 50.000));
		outputVariable.addTerm(new Triangle("EATPILLS", 50.000, 75.000, 150.000));
		engine.addOutputVariable(outputVariable);
		
		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setEnabled(true);
		ruleBlock.setName("");
		ruleBlock.setConjunction(null);
		ruleBlock.setDisjunction(null);
		ruleBlock.setImplication(new Minimum());	// Esto pa que es??
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
		//Place your game logic here to play the game as Ms Pac-Man
		
		// Save the distance to each ghost in a GHOST/DISTANCE dictionary
		// We need this in order to know who is the closest ghost (to know his name and
		// the distance to him at the same time, two birds in one shot)
		Map<GHOST, Double> ghostsDistanceDictionary = new HashMap<GHOST, Double>();
		for(int i = 0; i < GHOST.values().length; i++) {
			ghostsDistanceDictionary.put(GHOST.values()[i], game.getEuclideanDistance(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(GHOST.values()[i])));
		}
		System.out.println("Distances: " + ghostsDistanceDictionary.entrySet());
		
		Entry<GHOST, Double> closestGhostDistancePair = getMinimumGhostDistancePair(ghostsDistanceDictionary);
		System.out.println("Closest ghost: " + getMinimumGhostDistancePair(ghostsDistanceDictionary));
		
		// Una vez que hemos configurado el engine (en el método de arriba),
		// le pasamos las variables que hayamos elegido de game (cerca, mucho tiempo, pocas pills, etc.)
		engine.setInputValue("Ghost", closestGhostDistancePair.getValue());
		// Y le decimos al engine que nos calcule el siguiente movimiento.
		engine.process();
		// Pero el engine nos lo da borrosificado, pues lo tenemos que desborrosificar
		OutputVariable runOutput = engine.getOutputVariable("Action");	
		System.out.println("Todo: " + runOutput.fuzzyOutputValue());	// Esto nos da los dos valores de pertenencia de RUN y EATPILLS juntos (con texto)
		
		String s = getHighestActivatedTerm(runOutput.fuzzyOutputValue());
				
		System.out.println("\n");
		
		// Por último, nos queda mapear el output a un MOVE real. Y fin.	
		// Ahora que ya tenemos la acción final, la llevamos a cabo
		int pacmanCurrentNodeIndex = game.getPacmanCurrentNodeIndex();
		if(s.equals("RUN")) {
			System.out.println("El fantasma está cerca. Correeeee!");
			int closestGhostNodeIndex = game.getGhostCurrentNodeIndex(closestGhostDistancePair.getKey());
			MOVE nextMoveAwayFromTarget = game.getNextMoveAwayFromTarget(pacmanCurrentNodeIndex, closestGhostNodeIndex, DM.EUCLID);
			System.out.println("Next move away: " + nextMoveAwayFromTarget);
			return nextMoveAwayFromTarget;
		}
		else if(s.equals("EATPILLS")) {
			System.out.println("El fantasma está lejos. Comeeeee!");
			int[] pillCurrentNodeIndex = game.getActivePillsIndices();
			int closestPillNodeIndex = game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), pillCurrentNodeIndex, DM.EUCLID);
			MOVE nextMoveTowardsTarget = game.getNextMoveTowardsTarget(pacmanCurrentNodeIndex, closestPillNodeIndex, DM.EUCLID);
			System.out.println("Next move towards: " + nextMoveTowardsTarget);
			return nextMoveTowardsTarget;
		}
		
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
