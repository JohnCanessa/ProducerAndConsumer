

package com.canessa.producerconsumer;

// **** import files ****
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


/**
 * 
 */
public class Consumer {

    // **** class members ****


    /**
     * Constructor
     */
    public Consumer() {
    }


    /**
     * Receive from Producer merged file information regarding 
     * the embeded objects.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public MergeDirEntry[] receiveObjectList(DataInputStream dis) throws IOException, ClassNotFoundException {

        // **** ****
        byte[] bytes = new byte[ProducerAndConsumer.IO_BUFFER_SIZE];

        // **** receive the number of objects ****
        int len = dis.read(bytes, 0, 4);

        // ???? ????
        System.out.println("receiveObjectList <<< len: " + len);

        // **** convert bytes to integer ****
        int entryCount =    ((bytes[3] & 0xff) << 24) |
                            ((bytes[2] & 0xff) << 16) |
                            ((bytes[1] & 0xff) << 8) |
                             (bytes[0] & 0xff);

        // ???? ????
        System.out.println("receiveObjectList <<< entryCount: " + entryCount);

        // **** create the array ****
        MergeDirEntry[] arr = new MergeDirEntry[entryCount];

        // **** loop receiving the array of entries  ****
        for (int i = 0; i < entryCount; i++) {

            // **** receive a merged dir entry ****
            len = dis.read(bytes, 0, 160);

            // ???? ????
            System.out.println("receiveObjectList <<< len: " + len);

            // **** ****
            ByteArrayInputStream bis    = new ByteArrayInputStream(bytes);
            ObjectInputStream ois       = new ObjectInputStream(bis);

            // **** ****
            arr[i] = (MergeDirEntry)ois.readObject();
        }

        // **** return array of entries ****
        return arr;
    }


    /**
     * This is the main code for the Consumer.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        
        // **** initialization ****
        String ip           = ProducerAndConsumer.PRODUCER_IP;
        int port            = ProducerAndConsumer.PRODUCER_PORT;
        String inputFile    = ProducerAndConsumer.INPUT_FILE;
        byte[] data         = new byte[ProducerAndConsumer.IO_BUFFER_SIZE];
        String buffer       = "";

        Socket clientSocket = null;

        // **** open a buffered reader ****
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // **** prompt and get the IP ****
        System.out.print("consumer <<< producer ip [" + ip + "]: ");
        buffer = br.readLine().trim();
        if (!buffer.equals(""))
            ip = buffer;

        // **** check if ip is NOT valied ****
        if (!ProducerAndConsumer.isValidIPAddress(ip)) {
            System.out.println("consumer <<< ip ==>" + ip + "<== is NOT valid!!!");
            System.exit(-1);
        }

        // ???? ????
        System.out.println("consumer <<< ip ==>" + ip + "<==");

        // **** prompt and get the port ****
        System.out.print("consumer <<< producer port [" + port + "]: ");
        buffer = br.readLine().trim();
        if (!buffer.equals(""))
            port = Integer.parseInt(buffer);

        // **** check if port NOT valied ****
        if (!ProducerAndConsumer.isValidPortNumber(port)) {
            System.out.println("consumer <<< port: " + port + " is NOT valid!!!");
            System.exit(-1);
        }

        // ???? ????
        System.out.println("consumer <<< port: " + port);

        // **** prompt and get the input file name ****
        System.out.print("consumer <<< producer input file [" + inputFile + "]: ");
        buffer = br.readLine().trim();
        if (!buffer.equals(""))
            inputFile = buffer;

        // ???? ????
        System.out.println("consumer <<< inputFile ==>" + inputFile + "<==");

        // **** close the buffered reader ****
        br.close();

        // **** start timer ****
        long startTime = System.currentTimeMillis();

        // **** open client socket ****
        try {
            clientSocket = new Socket(ip, port);
        } catch (Exception ex) {
            System.err.println("consumer <<< ex: " + ex.toString());
            System.exit(-1);
        }

        // **** to read data from server (producer) socket ****
        DataInputStream dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

        // **** to write data to server (producer) socket ****
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

        // ???? ????
        System.out.println("consumer <<< BEFORE send request...");

        // **** send request to producer (input file name) ****
        try {
            Arrays.fill(data, (byte)0);
            System.arraycopy(inputFile.getBytes(), 0, data, 0, inputFile.length());

            // ???? ????
            String str = new String(data, "UTF-8").trim(); 
            System.out.println("consumer <<< str ==>" + str + "<==");

            dos.write(data, 0, ProducerAndConsumer.IO_BUFFER_SIZE);
        } catch (Exception ex) {
            System.err.println("consumer <<< ex: " + ex.toString());
            System.exit(-1);
        }

        // ???? ????
        System.out.println("consumer <<<  AFTER send request");

        // **** create consumer ****
        Consumer consumer = new Consumer();

        // **** receive information for each object that will follow ****
        MergeDirEntry arr[] = consumer.receiveObjectList(dis);

        // ???? ????
        System.out.println("consumer <<< arr.length: " + arr.length);
        for (int i = 0; i < arr.length; i++)
            System.out.println("consumer <<< arr[" + i + "]: " + arr[i].toString());

            
        // **** open local file to write data ****
        String outputFile       = "c:\\temp\\_received_file";
        OutputStream outStream  = new FileOutputStream(outputFile);
    
        // **** loop receiving data from producer (server) ****
        boolean done        = false;
        long bytesReceived  = 0;
        while (!done) {

            // **** ****
            int len = dis.read(data, 0, ProducerAndConsumer.IO_BUFFER_SIZE);

            // **** check if we are done receiving data from the producer (server) ****
            if (len == -1) {
                done = true;
                continue;
            }

            // **** count these bytes ****
            bytesReceived += (long)len;

            // ???? ????
            // System.out.println("consumer <<< bytesReceived: " + bytesReceived);

            // **** write data to the output file ****
            outStream.write(data, 0, len);
        }

        // **** end timer and compute duration ****
        long endTime    = System.currentTimeMillis();
        long duration   = (endTime - startTime);

        // ???? ????
        System.out.println("producer <<< duration: " + duration + " ms.");

        // ???? ????
        System.out.println("consumer <<< bytesReceived: " + bytesReceived);

        // **** close local file ****
        outStream.close();

        // **** close the data input stream ****
        dis.close();

        // **** close the data output stream ****
        dos.close();

        // **** close client socket ****
        clientSocket.close();
    }
}
