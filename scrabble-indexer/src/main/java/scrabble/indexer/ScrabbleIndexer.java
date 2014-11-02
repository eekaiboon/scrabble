package scrabble.indexer;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scrabble.common.NGramGenerator;
import scrabble.common.Word;
import scrabble.common.util.Util;

public class ScrabbleIndexer {	

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private static final int MAX_N_GRAMS = 4;
    private static final int INITIAL_CAPACITY = 11;
    private static final int NUM_OF_BUCKET = 50;
    
    private static final String INDEX_DIRECTORY = "index";
    private static final String N_GRAM_PATH = INDEX_DIRECTORY + "/ngrams_";
    private static final String WORD_FILE = INDEX_DIRECTORY + "/words";

    private final NGramGenerator nGramGenerator;
    private final Map<String, PriorityQueue<Integer>> nGramMap;
    private final Map<String, Integer> wordRankMap;
    private final int numOfBucket;

    public ScrabbleIndexer(int maxNGram) {
        this(maxNGram, NUM_OF_BUCKET);
    }
    
    public ScrabbleIndexer(int maxNGram, int numOfBukcet) {
        this.nGramGenerator = new NGramGenerator(maxNGram);
        this.nGramMap = new HashMap<>();
        this.wordRankMap = new HashMap<>();
        this.numOfBucket = numOfBukcet;
    }
    
    public void index(Path path) throws IOException {
        
        rankWord(path);
        
        log.info("Building nGramMap");
        final long start = System.currentTimeMillis();
        
        try (Stream<String> stream = Files.lines(path)) {
            stream.forEach(word -> {
            
                // Generate n-grams
                Set<String> nGrams = nGramGenerator.generateAll(word);

                // Store n-gram to word mapping in map
                // Note that we are using a priority queue with reverse
                // natural ordering (i.e. words will be sorted in score
                // followed by alphabetical descending fashion)
                //
                // In order to keep the index size smaller, we are translating
                // word (String) to its corresponding rank (Integer)
                for(String nGram: nGrams) {
                    if (nGramMap.containsKey(nGram)) {
                        nGramMap.get(nGram).add(wordRankMap.get(word));
                    } else {
                        PriorityQueue<Integer> pq = 
                                new PriorityQueue<>(INITIAL_CAPACITY,
                                        Collections.reverseOrder());
                        pq.add(wordRankMap.get(word));
                        nGramMap.put(nGram, pq);
                    }
                }
            });
        } catch (IOException e) {
            throw new IOException("Error while indexing", e);
        }

        log.info("Completed building nGramMap. Time Spent (msec) : " +
                ((System.currentTimeMillis() - start)));
        
        writeNGramToDisk();
    }
    
    // Rank all words descending by score followed by alphabetical order
    // Each word and its rank are being stored in wordRankMap
    // Write the sorted words to disk
    private void rankWord(Path path) throws IOException {
        log.info("Ranking word");
        final long start = System.currentTimeMillis();
        
        PriorityQueue<Word> pq = 
                new PriorityQueue<>(INITIAL_CAPACITY, Collections.reverseOrder());
        
        try (Stream<String> stream = Files.lines(path)) {
            stream.forEach(word -> { pq.add(new Word(word)); });
        } catch (IOException e) {
            throw new IOException("Error while calculating score", e);
        }
        
        int maxLength = -1;
        String wordWithMaxLength = null;
        
        StringBuilder sb = new StringBuilder();
        int rank = pq.size();
        sb.append(rank);
        sb.append(",");
        
        while(!pq.isEmpty()) {
            Word word = pq.poll();
            wordRankMap.put(word.getWord(), rank--);

            if (maxLength < word.getWord().length()) {
                maxLength = word.getWord().length();
                wordWithMaxLength = word.getWord();
            }
            
            sb.append(word.getWord());
            sb.append(",");
        }
        
        log.info("Word with max length : " + wordWithMaxLength + " (" + maxLength + ")");

        writeIndexWordToDisk(sb.toString());
        
        log.info("Completed ranking word. Time Spent (msec) : " +
                ((System.currentTimeMillis() - start)));
    }
    
    private void writeIndexWordToDisk(String s) throws IOException {
        log.info("Writing index word to disk");
        final long start = System.currentTimeMillis();
        
        Files.write(Paths.get(WORD_FILE), s.getBytes());

        log.info("Completed writing index word to disk. Time Spent (msec) : " +
                ((System.currentTimeMillis() - start)));
    }
    
    // Instead of writing all n-grams to a single file, we are splitting
    // them into buckets. Scrabble suggester will only need to load n-grams
    // partially when making suggestions. This will speed up the suggestion
    // computation time.
    private void writeNGramToDisk() throws IOException {
        log.info("Writing index ngram to disk");
        final long start = System.currentTimeMillis();

        List<StringBuilder> buckets = new ArrayList<>(numOfBucket);
        for(int i = 0; i < numOfBucket; i++) {
            buckets.add(new StringBuilder());
        }
        
        Stream.of(nGramMap)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .forEach(entry -> {
                int bucket = entry.getKey().hashCode() % numOfBucket;
                StringBuilder sb = buckets.get(bucket);
                
                sb.append(entry.getKey());
                sb.append("=");
    
                PriorityQueue<Integer> pq = entry.getValue();
                if (!pq.isEmpty()) {
                    sb.append(pq.poll());
                }
                while(!pq.isEmpty()) {
                    sb.append(",");
                    sb.append(pq.poll());
                }
                
                sb.append("\n");
            });

        for(int i = 0; i < numOfBucket; i++) {
            Files.write(Paths.get(N_GRAM_PATH + i), buckets.get(i).toString().getBytes());
        }
        
        log.info("Completed writing index ngram to disk. Time Spent (msec) : " +
                ((System.currentTimeMillis() - start)));
    }
    
    public static void main (String[] args) throws IOException {

        // check command line arguments
        if (args.length != 1) {
            log.error("Usage : scrabble-indexer <word-list-file>");
            System.exit(-1);
        }
        
        int maxNGram = MAX_N_GRAMS;
        if (args.length > 1) {
            maxNGram = Integer.parseInt(args[1]);
        }
        
        String pathStr = args[0];
        Path wordListPath = Paths.get(pathStr);
        
        // Create/clean index directory
        Path indexIndectory = Paths.get(INDEX_DIRECTORY);
        if (Files.exists(indexIndectory)) {
            Util.deleteDirectory(INDEX_DIRECTORY);
        }
        Files.createDirectories(Paths.get(INDEX_DIRECTORY));
        
        ScrabbleIndexer scrabbleIndexer = new ScrabbleIndexer(maxNGram);
        
        try {
            log.info("Start indexing");
            final long start = System.currentTimeMillis();

            scrabbleIndexer.index(wordListPath);

            log.info("Completed indexing. Time Spent (msec) : " +
                    ((System.currentTimeMillis() - start)));
        } catch (IOException e) {
            log.error("Error while indexing", e);
            System.exit(-1);
        }
    }
}
