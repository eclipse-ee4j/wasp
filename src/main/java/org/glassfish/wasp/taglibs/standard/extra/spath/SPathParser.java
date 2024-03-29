/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 * Copyright (c) 2020 Payara Services Ltd.
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
package org.glassfish.wasp.taglibs.standard.extra.spath;

import java.util.List;
import java.util.Vector;

public class SPathParser implements SPathParserConstants {

    /**
     * Simple command-line parser interface, primarily for testing.
     */
    public static void main(String args[]) throws ParseException {
        SPathParser parser = new SPathParser(System.in);
        Path path = parser.expression();
        List<Step> steps = path.getSteps();

        // output for simple testing
        System.out.println();
        if (path instanceof AbsolutePath) {
            System.out.println("Root: /");
        }
        
        for (Step step : steps) {
            System.out.print("Step: " + step.getName());
            if (step.isDepthUnlimited()) {
                System.out.print("(*)");
            }
            System.out.println();
        }
    }

    // custom constructor to accept a String
    public SPathParser(String x) {
        this(new java.io.StringReader(x));
    }

//*********************************************************************
// Actual SPath grammar
    final public Path expression() throws ParseException {
        Path expr;
        if (jj_2_1(2147483647)) {
            expr = absolutePath();
            jj_consume_token(0);
        } else {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case QNAME:
            case NSWILDCARD:
            case SLASH:
            case STAR:
                expr = relativePath();
                jj_consume_token(0);
                break;
            default:
                jj_la1[0] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
        }
        return expr;
    }

    final public AbsolutePath absolutePath() throws ParseException {
        RelativePath relPath;
        jj_consume_token(SLASH);
        relPath = relativePath();
        return new AbsolutePath(relPath);
    }

    // as an example, we use recursion here to handle a list
    
    final public RelativePath relativePath() throws ParseException {
        RelativePath relPath = null;
        Step step;
        step = step();
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case SLASH:
            jj_consume_token(SLASH);
            relPath = relativePath();
            break;
        default:
            jj_la1[1] = jj_gen;
            ;
        }
        return new RelativePath(step, relPath);
    }

    // as an example, we use inline code here to handle a list
    /*
     * (I'm doing something perhaps unusual here, including the <SLASH> as if it were part of the step. this mechanism for
     * differentiating '/' from '//' seems most natural, even if it is a bit unconventional.)
     */
    final public Step step() throws ParseException {
        Token slash = null;
        String nt;
        Vector<Predicate> pl = null;
        Predicate p;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case SLASH:
            slash = jj_consume_token(SLASH);
            break;
        default:
            jj_la1[2] = jj_gen;
            ;
        }
        nt = nameTest();
        label_1: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case START_BRACKET:
                ;
                break;
            default:
                jj_la1[3] = jj_gen;
                break label_1;
            }
            p = predicate();
            if (pl == null) {
                pl = new Vector<>();
            }
            pl.add(p);
        }
        // if 'slash != null', then we have '//' versus '/'
        return new Step(slash != null, nt, pl);
    }

    final public String nameTest() throws ParseException {
        Token name;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case STAR:
            name = jj_consume_token(STAR);
            break;
        case NSWILDCARD:
            name = jj_consume_token(NSWILDCARD);
            break;
        case QNAME:
            name = jj_consume_token(QNAME);
            break;
        default:
            jj_la1[4] = jj_gen;
            jj_consume_token(-1);
            throw new ParseException();
        }
        return name.toString();
    }

    final public Predicate predicate() throws ParseException {
        Predicate p;
        jj_consume_token(START_BRACKET);
        p = attributePredicate();
        jj_consume_token(END_BRACKET);
        return p;
    }

    final public Predicate attributePredicate() throws ParseException {
        Token attname, target;
        jj_consume_token(AT);
        attname = jj_consume_token(QNAME);
        jj_consume_token(EQUALS);
        target = jj_consume_token(LITERAL);
        return new AttributePredicate(attname.toString(), target.toString());
    }

    private boolean jj_2_1(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        boolean retval = !jj_3_1();
        jj_save(0, xla);
        return retval;
    }

    private boolean jj_3R_13() {
        if (jj_scan_token(AT)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(QNAME)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(EQUALS)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(LITERAL)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3_1() {
        if (jj_3R_2()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_10() {
        if (jj_scan_token(NSWILDCARD)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_11() {
        if (jj_scan_token(QNAME)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_2() {
        if (jj_scan_token(SLASH)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_3R_3()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_12() {
        if (jj_scan_token(START_BRACKET)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_3R_13()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(END_BRACKET)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_8() {
        if (jj_3R_12()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_5() {
        if (jj_scan_token(SLASH)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_3R_3()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_6() {
        if (jj_scan_token(SLASH)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_3() {
        if (jj_3R_4()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_5()) {
            jj_scanpos = xsp;
        } else if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_4() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_6()) {
            jj_scanpos = xsp;
        } else if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_3R_7()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        while (true) {
            xsp = jj_scanpos;
            if (jj_3R_8()) {
                jj_scanpos = xsp;
                break;
            }
            if (jj_la == 0 && jj_scanpos == jj_lastpos) {
                return false;
            }
        }
        return false;
    }

    private boolean jj_3R_9() {
        if (jj_scan_token(STAR)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_7() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_9()) {
            jj_scanpos = xsp;
            if (jj_3R_10()) {
                jj_scanpos = xsp;
                if (jj_3R_11()) {
                    return true;
                }
                if (jj_la == 0 && jj_scanpos == jj_lastpos) {
                    return false;
                }
            } else if (jj_la == 0 && jj_scanpos == jj_lastpos) {
                return false;
            }
        } else if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    public SPathParserTokenManager token_source;
    ASCII_UCodeESC_CharStream jj_input_stream;
    public Token token, jj_nt;
    private int jj_ntk;
    private Token jj_scanpos, jj_lastpos;
    private int jj_la;
    public boolean lookingAhead = false;
    private int jj_gen;
    final private int[] jj_la1 = new int[5];
    final private int[] jj_la1_0 = { 0x6014, 0x2000, 0x2000, 0x10000, 0x4014, };
    final private JJCalls[] jj_2_rtns = new JJCalls[1];
    private boolean jj_rescan = false;
    private int jj_gc = 0;

    public SPathParser(java.io.InputStream stream) {
        jj_input_stream = new ASCII_UCodeESC_CharStream(stream, 1, 1);
        token_source = new SPathParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public void ReInit(java.io.InputStream stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public SPathParser(java.io.Reader stream) {
        jj_input_stream = new ASCII_UCodeESC_CharStream(stream, 1, 1);
        token_source = new SPathParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public void ReInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public SPathParser(SPathParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public void ReInit(SPathParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 5; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null) {
            token = token.next;
        } else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            if (++jj_gc > 100) {
                jj_gc = 0;
                for (JJCalls c : jj_2_rtns) {
                    while (c != null) {
                        if (c.gen < jj_gen) {
                            c.first = null;
                        }
                        c = c.next;
                    }
                }
            }
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    private boolean jj_scan_token(int kind) {
        if (jj_scanpos == jj_lastpos) {
            jj_la--;
            if (jj_scanpos.next == null) {
                jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
            } else {
                jj_lastpos = jj_scanpos = jj_scanpos.next;
            }
        } else {
            jj_scanpos = jj_scanpos.next;
        }
        if (jj_rescan) {
            int i = 0;
            Token tok = token;
            while (tok != null && tok != jj_scanpos) {
                i++;
                tok = tok.next;
            }
            if (tok != null) {
                jj_add_error_token(kind, i);
            }
        }
        return (jj_scanpos.kind != kind);
    }

    final public Token getNextToken() {
        if (token.next != null) {
            token = token.next;
        } else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    final public Token getToken(int index) {
        Token t = lookingAhead ? jj_scanpos : token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) {
                t = t.next;
            } else {
                t = t.next = token_source.getNextToken();
            }
        }
        return t;
    }

    private int jj_ntk() {
        if ((jj_nt = token.next) == null) {
            return (jj_ntk = (token.next = token_source.getNextToken()).kind);
        } else {
            return (jj_ntk = jj_nt.kind);
        }
    }

    private Vector<int[]> jj_expentries = new Vector<>();
    private int[] jj_expentry;
    private int jj_kind = -1;
    private int[] jj_lasttokens = new int[100];
    private int jj_endpos;

    private void jj_add_error_token(int kind, int pos) {
        if (pos >= 100) {
            return;
        }
        if (pos == jj_endpos + 1) {
            jj_lasttokens[jj_endpos++] = kind;
        } else if (jj_endpos != 0) {
            jj_expentry = new int[jj_endpos];
            System.arraycopy(jj_lasttokens, 0, jj_expentry, 0, jj_endpos);
            boolean exists = false;
            for (int[] oldentry : jj_expentries) {
                if (oldentry.length == jj_expentry.length) {
                    exists = true;
                    for (int i = 0; i < jj_expentry.length; i++) {
                        if (oldentry[i] != jj_expentry[i]) {
                            exists = false;
                            break;
                        }
                    }
                    if (exists) {
                        break;
                    }
                }
            }
            if (!exists) {
                jj_expentries.addElement(jj_expentry);
            }
            if (pos != 0) {
                jj_lasttokens[(jj_endpos = pos) - 1] = kind;
            }
        }
    }

    final public ParseException generateParseException() {
        jj_expentries.removeAllElements();
        boolean[] la1tokens = new boolean[20];
        for (int i = 0; i < 20; i++) {
            la1tokens[i] = false;
        }
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 5; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 20; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.addElement(jj_expentry);
            }
        }
        jj_endpos = 0;
        jj_rescan_token();
        jj_add_error_token(0, 0);
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = jj_expentries.elementAt(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    final public void enable_tracing() {
    }

    final public void disable_tracing() {
    }

    private void jj_rescan_token() {
        jj_rescan = true;
        for (int i = 0; i < 1; i++) {
            JJCalls p = jj_2_rtns[i];
            do {
                if (p.gen > jj_gen) {
                    jj_la = p.arg;
                    jj_lastpos = jj_scanpos = p.first;
                    switch (i) {
                    case 0:
                        jj_3_1();
                        break;
                    }
                }
                p = p.next;
            } while (p != null);
        }
        jj_rescan = false;
    }

    private void jj_save(int index, int xla) {
        JJCalls p = jj_2_rtns[index];
        while (p.gen > jj_gen) {
            if (p.next == null) {
                p = p.next = new JJCalls();
                break;
            }
            p = p.next;
        }
        p.gen = jj_gen + xla - jj_la;
        p.first = token;
        p.arg = xla;
    }

    static final class JJCalls {

        int gen;
        Token first;
        int arg;
        JJCalls next;
    }

}
