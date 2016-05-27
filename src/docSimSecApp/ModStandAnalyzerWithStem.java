package docSimSecApp;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.std40.StandardTokenizer40;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;


/**
 * Created by nuplavikar on 5/24/16.
 */
public class ModStandAnalyzerWithStem extends StopwordAnalyzerBase
{

	/*TODO ./analysis/common/src/java/org/apache/lucene/analysis/standard/StandardTokenizer.java has the information about token length
	* can use this to remove tokens of size 2 or less to reduce dimensions*/

	/** Default maximum allowed token length */
	public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

	/** Default minimum allowed token length */
	public static final int DEFAULT_MIN_TOKEN_LENGTH = 1;

	private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;
	private int minTokenLength = DEFAULT_MIN_TOKEN_LENGTH;

	/** An unmodifiable set containing some common English words that are usually not
	 useful for searching. */
	public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

	/** Builds an analyzer with the given stop words.
	 * @param stopWords stop words */
	public ModStandAnalyzerWithStem(CharArraySet stopWords) {
		super(stopWords);
	}

	/** Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public ModStandAnalyzerWithStem() {
		this(STOP_WORDS_SET);
	}

	/** Builds an analyzer with the stop words from the given reader.
	 * @see WordlistLoader#getWordSet(Reader)
	 * @param stopwords Reader to read stop words from */
	public ModStandAnalyzerWithStem(Reader stopwords) throws IOException {
		this(loadStopwordSet(stopwords));
	}

	/**
	 * Set maximum allowed token length.  If a token is seen
	 * that exceeds this length then it is discarded.  This
	 * setting only takes effect the next time tokenStream or
	 * tokenStream is called.
	 */
	public void setMaxTokenLength(int length) {
		maxTokenLength = length;
	}

	/**
	 * Set minimum allowed token length.  If a token is seen
	 * that is shorter than this length then it is discarded.  This
	 * setting only takes effect the next time tokenStream or
	 * tokenStream is called.
	 */
	public void setMinTokenLength(int length) {
		minTokenLength = length;
		System.out.println("minTokenLength: "+this.getMinTokenLength());
	}

	/**
	 * @see #setMaxTokenLength
	 */
	public int getMaxTokenLength() {
		return maxTokenLength;
	}

	public int getMinTokenLength() {
		return minTokenLength;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {
		final Tokenizer src;
		if (getVersion().onOrAfter(Version.LUCENE_4_7_0)) {
			ModStandardTokenizer t = new ModStandardTokenizer();
			t.setMaxTokenLength(maxTokenLength);
			t.setMinTokenLength(minTokenLength);
			src = t;
		} else {
			StandardTokenizer40 t = new StandardTokenizer40();
			t.setMaxTokenLength(maxTokenLength);
			//Not added the minTokenLength as this is for backward compatibility
			src = t;
		}
		TokenStream tok = new StandardFilter(src);
		tok = new LowerCaseFilter(tok);
		tok = new StopFilter(tok, stopwords);
		//tok = new KStemFilter(tok);//14066
		tok = new PorterStemFilter(tok);//13123
		//tok = new EnglishMinimalStemFilter(tok);//15194
		return new TokenStreamComponents(src, tok) {
			@Override
			protected void setReader(final Reader reader) throws IOException {
				int m = ModStandAnalyzerWithStem.this.maxTokenLength;
				int n = ModStandAnalyzerWithStem.this.minTokenLength;
				if (src instanceof ModStandardTokenizer) {
					((ModStandardTokenizer)src).setMaxTokenLength(m);
					((ModStandardTokenizer)src).setMinTokenLength(n);
				} else {
					//Not added the minTokenLength as this is for backward compatibility
					((StandardTokenizer40)src).setMaxTokenLength(m);
				}
				super.setReader(reader);
			}
		};
	}

		}
