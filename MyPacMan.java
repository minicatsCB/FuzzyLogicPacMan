package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
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
	private MOVE myMove = MOVE.RIGHT;
	Engine engine = new Engine();	// Our Fuzzy Logic engine
	private EnumMap<GHOST, MOVE> myMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
	
	/*
	 * The Fuzzy Logic engine is set up in the constructor
	 * */
	public MyPacMan() {
		engine.setName("Fuzzy-PacMan");
		
		InputVariable inputVariable = new InputVariable();
		inputVariable.setEnabled(true);
		inputVariable.setName("Blinky");	// Distancia a Blinky (próximamente a CUALQUIER fantasma). Esta es la variable borrosa
		inputVariable.setRange(0.000, 150.000);	// Aqui hay que poner la maxima distancia posible a un fantasma (si está en el lado opuesto del laberinto (linea 61 DataTuple.java)?)
		inputVariable.addTerm(new Trapezoid("NEAR", 0.000, 0.000, 25.000, 50.000));	// Estos son los valores borrosos del conjunto borroso Distancia a Blinky
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
		ruleBlock.addRule(Rule.parse("if Blinky is NEAR then Action is RUN", engine));
		ruleBlock.addRule(Rule.parse("if Blinky is FAR then Action is EATPILLS", engine));
		engine.addRuleBlock(ruleBlock);
	}
	
	ArrayList<Double> outputMemberships = new ArrayList<Double>();
	Map<String, Double> dictionary = new HashMap<String, Double>();
	Map<String, Double> finalAction = new HashMap<String, Double>();
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as Ms Pac-Man
		// Una vez que hemos configurado el engine (en el método de arriba),
		// le pasamos las variables que hayamos elegido de game (cerca, mucho tiempo, pocas pills, etc.)
		double distanceToBlinky = game.getEuclideanDistance(game.getGhostCurrentNodeIndex(GHOST.BLINKY), game.getPacmanCurrentNodeIndex());
		//double distanceToBlinky = game.getDistance(game.getGhostCurrentNodeIndex(GHOST.BLINKY), game.getPacmanCurrentNodeIndex(), DM.EUCLID);
		engine.setInputValue("Blinky", distanceToBlinky);
		// Y le decimos al engine que nos calcule el siguiente movimiento.
		engine.process();
		// Pero el engine nos lo da borrosificado, pues lo tenemos que desborrosificar
		OutputVariable runOutput = engine.getOutputVariable("Action");
		//runOutput.fuzzyOutput().highestActivatedTerm();	// Esto te da la pertenencia final
		// Por último, nos queda mapear el output a un MOVE real. Y fin.
		System.out.println("Distance to Blinky: " + distanceToBlinky);
		
		System.out.println("Todo: " + runOutput.fuzzyOutputValue());	// Esto nos da los dos valores de pertenencia de RUN y EATPILLS juntos (con texto)
		// Extraemos los valores de pertenencia de la cadena de texto
		Pattern p = Pattern.compile("\\d+\\.\\d+/[A-Z]+");
		Matcher m = p.matcher(runOutput.fuzzyOutputValue());
		
		// Separa el número de las letras, y los guarda en un diccionario, cada variable con su valor de pertenencia
		Pattern oPattern = Pattern.compile("[A-Z]+");
		Pattern mPattern = Pattern.compile("\\d+\\.\\d+");
		Matcher oMatcher, mMatcher;
		String s = null;
		Double d = null;
		while (m.find()) {
			//outputMemberships.add(Double.parseDouble(m.group()));
			mMatcher = mPattern.matcher(m.group());
			while (mMatcher.find()) {
				//System.out.println("Aquí: " + mMatcher.group());
				d = Double.parseDouble(mMatcher.group());
			}
			oMatcher = oPattern.matcher(m.group());
			while (oMatcher.find()) {
				//System.out.println("Aquí: " + oMatcher.group());
				s = oMatcher.group();
			}
			dictionary.put(s, d);
			}
		
		//System.out.println(dictionary.entrySet());
		
		Object[] a = dictionary.entrySet().toArray();
		Arrays.sort(a, new Comparator() {
		    public int compare(Object o1, Object o2) {
		        return ((Map.Entry<String, Double>) o2).getValue()
		                   .compareTo(((Map.Entry<String, Double>) o1).getValue());
		    }
		});
		
		//System.out.println("Hey: " + a[0]);
		oMatcher = oPattern.matcher(a[0].toString());
		while (oMatcher.find()) {
			//System.out.println("Aquí final: " + oMatcher.group());
			s = oMatcher.group();
		}
		System.out.println("Aqui final s: " + s);
		
		
		// No hace falta imprimir el set ordenado, ya nos hemos quedado con el valor
		// de la primera posición antes
//		for (Object e : a) {
//		    System.out.println(((Map.Entry<String, Double>) e).getKey() + " : "
//		            + ((Map.Entry<String, Double>) e).getValue());
//		}
				
		// Mostramos los valores depertenencia extraídos por pantalla
//		for(int i = 0; i < outputMemberships.size(); i++) {
//			System.out.println(outputMemberships.get(i));
//		}
		
		// Nos quedamos con el mayor valor de pertenencia
//		double maxMemebership = Collections.max(outputMemberships);
//		System.out.println("Max: " + maxMemebership);
		//outputMemberships.clear();
		dictionary.clear();
		
		System.out.println("\n");
		
		
		// Ahora que ya tenemos la acción final, la llevamos a cabo
		if(s.equals("RUN")) {
			System.out.println("El fantasma está cerca. Correeeee!");
		}
		else if(s.equals("EATPILLS")) {
			System.out.println("El fantasma está lejos. Comeeeee!");
		}
		
		return myMove;
	}
}