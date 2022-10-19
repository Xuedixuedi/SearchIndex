/**
 * @author Group 16
 * This code is provided solely as sample code for using Lucene.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.ScoreDoc;

public class LuceneTester {

    /**
     * Define the paths for the data file and the lucene index
     */
    public static final String DATA_FILE = "/Users/xuedixuedi/lxdThings/Study/22Fall/text/text_dataset/Musical_Instruments_5.json";
    public static final String INDEX_PATH = "/Users/xuedixuedi/lxdThings/Study/22Fall/text/text_dataset/luceneIndex";


    public static String Analysis(String query) throws Exception {
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream token_string = analyzer.tokenStream(null, query);

        TokenStream lower_string = new LowerCaseFilter(token_string);
        // TokenStream stem_string = new KStemFilter(lower_string);
        TokenStream stop_string = new StopFilter(lower_string, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);


        CharTermAttribute attri = stop_string.addAttribute(CharTermAttribute.class);
        stop_string.reset();

        List<String> result_token = new ArrayList<>();
        while (stop_string.incrementToken()) {
            result_token.add(attri.toString());
        }
        analyzer.close();
        stop_string.close();

        return String.join(" ", result_token);
    }

    public static void runIndex() throws Exception {
        QAIndexer indexer = new QAIndexer(LuceneTester.INDEX_PATH);
        indexer.indexQAs(LuceneTester.DATA_FILE);
    }

    public static void main(String[] arg) throws Exception {

        Scanner s = new Scanner(System.in);
        String userInput;


        // To perform indexing. If there is no change to the data file, index only need to be created once
        System.out.println("Do you need index? Please input 'Y' or 'N'.");
        String index = s.nextLine();
        if ("Y".equals(index)) {
            runIndex();
        }

        System.out.println("Please enter your search content: ");
        userInput = s.nextLine();


        System.out.println("Please enter your search field: 1:reviewText, 2:asin, 3:reviewerID ");
        String searchField = s.nextLine();

        int userResult = 0;
        boolean inputCheck;
        System.out.println("Please enter the number of results returned: ");
        do {
            inputCheck = true;
            try {
                userResult = s.nextInt();
                if (userResult <= 0) {
                    System.out.println("You must enter a positive number! Please re-enter: ");
                }
            } catch (Exception e) {
                System.out.println("Input must be a positive integer! Please re-enter:");
                inputCheck = false;
                s.nextLine();
            }
        } while (inputCheck == false || userResult <= 0);


        //search index
        QASearcher searcher = new QASearcher(LuceneTester.INDEX_PATH);

        long queryStart;
        long queryFinish;
        ScoreDoc[] hits;
        long time;


        switch (searchField) {
            case "1":
                String inputAnalysis = userInput.toLowerCase();
                if (!userInput.contains("and") && !userInput.contains("or") && !userInput.contains("not")) {
                    inputAnalysis = Analysis(userInput);
                }

                //search for keywords "iphone" in field "question", and request for the top 20 results
                queryStart = System.currentTimeMillis();
                hits = searcher.search("reviewText", inputAnalysis, userResult);
                queryFinish = System.currentTimeMillis();
                time = queryFinish - queryStart;

                System.out.println("The time to process a query in reviewText field is " + time + " Millisecond(" + time / 1000 + " Second)");
                System.out.println("\n=================Results for review text search=============\n");
                searcher.printResult(hits);
                break;


            case "2":
                queryStart = System.currentTimeMillis();
                hits = searcher.search("asin", userInput.toLowerCase(), userResult);
                queryFinish = System.currentTimeMillis();
                time = queryFinish - queryStart;

                System.out.println("The time to process a query in asin field is " + time + " Millisecond(" + time / 1000 + " Second)");
                System.out.println("\n=================Results for asin search=============\n");
                searcher.printResult(hits);
                break;

            case "3":
                queryStart = System.currentTimeMillis();
                hits = searcher.search("reviewerId", userInput.toUpperCase(), userResult);
                queryFinish = System.currentTimeMillis();
                time = queryFinish - queryStart;

                System.out.println("The time to process a query in reviewerID field is " + time + " Millisecond(" + time / 1000 + " Second)");
                System.out.println("\n=================Results for reviewerID search=============\n");
                searcher.printResult(hits);
                break;

            default:
                break;
        }
        s.close();


    }

}