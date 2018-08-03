/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.remotecallback.compiler;

import static androidx.remotecallback.compiler.RemoteCallbackProcessor.REMOTE_CALLABLE;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/**
 * Processes annotations from RemoteCallbacks.
 */
@SupportedAnnotationTypes({REMOTE_CALLABLE})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class RemoteCallbackProcessor extends AbstractProcessor {

    public static final String REMOTE_CALLABLE = "androidx.remotecallback.RemoteCallable";

    private HashMap<Element, CallbackReceiver> mMap = new HashMap<>();
    private ProcessingEnvironment mEnv;
    private Messager mMessager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        mEnv = processingEnvironment;
        mMessager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) return true;
        TypeElement remoteCallable = findAnnotation(set, REMOTE_CALLABLE);

        for (Element element : roundEnvironment.getElementsAnnotatedWith(remoteCallable)) {
            Element cls = findClass(element);
            mMap.computeIfAbsent(cls, (c) -> new CallbackReceiver(c, mEnv, mMessager)).addMethod(element);
        }
        for (CallbackReceiver receiver: mMap.values()) {
            receiver.finish(mEnv, mMessager);
        }
        return true;
    }

    private Element findClass(Element element) {
        if (element != null && element.getKind() != ElementKind.CLASS) {
            return findClass(element.getEnclosingElement());
        }
        return element;
    }

    private AnnotationMirror findAnnotationMirror(List<? extends AnnotationMirror> set,
            String name) {
        for (AnnotationMirror annotation : set) {
            if (String.valueOf(annotation.getAnnotationType()).equals(name)) {
                return annotation;
            }
        }
        return null;
    }

    private TypeElement findAnnotation(Set<? extends TypeElement> set, String name) {
        for (TypeElement typeElement : set) {
            if (String.valueOf(typeElement).equals(name)) {
                return typeElement;
            }
        }
        return null;
    }

    private void error(String error) {
        mMessager.printMessage(Diagnostic.Kind.ERROR, error);
    }
}
