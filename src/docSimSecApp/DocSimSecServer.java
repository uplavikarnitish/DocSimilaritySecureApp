package docSimSecApp;

import preprocess.CollectionLevelInfo;
import preprocess.GenerateTFIDFVector;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Created by nuplavikar on 2/27/16.
 */
public class DocSimSecServer
{

	ServerSocket serverSocket;
	ClientServerConnConfigs connConfigs;
	String keyFileName;


	DocSimSecServer()
	{
		connConfigs = new ClientServerConnConfigs();
		keyFileName = connConfigs.getKeyFileName();
	}

	public void listenSocket()
	{
		try
		{
			serverSocket = new ServerSocket(connConfigs.getServerPortID());
			//TODO: Change as per requirement. If daemonized, this needs a review.
			serverSocket.setSoTimeout(connConfigs.getListeningTimeout());
		} catch (IOException e)
		{
			System.out.println(this.getClass().getName()+": Error! Cannot listen on server port id: "+connConfigs.getServerPortID()+"!");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public Socket acceptServiceRequest()
	{
		Socket acceptedSocket = null;
		try
		{
			System.out.println("Waiting for a connection from client ...");
			acceptedSocket = serverSocket.accept();
			System.out.println("Socket accepted from a client: " + acceptedSocket.getInetAddress() + "!");
		} catch (IOException e)
		{
			System.out.println(this.getClass().getName()+": Error! Service request accept error on port: "+connConfigs.getServerPortID()+"!");
			e.printStackTrace();
			System.exit(-2);
		}
		return acceptedSocket;
	}

	public int sendEncrypedIntermRandProdValues(LinkedList<String> listIntermValues, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		Iterator<String> it = listIntermValues.iterator();
		LinkedList<Integer> lineNosFromFileToBeSent = new LinkedList<Integer>();
		lineNosFromFileToBeSent.add(2);
		lineNosFromFileToBeSent.add(4);
		while(it.hasNext())
		{
			if (connConfigs.sendEncrIntermProdAsString(it.next(), lineNosFromFileToBeSent, objectInputStream, objectOutputStream) != 0)
			{
				System.out.println("ERROR! Some error in sendEncrIntermProdAsString!");
				return -1;
			}
		}
		return 0;
	}

	public int sendNumOfDocs(int value, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return connConfigs.sendInteger(value, objectInputStream, objectOutputStream);
	}
	public int sendNumOfGlobalTerms(int numberOgGlobalTerms, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return connConfigs.sendInteger(numberOgGlobalTerms, objectInputStream, objectOutputStream);
	}




	public LinkedList<String> acceptEncrRandProdValue(int totNumDocsInCol, String intermFileDir, String intermFileNameStart,
													   ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return connConfigs.acceptNValuesFromPeerAndWriteToDNumOfFilesEach(totNumDocsInCol, 1, intermFileDir, intermFileNameStart, objectInputStream, objectOutputStream);
	}

	public int sendEncryptedSimilarityScoreToClient(LinkedList<String> encrSimProdFileNameList, ObjectInputStream serObjectInputStream, ObjectOutputStream serObjectOutputStream)
	{
		int err = 0;

		Iterator<String> iterator = encrSimProdFileNameList.iterator();
		while(iterator.hasNext())
		{
			connConfigs.sendFileAsString(iterator.next(), 1, serObjectInputStream, serObjectOutputStream);
		}
		return err;
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

	int sendGlobalTermsToClient(Set<String> setOfGlobalTerms, ObjectInputStream serObjectInputStream, ObjectOutputStream serObjectOutputStream)
	{
		int err = 0;
		for (String term : setOfGlobalTerms)
		{
			if ( (err = connConfigs.sendString(term, serObjectInputStream, serObjectOutputStream)) != 0)
			{
				System.err.println("ERROR! in sending the term "+term+" to client err = "+err);
				return err;
			}
		}
		return err;
	}


	public static void main(String[] args)
	{

		int totNumGlobalTerms = 0, ret;
		DocSimSecServer docSimSecServer = new DocSimSecServer();
		docSimSecServer.listenSocket();
		String indexLocation = "/home/nuplavikar/temp/index/";//$

		String docVecLocation = "/home/nuplavikar/temp/doc_vectors";
		String encrRandProdMPDir = "/home/nuplavikar/temp/interm_files";
		String encrRandProdMPFileNameStart = "frmClEncrRandProdMP";
		String encrSimProdMPFileNameStart = encrRandProdMPDir+"/serCmpEncrSimProdMP";
		String keyFileName = docSimSecServer.getKeyFileName();

		//Per socket variable declarations
		Socket clientSocket = null;
		ObjectInputStream serObjectInputStream = null;
		ObjectOutputStream serObjectOutputStream = null;
		String encrTFIDFQueryFrmClientFileName = null;
		String encrBinQueryFrmClientFileName = null;
		File encrQueryFrmClientFile = null;
		LinkedList<String> opListIntermRandAndProdFileNames;
		LinkedList<String> encrSimProdFileNameList;

		clientSocket = docSimSecServer.acceptServiceRequest();
		encrTFIDFQueryFrmClientFileName = docVecLocation+"/encrTFIDFQFrmCl_"+clientSocket.hashCode()+"_"+clientSocket.getPort()+".txt";
		encrBinQueryFrmClientFileName = docVecLocation+"/encrBinQFrmCl_"+clientSocket.hashCode()+"_"+clientSocket.getPort()+".txt";
		//System.out.println("encrQueryFrmClientFileName: "+encrQueryFrmClientFileName);

		if (clientSocket == null)
		{
			System.out.println("Error! no client socket accepted!");
			System.exit(-1);
		}
		try
		{
			/*
			* Create input and output object streams based on the client socket - BEGIN
			* */
			OutputStream serOutputStream = clientSocket.getOutputStream();
			InputStream serInputStream = clientSocket.getInputStream();
			serObjectOutputStream = new ObjectOutputStream(serOutputStream);
			serObjectInputStream = new ObjectInputStream(serInputStream);
			/*
			* Create input and output object streams based on the client socket - END
			* */

			//Build the vectors for all documents in memory, later on we will write them to secondary storage
			GenerateTFIDFVector generateTFIDFVector = new GenerateTFIDFVector();
			//We will build vectors for all documents, hence queryDocFileName = null and listOfGlobalTerms = null
			CollectionLevelInfo CollectionLevelInfo = generateTFIDFVector.getDocTFIDFVectors(indexLocation, null, null);
			System.out.println("Created TFIDF vectors for all documents in collection!");
			totNumGlobalTerms = generateTFIDFVector.getNumGlobalTerms();

			System.out.println("Sending the number of global terms ...");
			//Send number of query terms.
			if ((ret = docSimSecServer.sendNumOfGlobalTerms(totNumGlobalTerms, serObjectInputStream, serObjectOutputStream))!=0)
			{
				System.err.println("ERROR! in docSimSecApp.sendNumOfQueryTerms! ret = "+ret);
				System.exit(ret);
			}
			System.out.println("The number of global terms sent!");

			System.out.println("Sending the global terms to client ...");
			//Now send each of the terms one by one to the client so that it can build its vector
			Set<String> setOfGlobalTerms = generateTFIDFVector.getSetOfGlobalTerms();
			if ((ret = docSimSecServer.sendGlobalTermsToClient(setOfGlobalTerms, serObjectInputStream, serObjectOutputStream))!=0)
			{
				System.err.println("ERROR! in docSimSecApp.sendNumOfQueryTerms! ret = "+ret);
				System.exit(ret);
			}
			System.out.println("Sent the global terms to client!");

			//TODO: Add code to send k, U_k

			System.out.println("Accepting the encrypted TFIDF vector query from client ...");
			//Accept encrypted TFIDF query from client and store it locally in a file
			if ( (ClientServerConnConfigs.acceptStrFromPeer(serObjectInputStream, serObjectOutputStream, totNumGlobalTerms, encrTFIDFQueryFrmClientFileName))!=0 )
			{
				System.err.println("ERROR! In execution of acceptStrFromClient for TFIDF encrypted query");
				System.exit(-2);
			}
			System.out.println("Accepted the encrypted TFIDF query vector from client and stored in "+encrTFIDFQueryFrmClientFileName);
			//Accept encrypted Binary TFIDF query from client and store it locally in a file
			if ( (ClientServerConnConfigs.acceptStrFromPeer(serObjectInputStream, serObjectOutputStream, totNumGlobalTerms, encrBinQueryFrmClientFileName))!=0 )
			{
				System.err.println("ERROR! In execution of acceptStrFromClient for Binary TFIDF encrypted query");
				System.exit(-3);
			}
			System.out.println("Accepted the encrypted Binary query vector from client and stored in "+encrBinQueryFrmClientFileName);


			System.out.println("Starting secure, two-party multiplication protocol;\n O/P:ciphertext of plaintext products[E(a.b)]; I/P:ciphertexts[E(a), E(b)] ...");
			//NOTHING TODO SSC ALGORITHM LINES 5, MP protocol part 1 STARTS%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
			/*
			* Write all the local documents in terms of their TF-IDF value and binary value vectors. i.e. two files
			 * per file in index.
			* */
			opListIntermRandAndProdFileNames = generateTFIDFVector.writeDocVectorsToDirAndComputeSecureDotProducts(totNumGlobalTerms, docVecLocation,
					new File(encrTFIDFQueryFrmClientFileName).getAbsolutePath(), new File(encrBinQueryFrmClientFileName).getAbsolutePath(), keyFileName);
			if ( opListIntermRandAndProdFileNames == null )
			{
				System.err.println("ERROR! Unable to writeDocVectorsToDir!");
				System.exit(-2);
			}
			//Verify whether you have got the same number of files that are there in the collection
			if( CollectionLevelInfo.getNumOfDocs() != opListIntermRandAndProdFileNames.size() )
			{
				System.err.println("ERROR! Invalid configuration CollectionLevelInfo.getNumOfDocs()["+
						CollectionLevelInfo.getNumOfDocs()+"]!=opListIntermRandAndProdFileNames.size()["+opListIntermRandAndProdFileNames.size()+"]");
				System.exit(-3);
			}
			//Send the number of documents in the collection. This is necessary to send the intermediate scores later!
			if ( (ret = docSimSecServer.sendNumOfDocs(CollectionLevelInfo.getNumOfDocs(), serObjectInputStream, serObjectOutputStream))!=0)
			{
				System.err.println("ERROR! docSimSecServer.sendNumOfDocs()");
				System.exit(ret);
			}
			//Send the two, encrypted, randomized, partial products for each file in the index to the client for getting
			//the final encrypted product of the randomized intermediate values.
			if ( (ret = docSimSecServer.sendEncrypedIntermRandProdValues(opListIntermRandAndProdFileNames, serObjectInputStream, serObjectOutputStream)) != 0 )
			{
				System.err.println("ERROR! Some error in execution of sendEncrypedIntermRandProdValues()");
				System.exit(ret);
			}
			//For each document in collection accept the encrypted, randomized product and write into file
			LinkedList<String> encrRandProdFileNameList;
			if ( (encrRandProdFileNameList = docSimSecServer.acceptEncrRandProdValue(CollectionLevelInfo.getNumOfDocs(),
					encrRandProdMPDir, encrRandProdMPFileNameStart, serObjectInputStream, serObjectOutputStream)) == null )
			{
				System.err.println("ERROR! Some error in execution of docSimSecServer.acceptEncrRandProdValue()");
				System.exit(ret);
			}
			System.out.println("Number of files containing encrypted, randomized product of MP protocol: "+encrRandProdFileNameList.size());
			if ( opListIntermRandAndProdFileNames.size() != encrRandProdFileNameList.size() )
			{
				System.err.println("ERROR! Number of files mismatch opListIntermRandAndProdFileNames.size()["+
						opListIntermRandAndProdFileNames.size()+"] != ["+encrRandProdFileNameList.size()+"]encrRandProdFileNameList.size()");
				System.exit(-5);
			}
			//As the encrypted, randomized similarity product has been obtained,
			//using the derandomizing value calculated in the previous call to "writeDocVectorsToDirAndComputeSecureDotProducts()"
			//we add this derandomizing value(multiply) to get the encrypted similarity value E(a.b) for each document in
			//collection. Hence calling the security module in native code.
			Iterator<String> itEncrRandProdFileNameList = encrRandProdFileNameList.iterator();
			Iterator<String> itDenonymizingFactor = opListIntermRandAndProdFileNames.iterator();
			int count = 1;
			encrSimProdFileNameList = new LinkedList<String>();
			while(itEncrRandProdFileNameList.hasNext())
			{
				String encrSimProdFileName = encrSimProdMPFileNameStart+"_"+count+".txt";
				encrSimProdFileNameList.add(encrSimProdFileName);
				if ( (ret = generateTFIDFVector.derandomizeEncryptedSimProd(itEncrRandProdFileNameList.next(), itDenonymizingFactor.next(), encrSimProdFileName, keyFileName))!=0 )
				{
					System.err.println("ERROR! in derandomizeEncryptedSimProd(): "+ret);
					System.exit(ret);
				}
				count++;
			}
			//NOTHING TODO SSC ALGORITHM LINES 5, MP protocol part 1 ENDS%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
			System.out.println("Send Encrypted similarity scores to peer!");
			/* Now send the encrypted similarity scores to the client for decryption
			* */
			if ((ret = docSimSecServer.sendEncryptedSimilarityScoreToClient(encrSimProdFileNameList, serObjectInputStream, serObjectOutputStream))!=0)
			{
				System.err.println("ERROR! in sendEncryptedSimilarityScoreToClient(): "+ret);
				System.exit(ret);
			}
			System.out.println("Encrypted similarity scores sent to peer!");


		} catch (IOException e)
		{
			System.err.println("IOException");
			e.printStackTrace();
		}


		//Socket clientSocket = serverSocket.accept()
		System.exit(0);
	}
}
