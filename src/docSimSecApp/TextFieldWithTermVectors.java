package docSimSecApp;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

public class TextFieldWithTermVectors extends FieldType
{


	public TextFieldWithTermVectors()
	{
		this.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		this.setTokenized(true);
		this.setStoreTermVectors(true);
		this.setStoreTermVectorPositions(true);
		this.setStoreTermVectorOffsets(true);
	}
}
