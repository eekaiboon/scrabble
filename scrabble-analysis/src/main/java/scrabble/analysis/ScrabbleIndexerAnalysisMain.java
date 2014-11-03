package scrabble.analysis;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scrabble.common.NGramGenerator;
import scrabble.common.util.Util;
import scrabble.indexer.ScrabbleIndexer;
import scrabble.suggester.ScrabbleSuggester;

public class ScrabbleIndexerAnalysisMain {
    
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private static final String WORD_LIST_FILE = 
            "src/main/resources/word_list_moby_crossword.flat.txt";
    private static final String INDEX_FILE = "index/ngrams_0";
    private static final String ANALYSIS_DIRECTORY = "analysis";
    private static final String EXTRACTED_WORDS_FILE = ANALYSIS_DIRECTORY +
            "/extracted_words_min_length_";
    private static final String RANDOM_WORDS_FILE = ANALYSIS_DIRECTORY +
            "/random_words_min_length_";
    private static final String TEST_WORDS_FILE = ANALYSIS_DIRECTORY +
            "/test_words_min_length_";
    private static final String INDEX_DIRECTORY = "index";
    private static final int MAX_LENGTH = 21;

    public static void wordCount() {
        int[] wordCount = new int[MAX_LENGTH + 1];
        
        try (Stream<String> stream = Files.lines(Paths.get(WORD_LIST_FILE))) {
            stream.forEach(word -> { wordCount[word.length()] += 1; });
        } catch (IOException e) {
            log.error("Error while reading word list file", e);
        }
        
        log.info("Word Count for each length n word : " + Arrays.toString(wordCount));
    }
    
    public static void nGramWordCount() {
        NGramGenerator nGramGenerator = new NGramGenerator(MAX_LENGTH);
        int[] nGramWordCount = new int[MAX_LENGTH + 1];
        
        try (Stream<String> stream = Files.lines(Paths.get(WORD_LIST_FILE))) {
            stream.forEach(word -> { 
                    Set<String> nGrams = nGramGenerator.generateAll(word);
                    nGrams.stream().forEach(nGram -> {
                        nGramWordCount[nGram.length()] += 1; });
                });
        } catch (IOException e) {
            log.error("Error while reading word list file", e);
        }
        
        log.info("Word Count for each n-gram : " + Arrays.toString(nGramWordCount));
    }
    
    public static void indexSize() {
        long[] indexSizeInMB = new long[MAX_LENGTH + 1];
        
        for (int i  = 1; i < MAX_LENGTH + 1; i++) {
            ScrabbleIndexer scrabbleIndexer = new ScrabbleIndexer(i, 1);
            
            try {
                scrabbleIndexer.index(Paths.get(WORD_LIST_FILE));
            } catch (IOException e) {
                log.error("Error while indexing", e);
            }
            
            try {
                long bytes = Files.size(Paths.get(INDEX_FILE));
                long MB = (long) (bytes / Math.pow(2, 10));
                indexSizeInMB[i] = MB;
            } catch (IOException e) {
                log.error("Error while reading index file", e);
            }
        }
        
        log.info("Index size for each max n-gram : " + Arrays.toString(indexSizeInMB));
    }
    
    public static void extractWords(int minLength) {
        List<String> words = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        
        try (Stream<String> stream = Files.lines(Paths.get(WORD_LIST_FILE))) {
            stream.forEach(word -> {
                    if (word.length() >= minLength) {
                        words.add(word);
                        sb.append(word);
                        sb.append(",");
                    }
                });
        } catch (IOException e) {
            log.error("Error while reading word list file", e);
        }
        
        try {
            Files.write(Paths.get(EXTRACTED_WORDS_FILE + minLength), sb.toString().getBytes());
        } catch (IOException e) {
            log.error("Error while writing to extracted words file", e);
        }
        
        System.out.println("Number of words extracted (with minimum length " + minLength + ") : " + words.size());
    }
    
    public static void randomWordGenerator(int minLength, int numOfWord) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        
        for (int i = 0; i < numOfWord; i++) {
            char[] word = new char[random.nextInt(MAX_LENGTH - minLength + 1) + minLength];
            
            for (int j = 0; j < word.length; j++) {
                word[j] = (char) ('a' + random.nextInt(26));
            }
            
            sb.append(word);
            sb.append(",");
        }
        
        try {
            Files.write(Paths.get(RANDOM_WORDS_FILE + minLength), sb.toString().getBytes());
        } catch (IOException e) {
            log.error("Error while writing to extracted words file", e);
        }
        
        log.info("Randomly generated " + numOfWord + " words with minimum length of " + minLength);
    }
    
    public static void generateTestWords(int minLength, int numOfWord) {
        Random random = new Random();
        Set<String> words = new HashSet<>();
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(EXTRACTED_WORDS_FILE + minLength));
            String[] candidates = lines.get(0).split(",");
            
            while (words.size() <= numOfWord / 2) {
                words.add(candidates[random.nextInt(candidates.length)]);
            }
        } catch (IOException e) {
            log.error("Error while reading extracted words file", e);
        }
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(RANDOM_WORDS_FILE + minLength));
            String[] candidates = lines.get(0).split(",");
            
            while (words.size() <= numOfWord) {
                words.add(candidates[random.nextInt(candidates.length)]);
            }
        } catch (IOException e) {
            log.error("Error while reading random words file", e);
        }
        
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word);
            sb.append(",");
        }
        
        try {
            Files.write(Paths.get(TEST_WORDS_FILE + minLength), sb.toString().getBytes());
        } catch (IOException e) {
            log.error("Error while writing to test words file", e);
        }
        
        log.info("Randomly picked " + numOfWord + " words with minimum length of " + minLength);
    }
    
    public static Map<Integer, Long> findOptimalNumOfBucket() {
        final int[] numOfBuckets = new int[] {250, 500, 750, 1000, 1250, 1500};
        Map<Integer, Long> records = new HashMap<>();
        final int maxNGram = 4;
        final int minLength = 5;
        String[] queries = null;
        int top = 100;
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(TEST_WORDS_FILE + minLength));
            queries = lines.get(0).split(",");
        } catch (IOException e) {
            log.error("Error while reading extracted words file", e);
        }

        try {
            for (int numOfBucket :numOfBuckets) {
                // Create/clean index directory
                Path indexIndectory = Paths.get(INDEX_DIRECTORY);
                if (Files.exists(indexIndectory)) {
                        Util.deleteDirectory(INDEX_DIRECTORY);
                }
                Files.createDirectories(Paths.get(INDEX_DIRECTORY));
                
                ScrabbleIndexer scrabbleIndexer = new ScrabbleIndexer(maxNGram, numOfBucket);
                
                try {
                    scrabbleIndexer.index(Paths.get(WORD_LIST_FILE));
                } catch (IOException e) {
                    log.error("Error while reading word list file", e);
                }
                
                ScrabbleSuggester scrabbleSuggester = new ScrabbleSuggester(maxNGram, INDEX_DIRECTORY);
                scrabbleSuggester.setNumOfBucket(numOfBucket);
                
                System.out.println("Running queries with " + numOfBucket + " buckets");
                final long start = System.currentTimeMillis();
                int count = 0;
                for (String query : queries) {
                    scrabbleSuggester.suggest(query, top);
                    
                    count++;
                    if (count % 500 == 0) {
                        System.out.println("Number of query ran : " + count);
                    }
                }
                long timeTake = (System.currentTimeMillis() - start);
                System.out.println("Completed running queries with " + numOfBucket +
                        " buckets. Time Spent (msec) : " + timeTake);
                
                records.put(numOfBucket, timeTake);
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error("Error while running iteration", e);
        }
        
        System.out.println("Number of bucket to time taken map : " + records);
        return records;
    }
    
    public static void main (String[] args) {
        
        final int numOfQueries = 500;
        final int numOfIteration = 5;
        System.out.println("Number of queries = " + numOfQueries);
        
        Map<Integer, List<Long>> records = new TreeMap<>();
        
        for (int i = 0; i < numOfIteration; i++) {
            System.out.println("Iter-" + i);
            
            generateTestWords(5, numOfQueries);
            Map<Integer, Long> iterRecord = findOptimalNumOfBucket();
            
            Stream.of(iterRecord)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .forEach(e -> {
                    if (records.containsKey(e.getKey())) {
                        records.get(e.getKey()).add(e.getValue());
                    } else {
                        List<Long> times = new ArrayList<>();
                        times.add(e.getValue());
                        records.put(e.getKey(), times);
                    }
                });
        }
        
        Stream.of(records)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .forEach(e -> { 
            System.out.println(e.getKey() + " = " + e.getValue());
            int total = 0;
            for (long time : e.getValue()) {
                total += time;
            }
            System.out.println(e.getKey() + " = " + (total / e.getValue().size()));
        });
            
    }
}
