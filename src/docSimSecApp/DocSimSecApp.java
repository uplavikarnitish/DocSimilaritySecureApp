package docSimSecApp;

import preprocess.DocVectorInfo;
import preprocess.GenerateTFIDFVector;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

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
	int totNumQueryTerms;
	int totNumDocsInCol;


	DocSimSecApp(int numQueryTerms)
	{
		totNumQueryTerms = numQueryTerms;
		connConfigs = new ClientServerConnConfigs();
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
			System.out.println("1====");
			//TODO: case for optimization: begins
			clOutputStream = clientSocket.getOutputStream();
			System.out.println("2====");
			clInputStream = clientSocket.getInputStream();
			System.out.println("3====");
			//TODO: case for optimization: ends

			clObjectOutputStream = new ObjectOutputStream(clOutputStream);
			System.out.println("4====");
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
		return connConfigs.sendFileAsString(encrFileName, getNumQueryTerms(), clObjectInputStream, clObjectOutputStream);
	}

	public int getNumQueryTerms()
	{return totNumQueryTerms;}

	public ObjectInputStream getClObjectInputStream()
	{
		return clObjectInputStream;
	}

	public ObjectOutputStream getClObjectOutputStream()
	{
		return clObjectOutputStream;
	}

	public int sendNumOfQueryTerms()
	{
		return connConfigs.sendInteger(this.getNumQueryTerms(), this.getClObjectInputStream(), this.getClObjectOutputStream());
	}

	public int receiveTotNumOfDocs(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return totNumDocsInCol = this.connConfigs.receiveInteger(objectInputStream, objectOutputStream);
	}

	public int getTotNumDocsInCol()
	{
		return totNumDocsInCol;
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

    public static void main(String args[]) throws IOException
	{
        String indexLocation = "/home/nuplavikar/index/crawled/stw-10/";//$
        String queryDocName = "01.txt";//$
		int ret;




		String encrQueryTFIDFVectorFile = "/home/nuplavikar/IdeaProjects/DocSimilaritySecureApp/encr_tfidf_q.txt";
		String encrQueryBinVectorFile = "/home/nuplavikar/IdeaProjects/DocSimilaritySecureApp/encr_bin_q.txt";
        String docVecLocation = "/home/nuplavikar/temp/doc_vectors";
		String keyFileName = "/home/nuplavikar/IdeaProjects/DocSimilaritySecureApp/keys.txt";
		String intermFileDir = "/home/nuplavikar/temp/interm_files";
		String intermFileNameStart = "frmServerEncrMultiplicandMultiplier";
		String encrSimScoreFileNameStart = "frmServerEncrSimScore";
		String simScoreFileNameStart = "clCalSimScoresFinal";
		String calculatedIntermEncrRandProd = "clCalIntermEncrRandProdMP";

        GenerateTFIDFVector generateTFIDFVector = new GenerateTFIDFVector();
        DocVectorInfo docVectorInfo = generateTFIDFVector.getDocTFIDFVectors(indexLocation);

		if (generateTFIDFVector.writeDocVectorToFile(queryDocName, encrQueryTFIDFVectorFile, encrQueryBinVectorFile, keyFileName) == -1 )
		{
			System.err.println("ERROR - FILE NOT FOUND! File "+queryDocName+" is not indexed!");
			System.exit(-1);
		}

        System.out.println("Size of double:" + Double.SIZE + " bits  ==  " + Double.BYTES + " bytes");

		DocSimSecApp docSimSecApp = new DocSimSecApp(generateTFIDFVector.getNumGlobalTerms());
		//Code to create a socket
		//Time start
		Date date= new Date();
		//getTime() returns current time in milliseconds
		long time = date.getTime();
		//Passed the milliseconds to constructor of Timestamp class
		Timestamp ts1 = new Timestamp(time);


		docSimSecApp.sendServiceRequest();
		System.out.println("Calling createSocketIOStreams to create objects");
		docSimSecApp.createSocketIOStreams();

		//Send number of query terms.
		if ((ret = docSimSecApp.sendNumOfQueryTerms())!=0)
		{
			System.err.println("ERROR! in docSimSecApp.sendNumOfQueryTerms! ret = "+ret);
			System.exit(ret);
		}
		//Send encrypted TFIDF query vector stored in file
		docSimSecApp.sendEncrQuery(encrQueryTFIDFVectorFile);

		//Send encrypted Binary TFIDF query vector stored in file
		docSimSecApp.sendEncrQuery(encrQueryBinVectorFile);

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
		System.out.println("Similarity Scores: "+simScores);
		//Time end
		Date date2= new Date();
		//getTime() returns current time in milliseconds
		long time2 = date2.getTime();
		//Passed the milliseconds to constructor of Timestamp class
		Timestamp ts2 = new Timestamp(time2);
		System.out.println("START Time Stamp: "+ts1);
		System.out.println("END Time Stamp: "+ts2);
		System.out.println("Number of query terms = "+docSimSecApp.getNumQueryTerms());


	}

}
