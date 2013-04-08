package edu.nyu.cs.cs2580;

import java.util.ArrayList;

public class ByteAlignUtil {
    public static short[] encodeVbyte(int value) {
        short[] alignedCode;

        if (value < Math.pow(2, 7)) {
            alignedCode = new short[1];
            alignedCode[0] = (short) ((value & 0x0000007F) | 0x00000080);
        } else if (Math.pow(2, 7) <= value && value < Math.pow(2, 14)) {
            alignedCode = new short[2];
            alignedCode[1] = (short) ((value & 0x0000007F) | 0x00000080);
            alignedCode[0] = (short) ((value >> 7) & 0x0000007F);
        } else if (Math.pow(2, 14) <= value && value < Math.pow(2, 21)) {
            alignedCode = new short[3];
            alignedCode[2] = (short) ((value & 0x0000007F) | 0x00000080);
            alignedCode[1] = (short) ((value >> 7) & 0x0000007F);
            alignedCode[0] = (short) ((value >> 14) & 0x0000007F);
        } else if (Math.pow(2, 21) <= value && value < Math.pow(2, 28)) {
            alignedCode = new short[4];
            alignedCode[3] = (short) ((value & 0x0000007F) | 0x00000080);
            alignedCode[2] = (short) ((value >> 7) & 0x0000007F);
            alignedCode[1] = (short) ((value >> 14) & 0x0000007F);
            alignedCode[0] = (short) ((value >> 21) & 0x0000007F);
        } else {
            throw new RuntimeException("Value : " + value + " cannot be handled by shortAlignedCode");
        }

        return alignedCode;
    }

    public static int decodeVbyte(int startPosition, ArrayList<Short> list) {
        int value = 0;
        Short s = list.get(startPosition);
        while ((s & 0x00000080) == 0) {
            value = value << 7;
            value = value | (s & 0x0000007F);
            startPosition++;
            s = list.get(startPosition);
        }
        value = value << 7;
        value = value | (s & 0x0000007F);

        return value;
    }

    public static int nextPosition(int startPosition, ArrayList<Short> list) {
        int offset = 1;
        int tempP = startPosition;
        for (; tempP < list.size(); tempP++) {
            if ((list.get(tempP) & 0x00000080) == 0) {
                offset++;
            } else {
                break;
            }
        }
        return startPosition + offset;
    }

    public static int howManyAppeared(int positionOfDocId, ArrayList<Short> docMap) {
        return decodeVbyte(nextPosition(positionOfDocId, docMap), docMap);
    }
    
    public static  int appendEncodedValueToList(ArrayList<Short> target, int value) {
        int length = 0;
        for (Short v : encodeVbyte(value)) {
            target.add(v);
            length++;
        }

        return length;
    }    
}