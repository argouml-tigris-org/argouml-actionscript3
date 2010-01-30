/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2010 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Romain Flacher    
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2004-2008 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.language.actionscript3.generator;

import static org.argouml.model.Model.getFacade;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.argouml.application.api.Argo;
import org.argouml.configuration.Configuration;
import org.argouml.language.actionscript3.template.SourceTemplate;
import org.argouml.model.Model;
import org.argouml.uml.generator.CodeGenerator;
import org.argouml.uml.generator.SourceUnit;
import org.argouml.uml.generator.TempFileUtils;

/**
 * Generator for ActionScript3.
 *
 * @author Flacher Romain
 *
 */
public class GeneratorAS3 implements CodeGenerator {
    private static final String LINE_SEPARATOR =
        System.getProperty("line.separator");
    protected static final String INDENT = "\t";
    
    private static final Set<String> BASE_TYPES;
    static {
        Set<String> types = new HashSet<String>();
        types.add("Number");
        types.add("Boolean");
        types.add("String");
        types.add("uint");
        types.add("int");
        types.add("Array");
        types.add("Vector");
        types.add("Object");
        types.add("XML");
        types.add("XMLList");
        BASE_TYPES = Collections.unmodifiableSet(types);
    }
    
    
    private static final Map<Object,String> classVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPrivate(),"internal ");
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"internal ");
        classVisibility = Collections.unmodifiableMap(types);
    }
    private static final Map<Object,String> innerClassVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPrivate(),"internal ");
        types.put(Model.getVisibilityKind().getPublic(),"internal ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"internal ");
        innerClassVisibility = Collections.unmodifiableMap(types);
    }
    private static final Map<Object,String> attributesVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPrivate(),"private ");
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"protected ");
        attributesVisibility = Collections.unmodifiableMap(types);
    }
    private static final Map<Object,String> functionVisibility;
    static {
        Map<Object, String> types = new HashMap<Object, String>();
        types.put(Model.getVisibilityKind().getPrivate(),"private ");
        types.put(Model.getVisibilityKind().getPublic(),"public ");
        types.put(Model.getVisibilityKind().getPackage(),"internal ");
        types.put(Model.getVisibilityKind().getProtected(),"protected ");
        functionVisibility = Collections.unmodifiableMap(types);
    }
    private  Set<Object> importSet;
    private  Map<String, Object> FunctionGeneralisationMap;
    
  
    
    private  String classTemplate = 
            "package {package}"+LINE_SEPARATOR+
            "{{imports}{mainClass}"+LINE_SEPARATOR+
            "}{innerImports}{innerClass}"        
            ;
    
    private  String bodyClassTemplate =  
        LINE_SEPARATOR+"{doc}{dynamic}{visibility}{final}{classType}{name}{extends}{implements}"+
        LINE_SEPARATOR+"{{attributes}{functions}" +
        LINE_SEPARATOR+"}"
            ;
    
    private  String varTemplate =LINE_SEPARATOR+"{doc}{visibility}{static}{changeability}{name} : {type}{initValue};";
    
    private  String functionTemplate =
        LINE_SEPARATOR+"{doc}{override}{visibility}{static}{final}function {name}({arguments}) : {returnType}" +
        LINE_SEPARATOR+"{{code}{returnValue}" +
        LINE_SEPARATOR+"};"
            ;
    private  String functionInterfaceTemplate =
        LINE_SEPARATOR+"{doc}{static}{final}function {name}({arguments}) : {returnType};"
        ;
    private  String functionAgrumentTemplate =
            "{argumentSeparator}{name}{typeAssignment}{type}{initValue}"
            ;
    private  String importTemplate =
        LINE_SEPARATOR+INDENT+"import {importPath}{ClassName}";
    
    private  String packageElementTemplate =
        "{name}";
    
    private static final Map<String,String> languageDeffinitionToken;
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
        types.put("FunctionNoReturnType","Void");
        types.put("parameterAssignment"," = ");
        types.put("typeAssignment"," : ");
        types.put("defaultType","*");
        languageDeffinitionToken = Collections.unmodifiableMap(types);
    }
    private static final Map<String,String> JavaDocToken;
    static {
        Map<String, String> types = new HashMap<String, String>();
        types.put("param","@param ");
        types.put("return","@return ");
        types.put("author","@author ");
        types.put("version","@version ");
        types.put("see","@see ");
        types.put("since","@since ");
        types.put("deprecated","@deprecated ");
        
        types.put("default","@default ");
        types.put("example","@example ");
        types.put("link","@link ");
        types.put("inheritDoc","@inheritDoc ");
        
        /* @param @return @throws @exception @author @version @see @since @serial @serialField @serialData @deprecated {@link}*/
        JavaDocToken = Collections.unmodifiableMap(types);
    }
    private String classDocTemplate =
        " * @langversion\tActionScript 3.0"+LINE_SEPARATOR+
        " * @playerversion\tFlash 10"+LINE_SEPARATOR+
        "{author}{version}{see}{since}{deprecated}{example}";
        
        private String fieldDocTemplate =
        "{see}{since}{deprecated}{default}";

        private String methodDocTemplate =
        "{inheritDoc}{param}{return}{throws}{see}{since}{deprecated}{example}";

            
            
    private  String docTemplate =
            "/**" +LINE_SEPARATOR+
            "{doc}"+
            " *"+LINE_SEPARATOR+
            "{tags}"+
            " */"+LINE_SEPARATOR
            ;
    private  String tagTemplate=" * {tagName}\t{tagValue}{description}";
   
    private  String returnTemplate= "return new {type}()";
    
    private List<Object> functionRealizationList;

    private Object element;


    public GeneratorAS3() {

    } 
    
    /**  
     * Generate code for the specified classifiers. If generation of
     * dependencies is requested, then every file the specified elements
     * depends on is generated too (e.g. if the class MyClass has an attribute
     * of type OtherClass, then files for OtherClass are generated too).
     *
     * @param elements the UML model elements to generate code for.
     * @param deps Recursively generate dependency files too.
     * @return A collection of {@link SourceUnit} objects. The collection
     *         may be empty if no file is generated.
     * @see org.argouml.uml.generator.CodeGenerator#generate(java.util.Collection, boolean)
     */
    public Collection<SourceUnit> generate(Collection elements, boolean deps) {
        File tmpdir = null;
        try {
            tmpdir = TempFileUtils.createTempDir();
            if (tmpdir != null) {
                generateFiles(elements, tmpdir.getPath(), deps);
                return TempFileUtils.readAllFiles(tmpdir);
            }
            return Collections.EMPTY_LIST;
        } finally {
            if (tmpdir != null) {
                TempFileUtils.deleteDir(tmpdir);
            }
           
        }
    }

    /**
     * Returns a list of files that will be generated from the specified
     * model elements.
     * TODO: 'deps' is ignored here
     * 
     * @see #generate(Collection, boolean)
     * @param elements the UML model elements to generate code for.
     * @param deps Recursively generate dependency files too.
     * @return The filenames (with relative path) as a collection of Strings.
     *         The collection may be empty if no file will be generated.
     * @see org.argouml.uml.generator.CodeGenerator#generateFileList(java.util.Collection, boolean)
     */
    public Collection<String> generateFileList(Collection elements, boolean deps) {

        Collection<String> files=new ArrayList<String>();
        for (Object element : elements) {
            files.add(Model.getFacade().getName(element)+languageDeffinitionToken.get("fileExtension"));
        }
        return elements;
    }


    /**
     * Generate files for the specified classifiers.
     * TODO: 'deps' is ignored 
     * 
     * @see #generate(Collection, boolean)
     * @param elements the UML model elements to generate code for.
     * @param path The source base path.
     * @param deps Recursively generate dependency files too.
     * @return The filenames (with relative path) as a collection of Strings.
     *         The collection may be empty if no file will be generated.
     * @see org.argouml.uml.generator.CodeGenerator#generateFiles(java.util.Collection, java.lang.String, boolean)
     */
    public Collection<String> generateFiles(Collection elements, String path, boolean deps)
    {

        for (Object element : elements) {
            String file= generateFile(element);
            
            //***************write the tmp file
            String pathname = path;
            if (!path.endsWith(FILE_SEPARATOR)) {
                pathname+=FILE_SEPARATOR;
            }
            pathname+=Model.getFacade().getName(element)+languageDeffinitionToken.get("fileExtension");
           
            File f = new File(pathname);
       
            BufferedWriter fos = null;
            try {
                if (Configuration.getString(Argo.KEY_INPUT_SOURCE_ENCODING) == null
                    || Configuration.getString(Argo.KEY_INPUT_SOURCE_ENCODING)
                        .trim().equals("")) {
                    fos =
                        new BufferedWriter(
                                new OutputStreamWriter(new FileOutputStream(f),
                                        System.getProperty("file.encoding")));
                } else {
                    fos =
                        new BufferedWriter(
                                new OutputStreamWriter(new FileOutputStream(f),
                                        Configuration.getString(
                                                Argo.KEY_INPUT_SOURCE_ENCODING)));
                }
                fos.write(file);

            } catch (IOException exp) {
                //LOG.error("IO Exception: " + exp + ", for file: " + f.getPath());
            } finally {
                
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException exp) {
                    //LOG.error("FAILED: " + f.getPath());
                }
            }
            
        }
        return TempFileUtils.readFileNames(new File(path));
    }
    private String generateFile(Object element)
    {
        
        SourceTemplate classtpl = new SourceTemplate(classTemplate);
     
        importSet  = new HashSet<Object>();
        this.element = element;
       
        //****************** generate package path
        classtpl.putToken("package", getPackagePath(element));
        
        //**************** generate class body
        classtpl.putToken("mainClass", indentLines(INDENT,generateBody(element)));

        //****************** generate imports for main class
        SourceTemplate importtpl = new SourceTemplate(importTemplate);
        classtpl.appendToken("imports","");
        for(Object obj : importSet)
        {
            importtpl.clearTokens();
            importtpl.putToken("importPath",getClassPath(obj));
            importtpl.putToken("ClassName",Model.getFacade().getName(obj));
            classtpl.appendToken("imports",importtpl.parse(true));
        }
        
        
        //clear import list to get it ready for import of inner class
        importSet  = new HashSet<Object>();
        
        //**************** generate inner class body
        Collection<?> inClass = Model.getFacade().getOwnedElements(element);
        classtpl.putToken("innerClass","");
        for (Object aclass : inClass) {
            if (Model.getFacade().isAClass(aclass)
                || Model.getFacade().isAInterface(aclass))
            {

                classtpl.appendToken("innerClass",generateBody(aclass));
            }
        }
        
        //****************** generate imports for all inner class
        classtpl.putToken("innerImports", "");
        for(Object obj : importSet)
        {
            importtpl.clearTokens();
            importtpl.putToken("importPath",getClassPath(obj));
            importtpl.putToken("ClassName",Model.getFacade().getName(obj));
            classtpl.appendToken("innerImports", importtpl.parse(true));
        }
        
        return classtpl.parse();
    }

    private String generateBody(Object element)
    {
           
        functionRealizationList = new ArrayList<Object>();
        FunctionGeneralisationMap = new HashMap<String, Object>();  
        
        SourceTemplate bodytpl = new SourceTemplate(bodyClassTemplate);
        
        
        if (Model.getFacade().isAClass(element))
        {
            
            bodytpl.putToken("classType", languageDeffinitionToken.get("class"));
            //******************* Generate class implement interface
            String sImplements = "";
            Collection<?> realizations = Model.getFacade().getSpecifications(element);
            if(!realizations.isEmpty())
            {
                sImplements = " implements ";
                Iterator<?> iterator = realizations.iterator();
                while(iterator.hasNext())
                {
                    Object realization = iterator.next();
                    functionRealizationList.addAll(Model.getFacade().getOperations(realization));
                    checkImport(realization, element);
                    
                    sImplements += Model.getFacade().getName(realization);
                    if(iterator.hasNext())
                        sImplements +=" ,";
                }
            }
            bodytpl.putToken("implements", sImplements);
            
        } else if (Model.getFacade().isAInterface(element)) {
            bodytpl.putToken("classType",languageDeffinitionToken.get("interface"));
            bodytpl.putToken("implements", "");
        } else {
            return ""; // actors, use cases etc.
        }
        
        boolean isInnerClass = (Model.getFacade().isAClass(Model.getFacade().getNamespace(element)));
        
        //javadoc
        SourceTemplate  classDoc = getDoc(element);
        bodytpl.putToken("doc",classDoc.parse(true));
        //is it a dynamic class
        bodytpl.putToken("dynamic", "");
        
        //is it a final class
        bodytpl.putToken("final", (Model.getFacade().isLeaf(element))? languageDeffinitionToken.get("final"):"");
        
        // class name
        bodytpl.putToken("name", Model.getFacade().getName(element));
        // class visibility
        if(isInnerClass)
        {
            bodytpl.putToken("visibility", innerClassVisibility.get(Model.getFacade().getVisibility(element)));
        }else
        {
            bodytpl.putToken("visibility", classVisibility.get(Model.getFacade().getVisibility(element)));
        }
        
        
        
        //********************* generate class extends 
        String sExtends = "";
        Collection<?> generalizations = Model.getFacade().getGeneralizations(element);
        if(!generalizations.isEmpty())
        {
           Object generalization = Model.getFacade().getGeneral(generalizations.iterator().next());
           checkImport(generalization, element);
            sExtends = languageDeffinitionToken.get("extends")+Model.getFacade().getName(generalization);
            
            Collection<?> operations=Model.getFacade().getOperations(generalization);
            for(Object op:operations)
            {
                FunctionGeneralisationMap.put(Model.getFacade().getName(op),op);
            }
        }
        bodytpl.putToken("extends", sExtends);

        //****************generate attributes

        Collection<?> sFeatures =  Model.getFacade().getStructuralFeatures(element);
        for(Object structuralFeature : sFeatures)
        {
         
            SourceTemplate attribTpl= getAttribute(structuralFeature);
  
            //**attribute assignment
            Object init = Model.getFacade().getInitialValue(structuralFeature);
            String initValue = "";
            if (Model.getFacade().isAExpression(init))
            {
                initValue =  Model.getFacade().getBody(init).toString();
              
            }else if (Model.getFacade().isAConstraint(init))
            {
                while((init=Model.getDataTypesHelper().getBody(init))!="")
                {
                    //TODO: need to be tested
                    if (Model.getFacade().isAExpression(init))
                        initValue =  Model.getFacade().getBody(init).toString();
                }
            }
            
            //**
            if(initValue!="")
            {
                attribTpl.putToken("initValue",languageDeffinitionToken.get("attributeAssignment")+initValue );
                attribTpl.putToken("doc:tags:default",getTagDoc("default",initValue)+LINE_SEPARATOR);
                
            }

            bodytpl.appendToken("attributes", indentLines(INDENT,attribTpl.parse(true)));
           
        }
        
       
            
      //****************generate attributes from association
      
        Collection<?> ends = Model.getFacade().getAssociationEnds(element);

        if(Model.getFacade().isAAssociationClass(element))
        {
            ends.addAll(  getFacade().getConnections(element));
        }
        Collection<Object> listedEnds = new ArrayList<Object>();
        for(Object obj : ends)
        {   

            Object association = Model.getFacade().getAssociation(obj);

            Collection<?> connections = Model.getFacade().getOtherAssociationEnds(obj);
            
            //avoid double attribute for recursive association
            if (!listedEnds.contains(obj)|| Model.getFacade().isAAssociationClass(element))
            {

                for (Object associationEnd2 : connections)
                {
                    
                    if((Model.getFacade().isNavigable(associationEnd2)&&!Model.getFacade().isAbstract(association))
                            ||(Model.getFacade().isAAssociationClass(element)&&!Model.getFacade().isAbstract(association)))
                    {
                        SourceTemplate attribTpl= getAttribute(associationEnd2);
                        bodytpl.appendToken("attributes", indentLines(INDENT,attribTpl.parse(true)));

                    }
                    listedEnds.add(associationEnd2);
                } 

            }


        }

        
        //********generate function body
        
        bodytpl.appendToken("functions", "");
        functionRealizationList.addAll(Model.getFacade().getOperations(element));
        for (Object behavioralFeature : functionRealizationList)
        {
            SourceTemplate fctTpl;
            if (Model.getFacade().isAClass(element))
                fctTpl= new SourceTemplate(functionTemplate);
            else
                fctTpl = new SourceTemplate(functionInterfaceTemplate);
            
            if( FunctionGeneralisationMap.containsKey(Model.getFacade().getName(behavioralFeature)))
            {
                fctTpl.putToken("override",languageDeffinitionToken.get("override"));
                behavioralFeature=FunctionGeneralisationMap.get(Model.getFacade().getName(behavioralFeature));
            }
            fctTpl.putToken("visibility",functionVisibility.get(Model.getFacade().getVisibility(behavioralFeature)));
           
            if (Model.getFacade().isStatic(behavioralFeature))
            {
                fctTpl.putToken("static",languageDeffinitionToken.get("static"));
            }
            if(Model.getFacade().isLeaf(behavioralFeature))
                fctTpl.putToken("final", "final ");
            
            fctTpl.putToken("name",Model.getFacade().getName(behavioralFeature));
             
            fctTpl.putToken("returnType",languageDeffinitionToken.get("FunctionNoReturnType"));

            Collection<?> params = Model.getFacade().getParameters(behavioralFeature);
            Boolean isNotFirst=false;
            for(Object param : params)
            {
                SourceTemplate argTpl= new SourceTemplate(functionAgrumentTemplate);
                Object paramType = Model.getFacade().getType(param);
                
                if(! Model.getFacade().isReturn(param))
               {
                  // "{argumentSeparator}{name} : {type}{initValue}"
                   if(isNotFirst)
                   {
                       argTpl.putToken("argumentSeparator",languageDeffinitionToken.get("argumentSeparator"));
                       
                   }else{
                       argTpl.putToken("argumentSeparator","");
                       isNotFirst =true;
                   }
                   argTpl.putToken("name",Model.getFacade().getName(param));
                   argTpl.putToken("typeAssignment",languageDeffinitionToken.get("typeAssignment"));
                   argTpl.putToken("type",languageDeffinitionToken.get("defaultType"));
                   if (paramType!=null)
                       argTpl.putToken("type",Model.getFacade().getName(paramType));
                   Object init = Model.getFacade().getDefaultValue(param);
                   if (Model.getFacade().isAExpression(init))
                       argTpl.putToken("initValue",languageDeffinitionToken.get("parameterAssignment")+Model.getFacade().getBody(init));
                   else
                       argTpl.putToken("initValue","");
                   fctTpl.appendToken("arguments", argTpl.parse());   
                   
               }else
               {
                   if (paramType!=null)
                   {
                       fctTpl.putToken("returnType",Model.getFacade().getName(paramType));
                       SourceTemplate rtnTpl = new SourceTemplate(returnTemplate);
                       rtnTpl.putToken("type", Model.getFacade().getName(paramType));
                       fctTpl.putToken("returnValue",rtnTpl);
                   }
                   
               }
               checkImport(paramType, element);
               
            }
            for (Object m : Model.getFacade().getMethods(behavioralFeature))
            {
                if (Model.getFacade().getBody(m) != null)
                {
                    fctTpl.appendToken("code",LINE_SEPARATOR+indentLines(INDENT,((String) Model.getFacade().getBody(Model.getFacade().getBody(m)))));
                }       
            }
                
            
            bodytpl.appendToken("functions", indentLines(INDENT,fctTpl.parse()));
        }
        return bodytpl.parse();
    }
    
    /**
     * @param attrib AssociationEnd or StructuralFeature to generate the source code for
     * @return a SourceTemplate of the attribute without initValue
     */
    private SourceTemplate getAttribute(Object attrib)
    {
        SourceTemplate attribTpl= new SourceTemplate(varTemplate);
        
        attribTpl.putToken("visibility", attributesVisibility.get(Model.getFacade().getVisibility(attrib)));
        
        
             
        if (Model.getFacade().isReadOnly(attrib)) {
            attribTpl.putToken("changeability",languageDeffinitionToken.get("constant"));
        } else {
            attribTpl.putToken("changeability",languageDeffinitionToken.get("property"));
        }
        
        String type;
        int upperMultiplicity;
        String defaultName;
        Object attribType;
        if(Model.getFacade().isAAssociationEnd(attrib))
        {

            if(Model.getFacade().isAAssociationClass(Model.getFacade().getAssociation(attrib)))
            {//this is an association class association
                if(Model.getFacade().isAAssociationClass(element))
                {//we are parsing the association class
                    attribType = Model.getFacade().getType(attrib);
                    defaultName = "my" + Model.getFacade().getName(attribType);
                }else
                {//we are parsing the class linked with the association class
                    //so the type have to be the AssociationClass herself
                    attribType = Model.getFacade().getAssociation(attrib);
                    defaultName = "my" + Model.getFacade().getName(attribType);
                }
            }else //this is a non class association
            {
                attribType = Model.getFacade().getType(attrib);
                defaultName = Model.getFacade().getName(Model.getFacade().getAssociation(attrib));
                if(defaultName == null || defaultName.length() == 0)
                    defaultName = "my" + Model.getFacade().getName(attribType);
            }

            String AssociationEndName = Model.getFacade().getName(attrib); 
            if(AssociationEndName != null && !(AssociationEndName.length() == 0))
            {
                attribTpl.putToken("name",AssociationEndName);
            }else 
            {
                attribTpl.putToken("name",defaultName);
            }
            
            upperMultiplicity = Model.getFacade().getUpper(attrib);


        }else //this is a StructuralFeature
        {
            //throw error with AssociationEnd ... 
            if (Model.getFacade().isStatic(attrib))
                attribTpl.putToken("static",languageDeffinitionToken.get("static"));
           
            attribType = Model.getFacade().getType(attrib);
            upperMultiplicity = Model.getFacade().getUpper(Model.getFacade().getMultiplicity(attrib));
            attribTpl.putToken("name", Model.getFacade().getName(attrib));
        } 


        type = Model.getFacade().getName(attribType);
        if ( upperMultiplicity == 1) {
            attribTpl.putToken("type",type);
        } else {
            attribTpl.putToken("type","Vector.<"+type+">");
        }
        
        attribTpl.putToken("doc", getDoc(attrib));
        
        checkImport(attribType, element);
        
        return attribTpl;
    }
    
    
    /**
     * @param element
     * @return
     */
    private String getClassPath(Object element)
    {
        String path=getPackagePath(element);
        return path == "" ? path : path+languageDeffinitionToken.get("packageClassSeparator");
    }
       private String getPackagePath(Object element)
       {
           SourceTemplate elementTpl= new SourceTemplate(packageElementTemplate);
           
           if(element==null) return "";
           
           Object packageElement = element;
          
           boolean first= true;
           while ((packageElement = Model.getFacade().getNamespace(packageElement))
                   != null)
           {
               // Omit root package name; it's the model's root
               if (Model.getFacade().getNamespace(packageElement) != null)
               {
                   if (first)
                       elementTpl.putToken("name", Model.getFacade().getName(packageElement));
                   else
                       elementTpl.prependToken("name", Model.getFacade().getName(packageElement)+languageDeffinitionToken.get("packageSeparator"));
               }
               first = false;
           }
           
           return elementTpl.parse(true);
       }
       
       private void checkImport(Object obj, Object obj2)
       {
           if(obj!=null&&
                   obj2!=null&&
                   Model.getFacade().getNamespace(obj)!=Model.getFacade().getNamespace(obj2)&&
                   !BASE_TYPES.contains(Model.getFacade().getName(obj)))
               importSet.add(obj);
       }

       public String indentLines(String indent,String txt)
       {
           if(txt.length()>0)
               return indent+(txt.replace("\r", "").replace("\n", LINE_SEPARATOR).replace(LINE_SEPARATOR, LINE_SEPARATOR+indent));
           else
               return "";
       }
       public String indentLines(String txt)
       {
           return indentLines("",txt);
       }
    private SourceTemplate getDoc(Object obj)
    {
        SourceTemplate docTpl = new SourceTemplate(docTemplate);
        
        // base documentation
        String doc= indentLines(" *\t",getFacade().getTaggedValueValue(obj, Argo.DOCUMENTATION_TAG));
        if(doc != "")
        {
            doc= doc+LINE_SEPARATOR;
            docTpl.putToken("doc", doc);
        }
        SourceTemplate innerTpl;
        
        if (Model.getFacade().isAClass(obj) || Model.getFacade().isAInterface(obj))
        {
            innerTpl = new SourceTemplate(this.classDocTemplate);
        }
        else if (Model.getFacade().isAOperation(obj)) 
        {
            innerTpl = new SourceTemplate(this.methodDocTemplate);
        }
        else if (Model.getFacade().isAAttribute(obj) || Model.getFacade().isAAssociationEnd(obj))
        {
            innerTpl = new SourceTemplate(this.fieldDocTemplate);
        }else{
            innerTpl = new SourceTemplate();
        }
        
        //Commune agroUML documentation
        Collection<?> tagsValue = getFacade().getTaggedValuesCollection(obj);
        
        for(Object tv : tagsValue)
        {
            String tName = getFacade().getTagOfTag(tv);
            if(JavaDocToken.containsKey(tName))
            {
                innerTpl.putToken(tName,getTagDoc(tName, getFacade().getValueOfTag(tv))+LINE_SEPARATOR);
            }
        }
        docTpl.putToken("tags", innerTpl);
        return docTpl;
    }
    
    private String getTagDoc(String tagName, String tagValue,String description)
    {
        SourceTemplate docTpl = new SourceTemplate(tagTemplate);
        tagName = tagName.startsWith("@")? tagName : JavaDocToken.get(tagName);
        docTpl.putToken("tagName", tagName);
        docTpl.putToken("tagValue", tagValue);
        docTpl.putToken("description", description);
        return docTpl.parse(true);
    }
    
    private String getTagDoc(String tagName, String tagValue)
    {
        return getTagDoc(tagName, tagValue,"");
    }
   




}
