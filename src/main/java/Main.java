/**
 * Main.java
 * @author Griffin Ryan (glryan@uw.edu)
 */

import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

public class Main {

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Would you like to compute a (H)ash, (M)AC, (E)ncryption or (D)ecryption?");
        String action = scanner.nextLine().trim().toUpperCase();

        if (!action.equals("E") && !action.equals("D") && !action.equals("H") && !action.equals("M")) {
            System.out.println("Invalid option. Please enter 'E' for encrypt or 'D' for decrypt.");
            return;
        }
        
        // Ask the user for the input type
        System.out.println("Enter 'text' for text input or 'file' for file input:");
        String inputType = scanner.nextLine();
        
        byte[] inputBytes = null;
        String inputText = "";
        String filePath = "";
        byte[] fileContent = null;
        String kmacCustomString = "";
        byte[] diverseContents = null;
        String toConvString = "";

        if ("text".equalsIgnoreCase(inputType)) {
            System.out.println("Enter the text:");
            inputText = scanner.nextLine();
            inputBytes = inputText.getBytes();
        } else if ("file".equalsIgnoreCase(inputType)) {
            System.out.println("Enter the file path:");
            filePath = scanner.nextLine();
            try {
                inputBytes = Files.readAllBytes(Paths.get(filePath));
            } catch (Exception e) {
                System.err.println("Could not read the file.");
                return;
            }
        } else {
            System.err.println("Invalid input type.");
            scanner.close();
            return;
        }
        
        Hash hash = new Hash();
        byte[] output = null;

        // ******** Hash, MAC, Encrypt, or Decrypt ********
        if(action.equals("H")){

            // Ask the user for the hash function to use
            System.out.println("Choose the hash function (SHA256, cSHAKE256):");
            String hashFunction = scanner.nextLine();

            switch (hashFunction.toUpperCase()) {
                case "SHA256":
                    output = hash.computeSHA256(inputBytes);
                    break;
                case "CSHAKE256":

                    // cSHAKE256
                    System.out.println("Enter the customization string (leave empty if none):");
                    String customString = scanner.nextLine();
                    byte[] customBytes = customString.getBytes();

                    // Empty N string for cSHAKE256
                    output = hash.cSHAKE256(inputBytes, 512, new byte[0], customBytes);
                    break;
                default:
                    System.err.println("Invalid hash function.");
                    scanner.close();
                    return;
            }
        
            // Print the result in hexadecimal format
            if (output != null) {
                System.out.println("Hashed output: " + bytesToHex(output));
            }

        } else if(action.equals("M")) {

            // Compute an authentication tag (MAC).
            System.out.println("Generating a MAC using KMACXOF256...");
            System.out.println("Enter the key (leave empty if none):");
            String keyString = scanner.nextLine();
            byte[] keyBytes = keyString.getBytes();

            System.out.println("Enter the customization string (leave empty if none):");
            kmacCustomString = scanner.nextLine();
            byte[] kmacCustomBytes = kmacCustomString.getBytes();

            output = hash.KMACXOF256(keyBytes, inputBytes, 512, kmacCustomBytes);

        } else if(action.equals("E")) {
            System.out.println("Enter the passphrase:");
            String passphrase = scanner.nextLine();

            System.out.println("Enter the customization string (leave empty if none):");
            kmacCustomString = scanner.nextLine();
            byte[] kmacCustomBytes = kmacCustomString.getBytes();
            // Save the s.bin to grab later for decryption:
            try {
                FileOutputStream fos = new FileOutputStream("s.bin");
                fos.write(kmacCustomBytes);
                fos.close();
            } catch (IOException e) {
                System.err.println("Failed to save s.bin");
                // Handle the exception as needed
            }
            
            // Handle files vs. text input.
            if(inputType.equals("file")){
                File file = new File(filePath);
                fileContent = Files.readAllBytes(file.toPath());
                
            } else {
                String toByte = inputText;
                fileContent = toByte.getBytes();
            }
            
            // Encrypt a byte array m symmetrically under passphrase pw
            byte[] z = generateRandomBytes(512);  // You need to implement a method to generate random bytes
            byte[] concatenated = new byte[z.length + passphrase.getBytes().length];
            System.arraycopy(z, 0, concatenated, 0, z.length);
            System.arraycopy(passphrase.getBytes(), 0, concatenated, z.length, passphrase.getBytes().length);
            
            byte[] diversificationStringS = "S".getBytes(); // Convert the string to a byte array
            byte[] keka = hash.KMACXOF256(concatenated, new byte[0], 1024, diversificationStringS);

            byte[] ke = Arrays.copyOfRange(keka, 0, keka.length / 2);
            byte[] ka = Arrays.copyOfRange(keka, keka.length / 2, keka.length);

            // Now you need to obtain the byte array 'm' that you want to encrypt
            byte[] c = hash.KMACXOF256(ke, new byte[0], fileContent.length * 8, kmacCustomBytes);
            byte[] tPrime = hash.KMACXOF256(ka, fileContent, 512, kmacCustomBytes);

            // Save 'z' and 'c' to files
            Files.write(Paths.get("z.bin"), z);
            Files.write(Paths.get("c.bin"), c);
            Files.write(Paths.get("tPrime.bin"), tPrime);

            // The symmetric cryptogram is (z, c, t)
            // Now handle the symmetric cryptogram as you wish.
            // Write 'tPrime' to a binary file
            try {
                Files.write(Paths.get("tPrime.bin"), tPrime);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if(action.equals("D")) {
            // Grab diverse string
            
            // Load 'z', 'c', and 'tPrime' from files
            byte[] z = Files.readAllBytes(Paths.get("z.bin"));
            byte[] c = Files.readAllBytes(Paths.get("c.bin"));
            byte[] tPrime = Files.readAllBytes(Paths.get("tPrime.bin"));

            // Prompt the user for the passphrase
            System.out.println("Enter the passphrase:");
            String passphrase = scanner.nextLine();

            // Derive 'ke' and 'ka' as we did in the "E" case
            byte[] concatenated = new byte[z.length + passphrase.getBytes().length];
            System.arraycopy(z, 0, concatenated, 0, z.length);
            System.arraycopy(passphrase.getBytes(), 0, concatenated, z.length, passphrase.getBytes().length);

            // Grab the s.bin for the diversification string
            try {
                Path sFilePath = Paths.get("s.bin");
                toConvString = Files.readString(sFilePath);
                diverseContents = Files.readAllBytes(sFilePath);
            } catch (IOException e) {
                System.err.println("Failed to read s.bin");
                // Handle the exception as needed
            }

            byte[] keka = hash.KMACXOF256(concatenated, new byte[0], 1024, diverseContents);
            byte[] ke = Arrays.copyOfRange(keka, 0, keka.length / 2);
            byte[] ka = Arrays.copyOfRange(keka, keka.length / 2, keka.length);

            // Decrypt 'c' to obtain 'm'
            byte[] diversificationStringSKE = toConvString.getBytes();
            byte[] m = hash.KMACXOF256(ke, new byte[0], c.length * 8, diversificationStringSKE);

            // Calculate 't' for authentication
            byte[] diversificationStringSKA = toConvString.getBytes();
            byte[] t = hash.KMACXOF256(ka, m, 512, diversificationStringSKA);

            // Compare 't' with 'tPrime' to authenticate the message
            if (Arrays.equals(t, tPrime)) {
                // Authentication successful, save the decrypted data as 'decrypted_file.bin'
                Files.write(Paths.get("decrypted_file.bin"), m);
                System.out.println("Decryption successful. The decrypted data has been saved as 'decrypted_file.bin'.");
            } else {
                System.err.println("Authentication failed. The data may be tampered with.");
            }

        }

        scanner.close();
    }

}
