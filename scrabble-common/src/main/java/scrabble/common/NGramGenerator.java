package scrabble.common;

import java.util.HashSet;
import java.util.Set;

public class NGramGenerator {
    
    private final int max;

    public NGramGenerator(int max) {
        this.max = max;
    }
    
    /**
     * Given s, return all n-grams up to max or length of s
     * For example : If max = 5 and s = "good", we will return the following :
     *   (1) unigram   - "g", "o", "d"
     *   (2) bigram    - "go", "oo", "od"
     *   (3) trigram   - "goo", "ood"
     *   (4) four-gram - "good"
     * @param s
     * @return
     */
    public Set<String> generateAll(String s) {
        
        Set<String> nGrams = new HashSet<>();
        
        if (s == null || s.isEmpty()) {
            return nGrams;
        }
        
        // Pointers to keep track of the start and end positions each n-grams
        int numOfPointers = (max > s.length()) ? s.length() : max;
        int[] start = new int[numOfPointers];
        int[] end = new int[numOfPointers];
        
        // Initialize end pointer
        for (int i = 0; i < end.length; i++) {
            end[i] = i + 1;
        }

        // Populate n-grams
        while (start[0] < s.length()) {
            for (int i = 0; i < start.length; i++) {
                if (end[i] <= s.length()) {
                    nGrams.add(s.substring(start[i]++, end[i]++));
                }
            }
        }
        
        return nGrams;
    }
    
    /**
     * Given s, return n-gram of max or length of s
     * For example : 
     * (2) If max = 3 and s = "good", we will return trigram - "goo", "ood"
     * (1) If max = 5 and s = "good", we will return four-gram - "good"
     * @param s
     * @return
     */
    public Set<String> generate(String s) {
        
        Set<String> nGrams = new HashSet<>();
        
        if (s == null || s.isEmpty()) {
            return nGrams;
        }
        
        // Pointers to keep track of the start and end position
        int start = 0;
        int end = (max > s.length()) ? s.length() : max;
        
        // Populate n-grams
        while(end <= s.length()) {
            nGrams.add(s.substring(start++, end++));
        }
        
        return nGrams;
    }
}
