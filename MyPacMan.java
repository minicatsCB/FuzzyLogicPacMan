package pacman.entries.pacman;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.EnumMap;

import com.fuzzylite.Engine;
import com.fuzzylite.activation.General;
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
		inputVariable.setName("Blinky");	// Distancia a Blinky (próximamente a CUALQUIER fantasma)
		inputVariable.setRange(0.000, 150.000);	// Aqui hay que poner la maxima distancia posible a un fantasma (si está en el lado opuesto del laberinto (linea 61 DataTuple.java)?)
		inputVariable.addTerm(new Triangle("NEAR", 0.000, 25.000, 50.000));	// Seguro que triángulos es lo mejor? Porque una distancia de 0.000 es cerca no, lo siguiente...
		inputVariable.addTerm(new Triangle("MEDIUM", 25.000, 50.000, 75.000));
		inputVariable.addTerm(new Triangle("FAR", 75.000, 100.000, 150.000));
		engine.addInputVariable(inputVariable);
		
		
		OutputVariable outputVariable = new OutputVariable();
		outputVariable.setEnabled(true);
		outputVariable.setName("Run");
		outputVariable.setRange(0.000, 150.000); // Repasar esto
		outputVariable.fuzzyOutput().setAggregation(new Maximum());
		outputVariable.setDefuzzifier(new Centroid(200));	// Esto sí sé de lo que es
		outputVariable.setDefaultValue(Double.NaN);
		outputVariable.setLockValueInRange(false);
		outputVariable.setLockPreviousValue(false);
		outputVariable.addTerm(new Triangle("NEAR", 0.000, 0.000, 25.000, 50.000));
		outputVariable.addTerm(new Triangle("MEDIUM", 25.000, 50.000, 75.000));
		outputVariable.addTerm(new Triangle("FAR", 75.000, 100.000, 150.000));
		engine.addOutputVariable(outputVariable);
		
		RuleBlock ruleBlock = new RuleBlock();
		ruleBlock.setEnabled(true);
		ruleBlock.setName("");
		ruleBlock.setConjunction(null);
		ruleBlock.setDisjunction(null);
		ruleBlock.setImplication(new Minimum());	// Esto pa que es??
		ruleBlock.setActivation(new General());	// Y esto??
		ruleBlock.addRule(Rule.parse("if Blinky is NEAR then Run is FAR", engine));
		ruleBlock.addRule(Rule.parse("if Blinky is MEDIUM then Run is MEDIUM", engine));
		ruleBlock.addRule(Rule.parse("if Blinky is FAR then Run is NEAR", engine));
		engine.addRuleBlock(ruleBlock);
	}
	
	public MOVE getMove(Game game, long timeDue) 
	{
		//Place your game logic here to play the game as Ms Pac-Man
		// Una vez que hemos configurado el engine (en el método de arriba),
		// le pasamos las variables que hayamos elegido de game (cerca, mucho tiempo, pocas pills, etc.)
		double distanceToBlinky = game.getDistance(game.getGhostCurrentNodeIndex(GHOST.BLINKY), game.getPacmanCurrentNodeIndex(), DM.EUCLID);
		engine.setInputValue("Blinky", distanceToBlinky);
		// Y le decimos al engine que nos calcule el siguiente movimiento.
		engine.process();
		// Pero el engine nos lo da borrosificado, pues lo tenemos que desborrosificar
		OutputVariable runOutput = engine.getOutputVariable("Run");
		runOutput.fuzzyOutput().highestActivatedTerm();	// Esto te da la pertenencia final
		// Por último, nos queda mapear el output a un MOVE real. Y fin.
		System.out.println(runOutput);
		
		return myMove;
	}
}
