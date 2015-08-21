package servlets;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

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
    String message;   
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MarketingSDD() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public void init(ServletConfig config) throws ServletException {
    	message = "Hi";
    	String[] args = new String[5]; // This and things that use args need to be moved.
    	
    	TableInfo fullTable = null;
    	//InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(Marketing.DATAFILELOCATION);
    	InputStream input = null;
    	try {
    		/*
    		input = getClass().getClassLoader().getResourceAsStream("marketing.data.ser");
    		ObjectInputStream oi = new ObjectInputStream(input);
    		message = "damn";
        	fullTable = (TableInfo) oi.readObject();
        	message = "damn2";
        	message = ((Integer)fullTable.contents.size()).toString();
    		oi.close();
    		*/
    		
    		input = getClass().getClassLoader().getResourceAsStream("marketing.data.txt");
    		String data = convertStreamToString(input);
    		List<List<String>> dictionary = new ArrayList<List<String>>();
    		List<Map<String, Integer>> reverseDictionary = new ArrayList<Map<String, Integer>>();
    		List<List<Integer>> contents = new ArrayList<List<Integer>>();
    		String[] lines = data.split("\n");
    		boolean firstLine = true;
    		for (String line : lines) {
				String[] vals = line.split(" ");
				if (firstLine) {
    				for (int i = 0; i < vals.length; i++) {
    					dictionary.add(new ArrayList<String>());
    					reverseDictionary.add(new HashMap<String, Integer>());
    				}
    				firstLine = false;
				}
				List<Integer> tuple = new ArrayList<Integer>(vals.length);
				for (int i = 0; i < vals.length; i++) {
					final String value = vals[i];
					Map<String, Integer> columnDictionary = reverseDictionary.get(i);
					if (columnDictionary.containsKey(value)) {
						tuple.add(columnDictionary.get(value));
					} else {
						columnDictionary.put(value, columnDictionary.keySet().size());
						dictionary.get(i).add(value);
						tuple.add(columnDictionary.get(value));
					}
				}
				contents.add(tuple);
    		}
    		fullTable = new TableInfo(dictionary, reverseDictionary, contents);
    		Marketing.addNames(fullTable);    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    		
		List<Integer> columns = new ArrayList<Integer>();
		final Integer firstNumColumns = 7;//9;
		for (int i = 1; i < firstNumColumns; i++) {
			columns.add(i);
		}
		
		TableInfo table = fullTable.getSubTable(columns);
		/*Integer ruleNums = Integer.parseInt(args[0]); 
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
    	*/
    }
    
    public void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        //The following are CORS headers. Max age informs the 
        //browser to keep the results of this call for 1 day.
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setHeader("Access-Control-Max-Age", "86400");
        //Tell the browser what requests we allow.
        resp.setHeader("Allow", "GET, HEAD, POST, TRACE, OPTIONS");
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.setHeader("Access-Control-Allow-Origin", "*");
		//response.setContentType("text/html");
		response.setContentType("application/json");
		PrintWriter pw = response.getWriter();
	    //pw.println("<h1>" + message + "</h1>");
		pw.print("[1, 2, 4, 5]");
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Access-Control-Allow-Origin", "*");
		//response.setContentType("text/html");
		response.setContentType("application/json");
		PrintWriter pw = response.getWriter();
	    //pw.println("<h1>" + message + "</h1>");
		pw.print("[1, 2, 4, 5]");
		BufferedReader br = request.getReader();
		String line = "";
		line = br.readLine();
		line = line.substring(1, line.length() - 1);
		line = line + ",";
		int k = Integer.parseInt(line.substring(4 + line.indexOf("\"k\":"), line.indexOf(",", line.indexOf("\"k\":"))));
		int mw = Integer.parseInt(line.substring(5 + line.indexOf("\"mw\":"), line.indexOf(",", line.indexOf("\"mw\":"))));
		int rowNo = Integer.parseInt(line.substring(8 + line.indexOf("\"rowNo\":"), line.indexOf(",", line.indexOf("\"rowNo\":"))));
		String W = line.substring(5 + line.indexOf("\"W\":\""), line.indexOf("\",", line.indexOf("\"W\":\"")));
		String coloptString = line.substring(10 + line.indexOf("\"colopt\":{"), line.indexOf("}", line.indexOf("\"colopt\":{")));
		coloptString.replaceAll("\"", "");
		
		String rulesString = line.substring(9 + line.indexOf("\"rules\":["), 1 + line.indexOf("}]", line.indexOf("\"rules\":[")));
		rulesString = rulesString.substring(1, rulesString.length() - 1);
		String[] rulesStringsArray = rulesString.split("\\},\\{");
		List<String> valsList = new ArrayList<String>();
		List<Integer> depthsList = new ArrayList<Integer>();
		List<Boolean> expandedsList = new ArrayList<Boolean>(); 
		for (String s : rulesStringsArray) {
			s = s + ",";
			valsList.add(s.substring(1 + s.indexOf("["), s.indexOf("]")));
			depthsList.add(Integer.parseInt(s.substring(8 + s.indexOf("\"depth\":"), s.indexOf(",", s.indexOf("\"depth\":")))));
			expandedsList.add(s.substring(11 + s.indexOf("\"expanded\":"), 12 + s.indexOf("\"expanded\":")).equals("1"));
		}
		out.println(k);
		out.println(mw);
		out.println(W);
		out.println(rowNo);
		out.println(coloptString);
		out.println(valsList.toString());
		out.println(depthsList.toString());
		out.println(expandedsList.toString());
		// row and depthStr can be generated on the fly
		//List<String> ruleStringsList = new ArrayList<String>();
		out.println(rowNo);
		out.println(rulesString);
		out.println(line);
		
	}

}
