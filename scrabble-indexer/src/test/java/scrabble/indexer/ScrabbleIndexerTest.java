package scrabble.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import scrabble.common.util.Util;

public class ScrabbleIndexerTest {

    private static final String WORD_LIST_FILE = "src/test/resources/word_list_test.txt";
    private static final String INDEX_DIRECTORY = "index";
    
    @Before
    public void before() throws IOException {
     // Create/clean index directory
        Path indexIndectory = Paths.get(INDEX_DIRECTORY);
        if (Files.exists(indexIndectory)) {
            Util.deleteDirectory(INDEX_DIRECTORY);
        }
        Files.createDirectories(Paths.get(INDEX_DIRECTORY));
    }
    
    @Test
    public void test() throws IOException {
        ScrabbleIndexer scrabbleIndexer = new ScrabbleIndexer(3);
        scrabbleIndexer.index(Paths.get(WORD_LIST_FILE));
    }
}
