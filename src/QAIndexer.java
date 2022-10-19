
/**
 * @author Group 16
 * This code is provided solely as sample code for using Lucene.
 */


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.analysis.en.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

public class QAIndexer {

    private IndexWriter writer = null;
    private ArrayList<Long> recorded_time;

    //for recording time used for indexing
    private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private long dataCount = 0;

    public QAIndexer(String dir) throws IOException {
        this.recorded_time = new ArrayList<>();

        //specify the directory to store the Lucene index
        Directory indexDir = FSDirectory.open(Paths.get(dir));

        //specify the analyzer used in indexing
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(OpenMode.CREATE);
        //create the IndexWriter
        writer = new IndexWriter(indexDir, cfg);
    }


    //specify what is a document, and how its fields are indexed
    protected Document getDocument(String review, String asin, String reviewerId, JSONArray helpful, String date, int overall) throws Exception {
        Document doc = new Document();

        FieldType ft = new FieldType(TextField.TYPE_STORED);
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        ft.setStoreTermVectors(true);

        doc.add(new Field("reviewText", review, ft));
        doc.add(new Field("date", date, ft));
        doc.add(new Field("asin", asin, ft));
        doc.add(new Field("reviewerId", reviewerId, ft));
        doc.add(new StoredField("helpful", String.valueOf(helpful)));
        doc.add(new StoredField("overall", overall));

        return doc;
    }


    public void indexQAs(String fileName) throws Exception {
        Path path = Paths.get(fileName);
        this.dataCount = Files.lines(path).count();
        int gap = (int) (this.dataCount * 0.1);
        int gapEnd = gap;
        System.out.println(this.dataCount);

        System.out.println("Start indexing " + fileName + " " + sdf.format(new Date()));
        long start_time = System.currentTimeMillis();
        long start = System.currentTimeMillis();

        //read a JSON file
        Scanner in = new Scanner(new File(fileName));
        int lineNumber = 1;
        String jLine = "";

        while (in.hasNextLine()) {
            try {
                jLine = in.nextLine().trim();
                //parse the JSON file and extract the values for "question" and "answer"
                JSONObject jObj = new JSONObject(jLine);

                String reviewerId = jObj.getString("reviewerID"); //10261
                String asin = jObj.getString("asin"); //10261
                JSONArray helpful = jObj.getJSONArray("helpful");
                String date = jObj.getString("reviewTime");
                String review = jObj.getString("reviewText"); //10261
                int overall = jObj.getInt("overall");//10261
                //create a document for each JSON record
                Document doc = getDocument(review, asin, reviewerId, helpful, date, overall);

                //index the document
                writer.addDocument(doc);

                if (lineNumber == gapEnd) {
                    long current = System.currentTimeMillis();
                    long time_differ = current - start;
                    start = current;
                    this.recorded_time.add(time_differ);
                    gapEnd += gap;
                    System.out.println(lineNumber);
                }
                lineNumber++;
            } catch (Exception e) {
                System.out.println("Error at: " + lineNumber + "\t" + jLine);
                e.printStackTrace();
            }
        }

        this.recorded_time.add(System.currentTimeMillis() - start);
        System.out.println("The total line number is " + this.dataCount);
        //close the file reader
        in.close();
        System.out.println("Index completed at " + sdf.format(new Date()));

        System.out.print("Time needed to index every 10% of the documents: ");
        for (int i = 0; i < this.recorded_time.size(); i++) {
            System.out.print(this.recorded_time.get(i) + " Millisecond\n");
        }

        long finish_time = System.currentTimeMillis();
        long index_time = finish_time - start_time;
        System.out.println("Total indexing time: " + index_time + "Millisecond(" + index_time / 100 + " Second)");

        //close the index writer.
        writer.close();

    }

}