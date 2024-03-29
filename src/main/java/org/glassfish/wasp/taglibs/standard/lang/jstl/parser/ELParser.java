/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 * Copyright (c) 1997-2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 Payara Servicer Ltd.
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

package org.glassfish.wasp.taglibs.standard.lang.jstl.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.glassfish.wasp.taglibs.standard.lang.jstl.AndOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.ArraySuffix;
import org.glassfish.wasp.taglibs.standard.lang.jstl.BinaryOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.BinaryOperatorExpression;
import org.glassfish.wasp.taglibs.standard.lang.jstl.BooleanLiteral;
import org.glassfish.wasp.taglibs.standard.lang.jstl.ComplexValue;
import org.glassfish.wasp.taglibs.standard.lang.jstl.DivideOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.EmptyOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.EqualsOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.Expression;
import org.glassfish.wasp.taglibs.standard.lang.jstl.ExpressionString;
import org.glassfish.wasp.taglibs.standard.lang.jstl.FloatingPointLiteral;
import org.glassfish.wasp.taglibs.standard.lang.jstl.FunctionInvocation;
import org.glassfish.wasp.taglibs.standard.lang.jstl.GreaterThanOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.GreaterThanOrEqualsOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.IntegerLiteral;
import org.glassfish.wasp.taglibs.standard.lang.jstl.LessThanOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.LessThanOrEqualsOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.Literal;
import org.glassfish.wasp.taglibs.standard.lang.jstl.MinusOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.ModulusOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.MultiplyOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.NamedValue;
import org.glassfish.wasp.taglibs.standard.lang.jstl.NotEqualsOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.NotOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.NullLiteral;
import org.glassfish.wasp.taglibs.standard.lang.jstl.OrOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.PlusOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.PropertySuffix;
import org.glassfish.wasp.taglibs.standard.lang.jstl.StringLiteral;
import org.glassfish.wasp.taglibs.standard.lang.jstl.UnaryMinusOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.UnaryOperator;
import org.glassfish.wasp.taglibs.standard.lang.jstl.UnaryOperatorExpression;
import org.glassfish.wasp.taglibs.standard.lang.jstl.ValueSuffix;

/**
 * Generated EL parser.
 *
 * @author Nathan Abramson
 * @author Shawn Bayern
 */
public class ELParser implements ELParserConstants {

    public static void main(String args[]) throws ParseException {
        ELParser parser = new ELParser(System.in);
        parser.ExpressionString();
    }

    /**
     * *************************************** GRAMMAR PRODUCTIONS * ***************************************
     */
    /**
     *
     * Returns a String if the expression string is a single String, an Expression if the expression string is a single
     * Expression, an ExpressionString if it's a mixture of both.
     *
     */
    final public Object ExpressionString() throws ParseException {
        Object ret = "";
        List<Object> elems = null;
        Object elem;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case NON_EXPRESSION_TEXT:
            ret = AttrValueString();
            break;
        case START_EXPRESSION:
            ret = AttrValueExpression();
            break;
        default:
            jj_la1[0] = jj_gen;
            jj_consume_token(-1);
            throw new ParseException();
        }
        label_1: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case NON_EXPRESSION_TEXT:
            case START_EXPRESSION:
                break;
            default:
                jj_la1[1] = jj_gen;
                break label_1;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case NON_EXPRESSION_TEXT:
                elem = AttrValueString();
                break;
            case START_EXPRESSION:
                elem = AttrValueExpression();
                break;
            default:
                jj_la1[2] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            if (elems == null) {
                elems = new ArrayList<>();
                elems.add(ret);
            }
            elems.add(elem);
        }
        if (elems != null) {
            ret = new ExpressionString(elems.toArray());
        }
        return ret;
    }

    final public String AttrValueString() throws ParseException {
        Token t = jj_consume_token(NON_EXPRESSION_TEXT);
        return t.image;
    }

    final public Expression AttrValueExpression() throws ParseException {
        jj_consume_token(START_EXPRESSION);
        Expression exp = Expression();
        jj_consume_token(END_EXPRESSION);
        return exp;
    }

    final public Expression Expression() throws ParseException {
        return OrExpression();
    }

    final public Expression OrExpression() throws ParseException {
        Expression startExpression;
        BinaryOperator operator;
        Expression expression;
        List<BinaryOperator> operators = null;
        List<Expression> expressions = null;
        startExpression = AndExpression();
        label_2: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case OR1:
            case OR2:
                break;
            default:
                jj_la1[3] = jj_gen;
                break label_2;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case OR1:
                jj_consume_token(OR1);
                break;
            case OR2:
                jj_consume_token(OR2);
                break;
            default:
                jj_la1[4] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            operator = OrOperator.SINGLETON;
            expression = AndExpression();
            if (operators == null) {
                operators = new ArrayList<>();
                expressions = new ArrayList<>();
            }
            operators.add(operator);
            expressions.add(expression);
        }
        if (operators != null) {
            return new BinaryOperatorExpression(startExpression, operators, expressions);
        } else {
            return startExpression;
        }
    }

    final public Expression AndExpression() throws ParseException {
        Expression startExpression;
        BinaryOperator operator;
        Expression expression;
        List<BinaryOperator> operators = null;
        List<Expression> expressions = null;
        startExpression = EqualityExpression();
        label_3: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case AND1:
            case AND2:
                break;
            default:
                jj_la1[5] = jj_gen;
                break label_3;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case AND1:
                jj_consume_token(AND1);
                break;
            case AND2:
                jj_consume_token(AND2);
                break;
            default:
                jj_la1[6] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            operator = AndOperator.SINGLETON;
            expression = EqualityExpression();
            if (operators == null) {
                operators = new ArrayList<>();
                expressions = new ArrayList<>();
            }
            operators.add(operator);
            expressions.add(expression);
        }
        if (operators != null) {
            return new BinaryOperatorExpression(startExpression, operators, expressions);
        } else {
            return startExpression;
        }
    }

    final public Expression EqualityExpression() throws ParseException {
        Expression startExpression;
        BinaryOperator operator;
        Expression expression;
        List<BinaryOperator> operators = null;
        List<Expression> expressions = null;
        startExpression = RelationalExpression();
        label_4: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case EQ1:
            case EQ2:
            case NE1:
            case NE2:
                break;
            default:
                jj_la1[7] = jj_gen;
                break label_4;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case EQ1:
            case EQ2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case EQ1:
                    jj_consume_token(EQ1);
                    break;
                case EQ2:
                    jj_consume_token(EQ2);
                    break;
                default:
                    jj_la1[8] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = EqualsOperator.SINGLETON;
                break;
            case NE1:
            case NE2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case NE1:
                    jj_consume_token(NE1);
                    break;
                case NE2:
                    jj_consume_token(NE2);
                    break;
                default:
                    jj_la1[9] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = NotEqualsOperator.SINGLETON;
                break;
            default:
                jj_la1[10] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            expression = RelationalExpression();
            if (operators == null) {
                operators = new ArrayList<>();
                expressions = new ArrayList<>();
            }
            operators.add(operator);
            expressions.add(expression);
        }
        if (operators != null) {
            return new BinaryOperatorExpression(startExpression, operators, expressions);
        } else {
            return startExpression;
        }
    }

    final public Expression RelationalExpression() throws ParseException {
        Expression startExpression;
        BinaryOperator operator;
        Expression expression;
        List<BinaryOperator> operators = null;
        List<Expression> expressions = null;
        startExpression = AddExpression();
        label_5: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case GT1:
            case GT2:
            case LT1:
            case LT2:
            case LE1:
            case LE2:
            case GE1:
            case GE2:
                break;
            default:
                jj_la1[11] = jj_gen;
                break label_5;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case LT1:
            case LT2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case LT1:
                    jj_consume_token(LT1);
                    break;
                case LT2:
                    jj_consume_token(LT2);
                    break;
                default:
                    jj_la1[12] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = LessThanOperator.SINGLETON;
                break;
            case GT1:
            case GT2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case GT1:
                    jj_consume_token(GT1);
                    break;
                case GT2:
                    jj_consume_token(GT2);
                    break;
                default:
                    jj_la1[13] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = GreaterThanOperator.SINGLETON;
                break;
            case GE1:
            case GE2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case GE1:
                    jj_consume_token(GE1);
                    break;
                case GE2:
                    jj_consume_token(GE2);
                    break;
                default:
                    jj_la1[14] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = GreaterThanOrEqualsOperator.SINGLETON;
                break;
            case LE1:
            case LE2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case LE1:
                    jj_consume_token(LE1);
                    break;
                case LE2:
                    jj_consume_token(LE2);
                    break;
                default:
                    jj_la1[15] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = LessThanOrEqualsOperator.SINGLETON;
                break;
            default:
                jj_la1[16] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            expression = AddExpression();
            if (operators == null) {
                operators = new ArrayList<>();
                expressions = new ArrayList<>();
            }
            operators.add(operator);
            expressions.add(expression);
        }
        if (operators != null) {
            return new BinaryOperatorExpression(startExpression, operators, expressions);
        } else {
            return startExpression;
        }
    }

    final public Expression AddExpression() throws ParseException {
        Expression startExpression;
        BinaryOperator operator;
        Expression expression;
        List<BinaryOperator> operators = null;
        List<Expression> expressions = null;
        startExpression = MultiplyExpression();
        label_6: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case PLUS:
            case MINUS:
                break;
            default:
                jj_la1[17] = jj_gen;
                break label_6;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case PLUS:
                jj_consume_token(PLUS);
                operator = PlusOperator.SINGLETON;
                break;
            case MINUS:
                jj_consume_token(MINUS);
                operator = MinusOperator.SINGLETON;
                break;
            default:
                jj_la1[18] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            expression = MultiplyExpression();
            if (operators == null) {
                operators = new ArrayList<>();
                expressions = new ArrayList<>();
            }
            operators.add(operator);
            expressions.add(expression);
        }
        if (operators != null) {
            return new BinaryOperatorExpression(startExpression, operators, expressions);
        } else {
            return startExpression;
        }
    }

    final public Expression MultiplyExpression() throws ParseException {
        Expression startExpression;
        BinaryOperator operator;
        Expression expression;
        List<BinaryOperator> operators = null;
        List<Expression> expressions = null;
        startExpression = UnaryExpression();
        label_7: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case MULTIPLY:
            case DIVIDE1:
            case DIVIDE2:
            case MODULUS1:
            case MODULUS2:
                break;
            default:
                jj_la1[19] = jj_gen;
                break label_7;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case MULTIPLY:
                jj_consume_token(MULTIPLY);
                operator = MultiplyOperator.SINGLETON;
                break;
            case DIVIDE1:
            case DIVIDE2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case DIVIDE1:
                    jj_consume_token(DIVIDE1);
                    break;
                case DIVIDE2:
                    jj_consume_token(DIVIDE2);
                    break;
                default:
                    jj_la1[20] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = DivideOperator.SINGLETON;
                break;
            case MODULUS1:
            case MODULUS2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case MODULUS1:
                    jj_consume_token(MODULUS1);
                    break;
                case MODULUS2:
                    jj_consume_token(MODULUS2);
                    break;
                default:
                    jj_la1[21] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = ModulusOperator.SINGLETON;
                break;
            default:
                jj_la1[22] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            expression = UnaryExpression();
            if (operators == null) {
                operators = new ArrayList<>();
                expressions = new ArrayList<>();
            }
            operators.add(operator);
            expressions.add(expression);
        }
        if (operators != null) {
            return new BinaryOperatorExpression(startExpression, operators, expressions);
        } else {
            return startExpression;
        }
    }

    final public Expression UnaryExpression() throws ParseException {
        Expression expression;
        UnaryOperator singleOperator = null;
        UnaryOperator operator;
        List<UnaryOperator> operators = null;
        label_8: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case MINUS:
            case NOT1:
            case NOT2:
            case EMPTY:
                break;
            default:
                jj_la1[23] = jj_gen;
                break label_8;
            }
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case NOT1:
            case NOT2:
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case NOT1:
                    jj_consume_token(NOT1);
                    break;
                case NOT2:
                    jj_consume_token(NOT2);
                    break;
                default:
                    jj_la1[24] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                operator = NotOperator.SINGLETON;
                break;
            case MINUS:
                jj_consume_token(MINUS);
                operator = UnaryMinusOperator.SINGLETON;
                break;
            case EMPTY:
                jj_consume_token(EMPTY);
                operator = EmptyOperator.SINGLETON;
                break;
            default:
                jj_la1[25] = jj_gen;
                jj_consume_token(-1);
                throw new ParseException();
            }
            if (singleOperator == null) {
                singleOperator = operator;
            } else if (operators == null) {
                operators = new ArrayList<>();
                operators.add(singleOperator);
                operators.add(operator);
            } else {
                operators.add(operator);
            }
        }
        expression = Value();
        if (operators != null) {
            return new UnaryOperatorExpression(null, operators, expression);
        } else if (singleOperator != null) {
            return new UnaryOperatorExpression(singleOperator, null, expression);
        } else {
            return expression;
        }
    }

    final public Expression Value() throws ParseException {
        Expression prefix;
        ValueSuffix suffix;
        List<ValueSuffix> suffixes = null;
        prefix = ValuePrefix();
        label_9: while (true) {
            switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
            case DOT:
            case LBRACKET:
                break;
            default:
                jj_la1[26] = jj_gen;
                break label_9;
            }
            suffix = ValueSuffix();
            if (suffixes == null) {
                suffixes = new ArrayList<>();
            }
            suffixes.add(suffix);
        }
        if (suffixes == null) {
            return prefix;
        } else {
            return new ComplexValue(prefix, suffixes);
        }
    }

    /**
     * This is an element that can start a value
     *
     */
    final public Expression ValuePrefix() throws ParseException {
        Expression ret;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case INTEGER_LITERAL:
        case FLOATING_POINT_LITERAL:
        case STRING_LITERAL:
        case TRUE:
        case FALSE:
        case NULL:
            ret = Literal();
            break;
        case LPAREN:
            jj_consume_token(LPAREN);
            ret = Expression();
            jj_consume_token(RPAREN);
            break;
        default:
            jj_la1[27] = jj_gen;
            if (jj_2_1(2147483647)) {
                ret = FunctionInvocation();
            } else {
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case IDENTIFIER:
                    ret = NamedValue();
                    break;
                default:
                    jj_la1[28] = jj_gen;
                    jj_consume_token(-1);
                    throw new ParseException();
                }
            }
        }
        return ret;
    }

    final public NamedValue NamedValue() throws ParseException {
        Token t = jj_consume_token(IDENTIFIER);
        return new NamedValue(t.image);
    }

    final public FunctionInvocation FunctionInvocation() throws ParseException {
        String qualifiedName;
        List<Expression> argumentList = new ArrayList<>();
        Expression exp;
        qualifiedName = QualifiedName();
        jj_consume_token(LPAREN);
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case INTEGER_LITERAL:
        case FLOATING_POINT_LITERAL:
        case STRING_LITERAL:
        case TRUE:
        case FALSE:
        case NULL:
        case LPAREN:
        case MINUS:
        case NOT1:
        case NOT2:
        case EMPTY:
        case IDENTIFIER:
            exp = Expression();
            argumentList.add(exp);
            label_10: while (true) {
                switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
                case COMMA:
                    break;
                default:
                    jj_la1[29] = jj_gen;
                    break label_10;
                }
                jj_consume_token(COMMA);
                exp = Expression();
                argumentList.add(exp);
            }
            break;
        default:
            jj_la1[30] = jj_gen;
            break;
        }
        jj_consume_token(RPAREN);
        String allowed = System.getProperty(ELParserConstants.SYSTEM_PROPERTY_ALLOW_FUNCTIONS);
        if (allowed == null || !allowed.equalsIgnoreCase("true")) {
            throw new ParseException("EL functions are not supported.");
        }
        return new FunctionInvocation(qualifiedName, argumentList);
    }

    final public ValueSuffix ValueSuffix() throws ParseException {
        ValueSuffix suffix;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case DOT:
            suffix = PropertySuffix();
            break;
        case LBRACKET:
            suffix = ArraySuffix();
            break;
        default:
            jj_la1[31] = jj_gen;
            jj_consume_token(-1);
            throw new ParseException();
        }
        return suffix;
    }

    final public PropertySuffix PropertySuffix() throws ParseException {
        jj_consume_token(DOT);
        String property = Identifier();
        return new PropertySuffix(property);
    }

    final public ArraySuffix ArraySuffix() throws ParseException {
        jj_consume_token(LBRACKET);
        Expression index = Expression();
        jj_consume_token(RBRACKET);
        return new ArraySuffix(index);
    }

    final public Literal Literal() throws ParseException {
        Literal ret;
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case TRUE:
        case FALSE:
            ret = BooleanLiteral();
            break;
        case INTEGER_LITERAL:
            ret = IntegerLiteral();
            break;
        case FLOATING_POINT_LITERAL:
            ret = FloatingPointLiteral();
            break;
        case STRING_LITERAL:
            ret = StringLiteral();
            break;
        case NULL:
            ret = NullLiteral();
            break;
        default:
            jj_la1[32] = jj_gen;
            jj_consume_token(-1);
            throw new ParseException();
        }
        return ret;
    }

    final public BooleanLiteral BooleanLiteral() throws ParseException {
        switch ((jj_ntk == -1) ? jj_ntk() : jj_ntk) {
        case TRUE:
            jj_consume_token(TRUE);
            return BooleanLiteral.TRUE;
        case FALSE:
            jj_consume_token(FALSE);
            return BooleanLiteral.FALSE;
        default:
            jj_la1[33] = jj_gen;
            jj_consume_token(-1);
            throw new ParseException();
        }
    }

    final public StringLiteral StringLiteral() throws ParseException {
        Token t = jj_consume_token(STRING_LITERAL);
        return StringLiteral.fromToken(t.image);
    }

    final public IntegerLiteral IntegerLiteral() throws ParseException {
        Token t = jj_consume_token(INTEGER_LITERAL);
        return new IntegerLiteral(t.image);
    }

    final public FloatingPointLiteral FloatingPointLiteral() throws ParseException {
        Token t = jj_consume_token(FLOATING_POINT_LITERAL);
        return new FloatingPointLiteral(t.image);
    }

    final public NullLiteral NullLiteral() throws ParseException {
        jj_consume_token(NULL);
        return NullLiteral.SINGLETON;
    }

    final public String Identifier() throws ParseException {
        Token t = jj_consume_token(IDENTIFIER);
        return t.image;
    }

    final public String QualifiedName() throws ParseException {
        String prefix = null, localPart = null;
        if (jj_2_2(2147483647)) {
            prefix = Identifier();
            jj_consume_token(COLON);
        }

        localPart = Identifier();
        if (prefix == null) {
            return localPart;
        } else {
            return prefix + ":" + localPart;
        }
    }

    private boolean jj_2_1(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        boolean retval = !jj_3_1();
        jj_save(0, xla);
        return retval;
    }

    private boolean jj_2_2(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        boolean retval = !jj_3_2();
        jj_save(1, xla);
        return retval;
    }

    private boolean jj_3R_13() {
        if (jj_3R_12()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(COLON)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3_2() {
        if (jj_3R_12()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(COLON)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3_1() {
        if (jj_3R_11()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_scan_token(LPAREN)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_12() {
        if (jj_scan_token(IDENTIFIER)) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    private boolean jj_3R_11() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3R_13()) {
            jj_scanpos = xsp;
        } else if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        if (jj_3R_12()) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            return false;
        }
        return false;
    }

    public ELParserTokenManager token_source;
    SimpleCharStream jj_input_stream;
    public Token token, jj_nt;
    private int jj_ntk;
    private Token jj_scanpos, jj_lastpos;
    private int jj_la;
    public boolean lookingAhead = false;
    private int jj_gen;
    final private int[] jj_la1 = new int[34];
    final private int[] jj_la1_0 = { 0x6, 0x6, 0x6, 0x0, 0x0, 0x0, 0x0, 0x18600000, 0x600000, 0x18000000, 0x18600000, 0x79e0000, 0x180000,
            0x60000, 0x6000000, 0x1800000, 0x79e0000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x10000, 0x20007580, 0x0, 0x80000000,
            0x20007580, 0x10000, 0x7580, 0x3000, };
    final private int[] jj_la1_1 = { 0x0, 0x0, 0x0, 0xc000, 0xc000, 0x3000, 0x3000, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x18,
            0x18, 0x3e0, 0xc0, 0x300, 0x3e0, 0x10c10, 0xc00, 0x10c10, 0x2, 0x0, 0x20000, 0x0, 0x30c10, 0x2, 0x0, 0x0, };
    final private JJCalls[] jj_2_rtns = new JJCalls[2];
    private boolean jj_rescan = false;
    private int jj_gc = 0;

    public ELParser(java.io.InputStream stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new ELParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 34; i++) {
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
        for (int i = 0; i < 34; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public ELParser(java.io.Reader stream) {
        jj_input_stream = new SimpleCharStream(stream, 1, 1);
        token_source = new ELParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 34; i++) {
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
        for (int i = 0; i < 34; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public ELParser(ELParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 34; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    public void ReInit(ELParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 34; i++) {
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

    private final Vector<int[]> jj_expentries = new Vector<>();
    private int[] jj_expentry;
    private int jj_kind = -1;
    private final int[] jj_lasttokens = new int[100];
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
        boolean[] la1tokens = new boolean[54];
        for (int i = 0; i < 54; i++) {
            la1tokens[i] = false;
        }
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 34; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & (1 << j)) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & (1 << j)) != 0) {
                        la1tokens[32 + j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 54; i++) {
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
        for (int i = 0; i < 2; i++) {
            JJCalls p = jj_2_rtns[i];
            do {
                if (p.gen > jj_gen) {
                    jj_la = p.arg;
                    jj_lastpos = jj_scanpos = p.first;
                    switch (i) {
                    case 0:
                        jj_3_1();
                        break;
                    case 1:
                        jj_3_2();
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
