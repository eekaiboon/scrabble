package scrabble.common;

import java.io.Serializable;

public class Word implements Comparable<Word>, Serializable {
    
    private static final long serialVersionUID = 2052384923547604365L;

    private static final int[] POINTS = new int[] {
        // a, b, c, d, e, f, g, h, i, j,
           1, 3, 3, 2, 1, 4, 2, 4, 1, 8,
        // k, l, m, n, o, p, q, r, s, t,  
           5, 1, 3, 1, 1, 3, 10, 1, 1, 1,
        // u, v, w, x, y, z   
           1, 4, 4, 8, 4, 10  
    };
    
    private final int score;
    private final String word;
    
    public Word (String word) {
        this.word = word;
        this.score = computeScore(word);
    }
    
    public static int computeScore(String s) {
        int score = 0;
        for (char c : s.toLowerCase().toCharArray()) {
            score += POINTS[c - 'a'];
        }
        return score;
    }
    
    public int getScore() {
        return score;
    }
    
    public String getWord() {
        return word;
    }

    @Override
    public int compareTo(Word o) {
        // sort by score
        int ret = this.getScore() - o.getScore();
        
        // if scores are equal, then we sort alphabetically
        if (ret == 0) {
            return this.getWord().compareTo(o.getWord());
        }
        return ret;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + score;
        result = prime * result + ((word == null) ? 0 : word.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Word other = (Word) obj;
        if (score != other.score)
            return false;
        if (word == null) {
            if (other.word != null)
                return false;
        } else if (!word.equals(other.word))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Word [score=" + score + ", word=" + word + "]";
    }
}
