/*
 * Copyright (c) 2022, 2022 Contributors to the Eclipse Foundation.
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
package org.glassfish.wasp.runtime;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.FunctionMapper;
import jakarta.el.ImportHandler;
import jakarta.el.VariableMapper;
import jakarta.servlet.jsp.el.NotFoundELResolver;

public class ELContextWrapper extends ELContext {

    private final ELContext wrapped;
    private final boolean errorOnELNotFound;

    public ELContextWrapper(ELContext wrapped, boolean errorOnELNotFound) {
        this.wrapped = wrapped;
        this.errorOnELNotFound = errorOnELNotFound;
    }

    ELContext getWrappedELContext() {
        return wrapped;
    }

    @Override
    public void setPropertyResolved(boolean resolved) {
        wrapped.setPropertyResolved(resolved);
    }

    @Override
    public void setPropertyResolved(Object base, Object property) {
        wrapped.setPropertyResolved(base, property);
    }

    @Override
    public boolean isPropertyResolved() {
        return wrapped.isPropertyResolved();
    }

    @Override
    public void putContext(Class<?> key, Object contextObject) {
        wrapped.putContext(key, contextObject);
    }

    @Override
    public Object getContext(Class<?> key) {
        if (key == NotFoundELResolver.class) {
            return errorOnELNotFound;
        }

        return wrapped.getContext(key);
    }

    @Override
    public ImportHandler getImportHandler() {
        return wrapped.getImportHandler();
    }

    @Override
    public Locale getLocale() {
        return wrapped.getLocale();
    }

    @Override
    public void setLocale(Locale locale) {
        wrapped.setLocale(locale);
    }

    @Override
    public void addEvaluationListener(EvaluationListener listener) {
        wrapped.addEvaluationListener(listener);
    }

    @Override
    public List<EvaluationListener> getEvaluationListeners() {
        return wrapped.getEvaluationListeners();
    }

    @Override
    public void notifyBeforeEvaluation(String expression) {
        wrapped.notifyBeforeEvaluation(expression);
    }

    @Override
    public void notifyAfterEvaluation(String expression) {
        wrapped.notifyAfterEvaluation(expression);
    }

    @Override
    public void notifyPropertyResolved(Object base, Object property) {
        wrapped.notifyPropertyResolved(base, property);
    }

    @Override
    public boolean isLambdaArgument(String name) {
        return wrapped.isLambdaArgument(name);
    }

    @Override
    public Object getLambdaArgument(String name) {
        return wrapped.getLambdaArgument(name);
    }

    @Override
    public void enterLambdaScope(Map<String, Object> arguments) {
        wrapped.enterLambdaScope(arguments);
    }

    @Override
    public void exitLambdaScope() {
        wrapped.exitLambdaScope();
    }

    @Override
    public <T> T convertToType(Object obj, Class<T> type) {
        return wrapped.convertToType(obj, type);
    }

    @Override
    public ELResolver getELResolver() {
        return wrapped.getELResolver();
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return wrapped.getFunctionMapper();
    }

    @Override
    public VariableMapper getVariableMapper() {
        return wrapped.getVariableMapper();
    }
}