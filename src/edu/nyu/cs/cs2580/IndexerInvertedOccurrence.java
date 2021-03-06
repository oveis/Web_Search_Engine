package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends IndexerCommon implements
		Serializable {
	private static final long serialVersionUID = 1077111905740085030L;

	// Inverted Index : ( term key, (document id, (appearance positions) ) )
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _index = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();

	// Phrase document list : (phrase, (document id, (appearance positions) ) )  
	private HashMap<String, HashMap<Integer, ArrayList<Integer>>> _phraseDocMap = new HashMap<String, HashMap<Integer, ArrayList<Integer>>>();
	
	// Back-up variables for serializable file write.
	protected Map<String, Document> t_documents;
	protected Map<String, Integer> t_dictionary;
	protected int t_numDocs;
	protected long t_totalTermFrequency;

	private Map<Integer, Document> _documentsById = null;
	
	// Provided for serialization
	public IndexerInvertedOccurrence() {
	}

	// The real constructor
	public IndexerInvertedOccurrence(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	/**
	 * After making index files, save _dictionary into file
	 **/
	@Override
	public void writeIndexerToFile() throws IOException {
		String dicFile = _options._indexPrefix + "/dictionary.idx";
		ObjectOutputStream writer = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(dicFile),2048));
		// back-up variables from Indexer class
		t_documents = _documents;
		t_dictionary = _dictionary;
		t_numDocs = _numDocs;
		t_totalTermFrequency = _totalTermFrequency;

		writer.writeObject(this);
		writer.close();
	}

	/**
	 * Make Index with content string of html.
	 * 
	 * @param content
	 * @param did
	 * @return token size
	 **/
	@Override
	public int makeIndex(String content, int did) {
		Scanner s = new Scanner(content);
		int position = 1;
		int tokenSize = 0;
		while (s.hasNext()) {
			String token = porterAlg(s.next()).toLowerCase();
			int idx = -1;

			// This Token is already in the dictionary
			if (_dictionary.containsKey(token)) {
				idx = _dictionary.get(token);
				HashMap<Integer, ArrayList<Integer>> docMap;

				// Check Term
				if (!_index.containsKey(idx)) {
					docMap = new HashMap<Integer, ArrayList<Integer>>();
					_index.put(idx, docMap);
				} else {
					docMap = _index.get(idx);
				}

				// Check Doc_ID in the Term
				if (docMap.containsKey(did))
					docMap.get(did).add(position);
				else {
					ArrayList<Integer> occurList = new ArrayList<Integer>();
					occurList.add(position);
					docMap.put(did, occurList);
				}
			}
			// This is new Token
			else {
				idx = _dictionary.size();
				_dictionary.put(token, idx);
				HashMap<Integer, ArrayList<Integer>> docMap = new HashMap<Integer, ArrayList<Integer>>();
				ArrayList<Integer> occurList = new ArrayList<Integer>();
				occurList.add(position);
				docMap.put(did, occurList);
				_index.put(idx, docMap);
			}
			++_totalTermFrequency;
			++position;
			++tokenSize;
		}
		s.close();
		return tokenSize;
	}

	/*
	 *  Write memory data into file
	 * after 1000 documents processing, it saved in one file
	 * private int tmpId = 0;
	 */
	@Override
	public void writeToFile(int round) {
		
		Integer[] idxList = _index.keySet().toArray(new Integer[1]);
		Arrays.sort(idxList, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return (o1.intValue() % MAXCORPUS) - (o2.intValue() % MAXCORPUS);
			}
			
		});
		
		int corpusId = idxList[0] % MAXCORPUS;
		ObjectOutputStream writer = null;
		
		Map<Integer, HashMap<Integer, ArrayList<Integer>>> tempIndex = null;

		for(int wId : idxList) {
			if( corpusId != (wId % MAXCORPUS) ) {
				if(! tempIndex.isEmpty() ) {
					System.out.println("Writing partial index " + corpusId);
					try {
						writer = createObjOutStream(getPartialIndexName(corpusId, round));
						writer.writeObject(tempIndex);
						writer.close();
						writer = null;
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException("Error during partial index writing");
					}
					
					tempIndex.clear();
					tempIndex = null;
				}
				
				corpusId = wId % MAXCORPUS;
			}
			
			if(tempIndex == null) {
				tempIndex = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
			}
			
			tempIndex.put(wId, _index.remove(wId));
		}
		
		//last partial index
		System.out.println("Writing partial index " + corpusId);
        try {
            writer = createObjOutStream(getPartialIndexName(corpusId, round));
            writer.writeObject(tempIndex);
            writer.close();
            writer = null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during partial index writing");
        }
		
		_index.clear();
	}
	
	@SuppressWarnings("unchecked")
    @Override
    protected void mergePartialIndex(int lastRound) {
        ObjectInputStream reader = null;
        
        for(int idx = 0; idx < MAXCORPUS; idx++) {
            Map<Integer, Map<Integer, ArrayList<Integer>>> finalIndex = 
                    new HashMap<Integer, Map<Integer, ArrayList<Integer>>>();
            
            for(int round=1; round <= lastRound; round++) {
                File partialIdx = new File(getPartialIndexName(idx, round));
                if(partialIdx.exists()) {
                    System.out.println("Merging partial index " + idx + " of round " + round);
                    reader = createObjInStream(partialIdx.getAbsolutePath());
                    try {
                        Map<Integer, Map<Integer, ArrayList<Integer>>> pIdx = 
                                (Map<Integer, Map<Integer, ArrayList<Integer>>>)reader.readObject();
                        for(int wordId : pIdx.keySet()) {
                            if(finalIndex.containsKey(wordId)) {
                                Map<Integer, ArrayList<Integer>> old = finalIndex.get(wordId);
                                Map<Integer, ArrayList<Integer>> curr = pIdx.get(wordId);
                                
                                for(int docId : curr.keySet()) {
                                    if(old.containsKey(docId)) {
                                        ArrayList<Integer> oldPositions = old.get(docId);
                                        ArrayList<Integer> currPositions = curr.get(docId);
                                        
                                        oldPositions.addAll(currPositions);
                                        
                                        //do we need below line, really?
                                        old.put(docId, oldPositions);
                                    } else {
                                        old.put(docId, curr.get(docId));
                                    }
                                }
                                
                                //do we need below line, really, again?
                                finalIndex.put(wordId, old);
                            } else {
                                finalIndex.put(wordId, pIdx.get(wordId));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Error during reading partial index");
                    }
                }
            }
            
            writeFinalIndex(idx, finalIndex);
            cleaningPartialIndex(idx, lastRound);
        }
    }
	
	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
		// Load Documents file
		/*
		 * String docFile = _options._indexPrefix + "/documents.idx";
		 * ObjectInputStream reader = new ObjectInputStream(new
		 * FileInputStream(docFile)); _documents =
		 * (Vector<Document>)reader.readObject(); reader.close();
		 * System.out.println("Load documents from: " + docFile);
		 */

		// Load Dictionary file
		String dicFile = _options._indexPrefix + "/dictionary.idx";
		ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
				dicFile)));
		IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence) reader
				.readObject();
		System.out.println("Load dictionary from: " + dicFile);
		reader.close();
		
		this._documents = loaded.t_documents;
		this._dictionary = loaded.t_dictionary;
		this._numDocs = loaded.t_numDocs;
		this._totalTermFrequency = loaded.t_totalTermFrequency;
		
		_documentsById = new HashMap<Integer, Document>();
        
        for(Document d : _documents.values()) {
            _documentsById.put(d._docid, d);
        }

		System.out.println(Integer.toString(_numDocs) + " documents loaded "
				+ "with " + Long.toString(_totalTermFrequency) + " terms!");
	}

	@Override
	public Document getDoc(int docid) {
		return _documentsById.get(docid);
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}.
	 * @param  phrase
	 * @param  current document id
	 * @return  next document id
	 */
	@Override
	public int nextPhrase(String phrase, int docId) {
	    HashMap<Integer, ArrayList<Integer>> phraseDocMap = getPhraseDocMap(phrase);
	    Set<Integer> keySet = phraseDocMap.keySet();
	    int nextDocId = Integer.MAX_VALUE;
	    for(Integer key : keySet)
	        if(key.intValue() > docId && key.intValue() < nextDocId)
	            nextDocId = key.intValue();
	    
	    int result = (nextDocId == Integer.MAX_VALUE) ? -1 : nextDocId;
	    return result;
	}

	protected Vector<Integer> retriveDocList(String word) throws IOException,
			ClassNotFoundException {
	    if(!_dictionary.containsKey(word))
	        return new Vector<Integer>();
	    
		int idx = _dictionary.get(word);
		HashMap<Integer, ArrayList<Integer>> docMap = getDocMap(idx);
		Vector<Integer> docList = new Vector<Integer>();

		Set<Integer> keySet = docMap.keySet();
		for (Integer key : keySet)
			docList.add(key);

		// Sort the doc list
		Collections.sort(docList);

		return docList;
	}

	/**
	 * Read Corpus file related with idx retrieve the document list of idx
	 * 
	 * @return Vector<Integer> Document List
	 **/
	@SuppressWarnings("unchecked")
	private HashMap<Integer, ArrayList<Integer>> getDocMap(int idx){

		if (_index.containsKey(idx))
			return _index.get(idx);

		int pageNum = idx % MAXCORPUS;

		// Read corpus file
		String indexFile = _options._indexPrefix + "/index_" + String.format("%02d", pageNum)
				+ ".idx";
		
		HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> _tmpIndex
		        = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		try{
    		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(	indexFile));
    		_tmpIndex = (HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>) reader.readObject();
    		reader.close();
		}catch(Exception e){
		    System.err.print(e.getMessage());
		}
		
		if (!_tmpIndex.containsKey(idx))
			return new HashMap<Integer, ArrayList<Integer>>();
		else {
			HashMap<Integer, ArrayList<Integer>> docMap = _tmpIndex.get(idx);
			_index.put(idx, docMap);
			return docMap;
		}
	}

	/** 
	 * Get Indexer for phrase
	 * @param : String phrase
	 * @return : HashMap<Integer, ArrayList<Integer>> docMap           
	 **/
    HashMap<Integer, ArrayList<Integer>> getPhraseDocMap(String phrase){
        
        // If it is already existed in the _phraseDocMap, return this.
        if(_phraseDocMap.containsKey(phrase))
            return _phraseDocMap.get(phrase);
            
        Scanner scan = new Scanner(phrase);
        HashMap<Integer, ArrayList<Integer>> baseDocMap = new HashMap<Integer, ArrayList<Integer>>();
        int termDistance = 0;
        
        while(scan.hasNext()){
            String word = scan.next();
            if(!_dictionary.containsKey(word))  return null;
            int idx = _dictionary.get(word);
            
            if(baseDocMap.isEmpty()){      // If this is first word of phrase
                    baseDocMap = getDocMap(idx);                   
            }else{
                HashMap<Integer, ArrayList<Integer>> curDocMap = getDocMap(idx);
                Set<Integer> baseDocIDSet = baseDocMap.keySet();
                ArrayList<Integer> baseDocIDlist = new ArrayList<Integer>(baseDocIDSet);
                
                for(Integer docID : baseDocIDlist){
                    if(curDocMap.containsKey(docID)){
                        ArrayList<Integer> firstTermAppear = baseDocMap.get(docID);
                        ArrayList<Integer> curTermAppear = curDocMap.get(docID);
                 
                        for(int i=0; i<firstTermAppear.size(); i++){
                            for(int j=0; j<curTermAppear.size(); j++){
                                if(firstTermAppear.get(i) + termDistance == curTermAppear.get(j))   // found
                                    break;
                                else if(j == curTermAppear.size()-1)    // Not found
                                    firstTermAppear.remove(i--);
                            }       
                        }
                        
                        if(firstTermAppear.isEmpty())   baseDocMap.remove(docID);                        
                    }else{
                        baseDocMap.remove((Integer)docID);                        
                    }
                }
            }
            termDistance++;
        }
        _phraseDocMap.put(phrase, baseDocMap);
        return baseDocMap;
    }
    
    /**
     * Corpus Doc Frequency By Term
     * @param : String term
     * @return : total # of documents with term
     **/
    @Override
    public int corpusDocFrequencyByTerm(String term) {
        HashMap<Integer, ArrayList<Integer>> docMap = null;
        if(term.contains(" ")){ // Phrase
            docMap = getPhraseDocMap(term);
            return docMap.size();
        }else{ // Word
            if(!_dictionary.containsKey(term))
                return 0;
            int idx = _dictionary.get(term);
            docMap = getDocMap(idx);
            return docMap.size();
        }
    }

    /**
     * Corpus Term Frequency
     * @param : String term
     * @return : total # of term appearance in corpus
     **/
	@Override
	public int corpusTermFrequency(String term) {
		int count = 0;
		HashMap<Integer, ArrayList<Integer>> docMap = null; 
		if(term.contains(" ")){ // Phrase
		    docMap = getPhraseDocMap(term);		    
		}else{ // Word
		    if (!_dictionary.containsKey(term))
		        return 0;
            int idx = _dictionary.get(term);
            docMap = getDocMap(idx);
		}
		Set<Integer> keySet = docMap.keySet();
        for (Integer key : keySet) {
            count += docMap.get(key).size();
        }
		return count;
	}

	/**
     * Document Term Frequency
     * @param : String term, String url
     * @return : total # of term appearance in a document
     **/
	@Override
	public int documentTermFrequency(String term, String url) {
		// Get Document ID relating this url
	    int docid = 0;
		for (Document doc : _documents.values()) {
			if (doc.getUrl().equals(url))
				docid = doc._docid;
		}
		
		// Get Document Map
		HashMap<Integer, ArrayList<Integer>> docMap = null;
		if(term.contains(" ")){ // Phrase
            docMap = getPhraseDocMap(term);          
        }else{ // Word
            if (!_dictionary.containsKey(term))
                return 0;
            int idx = _dictionary.get(term);
            docMap = getDocMap(idx);
        }
		return docMap.get(docid).size();
	}

	@Override
	public List<Integer> getTermPositions(int wordId, int docId) {
		//return positions of the given word in the given document
		throw new UnsupportedOperationException();
	}
}
