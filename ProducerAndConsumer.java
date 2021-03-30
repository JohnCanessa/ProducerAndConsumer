package com.canessa.producerconsumer;

// **** import files ****
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * MergeDirEntry class to keep track of GUID, 
 * offset and length of objects.
 */
class MergeDirEntry implements Serializable {


    // **** ****
    private static final long serialVersionUID = 7326474381142349468L;


    // **** ****
    public String guid;
    public long offset;
    public long length;


    /**
     * Constructor
     */
    public MergeDirEntry(String guid, long offset, long length) {
        this.guid   = guid;
        this.offset = offset;
        this.length = length;
    }

    
    /**
     * String representation
     */
    @Override
    public String toString() {
        return guid + " " + "(" + offset + ", " + length + ")"; 
    }
}


/**
 * 
 */
public class ProducerAndConsumer {


    // **** constants ****
    public static final String INPUT_FILE  = "E:\\DICOM\\005056891b354c11ed076009e73fd878";
    public static final int PRODUCER_PORT  = 51515;
    public static final String PRODUCER_IP = "0.0.0.0";
    public static final int IO_BUFFER_SIZE = (1024 * 1024);


    /**
     * Check if TCP/IP IP is valid.
     */
    public static boolean isValidIPAddress(String ip) {
    
        // **** sanity checks ****
        if (ip == null || ip.length() == 0)
            return false;

        // **** regex for digit from 0 to 255 ****
        String zeroTo255 = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";

        // **** regex for a digit from 0 to 255 followed by a dot and repeat 4 times ****
        String regex = zeroTo255 + "\\."
                        + zeroTo255 + "\\."
                        + zeroTo255 + "\\."
                        + zeroTo255;

        // **** compile the regex ****
        Pattern p = Pattern.compile(regex);

        // **** match the pattern ****
        Matcher m = p.matcher(ip);

        // **** return true if the ip is valid ****
        return m.matches();
    }


    /**
     * Check if TCP/IP port is valid.
     * 
     * Ports 0 through 1023 are defined as well-known ports.
     * Registered ports are from 1024 to 49151. 
     * The remainder of the ports from 49152 to 65535 can be 
     * used dynamically by applications.
     */
    public static boolean isValidPortNumber(int port) {
        if (port < 49152)
            return false;
        else 
            return true;
    }


    /**
     * Test scaffold.
     * 
     * Starts the producer and consumer programs in this computer.
     * It starts the programs with the specified arguments.
     * 
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // **** display a prompt ****
        System.out.print("main >>> input file [" + INPUT_FILE + "]: ");

        // **** open a buffered reader ****
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // **** read name of file to be processed by producer ****
        String inputFile = br.readLine().trim();

        // **** close the buffered reader ****
        br.close();

        // **** check if we should use the default input file ****
        if (inputFile.equals("")) {
            inputFile = INPUT_FILE;
        }

        // ???? ????
        System.out.println("main <<< inputFile ==>" + inputFile + "<==");

        
        // **** start producer ****
        Process prod = Runtime.getRuntime().exec("java -cp C:\\Users\\johnc\\workspace8\\ProducerAndConsumer\\ProducerAndConsumer.jar; com.canessa.producerconsumer.Producer");


        // **** start consumer ****
        Process cons = Runtime.getRuntime().exec("java -cp C:\\Users\\johnc\\workspace8\\ProducerAndConsumer\\ProducerAndConsumer.jar; com.canessa.producerconsumer.Consumer");


    }

}
