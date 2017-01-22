package docSimSecApp;

import preprocess.CollectionLevelInfo;
import preprocess.GenerateTFIDFVector;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by nuplavikar on 2/26/16.
 */
public class DocSimSecApp
{

    /**
     * Created by nuplavikar on 2/14/16.
     */

	ClientServerConnConfigs connConfigs;
	Socket clientSocket;
	OutputStream clOutputStream;
	ObjectOutputStream clObjectOutputStream;
	InputStream clInputStream;
	ObjectInputStream clObjectInputStream;
	int totNumGlobalTerms;
	int totNumDocsInCol;
	protected String keyFileName;
	boolean useLSI;
	long k = -1;
	LinkedList<LinkedList<Double>> U_k;
	LinkedList<LinkedList<Double>> U_k_bin;

	DocSimSecApp(int numQueryTerms)
	{
		totNumGlobalTerms = numQueryTerms;
		connConfigs = new ClientServerConnConfigs();
		keyFileName = connConfigs.getKeyFileName();
	}

	public long getK()
	{
		return this.k;
	}

	int sendServiceRequest()
	{
		try
		{
			System.out.println("Connecting to server: " + connConfigs.getServerHostName() + " ...");
			clientSocket = new Socket(connConfigs.getServerHostName(), connConfigs.getServerPortID());
			System.out.println("Connected to server: "+connConfigs.getServerHostName()+"!");
		}
		catch (Exception e)
		{
			System.out.println(this.getClass().getName()+": Error! illegal hostname or IO exception! hostname: "
					+connConfigs.getServerHostName()+" PortID:"+connConfigs.getServerPortID());
			e.printStackTrace();
			System.exit(-1);
		}
		return 0;
	}

	public int createSocketIOStreams()
	{
		int err=0;

		try
		{
			//System.out.println("1====");
			//TODO: case for optimization: begins
			clOutputStream = clientSocket.getOutputStream();
			//System.out.println("2====");
			clInputStream = clientSocket.getInputStream();
			//System.out.println("3====");
			//TODO: case for optimization: ends

			clObjectOutputStream = new ObjectOutputStream(clOutputStream);
			//System.out.println("4====");
			clObjectInputStream = new ObjectInputStream(clInputStream);
			System.out.println("Created the object input and output streams");

		} catch (IOException e)
		{
			System.err.println("Error getting output stream");
			e.printStackTrace();
			err = -1;
		}

		return err;
	}



	public int sendEncrQuery(String encrFileName)
	{
		return connConfigs.sendFileAsString(encrFileName, getNumGlobalTerms(), clObjectInputStream, clObjectOutputStream);
	}

	public int getNumGlobalTerms()
	{return totNumGlobalTerms;}

	public int setNumGlobalTerms(int k)
	{
		totNumGlobalTerms = k;
		return 0;
	}

	public ObjectInputStream getClObjectInputStream()
	{
		return clObjectInputStream;
	}

	public ObjectOutputStream getClObjectOutputStream()
	{
		return clObjectOutputStream;
	}

	public int receiveTotNumOfGlobalTerms(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		totNumGlobalTerms = this.connConfigs.receiveInteger(objectInputStream, objectOutputStream);
		return totNumGlobalTerms;
	}

	public long receiveLSIkValue(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		this.k = this.connConfigs.receiveLong(objectInputStream, objectOutputStream);
		return this.k;
	}

	public int receiveTotNumOfDocs(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return totNumDocsInCol = this.connConfigs.receiveInteger(objectInputStream, objectOutputStream);
	}

	public int getTotNumDocsInCol()
	{
		return totNumDocsInCol;
	}

	public String getKeyFileName()
	{
		if(!(new File(keyFileName).exists()))
		{
			Exception e = new FileNotFoundException("ERROR! Key File does not exist!");
			e.printStackTrace();
			return null;
		}
		else
		{
			return keyFileName;
		}
	}

	public LinkedList<String> acceptIntermediateValues(int totNumDocsInCol, int noOfTermsToBeMultiplied,
													   String intermFileDir, String intermFileNameStart,
										ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return connConfigs.acceptNValuesFromPeerAndWriteToDNumOfFilesEach(totNumDocsInCol, noOfTermsToBeMultiplied, intermFileDir, intermFileNameStart, objectInputStream, objectOutputStream);
	}


	public int sendMPEncrRandProduct(int numDocs, LinkedList intermFileNamesList, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int err = 0;

		if ( numDocs != intermFileNamesList.size() )
		{
			System.err.println("ERROR! numDocs != intermFileNamesList.size()! ");
			err = -1;
		}
		else
		{
			Iterator<String> iterator = intermFileNamesList.iterator();
			while( iterator.hasNext() )
			{
				if ( (err = connConfigs.sendFileAsString(iterator.next(), 1, objectInputStream, objectOutputStream)) != 0)
				{
					System.err.println("ERROR! connConfigs.sendFileAsString(): "+err+"!");
					err = -2;
					break;
				}
			}
		}
		return err;
	}

	public LinkedList<LinkedList<Double>> receiveU_kMatrix(long m, long k, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		long i;
		LinkedList<Double> row = null;
		LinkedList<LinkedList<Double>> U_k = new LinkedList<LinkedList<Double>>();
		for ( i = 0; i < m; i++ )
		{
			row = connConfigs.acceptListOfDoublesFromPeer(k, objectInputStream, objectOutputStream);
			if (row == null)
			{
				System.err.println("ERROR!!! While receiving row Double list for row index:"+ i +" in matrix U_k!");
				Iterator<LinkedList<Double>> rowList= U_k.iterator();
				while (rowList.hasNext())
				{
					LinkedList<Double> temp = rowList.next();
					temp.clear();
				}
				U_k.clear();
				U_k = null;
				return null;
			}
			U_k.add(row);
		}
		return U_k;
	}

	public LinkedList<String> receiveEncryptedSimilarityScoreFrmServer(int totNumDocsInCol, String intermFileDir, String encrSimScoreFileNameStart, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int err = 0, count=1;
		LinkedList<String> rcvdEncrSimScoresFileNameList = new LinkedList<String>();

		for (int i=1; i<=totNumDocsInCol; i++)
		{
			String destnFileName = intermFileDir+"/"+encrSimScoreFileNameStart+"_"+i+".txt";
			rcvdEncrSimScoresFileNameList.add(destnFileName);
			if ((err = ClientServerConnConfigs.acceptStrFromPeer(objectInputStream, objectOutputStream, 1, destnFileName)) != 0)
			{
				System.err.println("ERROR! in ClientServerConnConfigs.acceptStrFromPeer: "+err);
				return null;
			}
		}
		return rcvdEncrSimScoresFileNameList;
	}

	public LinkedList<Double> decryptSimilariyScores(LinkedList <String> encrSimScoresFileNameList, String intermFileDir, String simScoreFileNameStart, GenerateTFIDFVector generateTFIDFVector, String keyFileName)
	{
		int count = 1;
		LinkedList <String> simScoreFileNameList = new LinkedList<String>();
		LinkedList<Double> simScores = new LinkedList<Double>();
		Iterator<String> iterator = encrSimScoresFileNameList.iterator();

		while (iterator.hasNext())
		{
			String simScoreFileName = intermFileDir+"/"+simScoreFileNameStart+"_"+count+".txt";
			simScoreFileNameList.add(simScoreFileName);
			simScores.add(generateTFIDFVector.decryptSimScore(iterator.next(), simScoreFileName, keyFileName));

		}
		return simScores;
	}

	public LinkedList<String> receiveGlobalTerms(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return connConfigs.acceptStrListFromPeer(objectInputStream, objectOutputStream, this.getNumGlobalTerms());
	}

	public boolean isLSIOn()
	{
		return connConfigs.isLSIEnabled();
	}

    public static void main(String args[]) throws IOException
	{
        String indexLocation = "/home/nuplavikar/temp/index/";//$
        String queryDocName = "1.txt";//$
		int ret;




		String encrQueryTFIDFVectorFile = "/home/nuplavikar/IdeaProjects/DocSimilaritySecureApp/encr_tfidf_q.txt";
		String encrQueryBinVectorFile = "/home/nuplavikar/IdeaProjects/DocSimilaritySecureApp/encr_bin_q.txt";
        String docVecLocation = "/home/nuplavikar/temp/doc_vectors";

		String intermFileDir = "/home/nuplavikar/temp/interm_files";
		String intermFileNameStart = "frmServerEncrMultiplicandMultiplier";
		String encrSimScoreFileNameStart = "frmServerEncrSimScore";
		String simScoreFileNameStart = "clCalSimScoresFinal";
		String calculatedIntermEncrRandProd = "clCalIntermEncrRandProdMP";

		//Code to create a socket
		//Time start
		Date date= new Date();
		//getTime() returns current time in milliseconds
		long time = date.getTime();
		//Passed the milliseconds to constructor of Timestamp class
		Timestamp ts1 = new Timestamp(time);
		Timer timer = new Timer();
		//Holds starting time in milliseconds since epoch time.
		long startTime;

		DocSimSecApp docSimSecApp = new DocSimSecApp(0);
		long clientTotalRequestTime = timer.startTimer();


		docSimSecApp.sendServiceRequest();
		System.out.println("Calling createSocketIOStreams to create objects");
		docSimSecApp.createSocketIOStreams();

		//NOTHING TODO PREPROCESSING STAGE(PreSSC) STARTS%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		System.out.println("Receiving the number of terms belonging to server's global vector space ...");
		startTime = timer.startTimer();
		/*keyFileName
		* Get the number of terms in query - BEGIN
		* */
		if (docSimSecApp.receiveTotNumOfGlobalTerms(docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream())<0)
		{
			System.err.println("ERROR! in receiveTotNumOfGlobalTerms() Invalid number of terms in the query! err: "+docSimSecApp.getNumGlobalTerms());
			System.exit(-11);
		}
		System.out.println("Received the number of terms belonging to server's global vector space!");
		/*
		* Get the number of terms in query - END
		* */
		//start receiving the global terms one by one- start
		//It is ASSUMED that both the nodes have used the same analyzer during indexing
		//because the tokens need to be matched with each other to construct vectors. You don't want a token, which
		//is stemmed and one that isn't.
		System.out.println("Obtaining the global terms from server to create query vector ...");
		LinkedList<String> listOfGlobalTerms = null;
		if ( (listOfGlobalTerms = docSimSecApp.receiveGlobalTerms(docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream())) ==null )
		{
			System.err.println("ERROR! in receiveGlobalTerms() listOfGlobalTerms: "+listOfGlobalTerms );
			System.exit(-11);
		}
		System.out.println("Obtained the global terms from server to create query vector! No. of terms received:"+listOfGlobalTerms.size()+" "+listOfGlobalTerms );
		//Not required for standalone feature
		/*if ( listOfGlobalTerms.size()!=docSimSecApp.getNumGlobalTerms() )
		{
			System.err.println("ERROR! Invalid number of global terms received! Expected:"+ docSimSecApp.getNumGlobalTerms()+" Obtained:"+listOfGlobalTerms.size());
			System.exit(-12);
		}*/
		//TODO add code to accept k, U_k, U_k_bin
		docSimSecApp.useLSI = docSimSecApp.connConfigs.isLSIEnabled();
		if ( docSimSecApp.useLSI == true )
		{
			//Receive value k
			System.out.println("Receiving the k value for LSI ...");
			long k = docSimSecApp.receiveLSIkValue(docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream());
			if (k<0)
			{
				System.err.println("ERROR! in receiveTotNumOfGlobalTerms() Invalid number of terms in the query! err: "+docSimSecApp.getNumGlobalTerms());
				System.exit(-12);
			}
			System.out.println("Received the k value for LSI! K:"+docSimSecApp.getK());
			//we know m and n are given as m = docSimSecApp.getNumGlobalTerms(), n = docSimSecApp.getTotNumDocsInCol();
            //TODO Accept U_k - function written above receiveU_kMatrix()
			docSimSecApp.U_k = docSimSecApp.receiveU_kMatrix(docSimSecApp.getNumGlobalTerms(),
					docSimSecApp.getK(), docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream());
			if ( docSimSecApp.U_k == null )
			{
				System.err.println("ERROR!!! Cannot obtain U_k from peer!");
				System.exit(-13);
			}
			System.out.println("Obtained U_k for TFIDF from server! Dim(U_k):["+docSimSecApp.U_k.size()+" x "+docSimSecApp.U_k.get(0).size()+"]");
			//TODO Accept U_k_bin - function written above receiveU_kMatrix()
			docSimSecApp.U_k_bin = docSimSecApp.receiveU_kMatrix(docSimSecApp.getNumGlobalTerms(),
					docSimSecApp.getK(), docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream());
			if ( docSimSecApp.U_k_bin == null )
			{
				System.err.println("ERROR!!! Cannot obtain U_k_bin from peer!");
				System.exit(-14);
			}
			System.out.println("Obtained U_k_bin for binary from server! Dim(U_k_bin):["+docSimSecApp.U_k_bin.size()+" x "+docSimSecApp.U_k_bin.get(0).size()+"]");

		}
		System.out.println("Generating the query vector ...");
		//start receiving the global terms one by one- end

		GenerateTFIDFVector generateTFIDFVector = new GenerateTFIDFVector();
        CollectionLevelInfo collectionLevelInfo = generateTFIDFVector.getDocTFIDFVectors(indexLocation, queryDocName, listOfGlobalTerms, docSimSecApp.getK(), docSimSecApp.isLSIOn(), docSimSecApp.U_k, docSimSecApp.U_k_bin);
		System.out.println(timer.getFormattedTime("Client Preprocessing TIME:", timer.endTimer(startTime) ));

		System.out.println("Generated the query vector!");
		//NOTHING TODO PREPROCESSING STAGE(PreSSC) ENDS%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

		//NOTHING TODO SSC ALGORITHM LINES 2, 3 STARTS__________________________________________________________________
		long clientActualProtStartTime = timer.startTimer();
		String keyFileName = docSimSecApp.getKeyFileName();
		if (generateTFIDFVector.writeDocVectorToFile(docSimSecApp.getK()/*only if lsi enabled*/, queryDocName, encrQueryTFIDFVectorFile, encrQueryBinVectorFile, keyFileName) == -1 )
		{
			System.err.println("ERROR - FILE NOT FOUND! File "+queryDocName+" is not indexed!");
			System.exit(-1);
		}

		if (docSimSecApp.isLSIOn())
		{
			//Prior to the writeDocVectorToFile() function, we were working with larger m dimensions. Beyond this point
			//we need to work with k reduced dimensions. Hence. modifying the global term count to k - TODO - make this better
			docSimSecApp.setNumGlobalTerms((int)docSimSecApp.getK()); //k ~ 100-200
		}
		//Send encrypted TFIDF query vector stored in file
		docSimSecApp.sendEncrQuery(encrQueryTFIDFVectorFile);
		//Send encrypted Binary TFIDF query vector stored in file
		docSimSecApp.sendEncrQuery(encrQueryBinVectorFile);
		//NOTHING TODO SSC ALGORITHM LINES 2, 3 ENDS____________________________________________________________________

		//Accept the number of documents in the peer's collection
		if ( (ret = docSimSecApp.receiveTotNumOfDocs(docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream())) < 0 )
		{
			System.err.println("ERROR! in execution of docSimSecApp.receiveTotNumOfDocs()! ret:"+ret);
			System.exit(ret);
		}
		System.out.println("Number of documents in Server's collection = " + docSimSecApp.getTotNumDocsInCol());

		//Accept the encrypted, randomized, intermediate product values
		LinkedList intermProdsFileNameList;
		if ( (intermProdsFileNameList = docSimSecApp.acceptIntermediateValues(docSimSecApp.getTotNumDocsInCol(), 2/*two nos.(products) expected*/,
				intermFileDir, intermFileNameStart, docSimSecApp.getClObjectInputStream(),docSimSecApp.getClObjectOutputStream())) == null )
		{
			System.err.println("ERROR! in execution of docSimSecApp.acceptIntermValues()! ret:" + ret);
			System.exit(ret);
		}
		System.out.println("Number of documents in Server's collection = "+docSimSecApp.getTotNumDocsInCol());

		if ( intermProdsFileNameList.size() != docSimSecApp.getTotNumDocsInCol() )
		{
			System.err.println("ERROR! No. of intermediate products("+ intermProdsFileNameList.size()+") != Tot. num. docs. collection("+docSimSecApp.getTotNumDocsInCol()+")!");
			System.exit(ret);
		}

		//Compute the encrypted, randomized product for both products per file in the collection.
		Iterator<String> iterator= intermProdsFileNameList.iterator();
		LinkedList<String> encrRandProdFileNameList =  new LinkedList<String>();
		int count = 1;
		while(iterator.hasNext())
		{
			String encrRandProdFileName = intermFileDir + "/"+calculatedIntermEncrRandProd+"_"+count+".txt";
			encrRandProdFileNameList.add(encrRandProdFileName);

			if (( ret = generateTFIDFVector.computeEncrPhaseOfSecMP(iterator.next(), encrRandProdFileName, keyFileName))!=0)
			{
				System.err.println("ERROR! in generateTFIDFVector.computeEncrPhaseOfSecMP(): "+ret);
				System.exit(ret);
			}
			count++;
		}
		//Send the computed encrypted and randomized product to the peer.
		if ( (ret = docSimSecApp.sendMPEncrRandProduct(docSimSecApp.getTotNumDocsInCol(), encrRandProdFileNameList, docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream())) !=0 )
		{
			System.err.println("ERROR! in docSimSecApp.sendMPEncrRandProduct(): "+ret);
			System.exit(ret);
		}

		System.out.println("Receiving Encrypted similarity scores from peer!");
		LinkedList<String> encrSimScoresFileNameList;
		//Accept the encrypted similarity scores
		if ((encrSimScoresFileNameList = docSimSecApp.receiveEncryptedSimilarityScoreFrmServer(docSimSecApp.getTotNumDocsInCol(), intermFileDir, encrSimScoreFileNameStart, docSimSecApp.getClObjectInputStream(), docSimSecApp.getClObjectOutputStream()))==null)
		{
			System.err.println("ERROR! in sendEncryptedSimilarityScoreToClient(): "+ret);
			System.exit(ret);
		}

		if ( encrSimScoresFileNameList.size() != docSimSecApp.getTotNumDocsInCol() )
		{
			System.err.println("ERROR! No. of encrypted products("+ encrSimScoresFileNameList.size()+") != Tot. num. docs. collection("+docSimSecApp.getTotNumDocsInCol()+")!");
			System.exit(ret);
		}

		System.out.println("Received "+encrSimScoresFileNameList.size()+" Encrypted similarity scores from peer!");
		System.out.println("Decrypting the received Encrypted Similarity Scores");
		LinkedList<String> listSimScores = new LinkedList<String>();
		LinkedList<Double> simScores;
		if ( (simScores = docSimSecApp.decryptSimilariyScores(encrSimScoresFileNameList, intermFileDir, simScoreFileNameStart, generateTFIDFVector, keyFileName)) == null)
		{
			System.err.println("ERROR! in docSimSecApp.decryptSimilariyScores(): "+ret);
			System.exit(ret);
		}

		System.out.println(timer.getFormattedTime("Client Actual Protocol TIME:", timer.endTimer(clientActualProtStartTime) ));
		System.out.println("Similarity Scores: "+simScores);
		System.out.println(timer.getFormattedTime("Client total request TIME:", timer.endTimer(clientTotalRequestTime) ));
		//Time end
		Date date2= new Date();
		//getTime() returns current time in milliseconds
		long time2 = date2.getTime();
		//Passed the milliseconds to constructor of Timestamp class
		Timestamp ts2 = new Timestamp(time2);
		System.out.println("START Time Stamp: "+ts1);
		System.out.println("END Time Stamp: "+ts2);
		System.out.println("Number of query terms = "+docSimSecApp.getNumGlobalTerms());


	}

}
