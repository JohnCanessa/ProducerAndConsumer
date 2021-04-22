package com.canessa.producerconsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * American Encryption Standard (AES)
 */
public class AES {
    

    // ***** class private variables ****
    private static final String SALT        = "Stress is the salt of life";
    private static final String PASSWORD    = "Server Password!Server Password!";
    //                                         01234567890123456789012345678901


    /**
     * The secret key should be generated from a 
     * Cryptographically Secure (Pseudo-)Random Number Generator 
     * like the SecureRandom class.
     * 
     * @throws NoSuchAlgorithmException
     */
    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {

        // **** sanity check(s) ****
        switch (n) {
            case 128:
            case 192:
            case 256:

                // **** valid values ****

            break;

            default:
                System.err.println("main <<< UNEXPECTED n: " + n);
                throw new InvalidParameterException();
            // break;
        }

        // **** ****
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }


    /**
     * The AES secret key can be derived from a given password 
     * using a password-based key derivation function like PBKDF2.
     * 
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static SecretKey getKeyFromPassword( String password, 
                                                String salt) 
                    throws NoSuchAlgorithmException, InvalidKeySpecException {

        // **** ****
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        // **** ****
        KeySpec spec = new PBEKeySpec(  password.toCharArray(), 
                                        salt.getBytes(),
                                        100000,
                                        256);

        // **** ****
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        // **** return secret key ****
        return secret;
    }


    /**
     * Method for generating an Initialization Vector (IV).
     */
    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }


    /**
     * Encrypt the specified raw text string.
     */
    public static String encrypt(   String algorithm, 
                                    String rawText, 
                                    SecretKey key, 
                                    IvParameterSpec iv)
        throws NoSuchPaddingException, NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {
    
        // **** initialize cipher ****
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        // **** ****
        byte[] cipherText = cipher.doFinal(rawText.getBytes());

        // **** return th estring representation ****
        return Base64.getEncoder().encodeToString(cipherText);
    }


    /**
     * Decrypt the specified cipher text string.
     */
    public static String decrypt(   String algorithm, 
                                    String cipherText,
                                    SecretKey key,
                                    IvParameterSpec iv) 
        throws NoSuchPaddingException, NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {
    
        // **** initialize cipher ****
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        // **** ****
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));

        // **** return the string representation ****
        return new String(plainText);
    }


    /**
     * Encrypt the specified raw file.
     */
    public static void encryptFile( String algorithm, 
                                    SecretKey key,
                                    IvParameterSpec iv,
                                    File inputFile,
                                    File outputFile) 
        throws IOException, NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {
        
        // **** initialize cipher ****
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        // **** open the input and output streams ****
        FileInputStream inputStream = new FileInputStream(inputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        // **** process the file one 64-byte block at a time ****
        byte[] buffer = new byte[64];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] output = cipher.update(buffer, 0, bytesRead);
            if (output != null) {
                outputStream.write(output);
            }
        }

        // **** ****
        byte[] outputBytes = cipher.doFinal();

        // ???? ????
        System.out.println("encryptFile <<< outputBytes.length: " + outputBytes.length);

        if (outputBytes != null) {
            outputStream.write(outputBytes);
        }

        // **** close the input and output streams ****
        inputStream.close();
        outputStream.close();
    }


    /**
     * Decrypt the specified encrypted file.
     */
    public static void decryptFile( String algorithm, 
                                    SecretKey key,
                                    IvParameterSpec iv,
                                    File inputFile,
                                    File outputFile) 
        throws IOException, NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {
        
        // **** initialize cipher ****
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        // **** open input and output streams ****
        FileInputStream inputStream = new FileInputStream(inputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        // **** ****
        byte[] buffer = new byte[64];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] output = cipher.update(buffer, 0, bytesRead);
            if (output != null) {
                outputStream.write(output);
            }
        }

        // **** ****
        byte[] outputBytes = cipher.doFinal();

        // ???? ????
        System.out.println("decryptFile <<< outputBytes.length: " + outputBytes.length);

        if (outputBytes != null) {
            outputStream.write(outputBytes);
        }

        // **** close input and output streams ****
        inputStream.close();
        outputStream.close();
    }


    /**
     * Check if the contents of the two specified files are equal.
     */
    private static boolean isEqual(Path firstFile, Path secondFile)
    {
        try {
            if (Files.size(firstFile) != Files.size(secondFile)) {
                return false;
            }
 
            byte[] first = Files.readAllBytes(firstFile);
            byte[] second = Files.readAllBytes(secondFile);
            return Arrays.equals(first, second);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Encrypt data.
     */
    private static String encryptData(String rawData) throws Exception, InvalidKeyException {

        // **** ****
        SecretKeySpec skeySpec = new SecretKeySpec(PASSWORD.getBytes(), "AES");

        // **** ****
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

        // // ???? ????
        // System.out.println("encryptData <<< Base64 encoded: " + Base64.getEncoder().encode(rawData.getBytes()).length);

        // **** ****
        byte[] original = Base64.getEncoder().encode(cipher.doFinal(rawData.getBytes()));
        // byte[] original = cipher.doFinal(rawData.getBytes());

        // ???? ????
        // System.out.println("encryptData <<< original.length: " + original.length);

        // **** return cipher data ****
        return new String(original);
    }


    /**
     * Decrypt data.
     */
    private static String decryptData(String cipherData)
        throws NoSuchAlgorithmException, NoSuchPaddingException,
        InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        // **** ****
        SecretKeySpec skeySpec = new SecretKeySpec(PASSWORD.getBytes(), "AES");

        // **** ****
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);

        // // ???? ????
        // System.out.println("decryptData <<< Base64 decoded: " + Base64.getDecoder().decode(cipherData.getBytes()).length);

        // **** ****
        byte[] original = cipher.doFinal(Base64.getDecoder().decode(cipherData.getBytes()));
        // byte[] original = cipher.doFinal(cipherData.getBytes());

        // ???? ????
        // System.out.println("decryptData <<< original.length: " + original.length);

        // **** return raw data ****
        return new String(original).trim();
    }


    /**
     * Test scaffold for this class.
     * 
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // **** initialization ****
        int n = 256;
        
        // ???? ????
        System.out.println("main <<< PASSWORD ==>" + PASSWORD + "<== bits: " + PASSWORD.length() * 8);
        System.out.println("main <<< SALT ==>" + SALT + "<== bits: " + SALT.length() * 8);


        // **** experiment with raw text ... ****
        String rawText = "Bond, James Bond 007";
        //                01234567890123456789012345678901

        // **** generate a secret key from the password and salt ****
        SecretKey secretKey = getKeyFromPassword(PASSWORD, SALT);

        // ???? ????
        System.out.println("main <<< secretKey: " + secretKey.toString());

        // **** generate an iv ****
        IvParameterSpec iv = generateIv();

        // ???? ????
        System.out.println("main <<< iv: " + Arrays.toString(iv.getIV()));

        // **** encrypt the raw text string ****
        String algorithm = "AES/CBC/PKCS5Padding";
        String cipherText = encrypt(algorithm, rawText, secretKey, iv);

        // **** decrypt the cipher text string ****
        String output = decrypt(algorithm, cipherText, secretKey, iv);

        // ???? display strings of interest ????
        System.out.println("main <<< rawText ==>" + rawText + "<== length: " + rawText.length());
        System.out.println("main <<< cypherText ==>" + cipherText + "<== length: " + cipherText.length());
        System.out.println("main <<< output ==>" + output + "<== length: " + output.length());

        // **** check we get the raw text back ****
        if (!rawText.equals(output)) {
            System.err.println("main <<< rawText != output !!!");
            throw new Exception("EXCEPTION output ==>" + output + "<== != rawText ==>" + rawText + "<==");
        }


        // ???? ... now experiment with a file ????
        System.out.println("\nmain <<< encrypting and decrypting a file");

        // **** ****
        SecretKey key = generateKey(n);
        algorithm = "AES/CBC/PKCS5Padding";
        IvParameterSpec ivParameterSpec = generateIv();

        // **** ****
        File inputFile = new File("C:\\Temp\\alice_in_wonderland.txt");
        File encryptedFile = new File("C:\\Temp\\alice_in_wonderland_encrypted.txt");
        File decryptedFile = new File("C:\\Temp\\alice_in_wonderland_decrypted.txt");

        // ???? ????
        System.out.println("main <<< inputFile ==>" + inputFile.getAbsolutePath() + "<==");
        System.out.println("main <<< encryptedFile ==>" + encryptedFile.getAbsolutePath() + "<==");
        System.out.println("main <<< decryptedFile ==>" + decryptedFile.getAbsolutePath() + "<==");

        // ???? display the size of the input file ????
        System.out.println("main <<< inputFile.length: " + inputFile.length());

        // **** encrypt the file ****
        encryptFile(algorithm, key, ivParameterSpec, inputFile, encryptedFile);

        // ???? display the size of the input file ????
        System.out.println("main <<< encryptedFile.length: " + encryptedFile.length());

        // **** decrypt the file ****
        decryptFile(algorithm, key, ivParameterSpec, encryptedFile, decryptedFile);

        // ***** check if the contents of these files are NOT equal ****
        Boolean equal = isEqual(inputFile.toPath(), decryptedFile.toPath());
        if (!equal) {
            System.err.println("main <<< UNEXPECTED equal: " + equal);
            throw new Exception("EXCEPTION contents equal: " + equal + " inputFile ==>" + inputFile.getAbsolutePath() + 
                "<== decryptedFile ==>" + decryptedFile.getAbsolutePath() + "<==");
        }
        System.out.println("main <<< contents of inputFile == decryptedFile");


        // ???? using ECB mode ????
        System.out.println("\nmain <<< using ECB mode");

        // ???? ????
        System.out.println("main <<< rawText ==>" + rawText + "<== length: " + rawText.length());

        // **** encrypt raw text using ECB mode  ****
        cipherText = encryptData(rawText);

        // ???? ????
        System.out.println("main <<< cipherText ==>" + cipherText + "<== length: " + cipherText.length());
        // System.out.println("main <<< cipherText.length: " + cipherText.length());

        // **** decrypt cipher text using ECB mode ****
        output = decryptData(cipherText);

        // ???? ????
        System.out.println("main <<< output ==>" + output + "<== length: " + output.length());
    }

}
