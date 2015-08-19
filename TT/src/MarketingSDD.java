

import static java.lang.System.out;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import solvers.NonStarCountSolvers;
import solvers.Rule;
import solvers.Scorer;
import dataextraction.Marketing;
import dataextraction.SampleHandler;
import dataextraction.TableInfo;

/**
 * Servlet implementation class MarketingSDD
 */
@WebServlet("/MarketingSDD")
public class MarketingSDD extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MarketingSDD() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void init(ServletConfig config) throws ServletException {
    	String[] args = new String[5]; // This and things that use args need to be moved.
    	
    	TableInfo fullTable = null;
		try {
			fullTable = Marketing.parseData();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//out.println(System.currentTimeMillis() - timer);
		List<Integer> columns = new ArrayList<Integer>();
		final Integer firstNumColumns = 7;//9;
		for (int i = 1; i < firstNumColumns; i++) {
			columns.add(i);
		}
		TableInfo table = fullTable.getSubTable(columns);
		Integer ruleNums = Integer.parseInt(args[0]); 
		Integer maxRuleScore = Integer.parseInt(args[1]); // program input
		final Scorer scorer; // program input
		switch(args[2]) {
			case "Size" :
				scorer = new Rule.sizeScorer(); 
				break;
			case "Bits" :
				scorer = new Rule.sizeBitsScorer();
				break;
			case "Square" :
				scorer = new Rule.sizeSquareScorer();
				break;
			case "Min2" : 
				scorer = new Rule.sizeMinusKScorer(1);
				break;
			default :
				scorer = new Rule.sizeScorer();
				break;
		}
		String ruleString = args[3];
		ruleString = ruleString.substring(1, ruleString.length() - 1);
		String[] vals = ruleString.split(",");
		List<Integer> ruleVals = new ArrayList<Integer>();
		for (int col = 0; col < table.dictionary.size(); col++) {
			final String valString = vals[col];
			ruleVals.add(-1);
			for (int val = 0; val < table.dictionary.get(col).size(); val++) {
				if (table.getName(col, val).equals(valString)) {
					ruleVals.set(col, val);
					break;
				}
			}
		}
		Rule rule = new Rule(ruleVals);
		final List<Integer> defaultCols = new ArrayList<Integer>();
		final List<Integer> ignoreCols = new ArrayList<Integer>();
		final List<Integer> forceCols = new ArrayList<Integer>();
		String coloptsString = args[4];
		coloptsString = coloptsString.substring(1, coloptsString.length() - 1);
		String[] colopts = coloptsString.split(",");
		for (String colopt : colopts) {
			int colonLocation = colopt.lastIndexOf(":");
			final String colName = colopt.substring(0, colonLocation);
			final String status = colopt.substring(colonLocation + 1); 
			int colNo = -1;
			for (colNo = 0; colNo < table.dictionary.size(); colNo++) {
				if(table.names.get(colNo).get("column").equals(colName)) {
					break;
				}
			}
			switch (status) {
				case "Default" :
					defaultCols.add(colNo);
					break;
				case "Ignore" :
					ignoreCols.add(colNo);
					break;
				case "Force" :
					forceCols.add(colNo);
					break;
			}
		}
		Scorer modifiedScorer = new Scorer () {
			@Override
			public void setScore(TableInfo table, Rule rule) {
				for (Integer col : forceCols) {
					if (rule.get(col) == -1) {
						rule.score = 0;
						return;
					}
				}
				Rule tempRule = rule.deepValuesCopy();
				for (Integer col : ignoreCols) {
					tempRule.addVal(col, -1);
				}
				scorer.setScore(table, tempRule);
				rule.score = tempRule.score;
			}
		};
		int minSampleSize = Integer.MAX_VALUE;
		int capacity = Integer.MAX_VALUE;
		SampleHandler sampleHandler = new SampleHandler(table, capacity, minSampleSize);
		Set<Rule> solutionSet = null;
		try{
			solutionSet = NonStarCountSolvers.getSolution (table, rule, ruleNums, maxRuleScore, modifiedScorer, -1, sampleHandler);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String solutionString = "[";
		boolean start = true;
		for (Rule solRule : solutionSet) {		
			if(!start) {
				solutionString = solutionString + ",";
			} else {
				start = false;				
			}
			solutionString = solutionString + "{\"vals\":[";
			for (Integer col = 0; col < table.names.size(); col++) {
				if (col > 0) {
					solutionString = solutionString + ",";
				}
				solutionString = solutionString + "\"" + table.getName(col, solRule.get(col)) + "\"";
			}
			solutionString = solutionString + "," + solRule.count;
			solutionString = solutionString + "," + solRule.score;
			solutionString = solutionString + "]}";
		}
		solutionString = solutionString + "]";
		out.println(solutionString);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

	      // Actual logic goes here.
	    PrintWriter pw = response.getWriter();
	    pw.println("<h1>" + "Hi" + "</h1>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

	      // Actual logic goes here.
	    PrintWriter pw = response.getWriter();
	    pw.println("<h1>" + "Hi" + "</h1>");
	}

}
