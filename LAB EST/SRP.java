import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SRP {
    private static final String SALT = "your_salt_value";
    private static final String VERIFIER = "your_verifier_value";

    public static String calculateSessionKey(String password, String challenge) {
        byte[] clientVerifier = ClientVerifier.calculateClientVerifier(password);
        byte[] combined = ValueConcatenator.concatenate(challenge, clientVerifier);
        byte[] sessionKey = SessionKeyCalculator.calculateSessionKeyHash(combined);
        return SessionKeyConverter.bytesToHex(sessionKey);
    }

    private static class ClientVerifier {
        private static byte[] calculateClientVerifier(String password) {
            byte[] saltedPasswordHash = SaltedPasswordHashCalculator.calculateSaltedPasswordHash(password);
            return Deriver.calculateClientVerifier(saltedPasswordHash);
        }
    }

    private static class ValueConcatenator {
        private static byte[] concatenate(byte[]... arrays) {
            int length = Arrays.stream(arrays).mapToInt(Byte::length).sum();
            byte[] result = new byte[length];
            int offset = 0;

            for (byte[] array : arrays) {
                System.arraycopy(array, 0, result, offset, array.length);
                offset += array.length;
            }

            return result;
        }
    }

    private static class SessionKeyCalculator {
        private static byte[] calculateSessionKeyHash(byte[] combined) {
            return HashCalculator.calculateHash(combined);
        }
    }

    private static class SessionKeyConverter {
        private static String bytesToHex(byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
    }

    private static class SaltedPasswordHashCalculator {
        private static byte[] calculateSaltedPasswordHash(String password) {
            byte[] passwordHash = HashCalculator.calculateHash(password);
            return HashCalculator.calculateHash(concatenate(SALT.getBytes(), passwordHash));
        }
    }

    private static class Deriver {
        private static byte[] calculateClientVerifier(byte[] saltedPasswordHash) {
            return HashCalculator.calculateHash(concatenate(VERIFIER.getBytes(), saltedPasswordHash));
        }
    }

    private static class HashCalculator {
        private static byte[] calculateHash(byte[] data) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(data, 0, data.length);
                return digest.digest();
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate hash", e);
            }
        }
    }
}