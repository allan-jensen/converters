import java.util.UUID;

public class UUIDConverter {
    private static final char[] BASE92 = {
            '!', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', ':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', ']', '^', '_', '`', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '{', '|', '}', '~'
    };

    private static final int BASE = BASE92.length;
    private static final int[] CHAR_TO_INDEX = new int[128];
    private static final int MAX_LENGTH = 19;

    static {
        for (int i = 0; i < BASE; i++) {
            CHAR_TO_INDEX[BASE92[i]] = i;
        }
    }

    public static String encode(UUID uuid) {
        long[] longs = new long[3];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        long variant = lsb >>> 60;
        variant = variant - 8;
        lsb = (lsb & 0x0FFFFFFFFFFFFFFFL) | (variant << 60);

        long msbHex = msb & 0xF000000000000000L;
        long lsbHex = lsb & 0xF000000000000000L;
        msb = (msb & 0x0FFFFFFFFFFFFFFFL) | lsbHex;
        lsb = (lsb & 0x0FFFFFFFFFFFFFFFL) | msbHex;

        long afterVersion = msb & 0xFFFL;
        msb = ((msb & 0xFFFFFFFFFFFF0000L) >>> 4 | afterVersion);

        longs[0] = (msb & 0xFFFFFFFF00000000L) >>> 32;
        longs[1] = ((msb & 0xFFFFFFFFL) << 16) | ((lsb & 0xFFFF000000000000L) >>> 48);
        longs[2] = lsb & 0xFFFFFFFFFFFFL;

        char[] result = new char[MAX_LENGTH];
        int index = MAX_LENGTH;

        while (longs[2] != 0) {
            long remainder = 0;
            for (int i = 0; i < longs.length; i++) {
                if (longs[i] == 0) continue;
                long cur = (remainder << 48) | (longs[i] & 0xFFFFFFFFFFFFL);
                longs[i] = cur / BASE;
                remainder = cur % BASE;
            }
            result[--index] = BASE92[(int) remainder];
        }

        return new String(result, index, MAX_LENGTH - index);
    }

    public static UUID decode(String str) {
        long[] longs = new long[3];

        for (int i = 0; i < str.length(); i++) {
            int carry = CHAR_TO_INDEX[str.charAt(i)];
            for (int j = longs.length - 1; j >= 0; j--) {
                long cur = longs[j] * BASE + carry;
                longs[j] = cur & 0xFFFFFFFFFFFFL;
                carry = (int) (cur >>> 48);
            }
        }

        long msb = (longs[0] << 32) | (longs[1] >>> 16);
        long lsb = ((longs[1] & 0xFFFFL) << 48) | longs[2];

        long afterVersion = (msb & 0xFFFL) + 0x4000L;
        msb = ((msb & 0xFFFFFFFFFFFF000L) << 4 | afterVersion);

        long msbHex = msb & 0xF000000000000000L;
        long lsbHex = lsb & 0xF000000000000000L;
        msb = (msb & 0x0FFFFFFFFFFFFFFFL) | lsbHex;
        lsb = (lsb & 0x0FFFFFFFFFFFFFFFL) | msbHex;

        long variant = lsb >>> 60;
        variant = variant - 8;
        lsb = (lsb & 0x0FFFFFFFFFFFFFFFL) | (variant << 60);

        return new UUID(msb, lsb);
    }
}