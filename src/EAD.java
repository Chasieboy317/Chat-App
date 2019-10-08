// this class is used to create an encrypted/decrypted byte array
// which is the cipher text
// and then read it back in its decoded 
// plain text
// EncryptionAndDecryption
// it will use ASE - advanced encryption standard which uses is a symmetric encryption algorithm
// (it will use the same key to encrypt and decrypt)

import java.io.*;
import java.lang.*;
import java.util.*;
import java.security.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

public class EAD{

   private static final String encryptionAl = "AES";
   private static final String encryptionKey = "exn11mep5d3A4kb4";
   private static byte[] encryptionKeyBytes;

   EAD(){
      encryptionKeyBytes = encryptionKey.getBytes();
   }
   // reads an encoded file of user names and passwords
   // and creates and returns the information stored
   // into a hash table
   public HashMap readEncryptedFile(){
      HashMap<String, String> userAndPass = new HashMap<>();
      try {
         //opens file to read from
         File file = new File("users.enc");
         if ((int)file.length()>0) {
            FileInputStream inFile = new FileInputStream(file);
            //reads in the file into a byte array
            byte[] inputBytes = new byte[(int) file.length()];
            inFile.read(inputBytes);
            //decrypts the byte array
            byte[] decryptedBytes = encryptOrDecrypt(inputBytes, Cipher.DECRYPT_MODE);
            int c = 0;
            // removes unnecessary characters created during encoding/decoding
            for (byte b : decryptedBytes) {
               if ((b <= 122 && b >= 48) || b == 32 || b == 44) {
                  decryptedBytes[c] = b;
                  c++;
               }
            }
            byte[] formattedBytes = new byte[c];
            formattedBytes = Arrays.copyOf(decryptedBytes, c);
            // forms the final formatted string and array of usernames + passwords
            String finalString = new String(formattedBytes);
            String[] stringArr = finalString.split(" ");
            //loops through every user + pass pair in the array
            // and adds it to the hash table
            for (int i = 0; i < stringArr.length; i += 2) {
               userAndPass.put(stringArr[i], stringArr[i + 1]);
            }
         }

      }
      catch (Exception e){
         System.out.println(e);
      }

      //loop through created users and passes and input into table
      return userAndPass;
   }


   // takes in the user name and password in the format
   // "username password" and returns the encrypted byte array
   public static byte[] encryptOrDecrypt(byte[] usernameAndPassword, int encryptOrDecrypt){
      try{
         // gets the cipher from AES
         Cipher cipher = Cipher.getInstance(encryptionAl);
         //creates a key based on the key defined as encryption key and the specified
         //encryption algorithm which is AES
         Key key = new SecretKeySpec(encryptionKeyBytes, encryptionAl);
         // sets cipher mode to either decrypt or encrypt, passes in the key to encrypt/decrypt
         cipher.init(encryptOrDecrypt, key);
         // performs the encryption or decryption based on ENCRYPT/DECRYPT mode
         byte[] encryptedUserAndPass = cipher.doFinal(usernameAndPassword);

         return encryptedUserAndPass;
      }
      catch (Exception e){
         System.out.println(e);
      }
      return null;
   }


   // prints the encrypted username and password to "users.enc" as
   // an encrypted byte array
   // which is used to store usernames and passwords

   public static void writeToFileEncrypted(String usernameAndPassword){

      try {
         FileOutputStream fOut = new FileOutputStream(new File("users.enc"),true);
         usernameAndPassword += " ";
         // write encrypted byte array to users.enc
         fOut.write(encryptOrDecrypt(usernameAndPassword.getBytes(), Cipher.ENCRYPT_MODE));
         fOut.close();
      }
      catch (Exception e){
         System.out.println(e);
      }

   }
   // main method was used for testing input/output
   public static void main(String[] args){
      EAD ead = new EAD();

      ead.writeToFileEncrypted("iedek 123pdA2");
      HashMap<String,String> users =ead.readEncryptedFile();



   }


}