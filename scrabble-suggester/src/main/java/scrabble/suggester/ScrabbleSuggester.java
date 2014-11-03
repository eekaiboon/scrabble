package scrabble.suggester;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scrabble.common.NGramGenerator;
import scrabble.common.Word;

public class ScrabbleSuggester {
    
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MAX_N_GRAMS = 4;
    private static final int INITIAL_CAPACITY = 11;
    private static final int NUM_OF_BUCKET = 700;

    private static final String INDEX_DIRECTORY = "index";
    
    private final NGramGenerator nGramGenerator;
    private final Map<Integer, String> rankToWordMap;
    private final Map<String, Set<String>> indexFileToNGramsMap;
    private final Map<String, Deque<Integer>> nGramToWordsMap;
    private final String nGramPath;
    private final String wordFile;
    private int numOfBucket = NUM_OF_BUCKET;
    
    public ScrabbleSuggester() throws ClassNotFoundException, IOException {
        this(MAX_N_GRAMS, INDEX_DIRECTORY);
    }
    
    public ScrabbleSuggester(int maxNGram) 
            throws ClassNotFoundException, IOException {
        this(maxNGram, INDEX_DIRECTORY);
    }
    
    public ScrabbleSuggester(String indexDirectory)  
            throws ClassNotFoundException, IOException {
        this(MAX_N_GRAMS, indexDirectory);
    }
    
    public ScrabbleSuggester(int maxNGram, String indexDirectory) 
            throws ClassNotFoundException, IOException {
        this.nGramGenerator = new NGramGenerator(maxNGram);
        this.rankToWordMap = new HashMap<>();
        this.indexFileToNGramsMap = new HashMap<>();
        this.nGramToWordsMap = new HashMap<>();
        this.nGramPath = indexDirectory + "/ngrams_";
        this.wordFile = indexDirectory + "/words";
        readWordFromDisk();
    }
    
    public void setNumOfBucket(int numOfBucket) {
        this.numOfBucket = numOfBucket;
    }

    // Load words
    // Build rank to word map
    private void readWordFromDisk() throws IOException {
        log.debug("Start loading words");
        final long start = System.currentTimeMillis();

        try {
            List<String> lines = Files.readAllLines(Paths.get(wordFile));

            String[] tokens = lines.get(0).split(",");
            
            int rank = Integer.parseInt(tokens[0]);
            for (int i = 1; i < tokens.length; i++) {
                rankToWordMap.put(rank--, tokens[i]);
            }
        } catch (IOException e) {
            throw new IOException("Error while reading words", e);
        }

        log.debug("Completed loading words. Time Spent (msec) : " +
                ((System.currentTimeMillis() - start)));
    }
    

    // Figure out which index file can we retrieve the n-grams from
    // Store the mapping in map :
    // (1) Key   : index file name
    // (2) Value : n-gram that can be found in the index file
    private void findIndexFile(Set<String> queryNGrams) {
        queryNGrams.stream()
            .forEach(e -> { 
                int bucket = e.hashCode() % numOfBucket;
                String bucketFile = nGramPath + bucket;
                
                if (indexFileToNGramsMap.containsKey(bucketFile)) {
                    indexFileToNGramsMap.get(bucketFile).add(e);
                } else {
                    Set<String> nGrams = new HashSet<>();
                    nGrams.add(e);
                    indexFileToNGramsMap.put(bucketFile, nGrams);
                }
            });
    }

    private void loadNGramToWordsMapping() {
        Stream.of(indexFileToNGramsMap)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .forEach(e -> {
                Path bucketFilePath = Paths.get(e.getKey());
                Set<String> nGrams = e.getValue();
                
                try (Stream<String> stream = Files.lines(bucketFilePath)) {
                    stream.forEach(line -> {
                        String[] tokens = line.split("=");
                        String nGram = tokens[0];
                        
                        if (nGrams.contains(nGram)) {
                            String[] wordArr = tokens[1].split(",");
                            Deque<Integer> words = new LinkedList<>();
                            for (String word : wordArr) {
                                words.add(Integer.parseInt(word));
                            }
                            nGramToWordsMap.put(nGram, words);
                        }
                    }); 
                } catch (IOException ioe) {
                    throw new RuntimeException("Error while loading n-grams to words mapping", ioe);
                }
            });
        
        log.debug("Number of index file read : " + indexFileToNGramsMap.size());
    }

    public List<Word> suggest(String query, int top) {
        log.debug("Computing suggestions");
        final long start = System.currentTimeMillis();
        
        List<Word> suggestions = new ArrayList<>();
        
        // Generate n-grams for query
        Set<String> queryNGrams = nGramGenerator.generate(query);
        
        findIndexFile(queryNGrams);
        
        loadNGramToWordsMapping();
            
        PriorityQueue<Word> candidates = 
                new PriorityQueue<>(INITIAL_CAPACITY, Collections.reverseOrder());
        Set<String> seen = new HashSet<>();
        
        for(String queryNGram : queryNGrams) {
            Deque<Integer> parentCandidates = nGramToWordsMap.get(queryNGram);
            
            if (parentCandidates != null) {
                List<Word> validCandidates = new ArrayList<>();

                while (validCandidates.size() < top &&
                        !parentCandidates.isEmpty()) {
                    String candidate = rankToWordMap.get(parentCandidates.poll());
                    
                    // filter suggestion that 
                    // (1) has been added previously
                    // (2) does not contain query word
                    if (!seen.contains(candidate) && 
                            candidate.contains(query)) {
                        validCandidates.add(new Word(candidate));
                        seen.add(candidate);
                    }
                }

                candidates.addAll(validCandidates);
            }
        }
        
        while (!candidates.isEmpty() && suggestions.size() < top) {
            suggestions.add(candidates.poll());
        }

        log.debug("Completed computing suggestions. Time Spent (msec) : " +
                ((System.currentTimeMillis() - start)));
        
        return suggestions;
    }

    private static boolean isAlpha(String s) {
        for (char c : s.toCharArray()) {
            if(!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }
    
    public static void main(String[] args) {
        // check command line arguments
        if (args.length != 2) {
            log.error("Usage : scrabble-suggester <query> <num-of-suggestion>");
            System.exit(-1);
        }
        
        String query = args[0];
        if (!isAlpha(query)) {
            log.error("<query> should only contain letter/s");
            System.exit(-1);
        }
        
        int numOfSuggestions = 0;
        try {
            numOfSuggestions = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            log.error("<num-of-suggestion> should be integer");
            System.exit(-1);
        }
        
        int maxNGram = MAX_N_GRAMS;
        if (args.length > 2) {
            maxNGram = Integer.parseInt(args[2]);
        }
        
        ScrabbleSuggester scrabbleSuggester = null;
        
        // Instantiate scrabble suggester
        try {
            scrabbleSuggester = new ScrabbleSuggester(maxNGram);
        } catch (ClassNotFoundException | IOException e) {
            log.error("Error while reading index", e);
            System.exit(-1);
        }
        
        // Skip the query execute if the query length is more than maximum
        // length of any suggestion
        final int suggestionMaxLength = 21;
        if (query.length() > suggestionMaxLength) {
            System.out.println("Sorry, there is no suggestion for " + query + ".");
            return;
        }
        
        // Execute query
        List<Word> suggestions = scrabbleSuggester.suggest(query, numOfSuggestions);
        for (Word suggestion : suggestions) {
            System.out.println(suggestion.getWord() + " (" + suggestion.getScore() + ")");
        }
        
        if (suggestions.isEmpty()) {
            System.out.println("Sorry, there is no suggestion for " + query + ".");
        }
    }
}
