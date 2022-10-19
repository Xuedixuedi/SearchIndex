

import java.io.IOException;

/**
 * @author Group 16
 * This code is provided solely as sample code for using Lucene.
 *
 */


import java.nio.file.Paths;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;

public class QASearcher {

    private IndexSearcher lSearcher;
    private IndexReader lReader;

    public QASearcher(String dir) {
        try {
            //create an index reader and index searcher
            lReader = DirectoryReader.open(FSDirectory.open(Paths.get(dir)));
            lSearcher = new IndexSearcher(lReader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //report the number of documents indexed
    public int getCollectionSize() {
        return this.lReader.numDocs();
    }

    //search for keywords in specified field, with the number of top results
    public ScoreDoc[] search(String field, String keywords, int numHits) {
        //the query has to be analyzed the same way as the documents being index
        //using the same Analyzer
        QueryBuilder builder = new QueryBuilder(new StandardAnalyzer());
        Query booleanQuery = builder.createBooleanQuery(field, keywords);
        Query phraseQuery = builder.createPhraseQuery(field, keywords);

        ScoreDoc[] hits = null;
        try {

            //Create a TopScoreDocCollector
            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, 300);
            if ((!keywords.contains("and") && !keywords.contains("or") && !keywords.contains("not")) && keywords.contains(" ")) {
                lSearcher.search(phraseQuery, collector);
            } else {
                lSearcher.search(booleanQuery, collector);
            }
            //collect results
            hits = collector.topDocs().scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hits;
    }


    public ScoreDoc[] sortResult(ScoreDoc[] hits, String field) throws NumberFormatException, IOException {
        int n = hits.length;
        int a;
        int b;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                a = Integer.parseInt(this.lReader.document(hits[j].doc).get(field));
                b = Integer.parseInt(this.lReader.document(hits[j + 1].doc).get(field));

                if (a < b) {
                    ScoreDoc temp = hits[j];
                    hits[j] = hits[j + 1];
                    hits[j + 1] = temp;
                }
            }
        }
        return hits;
    }

    //present the search results
    public void printResult(ScoreDoc[] hits) throws Exception {

        Scanner s = new Scanner(System.in);
        String[] field_arr = new String[]{"overall", "date", "reviewerId", "helpful"};
        int user_sort = 0;
        boolean input_check;
        System.out.println("Would you like the results be sorted by? 0:Lucene Score, 1:overall, 2:date, 3:reviewerId, 4:helpful. ");
        System.out.println("Please press 0 or 1 or 2 or 3 or 4: ");
        do {
            input_check = true;
            try {
                user_sort = s.nextInt();
                if (user_sort < 0 || user_sort > 4) {
                    System.out.println("You must enter 0 or 1 or 2 or 3 or 4! Please re-enter: ");
                    input_check = false;
                }
            } catch (Exception e) {
                System.out.println("Input must be a positive integer! Please re-enter:");
                input_check = false;
                s.nextLine();
            }
        } while (!input_check);

        if (user_sort >= 1) {
            hits = this.sortResult(hits, field_arr[user_sort - 1]);
        }

        int i = 1;
        for (ScoreDoc hit : hits) {
            System.out.println("\nResult " + i + "\tDocID: " + hit.doc + "\t Score: " + hit.score);
            try {
                System.out.println("Review Text: " + lReader.document(hit.doc).get("reviewText"));
                System.out.println("Reviewer Id: " + lReader.document(hit.doc).get("reviewerId"));
                System.out.println("Review Date: " + lReader.document(hit.doc).get("date"));
                System.out.println("Review asin: " + lReader.document(hit.doc).get("asin"));
                System.out.println("Helpful: " + lReader.document(hit.doc).get("helpful"));
                System.out.println("overall: " + lReader.document(hit.doc).get("overall"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            i++;

        }
    }


    //get term vector
    public Terms getTermVector(int docID, String field) throws Exception {
        return lReader.getTermVector(docID, field);
    }

    public void close() {
        try {
            if (lReader != null) {
                lReader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}