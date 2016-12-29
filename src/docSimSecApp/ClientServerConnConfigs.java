package docSimSecApp;

import java.io.*;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by nuplavikar on 2/27/16.
 */
public class ClientServerConnConfigs
{

	String serverHostName;
	int serverPortID;
	int listeningTimeout;
	String ack;
	String keyFileName;


	ClientServerConnConfigs()
	{
		serverHostName = "r11wjiang.managed.mst.edu";
		//serverHostName = "localhost";
		serverPortID = 19900;
		// 1,800,000 milliseconds = 30 minutes.
		listeningTimeout = 3600000;
		ack = "acknowledgement";
		keyFileName = "/home/nuplavikar/temp/key_files/key_512.txt";
		//keyFileName = "/home/nuplavikar/temp/key_files/key_1024.txt";
		//keyFileName = "/home/nuplavikar/temp/key_files/key_2048.txt";
	}

	String getKeyFileName()
	{
		return keyFileName;
	}

	String getServerHostName()
	{
		return serverHostName;
	}

	int getServerPortID()
	{
		return serverPortID;
	}

	int getListeningTimeout()
	{
		return listeningTimeout;
	}

	boolean isAck(String str)
	{
		boolean result = false;
		if (str.compareTo(ack)==0)
			result = true;
		else
			result = false;
		return result;
	}

	String getAck()
	{return ack;}
/*
* Sends the string and waits for an acknowledgement from peer.
* @param	str	string to be sent
* @param	obj. input stream
* @param	obj. output stream
* @return	return status
* */
	public int sendString( String str, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream )
	{
		int err=0;
		try
		{
			if ( str.length()>6 )
			{
				//System.out.println("Trying to send " + str.substring(0, 3) + " ... "+str.substring(str.length()-3, str.length())+" as string to server ...");
			}
			else
			{
				//System.out.println("Trying to send " + str + " as string to server ...");
			}
			objectOutputStream.writeObject(str);
			//System.out.println("Sent " + str + " to server! Waiting for acknowledgement ...");
			if (!isAck((String) objectInputStream.readObject()))
			{
				System.out.print("Error! No acknowledgement received!");
				return -3;
			}

			//System.out.println("Acknowledgement received from server regarding string!");
		} catch (IOException e)
		{
			err = -1;
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			err = -2;
			e.printStackTrace();
		}
		return err;
	}

	public int sendFileAsString(String encrQueryVectorFile, int totNumItemsToBeSent, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int count=0;
		File fp = new File(encrQueryVectorFile);
		if(fp.isDirectory())
		{
			Exception e = new Exception("ERROR! Required filename with encrypted vector, name given:"+encrQueryVectorFile);
			e.printStackTrace();
			return 1;
		}
		try
		{
			FileReader fileReader = new FileReader(fp);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;
			while( (( line = bufferedReader.readLine() ) != null) && ( count<totNumItemsToBeSent ) )
			{
				if (sendString(line, objectInputStream, objectOutputStream)!=0 )
				{
					System.err.println("Some issue in sending bigNum at "+(count+1)+"in "+encrQueryVectorFile);
					return -1;
				}
				count++;
			}


		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}


		return 0;
	}

	/*
	* Based on the line numbers in lineNos sends the corresponding line numbers from the file encrQueryVectorFile
	* @param	encrQueryVectorFile	name of file having data to be sent
	* @param	lineNos	list of line numbers indicating the line numbers in the file to be sent
	* @param	obj. input stream
	* @param	obj. output stream
	* */
	public int sendEncrIntermProdAsString(String encrQueryVectorFile, LinkedList<Integer> lineNos, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int count=0;
		File fp = new File(encrQueryVectorFile);
		//System.out.println("Sending the encrypted, randomized intermediate products to client ...");
		if(fp.isDirectory())
		{
			Exception e = new Exception("ERROR! Required filename with encrypted vector, name given:"+encrQueryVectorFile);
			e.printStackTrace();
			return -1;
		}

		if((lineNos==null))
		{
			Exception e = new Exception("ERROR! EFAULT! lineNos: "+lineNos);
			e.printStackTrace();
			return -2;
		}
		if((lineNos.size()==0))
		{
			Exception e = new Exception("ERROR! Nothing to send! lineNos.size(): "+lineNos.size());
			e.printStackTrace();
			return -3;
		}
		try
		{
			FileReader fileReader = new FileReader(fp);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;
			count = 0;
			while( (( line = bufferedReader.readLine() ) != null))
			{
				//Send only line number 2(TFIDF product) and 4(Binary TFIDF product)
				//Note here count 0 == line 1 and so on. We are sending line number 2 and 4.
				//lineNos linked list has been populated by line numbers 2, 4 i.e. line numbers starting from 1 onwards.
				//see sendEncrypedIntermRandProdValues() function.
				if ( lineNos.contains(new Integer(count+1)) )
				{
					if (sendString(line, objectInputStream, objectOutputStream) != 0)
					{
						System.err.println("Some issue in sending "+printBigNumInShort(line)+" at " + (count + 1) + "in " + encrQueryVectorFile);
						return -4;
					}
				}
				count++;
			}
			bufferedReader.close();
			fileReader.close();


		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}


		return 0;
	}

	/*
	* Accept a fixed number of [ encrypted ] strings from client
	* */
	public static int acceptStrFromPeer(ObjectInputStream serObjectInputStream, ObjectOutputStream serObjectOutputStream, int numItems, String storeIntoFileName)
	{
		int err=0;

		/*
			* Open/Close the file to write the encrypted query which would be obtained from client - BEGIN
			* */
		File encrQueryFrmClientFile = new File(storeIntoFileName);
		FileWriter fileWriter = null;
		try
		{
			fileWriter = new FileWriter(encrQueryFrmClientFile);

			for (int i=0; i<numItems; i++)
			{
				/*
				* Get the string - BEGIN
				* */
				//System.out.println((i+1)+"> Reading the number(in string) ...");
				String bigNum = null;

				bigNum = (String) serObjectInputStream.readObject();


				//System.out.println("Received " + printBigNumInShort(bigNum) + " from client!");

				if (!(numItems==1) && (i<numItems-1))
				{
					bigNum = bigNum + '\n';
				}
				fileWriter.write(bigNum);
				fileWriter.flush();
				//System.out.println((i + 1) + "> The bigNum has " + (bigNum.length()-1) + " chars! Sending acknowledgement ...");
				//Send ack
				serObjectOutputStream.writeObject(new ClientServerConnConfigs().getAck());
				//System.out.println((i + 1) + "> Acknowledgement sent for string!\n");
				/*
				* Get the string - END
				* */
			}
			fileWriter.close();
			/*
			* Open/Close the file to write the encrypted query which would be obtained from client - BEGIN
			* */
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		return err;
	}

	/*
	* Accept a fixed number of strings from client. The list of terms is returned.
	* */
	public LinkedList<String> acceptStrListFromPeer(ObjectInputStream serObjectInputStream, ObjectOutputStream serObjectOutputStream, int numItems)
	{
		int err=0;
		LinkedList<String> listOfStrings = new LinkedList<String>();
		try
		{
			for (int i=0; i<numItems; i++)
			{
				/*
				* Get the string - BEGIN
				* */
				//System.out.println((i+1)+"> Reading the number(in string) ...");
				String str = null;

				str = (String) serObjectInputStream.readObject();
				listOfStrings.add(str);
				//System.out.println("Received " + printBigNumInShort(bigNum) + " from client!");

				serObjectOutputStream.writeObject(new ClientServerConnConfigs().getAck());
				//System.out.println((i + 1) + "> Acknowledgement sent for string!\n");
				/*
				* Get the string - END
				* */
			}

		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}

		return listOfStrings;
	}

	public static String printBigNumInShort(String bigNum)
	{
		if ( bigNum.length()>6 )
		{
			return bigNum.substring(0, 3) + " ... "+bigNum.substring(bigNum.length() - 2, bigNum.length()) + " Sz.:("+bigNum.length()+")";
		}
		else
		{
			return bigNum.toString();
		}
	}

	/*
	* Accept a fixed number of [ encrypted ] strings from client
	* Input Parameters:
	* -input stream
	* -output stream
	* -num of items to receive
	* -file that will store the obtained value
	* Output Parameters:
	* -return status
	* */
//	public static int acceptStrFromPeerNoACK(ObjectInputStream serObjectInputStream, ObjectOutputStream serObjectOutputStream, int numItems, String storeIntoFileName)
//	{
//		int err=0;
//
//		/*
//			* Open/Close the file to write the encrypted query which would be obtained from client - BEGIN
//			* */
//		File encrQueryFrmClientFile = new File(storeIntoFileName);
//		FileWriter fileWriter = null;
//		try
//		{
//			fileWriter = new FileWriter(encrQueryFrmClientFile);
//
//			for (int i=0; i<numItems; i++)
//			{
//				/*
//				* Get the string - BEGIN
//				* */
//				System.out.println((i+1)+"> Reading the number(in string) ...");
//				String bigNum = null;
//
//				bigNum = (String) serObjectInputStream.readObject();
//
//
//				System.out.println("Received " + printBigNumInShort(bigNum) + " from client ...");
//
//				if (!(numItems==1) && (i<numItems-1))
//				{
//					bigNum = bigNum + '\n';
//				}
//				fileWriter.write(bigNum);
//				fileWriter.flush();
//				System.out.println((i + 1) + "> The bigNum has " + (bigNum.length()-1) + " chars! NOT Sending acknowledgement now!!");
//				/*
//				* Get the string - END
//				* */
//			}
//			fileWriter.close();
//			/*
//			* Open/Close the file to write the encrypted query which would be obtained from client - BEGIN
//			* */
//		} catch (IOException e)
//		{
//			e.printStackTrace();
//		} catch (ClassNotFoundException e)
//		{
//			e.printStackTrace();
//		}
//
//		return err;
//	}
//
//	public int sendAckOnly(ObjectOutputStream objectOutputStream, String msg)
//	{
//		//Send ack
//		try
//		{
//			objectOutputStream.writeObject(new ClientServerConnConfigs().getAck());
//		} catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		System.out.println("> Acknowledgement sent! "+msg+"\n");
//		return 0;
//	}

	public int sendInteger(int value, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int err=0;
		try
		{
			//System.out.println("Trying to send " + value + " as integer to server ...");
			objectOutputStream.writeObject(value);
			//System.out.println("Sent " + value + " to server! Waiting for acknowledgement ...");
			if (!this.isAck((String) objectInputStream.readObject()))
			{
				System.out.print("Error! No acknowledgement received for the "+value+" integer sent!");
				return -3;
			}

			//System.out.println("Acknowledgement received from server regarding the " + value + " integer sent!");
		} catch (IOException e)
		{
			err = -1;
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			err = -2;
			e.printStackTrace();
		}
		return err;
	}

	public int receiveInteger(ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int value = -1;
		try
		{
			value = (Integer)objectInputStream.readObject();
			//System.out.println("The read integer has value: " + value + " Sending acknowledgement ...");
			/*try
			{
				//Thread.sleep(10000);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}*/

			objectOutputStream.writeObject(this.getAck());
			//System.out.println("Acknowledgement sent for value read:"+ value +" !");
		} catch (IOException e)
		{
			System.out.println("ERROR! IOException!");
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			value = -2;
			System.out.println("ERROR! ClassNotFoundException!");
			e.printStackTrace();
		}
		return value;
	}

	/*
	* This function writes for each of the 'd' documents, 'n' number of items.
	* 'intermFile' tells where to write these 'd' number of documents and
	* 'fileNameStart' indicates what their name should start with.
	*
	*/
	public LinkedList<String> acceptNValuesFromPeerAndWriteToDNumOfFilesEach(int d, int n, String intermFileDir, String fileNameStart, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream)
	{
		int err = 0;
		LinkedList<String> intermFileNamesList = new LinkedList<String>();

		for (int i=0; i<d; i++)
		{
			String intermFileName = intermFileDir+"/"+fileNameStart+"_"+(i+1)+".txt";
			intermFileNamesList.add(intermFileName);
			acceptStrFromPeer(objectInputStream, objectOutputStream, n, intermFileName);
		}
		return intermFileNamesList;
	}
}
