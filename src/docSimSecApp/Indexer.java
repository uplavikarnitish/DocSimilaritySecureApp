package docSimSecApp;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.SimpleFSDirectory;

public class Indexer {

	public int debug;
	public static void main(String[] args) throws Exception{
		// write your code here
		if (args.length !=2 )
		{
			throw new Exception("Usage Java "+ Indexer.class.getName()+"<index dir> <data dir>");
		}
		String indexDir = args[0];
		String dataDir = args[1];

		long start = System.currentTimeMillis();
		Indexer indexer = new Indexer(indexDir, 1/*debug field*/);
		int numIndexed = indexer.index(dataDir);
		indexer.close();
		long end = System.currentTimeMillis();
		System.out.println("Indexing "+numIndexed+" files took "+(end-start)+" milliseconds.");
		System.out.println("Index dir = "+indexDir+":: data dir = "+dataDir);
	}

	private IndexWriter writer;

	public Indexer (String indexDir, int debug) throws IOException {
		this.debug = debug;
		Directory dir = new SimpleFSDirectory(Paths.get(indexDir));
		//writer = new IndexWriter(dir, new StandardAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
		Analyzer analyzer = new ModStandAnalyzerWithStem(ModStandAnalyzerWithStem.STOP_WORDS_SET);
		((ModStandAnalyzerWithStem)analyzer).setMinTokenLength(2);
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
		//Setting the similarity for the writer config to LuceneDefaultSimilarityModified
		//This is done to remove any effect added upon by the computeLength in Similarity class
		//So that purely tfidf is derived form the lucene scoring formula.
		indexWriterConfig.setSimilarity(new LuceneDefaultSimilarityModified());
		writer = new IndexWriter(dir, indexWriterConfig);
	}
	public void close() throws IOException {
		writer.close();
	}

	public int index(String dataDir) throws IOException {
		File[] files = new File(dataDir).listFiles();

		if ( files == null )
		{
			System.err.println("Error!! Segfault:: files: " + files);
			System.exit(1);
		}
		else
		{
			System.out.println("No. files to be indexed = "+files.length);
		}

		for ( int i=0; i<files.length; i++ )
		{
			File f = files[i];
			if ( !f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && acceptFile(f) )
			{
				indexFile(f);
			}

		}
		return writer.numDocs();
	}

	protected boolean acceptFile(File f)
	{
		return f.getName().endsWith(".txt");
	}

	protected Document getDocument(File f) throws IOException {
		Document doc = new Document();





		FieldType textWithTermVectors;
		textWithTermVectors = new TextFieldWithTermVectors();

		doc.add(new TextField("filename", f.getCanonicalPath(), Field.Store.YES));
		Field contentsField = new Field("contents", new FileReader(f), textWithTermVectors);
		//Although default itself is one, setting the contents field boost to 1(float);
		contentsField.setBoost(1);
		if (debug == 1) {
			System.out.println("contentsField.boost() = "+contentsField.boost()+"\n");
		}
		doc.add(contentsField);

		return doc;
	}

	private void indexFile(File f) throws IOException {
		System.out.println("Indexing "+f.getCanonicalPath());
		Document doc = getDocument(f);
		if ( doc != null )
		{
			writer.addDocument(doc);
		}
	}
}


