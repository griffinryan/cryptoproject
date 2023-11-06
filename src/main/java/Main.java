/**
 * Main.java
 * @author Griffin Ryan (glryan@uw.edu)
 */

import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Would you like to compute a (H)ash, (M)AC, (E)ncryption or (D)ecryption?");
        String action = scanner.nextLine().trim().toUpperCase();

        if (!action.equals("E") && !action.equals("D")) {
            System.out.println("Invalid option. Please enter 'E' for encrypt or 'D' for decrypt.");
            return;
        }
        
        // Ask the user for the input type
        System.out.println("Enter 'text' for text input or 'file' for file input:");
        String inputType = scanner.nextLine();
        
        byte[] inputBytes = null;
        if ("text".equalsIgnoreCase(inputType)) {
            System.out.println("Enter the text:");
            String inputText = scanner.nextLine();
            inputBytes = inputText.getBytes();
        } else if ("file".equalsIgnoreCase(inputType)) {
            System.out.println("Enter the file path:");
            String filePath = scanner.nextLine();
            try {
                inputBytes = Files.readAllBytes(Paths.get(filePath));
            } catch (Exception e) {
                System.err.println("Could not read the file.");
                return;
            }
        } else {
            System.err.println("Invalid input type.");
            return;
        }
        
        // You may need to ask for additional parameters depending on the chosen function, such as output length, customization string, etc.
        Hash hash = new Hash();
        byte[] output = null;

        // ******** Ecryption or Decryption ********
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
                    System.out.println("Enter the output length in bits for cSHAKE256:");
                    int outputLength = scanner.nextInt();
                    scanner.nextLine(); // consume the rest of the line

                    System.out.println("Enter the customization string (leave empty if none):");
                    String customString = scanner.nextLine();
                    byte[] customBytes = customString.getBytes();

                    // Empty N string for cSHAKE256
                    output = hash.cSHAKE256(inputBytes, outputLength, new byte[0], customBytes);
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

        } else if(action.equals("M")){
            // Compute an authentication tag (MAC).
            scanner.nextLine(); // consume the rest of the line

            System.out.println("Enter the key (leave empty if none):");
            String keyString = scanner.nextLine();
            byte[] keyBytes = keyString.getBytes();

            System.out.println("Enter the customization string (leave empty if none):");
            String kmacCustomString = scanner.nextLine();
            byte[] kmacCustomBytes = kmacCustomString.getBytes();

            output = hash.KMACXOF256(keyBytes, inputBytes, 256, kmacCustomBytes);

        } else if(action.equals("E")) {
            System.out.println("Not implemented yet.");

        } else if(action.equals("D")) {
            System.out.println("Not implemented yet.");

        }

        scanner.close();
    }

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
}
