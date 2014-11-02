package scrabble.suggester;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import scrabble.common.Word;

public class ScrabbleSuggesterTest {

    private static final String INDEX_FILE = "src/test/resources/index";
    
    private static List<Word> EXPECTED;
    
    private ScrabbleSuggester scrabbleSuggester;
    
    // 1 Point   - A, E, I, L, N, O, R, S, T and U.
    // 2 Points  - D and G.
    // 3 Points  - B, C, M and P.
    // 4 Points  - F, H, V, W and Y.
    // 5 Points  - K.
    // 8 Points  - J and X.
    // 10 Points - Q and Z.
    
    @BeforeClass
    public static void beforeClass() {
        EXPECTED = new ArrayList<>();
        EXPECTED.add(new Word("endogeny")); // score 13
        EXPECTED.add(new Word("fogdog")); // score 12
        EXPECTED.add(new Word("firedog")); // score 12
        EXPECTED.add(new Word("amidogen")); // score 12
        EXPECTED.add(new Word("hotdog")); // score 11
        EXPECTED.add(new Word("dogbane")); // score 11
        EXPECTED.add(new Word("cantdog")); // score 11
        EXPECTED.add(new Word("bandog")); // score 10
        EXPECTED.add(new Word("gundog")); // score 9
        EXPECTED.add(new Word("dog")); // score 5
    }
    
    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        scrabbleSuggester = new ScrabbleSuggester(3, INDEX_FILE);
    }

    @Test
    public void testExactlyOneSuggest() {
        List<Word> expected = new ArrayList<>();
        expected.add(new Word("endogeny"));
        assertEquals(expected, scrabbleSuggester.suggest("endogeny", 5));
    }
    
    @Test
    public void testNoSuggestion() {
        List<Word> emptyList = new ArrayList<>();
        assertEquals(emptyList, scrabbleSuggester.suggest("nosuggestion", 5));
    }
    
    @Test
    public void testTopMoreThanNumOfSuggestions() {
        assertEquals(getExpected(100), scrabbleSuggester.suggest("dog", 100));
    }
    
    @Test
    public void testTopEqualsToNumOfSuggestions() {
        assertEquals(getExpected(10), scrabbleSuggester.suggest("dog", 10));
    }
    
    @Test
    public void testTopLessThanNumOfSuggestions() {
        assertEquals(getExpected(5), scrabbleSuggester.suggest("dog", 5));
    }
    
    @Test
    public void testNegativeTop() {
        List<Word> emptyList = new ArrayList<>();
        assertEquals(emptyList, scrabbleSuggester.suggest("dog", -1));
    }
    
    @Test
    public void testZeroTop() {
        List<Word> emptyList = new ArrayList<>();
        assertEquals(emptyList, scrabbleSuggester.suggest("dog", 0));
    }
    
    private List<Word> getExpected(int top) {
        List<Word> expected = new ArrayList<>();
        for (int i = 0; i < top && i <EXPECTED.size(); i++) {
            expected.add(EXPECTED.get(i));
        }
        return expected;
    }

}
