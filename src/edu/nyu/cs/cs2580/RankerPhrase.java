package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

class RankerPhrase extends Ranker {

	public RankerPhrase(Options options, CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    	ScoredDocs scoredDocs = new ScoredDocs();
    	runQuery(query, numResults, scoredDocs);
        
    	return scoredDocs.getScoredDocs();
	}

	private ScoredDocument scoreDocument(Query query, int did) {
		// Process the raw query into tokens.
		query.processQuery();

		// Get the document tokens.
		Document doc = _indexer.getDoc(did);

		double score = calScore(query, doc);
		return new ScoredDocument(doc, score);
	}

	public double calScore(Query query, Document doc) {
		Vector<String> qv = query._tokens;
		Vector<String> docTokens = ((DocumentFull) doc)
				.getConvertedTitleTokens();
		docTokens.addAll(((DocumentFull) doc).getConvertedBodyTokens());
		double score = 0.0;

		// Score the document.
		if (qv.size() == 1) {
			for (String queryToken : query._tokens) {
				for (String docToken : docTokens) {
					if (queryToken.equals(docToken))
						score++;
				}
			}
		} else {
			for (int i = 0; i < qv.size() - 1; i++) {
				if (docTokens.size() == 1) {
					if (qv.get(i).equals(docTokens.get(0)))
						score++;
				} else {
					for (int j = 0; j < docTokens.size() - 1; j++)
						if (qv.get(i).equals(docTokens.get(j))
								&& qv.get(i + 1).equals(docTokens.get(j + 1)))
							score++;
				}
			}
		}
		return score;
	}

	@Override
	public void runQuery(Query query, int numResults, ScoredDocs scoredDocs) {
		Vector<ScoredDocument> all = new Vector<ScoredDocument>();
		long count = 0;
		for (int i = 0; i < _indexer.numDocs(); ++i) {
			count++;
			all.add(scoreDocument(query, i));
		}
		Collections.sort(all, Collections.reverseOrder());
		for (int i = 0; i < all.size() && i < numResults; ++i) {
			scoredDocs.add(all.get(i));
		}
		scoredDocs.set_num_of_result(count);
	}
	
	@Override
	public void runQueryForAd(Query processedQuery, int _numResults,
			ScoredDocs scoredAdDocs) {
		throw new UnsupportedOperationException("should be implemented first");
	}	
}
