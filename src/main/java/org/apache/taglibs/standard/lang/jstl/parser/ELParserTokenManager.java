/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.taglibs.standard.lang.jstl.parser;

public class ELParserTokenManager implements ELParserConstants {
    public java.io.PrintStream debugStream = System.out;

    public void setDebugStream(java.io.PrintStream ds) {
        debugStream = ds;
    }

    private final int jjStopStringLiteralDfa_0(int pos, long active0) {
        switch (pos) {
        case 0:
            if ((active0 & 0x4L) != 0L) {
                jjmatchedKind = 1;
                return 2;
            }
            return -1;
        default:
            return -1;
        }
    }

    private final int jjStartNfa_0(int pos, long active0) {
        return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
    }

    private final int jjStopAtPos(int pos, int kind) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        return pos + 1;
    }

    private final int jjStartNfaWithStates_0(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_0(state, pos + 1);
    }

    private final int jjMoveStringLiteralDfa0_0() {
        switch (curChar) {
        case 36:
            return jjMoveStringLiteralDfa1_0(0x4L);
        default:
            return jjMoveNfa_0(1, 0);
        }
    }

    private final int jjMoveStringLiteralDfa1_0(long active0) {
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            jjStopStringLiteralDfa_0(0, active0);
            return 1;
        }
        switch (curChar) {
        case 123:
            if ((active0 & 0x4L) != 0L)
                return jjStopAtPos(1, 2);
            break;
        default:
            break;
        }
        return jjStartNfa_0(0, active0);
    }

    private final void jjCheckNAdd(int state) {
        if (jjrounds[state] != jjround) {
            jjstateSet[jjnewStateCnt++] = state;
            jjrounds[state] = jjround;
        }
    }

    private final void jjAddStates(int start, int end) {
        do {
            jjstateSet[jjnewStateCnt++] = jjnextStates[start];
        } while (start++ != end);
    }

    private final void jjCheckNAddTwoStates(int state1, int state2) {
        jjCheckNAdd(state1);
        jjCheckNAdd(state2);
    }

    private final void jjCheckNAddStates(int start, int end) {
        do {
            jjCheckNAdd(jjnextStates[start]);
        } while (start++ != end);
    }

    private final void jjCheckNAddStates(int start) {
        jjCheckNAdd(jjnextStates[start]);
        jjCheckNAdd(jjnextStates[start + 1]);
    }

    static final long[] jjbitVec0 = { 0xfffffffffffffffeL, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL };
    static final long[] jjbitVec2 = { 0x0L, 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL };

    private final int jjMoveNfa_0(int startState, int curPos) {
        int[] nextStates;
        int startsAt = 0;
        jjnewStateCnt = 3;
        int i = 1;
        jjstateSet[0] = startState;
        int j, kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff)
                ReInitRounds();
            if (curChar < 64) {
                long l = 1L << curChar;
                MatchLoop: do {
                    switch (jjstateSet[--i]) {
                    case 1:
                        if ((0xffffffefffffffffL & l) != 0L) {
                            if (kind > 1)
                                kind = 1;
                            jjCheckNAdd(0);
                        } else if (curChar == 36) {
                            if (kind > 1)
                                kind = 1;
                            jjCheckNAdd(2);
                        }
                        break;
                    case 0:
                        if ((0xffffffefffffffffL & l) == 0L)
                            break;
                        if (kind > 1)
                            kind = 1;
                        jjCheckNAdd(0);
                        break;
                    case 2:
                        if ((0xffffffefffffffffL & l) == 0L)
                            break;
                        if (kind > 1)
                            kind = 1;
                        jjCheckNAdd(2);
                        break;
                    default:
                        break;
                    }
                } while (i != startsAt);
            } else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                MatchLoop: do {
                    switch (jjstateSet[--i]) {
                    case 1:
                    case 0:
                        if (kind > 1)
                            kind = 1;
                        jjCheckNAdd(0);
                        break;
                    case 2:
                        if ((0xf7ffffffffffffffL & l) == 0L)
                            break;
                        if (kind > 1)
                            kind = 1;
                        jjstateSet[jjnewStateCnt++] = 2;
                        break;
                    default:
                        break;
                    }
                } while (i != startsAt);
            } else {
                int hiByte = (int) (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                MatchLoop: do {
                    switch (jjstateSet[--i]) {
                    case 1:
                    case 0:
                        if (!jjCanMove_0(hiByte, i1, i2, l1, l2))
                            break;
                        if (kind > 1)
                            kind = 1;
                        jjCheckNAdd(0);
                        break;
                    case 2:
                        if (!jjCanMove_0(hiByte, i1, i2, l1, l2))
                            break;
                        if (kind > 1)
                            kind = 1;
                        jjstateSet[jjnewStateCnt++] = 2;
                        break;
                    default:
                        break;
                    }
                } while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 3 - (jjnewStateCnt = startsAt)))
                return curPos;
            try {
                curChar = input_stream.readChar();
            } catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    private final int jjStopStringLiteralDfa_1(int pos, long active0) {
        switch (pos) {
        case 0:
            if ((active0 & 0x1568015547000L) != 0L) {
                jjmatchedKind = 49;
                return 6;
            }
            if ((active0 & 0x10000L) != 0L)
                return 1;
            return -1;
        case 1:
            if ((active0 & 0x400015540000L) != 0L)
                return 6;
            if ((active0 & 0x1168000007000L) != 0L) {
                jjmatchedKind = 49;
                jjmatchedPos = 1;
                return 6;
            }
            return -1;
        case 2:
            if ((active0 & 0x168000000000L) != 0L)
                return 6;
            if ((active0 & 0x1000000007000L) != 0L) {
                jjmatchedKind = 49;
                jjmatchedPos = 2;
                return 6;
            }
            return -1;
        case 3:
            if ((active0 & 0x5000L) != 0L)
                return 6;
            if ((active0 & 0x1000000002000L) != 0L) {
                jjmatchedKind = 49;
                jjmatchedPos = 3;
                return 6;
            }
            return -1;
        default:
            return -1;
        }
    }

    private final int jjStartNfa_1(int pos, long active0) {
        return jjMoveNfa_1(jjStopStringLiteralDfa_1(pos, active0), pos + 1);
    }

    private final int jjStartNfaWithStates_1(int pos, int kind, int state) {
        jjmatchedKind = kind;
        jjmatchedPos = pos;
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            return pos + 1;
        }
        return jjMoveNfa_1(state, pos + 1);
    }

    private final int jjMoveStringLiteralDfa0_1() {
        switch (curChar) {
        case 33:
            jjmatchedKind = 43;
            return jjMoveStringLiteralDfa1_1(0x8000000L);
        case 37:
            return jjStopAtPos(0, 40);
        case 38:
            return jjMoveStringLiteralDfa1_1(0x200000000000L);
        case 40:
            return jjStopAtPos(0, 29);
        case 41:
            return jjStopAtPos(0, 30);
        case 42:
            return jjStopAtPos(0, 37);
        case 43:
            return jjStopAtPos(0, 35);
        case 44:
            return jjStopAtPos(0, 31);
        case 45:
            return jjStopAtPos(0, 36);
        case 46:
            return jjStartNfaWithStates_1(0, 16, 1);
        case 47:
            return jjStopAtPos(0, 38);
        case 58:
            return jjStopAtPos(0, 32);
        case 60:
            jjmatchedKind = 19;
            return jjMoveStringLiteralDfa1_1(0x800000L);
        case 61:
            return jjMoveStringLiteralDfa1_1(0x200000L);
        case 62:
            jjmatchedKind = 17;
            return jjMoveStringLiteralDfa1_1(0x2000000L);
        case 91:
            return jjStopAtPos(0, 33);
        case 93:
            return jjStopAtPos(0, 34);
        case 97:
            return jjMoveStringLiteralDfa1_1(0x100000000000L);
        case 100:
            return jjMoveStringLiteralDfa1_1(0x8000000000L);
        case 101:
            return jjMoveStringLiteralDfa1_1(0x1000000400000L);
        case 102:
            return jjMoveStringLiteralDfa1_1(0x2000L);
        case 103:
            return jjMoveStringLiteralDfa1_1(0x4040000L);
        case 108:
            return jjMoveStringLiteralDfa1_1(0x1100000L);
        case 109:
            return jjMoveStringLiteralDfa1_1(0x20000000000L);
        case 110:
            return jjMoveStringLiteralDfa1_1(0x40010004000L);
        case 111:
            return jjMoveStringLiteralDfa1_1(0x400000000000L);
        case 116:
            return jjMoveStringLiteralDfa1_1(0x1000L);
        case 124:
            return jjMoveStringLiteralDfa1_1(0x800000000000L);
        case 125:
            return jjStopAtPos(0, 15);
        default:
            return jjMoveNfa_1(0, 0);
        }
    }

    private final int jjMoveStringLiteralDfa1_1(long active0) {
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(0, active0);
            return 1;
        }
        switch (curChar) {
        case 38:
            if ((active0 & 0x200000000000L) != 0L)
                return jjStopAtPos(1, 45);
            break;
        case 61:
            if ((active0 & 0x200000L) != 0L)
                return jjStopAtPos(1, 21);
            else if ((active0 & 0x800000L) != 0L)
                return jjStopAtPos(1, 23);
            else if ((active0 & 0x2000000L) != 0L)
                return jjStopAtPos(1, 25);
            else if ((active0 & 0x8000000L) != 0L)
                return jjStopAtPos(1, 27);
            break;
        case 97:
            return jjMoveStringLiteralDfa2_1(active0, 0x2000L);
        case 101:
            if ((active0 & 0x1000000L) != 0L)
                return jjStartNfaWithStates_1(1, 24, 6);
            else if ((active0 & 0x4000000L) != 0L)
                return jjStartNfaWithStates_1(1, 26, 6);
            else if ((active0 & 0x10000000L) != 0L)
                return jjStartNfaWithStates_1(1, 28, 6);
            break;
        case 105:
            return jjMoveStringLiteralDfa2_1(active0, 0x8000000000L);
        case 109:
            return jjMoveStringLiteralDfa2_1(active0, 0x1000000000000L);
        case 110:
            return jjMoveStringLiteralDfa2_1(active0, 0x100000000000L);
        case 111:
            return jjMoveStringLiteralDfa2_1(active0, 0x60000000000L);
        case 113:
            if ((active0 & 0x400000L) != 0L)
                return jjStartNfaWithStates_1(1, 22, 6);
            break;
        case 114:
            if ((active0 & 0x400000000000L) != 0L)
                return jjStartNfaWithStates_1(1, 46, 6);
            return jjMoveStringLiteralDfa2_1(active0, 0x1000L);
        case 116:
            if ((active0 & 0x40000L) != 0L)
                return jjStartNfaWithStates_1(1, 18, 6);
            else if ((active0 & 0x100000L) != 0L)
                return jjStartNfaWithStates_1(1, 20, 6);
            break;
        case 117:
            return jjMoveStringLiteralDfa2_1(active0, 0x4000L);
        case 124:
            if ((active0 & 0x800000000000L) != 0L)
                return jjStopAtPos(1, 47);
            break;
        default:
            break;
        }
        return jjStartNfa_1(0, active0);
    }

    private final int jjMoveStringLiteralDfa2_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L)
            return jjStartNfa_1(0, old0);
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(1, active0);
            return 2;
        }
        switch (curChar) {
        case 100:
            if ((active0 & 0x20000000000L) != 0L)
                return jjStartNfaWithStates_1(2, 41, 6);
            else if ((active0 & 0x100000000000L) != 0L)
                return jjStartNfaWithStates_1(2, 44, 6);
            break;
        case 108:
            return jjMoveStringLiteralDfa3_1(active0, 0x6000L);
        case 112:
            return jjMoveStringLiteralDfa3_1(active0, 0x1000000000000L);
        case 116:
            if ((active0 & 0x40000000000L) != 0L)
                return jjStartNfaWithStates_1(2, 42, 6);
            break;
        case 117:
            return jjMoveStringLiteralDfa3_1(active0, 0x1000L);
        case 118:
            if ((active0 & 0x8000000000L) != 0L)
                return jjStartNfaWithStates_1(2, 39, 6);
            break;
        default:
            break;
        }
        return jjStartNfa_1(1, active0);
    }

    private final int jjMoveStringLiteralDfa3_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L)
            return jjStartNfa_1(1, old0);
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(2, active0);
            return 3;
        }
        switch (curChar) {
        case 101:
            if ((active0 & 0x1000L) != 0L)
                return jjStartNfaWithStates_1(3, 12, 6);
            break;
        case 108:
            if ((active0 & 0x4000L) != 0L)
                return jjStartNfaWithStates_1(3, 14, 6);
            break;
        case 115:
            return jjMoveStringLiteralDfa4_1(active0, 0x2000L);
        case 116:
            return jjMoveStringLiteralDfa4_1(active0, 0x1000000000000L);
        default:
            break;
        }
        return jjStartNfa_1(2, active0);
    }

    private final int jjMoveStringLiteralDfa4_1(long old0, long active0) {
        if (((active0 &= old0)) == 0L)
            return jjStartNfa_1(2, old0);
        try {
            curChar = input_stream.readChar();
        } catch (java.io.IOException e) {
            jjStopStringLiteralDfa_1(3, active0);
            return 4;
        }
        switch (curChar) {
        case 101:
            if ((active0 & 0x2000L) != 0L)
                return jjStartNfaWithStates_1(4, 13, 6);
            break;
        case 121:
            if ((active0 & 0x1000000000000L) != 0L)
                return jjStartNfaWithStates_1(4, 48, 6);
            break;
        default:
            break;
        }
        return jjStartNfa_1(3, active0);
    }

    static final long[] jjbitVec3 = { 0x1ff00000fffffffeL, 0xffffffffffffc000L, 0xffffffffL, 0x600000000000000L };
    static final long[] jjbitVec4 = { 0x0L, 0x0L, 0x0L, 0xff7fffffff7fffffL };
    static final long[] jjbitVec5 = { 0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffffffffffffffL };
    static final long[] jjbitVec6 = { 0xffffffffffffffffL, 0xffffffffffffffffL, 0xffffL, 0x0L };
    static final long[] jjbitVec7 = { 0xffffffffffffffffL, 0xffffffffffffffffL, 0x0L, 0x0L };
    static final long[] jjbitVec8 = { 0x3fffffffffffL, 0x0L, 0x0L, 0x0L };

    private final int jjMoveNfa_1(int startState, int curPos) {
        int[] nextStates;
        int startsAt = 0;
        jjnewStateCnt = 35;
        int i = 1;
        jjstateSet[0] = startState;
        int j, kind = 0x7fffffff;
        for (;;) {
            if (++jjround == 0x7fffffff)
                ReInitRounds();
            if (curChar < 64) {
                long l = 1L << curChar;
                MatchLoop: do {
                    switch (jjstateSet[--i]) {
                    case 0:
                        if ((0x3ff000000000000L & l) != 0L) {
                            if (kind > 7)
                                kind = 7;
                            jjCheckNAddStates(0, 4);
                        } else if ((0x1800000000L & l) != 0L) {
                            if (kind > 49)
                                kind = 49;
                            jjCheckNAdd(6);
                        } else if (curChar == 39)
                            jjCheckNAddStates(5, 9);
                        else if (curChar == 34)
                            jjCheckNAddStates(10, 14);
                        else if (curChar == 46)
                            jjCheckNAdd(1);
                        break;
                    case 1:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 8)
                            kind = 8;
                        jjCheckNAddTwoStates(1, 2);
                        break;
                    case 3:
                        if ((0x280000000000L & l) != 0L)
                            jjCheckNAdd(4);
                        break;
                    case 4:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 8)
                            kind = 8;
                        jjCheckNAdd(4);
                        break;
                    case 5:
                        if ((0x1800000000L & l) == 0L)
                            break;
                        if (kind > 49)
                            kind = 49;
                        jjCheckNAdd(6);
                        break;
                    case 6:
                        if ((0x3ff001000000000L & l) == 0L)
                            break;
                        if (kind > 49)
                            kind = 49;
                        jjCheckNAdd(6);
                        break;
                    case 7:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 7)
                            kind = 7;
                        jjCheckNAddStates(0, 4);
                        break;
                    case 8:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 7)
                            kind = 7;
                        jjCheckNAdd(8);
                        break;
                    case 9:
                        if ((0x3ff000000000000L & l) != 0L)
                            jjCheckNAddTwoStates(9, 10);
                        break;
                    case 10:
                        if (curChar != 46)
                            break;
                        if (kind > 8)
                            kind = 8;
                        jjCheckNAddTwoStates(11, 12);
                        break;
                    case 11:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 8)
                            kind = 8;
                        jjCheckNAddTwoStates(11, 12);
                        break;
                    case 13:
                        if ((0x280000000000L & l) != 0L)
                            jjCheckNAdd(14);
                        break;
                    case 14:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 8)
                            kind = 8;
                        jjCheckNAdd(14);
                        break;
                    case 15:
                        if ((0x3ff000000000000L & l) != 0L)
                            jjCheckNAddTwoStates(15, 16);
                        break;
                    case 17:
                        if ((0x280000000000L & l) != 0L)
                            jjCheckNAdd(18);
                        break;
                    case 18:
                        if ((0x3ff000000000000L & l) == 0L)
                            break;
                        if (kind > 8)
                            kind = 8;
                        jjCheckNAdd(18);
                        break;
                    case 19:
                        if (curChar == 34)
                            jjCheckNAddStates(10, 14);
                        break;
                    case 20:
                        if ((0xfffffffbffffffffL & l) != 0L)
                            jjCheckNAddStates(15, 17);
                        break;
                    case 22:
                        if (curChar == 34)
                            jjCheckNAddStates(15, 17);
                        break;
                    case 23:
                        if (curChar == 34 && kind > 10)
                            kind = 10;
                        break;
                    case 24:
                        if ((0xfffffffbffffffffL & l) != 0L)
                            jjCheckNAddTwoStates(24, 25);
                        break;
                    case 26:
                        if ((0xfffffffbffffffffL & l) != 0L && kind > 11)
                            kind = 11;
                        break;
                    case 27:
                        if (curChar == 39)
                            jjCheckNAddStates(5, 9);
                        break;
                    case 28:
                        if ((0xffffff7fffffffffL & l) != 0L)
                            jjCheckNAddStates(18, 20);
                        break;
                    case 30:
                        if (curChar == 39)
                            jjCheckNAddStates(18, 20);
                        break;
                    case 31:
                        if (curChar == 39 && kind > 10)
                            kind = 10;
                        break;
                    case 32:
                        if ((0xffffff7fffffffffL & l) != 0L)
                            jjCheckNAddTwoStates(32, 33);
                        break;
                    case 34:
                        if ((0xffffff7fffffffffL & l) != 0L && kind > 11)
                            kind = 11;
                        break;
                    default:
                        break;
                    }
                } while (i != startsAt);
            } else if (curChar < 128) {
                long l = 1L << (curChar & 077);
                MatchLoop: do {
                    switch (jjstateSet[--i]) {
                    case 0:
                    case 6:
                        if ((0x7fffffe87fffffeL & l) == 0L)
                            break;
                        if (kind > 49)
                            kind = 49;
                        jjCheckNAdd(6);
                        break;
                    case 2:
                        if ((0x2000000020L & l) != 0L)
                            jjAddStates(21, 22);
                        break;
                    case 12:
                        if ((0x2000000020L & l) != 0L)
                            jjAddStates(23, 24);
                        break;
                    case 16:
                        if ((0x2000000020L & l) != 0L)
                            jjAddStates(25, 26);
                        break;
                    case 20:
                        if ((0xffffffffefffffffL & l) != 0L)
                            jjCheckNAddStates(15, 17);
                        break;
                    case 21:
                        if (curChar == 92)
                            jjstateSet[jjnewStateCnt++] = 22;
                        break;
                    case 22:
                        if (curChar == 92)
                            jjCheckNAddStates(15, 17);
                        break;
                    case 24:
                        if ((0xffffffffefffffffL & l) != 0L)
                            jjAddStates(27, 28);
                        break;
                    case 25:
                        if (curChar == 92)
                            jjstateSet[jjnewStateCnt++] = 26;
                        break;
                    case 26:
                    case 34:
                        if ((0xffffffffefffffffL & l) != 0L && kind > 11)
                            kind = 11;
                        break;
                    case 28:
                        if ((0xffffffffefffffffL & l) != 0L)
                            jjCheckNAddStates(18, 20);
                        break;
                    case 29:
                        if (curChar == 92)
                            jjstateSet[jjnewStateCnt++] = 30;
                        break;
                    case 30:
                        if (curChar == 92)
                            jjCheckNAddStates(18, 20);
                        break;
                    case 32:
                        if ((0xffffffffefffffffL & l) != 0L)
                            jjAddStates(29, 30);
                        break;
                    case 33:
                        if (curChar == 92)
                            jjstateSet[jjnewStateCnt++] = 34;
                        break;
                    default:
                        break;
                    }
                } while (i != startsAt);
            } else {
                int hiByte = (int) (curChar >> 8);
                int i1 = hiByte >> 6;
                long l1 = 1L << (hiByte & 077);
                int i2 = (curChar & 0xff) >> 6;
                long l2 = 1L << (curChar & 077);
                MatchLoop: do {
                    switch (jjstateSet[--i]) {
                    case 0:
                    case 6:
                        if (!jjCanMove_1(hiByte, i1, i2, l1, l2))
                            break;
                        if (kind > 49)
                            kind = 49;
                        jjCheckNAdd(6);
                        break;
                    case 20:
                        if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                            jjAddStates(15, 17);
                        break;
                    case 24:
                        if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                            jjAddStates(27, 28);
                        break;
                    case 26:
                    case 34:
                        if (jjCanMove_0(hiByte, i1, i2, l1, l2) && kind > 11)
                            kind = 11;
                        break;
                    case 28:
                        if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                            jjAddStates(18, 20);
                        break;
                    case 32:
                        if (jjCanMove_0(hiByte, i1, i2, l1, l2))
                            jjAddStates(29, 30);
                        break;
                    default:
                        break;
                    }
                } while (i != startsAt);
            }
            if (kind != 0x7fffffff) {
                jjmatchedKind = kind;
                jjmatchedPos = curPos;
                kind = 0x7fffffff;
            }
            ++curPos;
            if ((i = jjnewStateCnt) == (startsAt = 35 - (jjnewStateCnt = startsAt)))
                return curPos;
            try {
                curChar = input_stream.readChar();
            } catch (java.io.IOException e) {
                return curPos;
            }
        }
    }

    static final int[] jjnextStates = { 8, 9, 10, 15, 16, 28, 29, 31, 32, 33, 20, 21, 23, 24, 25, 20, 21, 23, 28, 29, 31, 3, 4, 13, 14, 17,
            18, 24, 25, 32, 33, };

    private static final boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2) {
        switch (hiByte) {
        case 0:
            return ((jjbitVec2[i2] & l2) != 0L);
        default:
            if ((jjbitVec0[i1] & l1) != 0L)
                return true;
            return false;
        }
    }

    private static final boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2) {
        switch (hiByte) {
        case 0:
            return ((jjbitVec4[i2] & l2) != 0L);
        case 48:
            return ((jjbitVec5[i2] & l2) != 0L);
        case 49:
            return ((jjbitVec6[i2] & l2) != 0L);
        case 51:
            return ((jjbitVec7[i2] & l2) != 0L);
        case 61:
            return ((jjbitVec8[i2] & l2) != 0L);
        default:
            if ((jjbitVec3[i1] & l1) != 0L)
                return true;
            return false;
        }
    }

    public static final String[] jjstrLiteralImages = { "", null, "\44\173", null, null, null, null, null, null, null, null, null,
            "\164\162\165\145", "\146\141\154\163\145", "\156\165\154\154", "\175", "\56", "\76", "\147\164", "\74", "\154\164", "\75\75",
            "\145\161", "\74\75", "\154\145", "\76\75", "\147\145", "\41\75", "\156\145", "\50", "\51", "\54", "\72", "\133", "\135", "\53",
            "\55", "\52", "\57", "\144\151\166", "\45", "\155\157\144", "\156\157\164", "\41", "\141\156\144", "\46\46", "\157\162",
            "\174\174", "\145\155\160\164\171", null, null, null, null, null, };
    public static final String[] lexStateNames = { "DEFAULT", "IN_EXPRESSION", };
    public static final int[] jjnewLexState = { -1, -1, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, };
    static final long[] jjtoToken = { 0x23fffffffffd87L, };
    static final long[] jjtoSkip = { 0x78L, };
    private SimpleCharStream input_stream;
    private final int[] jjrounds = new int[35];
    private final int[] jjstateSet = new int[70];
    protected char curChar;

    public ELParserTokenManager(SimpleCharStream stream) {
        if (SimpleCharStream.staticFlag)
            throw new Error("ERROR: Cannot use a static CharStream class with a non-static lexical analyzer.");
        input_stream = stream;
    }

    public ELParserTokenManager(SimpleCharStream stream, int lexState) {
        this(stream);
        SwitchTo(lexState);
    }

    public void ReInit(SimpleCharStream stream) {
        jjmatchedPos = jjnewStateCnt = 0;
        curLexState = defaultLexState;
        input_stream = stream;
        ReInitRounds();
    }

    private final void ReInitRounds() {
        int i;
        jjround = 0x80000001;
        for (i = 35; i-- > 0;)
            jjrounds[i] = 0x80000000;
    }

    public void ReInit(SimpleCharStream stream, int lexState) {
        ReInit(stream);
        SwitchTo(lexState);
    }

    public void SwitchTo(int lexState) {
        if (lexState >= 2 || lexState < 0)
            throw new TokenMgrError("Error: Ignoring invalid lexical state : " + lexState + ". State unchanged.",
                    TokenMgrError.INVALID_LEXICAL_STATE);
        else
            curLexState = lexState;
    }

    private final Token jjFillToken() {
        Token t = Token.newToken(jjmatchedKind);
        t.kind = jjmatchedKind;
        String im = jjstrLiteralImages[jjmatchedKind];
        t.image = (im == null) ? input_stream.GetImage() : im;
        t.beginLine = input_stream.getBeginLine();
        t.beginColumn = input_stream.getBeginColumn();
        t.endLine = input_stream.getEndLine();
        t.endColumn = input_stream.getEndColumn();
        return t;
    }

    int curLexState = 0;
    int defaultLexState = 0;
    int jjnewStateCnt;
    int jjround;
    int jjmatchedPos;
    int jjmatchedKind;

    public final Token getNextToken() {
        int kind;
        Token specialToken = null;
        Token matchedToken;
        int curPos = 0;

        EOFLoop: for (;;) {
            try {
                curChar = input_stream.BeginToken();
            } catch (java.io.IOException e) {
                jjmatchedKind = 0;
                matchedToken = jjFillToken();
                return matchedToken;
            }

            switch (curLexState) {
            case 0:
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_0();
                break;
            case 1:
                try {
                    input_stream.backup(0);
                    while (curChar <= 32 && (0x100002600L & (1L << curChar)) != 0L)
                        curChar = input_stream.BeginToken();
                } catch (java.io.IOException e1) {
                    continue EOFLoop;
                }
                jjmatchedKind = 0x7fffffff;
                jjmatchedPos = 0;
                curPos = jjMoveStringLiteralDfa0_1();
                if (jjmatchedPos == 0 && jjmatchedKind > 53) {
                    jjmatchedKind = 53;
                }
                break;
            }
            if (jjmatchedKind != 0x7fffffff) {
                if (jjmatchedPos + 1 < curPos)
                    input_stream.backup(curPos - jjmatchedPos - 1);
                if ((jjtoToken[jjmatchedKind >> 6] & (1L << (jjmatchedKind & 077))) != 0L) {
                    matchedToken = jjFillToken();
                    if (jjnewLexState[jjmatchedKind] != -1)
                        curLexState = jjnewLexState[jjmatchedKind];
                    return matchedToken;
                } else {
                    if (jjnewLexState[jjmatchedKind] != -1)
                        curLexState = jjnewLexState[jjmatchedKind];
                    continue EOFLoop;
                }
            }
            int error_line = input_stream.getEndLine();
            int error_column = input_stream.getEndColumn();
            String error_after = null;
            boolean EOFSeen = false;
            try {
                input_stream.readChar();
                input_stream.backup(1);
            } catch (java.io.IOException e1) {
                EOFSeen = true;
                error_after = curPos <= 1 ? "" : input_stream.GetImage();
                if (curChar == '\n' || curChar == '\r') {
                    error_line++;
                    error_column = 0;
                } else
                    error_column++;
            }
            if (!EOFSeen) {
                input_stream.backup(1);
                error_after = curPos <= 1 ? "" : input_stream.GetImage();
            }
            throw new TokenMgrError(EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);
        }
    }

}
