package scrabble.common;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import org.junit.Test;

import scrabble.common.Word;

public class WordTest {

    @Test
    public void test() {
        assertEquals(9, new Word("home").getScore());
        assertEquals(9, new Word("Home").getScore());
    }
    
    @Test
    public void testAscendingSort() {
        Word aback = new Word("aback"); // score 13
        Word abacas = new Word("abacas"); // score 10
        Word abacuses = new Word("abacuses"); // score 12
        Word abaca = new Word("abaca"); // score 9
        Word abacus = new Word("abacus"); // score 10
        Word abaci = new Word("abaci"); // score 9
        
        Word[] arr = new Word[] {aback, abacas, abacuses, abaca, abacus, abaci};
        PriorityQueue<Word> words = new PriorityQueue<>(Arrays.asList(arr));
        List<Word> sorted = new ArrayList<>();
        while(!words.isEmpty()) {
            sorted.add(words.poll());
        }
        
        List<Word> expected = new ArrayList<>();
        expected.add(abaca);
        expected.add(abaci);
        expected.add(abacas);
        expected.add(abacus);
        expected.add(abacuses);
        expected.add(aback);
        
        assertEquals(expected, sorted);
    }

    @Test
    public void testDescendingSort() {
        Word aback = new Word("aback"); // score 13
        Word abacas = new Word("abacas"); // score 10
        Word abacuses = new Word("abacuses"); // score 12
        Word abaca = new Word("abaca"); // score 9
        Word abacus = new Word("abacus"); // score 10
        Word abaci = new Word("abaci"); // score 9
        
        Word[] arr = new Word[] {aback, abacas, abacuses, abaca, abacus, abaci};
        PriorityQueue<Word> words = new PriorityQueue<>(11, Collections.reverseOrder());
        words.addAll(Arrays.asList(arr));
        List<Word> sorted = new ArrayList<>();
        while(!words.isEmpty()) {
            sorted.add(words.poll());
        }
        
        List<Word> expected = new ArrayList<>();
        expected.add(aback);
        expected.add(abacuses);
        expected.add(abacus);
        expected.add(abacas);
        expected.add(abaci);
        expected.add(abaca);
        
        assertEquals(expected, sorted);
    }
}
