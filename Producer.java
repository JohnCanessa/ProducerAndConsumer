package com.canessa.producerconsumer;

// **** import files ****
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * 
 */
public class Producer {

    // **** class members ****
    public int      port;
    public String   ip;

    /**
     * Constructor
     */
    public Producer(String ip, int port) {
        this.ip     = ip;
        this.port   = port;
    }

    /**
     * Software version
     */
    public String version() {
        return "1.0.00";
    }

    /**
     * Read from the merged file information regarding the embeded objects.
     * 
     * @throws IOException
     */
    public MergeDirEntry[] readMergedFileInfo(InputStream inStream) throws IOException {

        // **** initialization ****
        byte[] bytes = new byte[64];

        // **** skip signature (8 bytes) ****
        long skip = 8;
        long skipped = inStream.skip(skip);

        // ???? ????
        System.out.println("readMergedFileInfo <<< skip: " + skip + " skipped: " + skipped);

        // **** read bitfile count ****
        int len = inStream.read(bytes, 0, 4);

        // **** convert bytes to integer ****
        int entryCount =    ((bytes[3] & 0xff) << 24) |
                            ((bytes[2] & 0xff) << 16) |
                            ((bytes[1] & 0xff) << 8) |
                             (bytes[0] & 0xff);

        // ???? ????
        System.out.println("readMergedFileInfo <<< entryCount: " + entryCount);

        // **** allocate array ****
        MergeDirEntry[] arr = new MergeDirEntry[entryCount];

        // **** loop reading info populating the array ****
        for (int i = 0; i < entryCount; i++) {

            // **** read GUID ****
            len = inStream.read(bytes, 0, 40);

            // **** get the GUID ****
            String guid = new String(bytes, "UTF-8").trim(); 

            // ???? ????
            System.out.println("readMergedFileInfo <<< len: " + len + " guid ==>" + guid + "<==");

            // **** read offset ****
            len = inStream.read(bytes, 0, 8);

            // **** convert buffer to long ****
            long offset =   ((bytes[7] & 0xff) << 56) |
                            ((bytes[6] & 0xff) << 48) |
                            ((bytes[5] & 0xff) << 40) |
                            ((bytes[4] & 0xff) << 32) |
                            ((bytes[3] & 0xff) << 24) |
                            ((bytes[2] & 0xff) << 16) |
                            ((bytes[1] & 0xff) << 8) |
                             (bytes[0] & 0xff);

            // **** take into account the CAS_MERGE_BITFILE data structure ****
            offset += 512;

            // ???? ????
            System.out.println("readMergedFileInfo <<< len: " + len + " offset: " + offset);

            // **** read length ****
            len = inStream.read(bytes, 0, 8);

            // **** convert buffer to long ****
            long length =   ((bytes[7] & 0xff) << 56) |
                            ((bytes[6] & 0xff) << 48) |
                            ((bytes[5] & 0xff) << 40) |
                            ((bytes[4] & 0xff) << 32) |
                            ((bytes[3] & 0xff) << 24) |
                            ((bytes[2] & 0xff) << 16) |
                            ((bytes[1] & 0xff) << 8) |
                             (bytes[0] & 0xff);


            // ???? ????
            System.out.println("readMergedFileInfo <<< len: " + len + " length: " + length);

            // **** ****
            arr[i] = new MergeDirEntry(guid, offset, length);

            // ???? ????
            System.out.println("readMergedFileInfo <<< arr[" + i + "]: " + arr[i].toString());
        }

        // **** return the array ****
        return arr;
    }

    
    /**
     * Send to Consumer a list of object names, offsets and lengths.
     * 
     * @throws IOException
     */
    public void sendObjectList(DataOutputStream dos, MergeDirEntry[] arr) throws IOException {

        // ***** initialization ****
        int entryCount  = arr.length;
        byte[] data     = new byte[ProducerAndConsumer.IO_BUFFER_SIZE];

        // ???? ????
        System.out.println("sendObjectList <<< entryCount: " + entryCount);

        // **** ****
        data[0] = (byte)(entryCount & 0xff);
        data[1] = (byte)((entryCount << 8) & 0xff);
        data[2] = (byte)((entryCount << 16) & 0xff);
        data[3] = (byte)((entryCount << 24) & 0xff);

        // **** send the number of merge dir entries to the Consumer ****
        dos.write(data, 0, 4);

        // **** loop sending the merge dir entries to the Consumer ****
        for (int i = 0; i < entryCount; i++) {

            // **** ****
            ByteArrayOutputStream bos   = new ByteArrayOutputStream();
            ObjectOutputStream oos      = new ObjectOutputStream(bos);

            // **** ****
            oos.writeObject(arr[i]);
            oos.flush();
            data = bos.toByteArray();

            // ???? ????
            System.out.println("sendObjectList <<< data.length: " + data.length);
            
            // **** send the entry to the Consumer ****
            dos.write(data, 0, data.length);
        }
    }


    /**
     * This is the main code for the Producer.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        
        // **** initialization ****
        String ip                   = ProducerAndConsumer.PRODUCER_IP;
        int port                    = ProducerAndConsumer.PRODUCER_PORT;
        byte[] data                 = new byte[ProducerAndConsumer.IO_BUFFER_SIZE];
        String buffer               = "";

        ServerSocket serverSocket   = null;
        Socket clientSocket         = null;
    
        // **** open a buffered reader ****
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // **** prompt and get the IP ****
        System.out.print("producer <<< ip [" + ip + "]: ");
        buffer = br.readLine().trim();
        if (!buffer.equals(""))
            ip = buffer;

        // **** check if ip is NOT valied ****
        if (!ProducerAndConsumer.isValidIPAddress(ip)) {
            System.out.println("producer <<< ip ==>" + ip + "<== is NOT valid!!!");
            System.exit(-1);
        }

        // ???? ????
        System.out.println("producer <<< ip ==>" + ip + "<==");

        // **** prompt and get the port ****
        System.out.print("producer <<< producer port [" + port + "]: ");
        buffer = br.readLine().trim();
        if (!buffer.equals(""))
            port = Integer.parseInt(buffer);

        // **** check if port NOT valied ****
        if (!ProducerAndConsumer.isValidPortNumber(port)) {
            System.out.println("producer <<< port: " + port + " is NOT valid!!!");
            System.exit(-1);
        }

        // ???? ????
        System.out.println("producer <<< port: " + port);

        // **** close the buffered reader ****
        br.close();

        // **** create a producer ****
        Producer producer = new Producer(ip, port);

        // **** get and display producer version ****
        System.out.println("producer <<< version: " + producer.version());

        // **** open server socket and accept client (consumer) connection ****
        try {

            // **** server socket ****
            serverSocket    = new ServerSocket(port);

            // ???? ????
            System.out.println("producer <<< waiting for client connection...");

            // **** accept client (consumer) connection ****
            clientSocket    = serverSocket.accept();

            // ???? ????
            System.out.println("producer <<< received client connection");
        } catch (Exception ex) {
            System.err.println("producer <<< ex: " + ex);
            System.exit(-1);
        }

        // **** start timer ****
        long startTime = System.currentTimeMillis();

        // **** to read from client (consumer) socket ****
        DataInputStream dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));

        // **** to write to client (consumer) socket ****
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

        // ???? ????
        System.out.println("producer <<< BEFORE reading request...");

        // **** read consumer (client) request ****
        int dataLen = dis.read(data, 0, data.length);

        // ???? ????
        System.out.println("producer <<<  AFTER reading request dataLen: " + dataLen);

        // **** get the input file name ****
        String inputFile = new String(data, "UTF-8").trim(); 

        // ???? ????
        System.out.println("producer <<< inputFile ==>" + inputFile + "<==");

        // **** check if input file does NOT exist ****
        File f = new File(inputFile);
        if (!f.exists() || f.isDirectory()) { 
            System.err.println("producer <<< inputFile ==>" + inputFile + "<== NOT available!!!");
            System.exit(-1);
        }

        // **** get input file size ****
        Path path = Paths.get(inputFile);
        long fileSize = Files.size(path);
        if (fileSize <= 0) {
            System.err.println("producer <<< unexpected fileSize: " + fileSize);
            System.exit(-1);
        }

        // ???? ????
        System.out.println("producer <<< fileSize: " + fileSize);

        // **** open input file ****
        InputStream inStream = new FileInputStream(inputFile);

        // **** extract information for each object that will follow ****
        MergeDirEntry[] arr =  producer.readMergedFileInfo(inStream);

        // **** close input stream ****
        inStream.close();

        // **** open input stream ****
        inStream = new FileInputStream(inputFile);

        // **** send to Consumer information for each object that will follow ****
        producer.sendObjectList(dos, arr);

        // **** for ease of use ****
        int entryCount = arr.length;

        // ???? ????
        System.out.println("producer <<< entryCount: " + entryCount);

        // **** set the file position ****
        long filePos = 0;

        // **** loop sending file objects to Consumer ****
        for (int i = 0; i < entryCount; i++) {

            // **** for ease of use ****
            String guid = arr[i].guid;
            long length = arr[i].length;

            // ???? ????
            System.out.println("producer <<< length: " + length);

            // **** number of bytes to skip ****
            long skip = 0;
            if (i == 0) {
                skip = arr[i].offset;
            } else {
                skip = arr[i].offset - (arr[i - 1].offset + arr[i - 1].length);
            }

            // **** get to the specified offset ****
            long skipped = inStream.skip(skip);

            // ???? ????
            System.out.println("producer <<< skip: " + skip + " skipped: " + skipped);

            // **** loop reading and sending data to the Consumer (client) ****
            int bytesToRead     = 0;
            long bytesSent      = 0;
            while (bytesSent < length) {

                // **** determine number of bytes to read ****
                if (length - bytesSent >= ProducerAndConsumer.IO_BUFFER_SIZE)
                    bytesToRead = ProducerAndConsumer.IO_BUFFER_SIZE;
                else
                    bytesToRead = (int)(length - bytesSent);

                // ???? ????
                // System.out.println("producer <<< bytesToRead: " + bytesToRead);

                // **** read data from the local file ****
                int len = inStream.read(data, 0, bytesToRead);

                // ???? ????
                // System.out.println("producer <<<         len: " + len);

                // **** send data to consumer (client) ****
                dos.write(data, 0, len);

                // **** update the number of bytes sent to consumer (client) ****
                bytesSent += len;

                // ???? ????
                // System.out.println("producer <<<   bytesSent: " + bytesSent);
            }

            // ???? ????
            System.out.println( "producer <<< guid ==>" + guid + "<== bytesSent: " + bytesSent + " length: " + length);

            // **** update the file position ****
            filePos += (skip + bytesSent);

            // ???? ????
            System.out.println("producer <<< filePos: " + filePos);
        }

        // ???? ????
        System.out.println("producer <<< diff: " + (fileSize - filePos));

        // **** end timer and compute duration ****
        long endTime    = System.currentTimeMillis();
        long duration   = (endTime - startTime);

        // ???? ????
        System.out.println("producer <<< duration: " + duration + " ms.");

        // **** close input stream ****
        inStream.close();

        // **** close the data input stream ****
        dis.close();

        // **** close the data output stream ****
        dos.close();

        // **** close client socket ****
        clientSocket.close();

        // **** close server socket ****
        serverSocket.close();
    }
}
