/* $Id: AS3CodeFormats.java 19 2010-09-01 19:30:28Z cwallenpoole $
 *****************************************************************************
 * Copyright (c) 2009-2010 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    cwallenpoole
 *****************************************************************************
 */

package org.argouml.language.actionscript3.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.argouml.model.Model;
/**
 *
 * @author cwallenpoole
 */
public class AS3CodeFormats {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String INDENT = "\t";
    public static final Set<String> BASE_TYPES;
    static {
        Set<String> types = new HashSet<String>();
        types.add("ArgumentError");
        types.add("Array");
        types.add("Boolean");
        types.add("Class");
        types.add("Date");
        types.add("DefinitionError");
        types.add("Error");
        types.add("EvalError");
        types.add("Function");
        types.add("int");
        types.add("Math");
        types.add("Namespace");
        types.add("Number");
        types.add("Object");
        types.add("QName");
        types.add("RangeError");
        types.add("ReferenceError");
        types.add("RegExp");
        types.add("SecurityError");
        types.add("String");
        types.add("SyntaxError");
        types.add("TypeError");
        types.add("uint");
        types.add("URIError");
        types.add("Vector");
        types.add("VerifyError");
        types.add("XML");
        types.add("XMLList");
        BASE_TYPES = Collections.unmodifiableSet(types);
    }


    public static final Map<Object,String> classVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPublic(),"internal ");
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"internal ");
        classVisibility = Collections.unmodifiableMap(types);
    }
    public static final Map<Object,String> innerClassVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPublic(),"internal ");
        types.put(Model.getVisibilityKind().getPublic(),"internal ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"internal ");
        innerClassVisibility = Collections.unmodifiableMap(types);
    }
    public static final Map<Object,String> attributesVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"protected ");
        attributesVisibility = Collections.unmodifiableMap(types);
    }
    public static final Map<Object,String> functionVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"protected ");
        functionVisibility = Collections.unmodifiableMap(types);
    }
    
    public static final String classTemplate =
            "package {package}"+LINE_SEPARATOR+
            "{{imports}{mainClass}"+LINE_SEPARATOR+
            "}" + LINE_SEPARATOR +
            "{innerImports}{innerClass}"
            ;

    public static final String bodyClassTemplate =
        LINE_SEPARATOR+"{doc}{dynamic}{visibility}{final}{classType}{name}{extends}{implements}"+
        LINE_SEPARATOR+"{{attributes}{functions}" +
        LINE_SEPARATOR+"}"
            ;

    public static final String varTemplate =
        LINE_SEPARATOR+
        "{doc}{visibility}{static}{changeability}{name} : {type}{initValue};" +
        LINE_SEPARATOR;

    public static final String functionTemplate =
        LINE_SEPARATOR+"{doc}{override}{visibility}{static}{final}function {name}({arguments}) : {returnType}" +
        LINE_SEPARATOR+"{"+
        LINE_SEPARATOR+INDENT+"{code}{returnValue}" +
        LINE_SEPARATOR+"}" + LINE_SEPARATOR;
            ;
    public static final String functionInterfaceTemplate =
        LINE_SEPARATOR+"{doc}{static}{final}function {name}({arguments}): {returnType};"
        ;
    public static final String functionAgrumentTemplate =
            "{argumentSeparator}{name}{typeAssignment}{type}{initValue}"
            ;
    public static final String importTemplate =
        LINE_SEPARATOR+INDENT+"import {importPath}{ClassName}";

    public static final String packageElementTemplate =
        "{name}";

    public static final Map<String,String> languageDeffinitionToken;
    static {
        Map<String, String> types = new HashMap<String, String>();
        types.put("dynamic","dynamic ");
        types.put("extends","extends ");
        types.put("implements","implements ");
        types.put("interface","interface ");
        types.put("class","class ");
        types.put("property","var ");
        types.put("constant","const ");
        types.put("override","override ");
        types.put("final","final ");
        types.put("static","static ");
        types.put("argumentSeparator",", ");
        types.put("packageClassSeparator",".");
        types.put("defaultType","*");
        types.put("fileExtension",".as");
        types.put("attributeAssignment"," = ");
        types.put("functionNoReturnType","void");
        types.put("parameterAssignment"," = ");
        types.put("typeAssignment"," : ");
        types.put("defaultType","*");
        languageDeffinitionToken = Collections.unmodifiableMap(types);
    }

    public static final String classDocTemplate =
        " * @langversion\tActionScript 3.0"+LINE_SEPARATOR+
        " * @playerversion\tFlash 10"+LINE_SEPARATOR+
        "{author}{version}{see}{since}{deprecated}{example}";

    public static final String fieldDocTemplate =
        "{see}{since}{deprecated}{default}";

    public static final String methodDocTemplate =
        "{inheritDoc}{param}{return}{throws}{see}{since}{deprecated}{example}";



    public static final String docTemplate =
            "/**" +LINE_SEPARATOR+
            "{doc}"+
            " *"+LINE_SEPARATOR+
            "{tags}"+
            " */"+LINE_SEPARATOR
            ;
    public static final String tagTemplate=" * {tagName}\t{tagValue}{description}";

    public static final String returnTemplate= "var ret:{type} = null\n\treturn ret";
}
