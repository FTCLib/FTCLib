/*
Copyright (c) 2018 Craig MacFarlane
  Based upon work done by Hashids.  See below.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Craig MacFarlane nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.qualcomm.robotcore.util;

import android.util.Log;

/**
 * ShortHash, based heavily off of Hashids.
 *
 * This is a reimplementation of http://hashids.org v1.0.0 version.
 *
 * This implementation is immutable, thread-safe, no lock is necessary.
 *
 * @author <a href="mailto:fanweixiao@gmail.com">fanweixiao</a>
 * @author <a href="mailto:terciofilho@gmail.com">Tercio Gaudencio Filho</a>
 * @since 0.3.3
 */
public class ShortHash {
    /**
     * Max number that can be encoded with Hashids.
     */
    public static final long MAX_NUMBER = 9007199254740992L;

    private static final String DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    private static final String DEFAULT_SEPS = "cfhistuCFHISTU";
    private static final String DEFAULT_SALT = "";
    private static final String TAG = "ShortHash";

    private static final int DEFAULT_MIN_HASH_LENGTH = 0;
    private static final int MIN_ALPHABET_LENGTH = 16;
    private static final double SEP_DIV = 3.5;
    private static final int GUARD_DIV = 12;

    private final String salt;
    private final int minHashLength;
    private final String alphabet;
    private final String seps;
    private final String guards;

    public ShortHash() {
        this(DEFAULT_SALT);
    }

    public ShortHash(String salt) {
        this(salt, 0);
    }

    public ShortHash(String salt, int minHashLength) {
        this(salt, minHashLength, DEFAULT_ALPHABET);
    }

    public ShortHash(String salt, int minHashLength, String alphabet) {
        this.salt = salt != null ? salt : DEFAULT_SALT;
        this.minHashLength = minHashLength > 0 ? minHashLength : DEFAULT_MIN_HASH_LENGTH;

        final StringBuilder uniqueAlphabet = new StringBuilder();
        for (int i = 0; i < alphabet.length(); i++) {
            if (uniqueAlphabet.indexOf(String.valueOf(alphabet.charAt(i))) == -1) {
                uniqueAlphabet.append(alphabet.charAt(i));
            }
        }

        alphabet = uniqueAlphabet.toString();

        if (alphabet.length() < MIN_ALPHABET_LENGTH) {
            throw new IllegalArgumentException(
                    "alphabet must contain at least " + MIN_ALPHABET_LENGTH + " unique characters");
        }

        if (alphabet.contains(" ")) {
            throw new IllegalArgumentException("alphabet cannot contains spaces");
        }

        // seps should contain only characters present in alphabet;
        // alphabet should not contains seps
        String seps = DEFAULT_SEPS;
        for (int i = 0; i < seps.length(); i++) {
            final int j = alphabet.indexOf(seps.charAt(i));
            if (j == -1) {
                seps = seps.substring(0, i) + " " + seps.substring(i + 1);
            } else {
                alphabet = alphabet.substring(0, j) + " " + alphabet.substring(j + 1);
            }
        }

        alphabet = alphabet.replaceAll("\\s+", "");
        seps = seps.replaceAll("\\s+", "");
        seps = ShortHash.consistentShuffle(seps, this.salt);

        if ((seps.isEmpty()) || (((float) alphabet.length() / seps.length()) > SEP_DIV)) {
            int seps_len = (int) Math.ceil(alphabet.length() / SEP_DIV);

            if (seps_len == 1) {
                seps_len++;
            }

            if (seps_len > seps.length()) {
                final int diff = seps_len - seps.length();
                seps += alphabet.substring(0, diff);
                alphabet = alphabet.substring(diff);
            } else {
                seps = seps.substring(0, seps_len);
            }
        }

        alphabet = ShortHash.consistentShuffle(alphabet, this.salt);
        // use double to round up
        final int guardCount = (int) Math.ceil((double) alphabet.length() / GUARD_DIV);

        String guards;
        if (alphabet.length() < 3) {
            guards = seps.substring(0, guardCount);
            seps = seps.substring(guardCount);
        } else {
            guards = alphabet.substring(0, guardCount);
            alphabet = alphabet.substring(guardCount);
        }
        this.guards = guards;
        this.alphabet = alphabet;
        this.seps = seps;
    }

    /**
     * Encrypt numbers to string
     *
     * @param number
     *          the numbers to encrypt
     * @return the encrypt string
     */
    public String encode(long number) {
        return this._encode(number);
    }

    public int getAlphabetLength() {
        return alphabet.length();
    }

    private String _encode(long number) {
        String alphabet = this.alphabet;

        long num;
        long sepsIndex, guardIndex;
        String buffer;

        buffer = this.salt + alphabet;
        alphabet = ShortHash.consistentShuffle(alphabet, buffer.substring(0, alphabet.length()));
        final String last = ShortHash.hash(number, alphabet);

        return last;
    }

    private static String consistentShuffle(String alphabet, String salt) {
        if (salt.length() <= 0) {
            return alphabet;
        }

        int asc_val, j;
        final char[] tmpArr = alphabet.toCharArray();
        for (int i = tmpArr.length - 1, v = 0, p = 0; i > 0; i--, v++) {
            v %= salt.length();
            asc_val = salt.charAt(v);
            p += asc_val;
            j = (asc_val + v + p) % i;
            final char tmp = tmpArr[j];
            tmpArr[j] = tmpArr[i];
            tmpArr[i] = tmp;
        }

        return new String(tmpArr);
    }

    private static String hash(long input, String alphabet) {
        String hash = "";
        final int alphabetLen = alphabet.length();

        Log.i(TAG, "Alphabet length " + alphabetLen);
        do {
            final int index = (int) (input % alphabetLen);
            if (index >= 0 && index < alphabet.length()) {
                hash = alphabet.charAt(index) + hash;
            }
            input /= alphabetLen;
        } while (input > 0);

        return hash;
    }

    /**
     * Get Hashid algorithm version.
     *
     * @return Hashids algorithm version implemented.
     */
    public String getVersion() {
        return "1.0.0";
    }
}
