package com.example.smartpos4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.example.smartpos4.utils.CRC;

public class Utils {

    private static final String CIPHER_NAME = "DESede/CBC/NoPadding";
    private static final String CIPHER_PROVIDER = "BC";

    public static String bytes2HexStr(byte[] bytes) {
        /*
        StringBuilder sb = new StringBuilder();
        String temp;
        for (byte b : bytes) {
            // Combine each byte with 0xFF, convert it to decimal, and then convert it to hex by Integer
            temp = Integer.toHexString(0xFF & b);
            if (temp.length() == 1) {
                // Each byte 8 is converted to a hexadecimal flag, 2 hexadecimal digits
                sb.append("0");
            }
            sb.append(temp);
        }
        return sb.toString().toUpperCase();

         */
        byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static byte [] calcCrc(byte[] data){
        byte[] crc = CRC.swapCrcBytes(CRC.calculateCRC(CRC.Parameters.CRCA,(data)));
        List list = new ArrayList(Arrays.asList(data));
        list.addAll(Arrays.asList(crc));
        byte[] both = concatenate(data, crc);
        return both;
    }

    public static byte [] concatenate (final byte array1[], final byte array2[])
    {
        byte[] both = Arrays.copyOf(array1, array1.length + array2.length);

        System.arraycopy(array2, 0, both, array1.length, array2.length);

        return both;
    }

    public static byte[] encrypt(byte[] data, Key key, IvParameterSpec iv) throws NoSuchProviderException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, key, iv);
        CipherOutputStream chiperOut = new CipherOutputStream(out, cipher);

        try {
            chiperOut.write(data);
        } finally {
            chiperOut.close();
        }

        return out.toByteArray();

    }

    public static byte[] decrypt(byte[] encryptedData, Key key, IvParameterSpec iv) throws NoSuchProviderException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(encryptedData);
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, key, iv);
        CipherInputStream chiperIn = new CipherInputStream(in, cipher);

        try {
            return fromStream(chiperIn, 1024);

        } finally {
            chiperIn.close();

        }
    }

    public static byte[] fromStream(InputStream in, int bufferSize) throws IOException {
        ArrayList<Byte> result = new ArrayList<>();
        int offset = 0;
        int bytesRead;
        byte[] buffer = new byte[bufferSize];
        while((bytesRead = in.read(buffer, offset, bufferSize)) != -1) {
            for(int i=0; i<bytesRead; ++i) {
                result.add(buffer[i]);
            }
        }

        int numberOfbytes = result.size();
        byte[] byteResult = new byte[numberOfbytes];
        for(int i=0; i<numberOfbytes; ++i) {
            byteResult[i] = result.get(i);
        }
        return byteResult;
    }

    public static Cipher getCipher(int cipherMode, Key key, IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {

        Cipher cipher = Cipher.getInstance(CIPHER_NAME, CIPHER_PROVIDER);
        cipher.init(cipherMode, key, iv);
        return cipher;

    }

    public static IvParameterSpec createIvSpecFromZeros(int size) {
        byte[] buffer = createZeros(size);
        return new IvParameterSpec(buffer);
    }

    public static byte[] createZeros(int size) {
        byte[] buffer = new byte[size];
        for(int i=0; i<size; ++i) {
            buffer[i] = 0;
        }

        return buffer;
    }

    public String toHexString(byte[] bytes) {
        if(bytes == null) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        for(byte b: bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString().toUpperCase();
    }

    public static boolean checkHex(String s)
    {
        // Size of string
        int n = s.length();

        // Iterate over string
        for (int i = 0; i < n; i++) {

            char ch = s.charAt(i);

            // Check if the character
            // is invalid
            if ((ch < '0' || ch > '9')
                    && (ch < 'A' || ch > 'F')) {

                return false;
            }
        }

        // Print true if all
        // characters are valid
        return true;
    }

}
