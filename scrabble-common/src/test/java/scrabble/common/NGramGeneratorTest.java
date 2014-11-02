package scrabble.common;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import scrabble.common.NGramGenerator;

public class NGramGeneratorTest {
    
    private NGramGenerator nGramGenerator;
    
    @Before
    public void setUp() {
        int max = 3;
        nGramGenerator = new NGramGenerator(max);
    }
    
    @Test
    public void testGenerateAllWithLessThanMax() {
        assertEquals(new HashSet<>(Arrays.asList("g")), 
                nGramGenerator.generateAll("g"));
        
        assertEquals(new HashSet<>(Arrays.asList("g", "o", "go")),
                nGramGenerator.generateAll("go"));
    }
    
    @Test
    public void testGenerateAllWithEqualMax() {
        assertEquals(new HashSet<>(Arrays.asList("g", "o", "go", "oo", "goo")), 
                nGramGenerator.generateAll("goo"));
    }
    
    @Test
    public void testGenerateAllWithMoreThanMax() {
        assertEquals(new HashSet<>(
                Arrays.asList("g", "o", "d", "go", "oo", "od", "goo", "ood")), 
                nGramGenerator.generateAll("good"));
    }
    
    @Test
    public void testGenerateAllWithNull() {
        assertEquals(new HashSet<String>(), nGramGenerator.generateAll(null));
    }
    
    @Test
    public void testGenerateAllWithEmpty() {
        assertEquals(new HashSet<String>(), nGramGenerator.generateAll(""));
    }

    @Test
    public void testGenerateWithLessThanMax() {
        assertEquals(new HashSet<>(Arrays.asList("g")), 
                nGramGenerator.generate("g"));
        
        assertEquals(new HashSet<>(Arrays.asList("go")),
                nGramGenerator.generate("go"));
    }
    
    @Test
    public void testGenerateWithEqualMax() {
        assertEquals(new HashSet<>(Arrays.asList("goo")), 
                nGramGenerator.generate("goo"));
    }
    
    @Test
    public void testGenerateWithMoreThanMax() {
        assertEquals(new HashSet<>(
                Arrays.asList("goo", "ood")), 
                nGramGenerator.generate("good"));
    }
    
    @Test
    public void testGenerateWithNull() {
        assertEquals(new HashSet<String>(), nGramGenerator.generate(null));
    }
    
    @Test
    public void testGenerateWithEmpty() {
        assertEquals(new HashSet<String>(), nGramGenerator.generate(""));
    }
}
