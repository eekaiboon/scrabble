package scrabble.indexer;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;

public class ScrabbleIndexerTest {

    private static final String WORD_LIST_FILE = "src/test/resources/word_list_test.txt";

    @Test
    public void test() throws IOException {
        ScrabbleIndexer scrabbleIndexer = new ScrabbleIndexer(3);
        scrabbleIndexer.index(Paths.get(WORD_LIST_FILE));
    }
}
