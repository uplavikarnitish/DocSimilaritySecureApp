package docSimSecApp;

import preprocess.DocVectorInfo;
import preprocess.GenerateTFIDFVector;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Created by nuplavikar on 2/27/16.
 */
public class DocSimSecServer
{

	ServerSocket serverSocket;
	ClientServerConnConfigs connConfigs;


	DocSimSecServer()
	{
		connConfigs = new ClientServerConnConfigs();
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


	public int receiveTotNumOfQueryTerms(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		return this.connConfigs.receiveInteger(objectInputStream, objectOutputStream);
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


	public static void main(String[] args)
	{

		int totNumQueryTerms, ret;
		DocSimSecServer docSimSecServer = new DocSimSecServer();
		docSimSecServer.listenSocket();
		String indexLocation = "/home/nuplavikar/temp/index/";//$

		String docVecLocation = "/home/nuplavikar/temp/doc_vectors";
		String encrRandProdMPDir = "/home/nuplavikar/temp/interm_files";
		String encrRandProdMPFileNameStart = "frmClEncrRandProdMP";
		String encrSimProdMPFileNameStart = encrRandProdMPDir+"/serCmpEncrSimProdMP";
		String keyFileName = "/home/nuplavikar/IdeaProjects/DocSimilaritySecureApp/keys.txt";

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
			//TODO: case for optimization: begins
			OutputStream serOutputStream = clientSocket.getOutputStream();
			InputStream serInputStream = clientSocket.getInputStream();
			//TODO: case for optimization: ends
			serObjectOutputStream = new ObjectOutputStream(serOutputStream);
			serObjectInputStream = new ObjectInputStream(serInputStream);
			/*
			* Create input and output object streams based on the client socket - END
			* */


			/*
			* Get the number of terms in query - BEGIN
			* */
			System.out.println("Reading the number of terms belonging to query ...");

			totNumQueryTerms = docSimSecServer.receiveTotNumOfQueryTerms(serObjectInputStream, serObjectOutputStream);
			if (totNumQueryTerms<0)
			{
				System.out.println("ERROR! in receiveTotNumOfQueryTerms() Invalid number of terms in the query! err: "+totNumQueryTerms);
				System.exit(totNumQueryTerms);
			}
			/*
			* Get the number of terms in query - END
			* */



			//Accept encrypted TFIDF query from client and store it locally in a file
			if ( (ClientServerConnConfigs.acceptStrFromPeer(serObjectInputStream, serObjectOutputStream, totNumQueryTerms, encrTFIDFQueryFrmClientFileName))!=0 )
			{
				System.err.println("ERROR! In execution of acceptStrFromClient for TFIDF encrypted query");
				System.exit(-2);
			}
			System.out.println("Accepted the encrypted TFIDF query vector from client and stored in "+encrTFIDFQueryFrmClientFileName);

			//Accept encrypted Binary TFIDF query from client and store it locally in a file
			if ( (ClientServerConnConfigs.acceptStrFromPeer(serObjectInputStream, serObjectOutputStream, totNumQueryTerms, encrBinQueryFrmClientFileName))!=0 )
			{
				System.err.println("ERROR! In execution of acceptStrFromClient for Binary TFIDF encrypted query");
				System.exit(-3);
			}
			System.out.println("Accepted the encrypted Binary query vector from client and stored in "+encrBinQueryFrmClientFileName);


			/*
			* Write all the local documents in terms of their TF-IDF value and binary value vectors. i.e. two files
			 * per file in index.
			* */
			GenerateTFIDFVector generateTFIDFVector = new GenerateTFIDFVector();
			DocVectorInfo docVectorInfo = generateTFIDFVector.getDocTFIDFVectors(indexLocation);
			opListIntermRandAndProdFileNames = generateTFIDFVector.writeDocVectorsToDirAndComputeSecureDotProducts(totNumQueryTerms, docVecLocation,
					new File(encrTFIDFQueryFrmClientFileName).getAbsolutePath(), new File(encrBinQueryFrmClientFileName).getAbsolutePath(), keyFileName);
			if ( opListIntermRandAndProdFileNames == null )
			{
				System.err.println("ERROR! Unable to writeDocVectorsToDir!");
				System.exit(-2);
			}


			//Verify whether you have got the same number of files that are there in the collection
			if( docVectorInfo.getNumOfDocs() != opListIntermRandAndProdFileNames.size() )
			{
				System.err.println("ERROR! Invalid configuration docVectorInfo.getNumOfDocs()["+
						docVectorInfo.getNumOfDocs()+"]!=opListIntermRandAndProdFileNames.size()["+opListIntermRandAndProdFileNames.size()+"]");
				System.exit(-3);
			}
			//Send the number of documents in the collection. This is necessary to send the intermediate scores later!
			if ( (ret = docSimSecServer.sendNumOfDocs(docVectorInfo.getNumOfDocs(), serObjectInputStream, serObjectOutputStream))!=0)
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
			if ( (encrRandProdFileNameList = docSimSecServer.acceptEncrRandProdValue(docVectorInfo.getNumOfDocs(),
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

			System.out.println("Send Encrypted similarity scores to peer!");
			/* Now send the encrypted similarity scores to the client for decryption
			*
			* */
			if ((ret = docSimSecServer.sendEncryptedSimilarityScoreToClient(encrSimProdFileNameList, serObjectInputStream, serObjectOutputStream))!=0)
			{
				System.err.println("ERROR! in sendEncryptedSimilarityScoreToClient(): "+ret);
				System.exit(ret);
			}



		} catch (IOException e)
		{
			System.err.println("IOException");
			e.printStackTrace();
		}


		//Socket clientSocket = serverSocket.accept()
		System.exit(0);
	}
}
