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

import org.argouml.model.Facade;

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

import org.apache.log4j.Logger;
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
    private static final Logger LOG = Logger.getLogger(GeneratorAS3.class);
    protected Map<String, Object> functionGeneralisationMap;
    private List<Object> functionRealizationList;

    private Facade facade;

    public GeneratorAS3() {
        facade = Model.getFacade();
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
            if( tmpdir != null )
            {
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
            files.add(
                        facade.getName(element)+
                        AS3CodeFormats.languageDeffinitionToken.get("fileExtension")
                    );
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
        writeElementsToPath( elements, path );
        Collection<String> files = TempFileUtils.readFileNames(new File(path));
        return files;
    }

    protected void appendImports( SourceTemplate classTpl, HashSet imports, String importType )
    {
        classTpl.appendToken( importType, "" );
        SourceTemplate importTpl = new SourceTemplate( AS3CodeFormats.importTemplate );
        for(Object obj : imports )
        {
            LOG.error( obj.getClass() + " => " + obj );
            importTpl.clearTokens();
            importTpl.putToken("importPath",getClassPath(obj));
            importTpl.putToken("ClassName",facade.getName(obj));
            classTpl.appendToken( importType, importTpl.parse() );
        }        
    }

    protected void appendInnerImports( SourceTemplate classTpl, Object element )
    {
        appendImports( classTpl, getInnerImports( element ), "innerImports" );
    }
    protected void appendClassImports( SourceTemplate classTpl, Object element )
    {
        appendImports( classTpl, getClassImports( element ), "imports" );
    }
    
    /**
     * Generates the contents of a class/interface file based on
     *
     * @param element   Whatever is passed in as an item in generate
     * @return          The contents of the file.
     */
    private String getFileContents( Object element )
    {        
        SourceTemplate classTpl = new SourceTemplate( AS3CodeFormats.classTemplate );
        addMainClassToTemplate( classTpl, element );
        addPrivateClassesToTemplate( classTpl, element );
        return classTpl.parse();
    }

    protected void addMainClassToTemplate( SourceTemplate classTpl, Object publicClass )
    {
        //****************** generate package path
        classTpl.putToken("package", getPackagePath( publicClass ) );
        //**************** generate class body
        classTpl.putToken("mainClass", indentLines( generateBody(publicClass), AS3CodeFormats.INDENT ));

        //****************** generate imports for main class
        appendClassImports( classTpl, publicClass );
    }

    protected void addPrivateClassesToTemplate( SourceTemplate classTpl, Object privateClass )
    {
        //**************** generate inner class body
        Collection<?> inClass = facade.getOwnedElements(privateClass);
        for (Object klass : inClass)
        {
            if (facade.isAClass(klass) || facade.isAInterface(klass))
            {
                classTpl.appendToken("innerClass", generateBody( klass ) );
            }
        }

        //****************** generate imports for all inner class
        appendInnerImports( classTpl, privateClass );
    }


    /**
     * Creates a list of imports for the class provided.
     *
     * @param klass The class to get imports for.
     * @return  The list of imports
     */
    private HashSet getClassImports( Object klass )
    {
        HashSet imports = new HashSet<Object>();
        Collection<?> realizations = facade.getSpecifications( klass );
        if(!realizations.isEmpty())
        {
            Iterator<?> iterator = realizations.iterator();
            while(iterator.hasNext())
            {
                Object realization = iterator.next();
                if( elementNeedsImport( realization, klass ) )
                {
                    imports.add( realization );
                }
            }
        }
        return imports;
    }

    protected HashSet getInnerImports( Object element )
    {
        HashSet imports = new HashSet<Object>();
        for( Object klass : facade.getOwnedElements(element) )
        {
            if (facade.isAClass(klass) || facade.isAInterface(klass))
            {
               imports.addAll( getClassImports( klass ) );
            }
        }
        return imports;
    }

    /**
     * Determines whether an object is differentiated from its context and is
     * not a base class.
     *
     * @param test      The object to test
     * @param context   The context to compare it to
     * @return  Whether the test element is unique enough for import
     */
    protected boolean elementNeedsImport( Object test, Object context )
    {
        return ( test       !=null &&
                 context    !=null &&
                 facade.getNamespace( test ) != facade.getNamespace( context )&&
                 !AS3CodeFormats.BASE_TYPES.contains( facade.getName( test ) ) );
    }

    protected void addImplementations( SourceTemplate bodyTpl, Object element )
    {
        String sImplements = "";
        Collection<?> realizations = facade.getSpecifications( element );
        if(!realizations.isEmpty())
        {
            sImplements = " implements ";
            Iterator<?> iterator = realizations.iterator();
            while(iterator.hasNext())
            {
                Object implemented = iterator.next();
                sImplements += facade.getName(implemented);
                if(iterator.hasNext()) sImplements += AS3CodeFormats.languageDeffinitionToken.get( "argumentSeparator" );
            }
        }
        bodyTpl.putToken("implements", sImplements);
    }

    protected String generateBody(Object klass)
    {
        functionRealizationList = new ArrayList<Object>();
        functionGeneralisationMap = new HashMap<String, Object>();

        SourceTemplate bodyTpl = new SourceTemplate(AS3CodeFormats.bodyClassTemplate);

        String classType =  AS3CodeFormats.languageDeffinitionToken.get("class");        
        if (facade.isAInterface(klass)) {
            classType = AS3CodeFormats.languageDeffinitionToken.get("interface");
        } else if ( !facade.isAClass(klass) ) {
            return ""; // actors, use cases etc.
        }
        
        bodyTpl.putToken("classType",classType);
        addImplementations( bodyTpl, klass );

        //javadoc
        bodyTpl.putToken("doc", getDoc(klass).parse() );
        //is it a dynamic class
        bodyTpl.putToken( "dynamic", "" );

        //is it a final class
        bodyTpl.putToken("final", (facade.isLeaf(klass))? AS3CodeFormats.languageDeffinitionToken.get("final"):"");

        // class name
        bodyTpl.putToken("name", facade.getName(klass));
        // class visibility
        Map<Object, String> source = ( facade.isAClass(facade.getNamespace(klass)) )?AS3CodeFormats.innerClassVisibility: AS3CodeFormats.classVisibility;
        bodyTpl.putToken("visibility", source.get(facade.getVisibility(klass)));



        //********************* generate class extends

        bodyTpl.putToken("extends", getExtends( klass ));

        //****************generate attributes

        Collection<?> sFeatures =  facade.getStructuralFeatures(klass);
        for(Object structuralFeature : sFeatures)
        {
            SourceTemplate attribTpl= getAttributes(structuralFeature, klass);

            //**attribute assignment
            Object init = facade.getInitialValue(structuralFeature);
            String initValue = "";
            if (facade.isAExpression(init))
            {
                initValue =  facade.getBody(init).toString();

            }else if (facade.isAConstraint(init))
            {
                while((init=Model.getDataTypesHelper().getBody(init))!="")
                {
                    //TODO: need to be tested
                    if (facade.isAExpression(init))
                        initValue =  facade.getBody(init).toString();
                }
            }

            //**
            if( !initValue.equals( "" ) )
            {
                attribTpl.putToken("initValue",AS3CodeFormats.languageDeffinitionToken.get("attributeAssignment")+initValue );
                attribTpl.putToken("doc:tags:default",getTagDoc("default",initValue)+AS3CodeFormats.LINE_SEPARATOR);

            }

            bodyTpl.appendToken("attributes", indentLines(attribTpl.parse(), AS3CodeFormats.INDENT));

        }

        //****************generate attributes from association
	addAttributes( bodyTpl, klass );
        bodyTpl.appendToken("functions", "");
        functionRealizationList.addAll(facade.getOperations(klass));
        addFunctions( bodyTpl, klass );
        return bodyTpl.parse();
    }

    protected SourceTemplate getDoc(Object obj)
    {
        SourceTemplate docTpl = new SourceTemplate( AS3CodeFormats.docTemplate );

        // base documentation
        String doc= indentLines(facade.getTaggedValueValue(obj, Argo.DOCUMENTATION_TAG)," *\t");
        if( !doc.equals( "" ) )
        {
            doc= doc + AS3CodeFormats.LINE_SEPARATOR;
            docTpl.putToken("doc", doc);
        }
        SourceTemplate innerTpl = getTypedDocTemplate( obj );

        //Commune agroUML documentation
        Collection<?> tagsValue = facade.getTaggedValuesCollection(obj);

        for(Object tv : tagsValue)
        {
            String tName = facade.getTagOfTag(tv);
            innerTpl.putToken(tName,getTagDoc(tName, facade.getValueOfTag(tv))+ AS3CodeFormats.LINE_SEPARATOR);
        }
        docTpl.putToken("tags", innerTpl);
        return docTpl;
    }

    protected void addFunctions( SourceTemplate bodyTpl, Object klass )
    {
        String template = ( facade.isAClass(klass) )? AS3CodeFormats.functionTemplate: AS3CodeFormats.functionInterfaceTemplate;
        SourceTemplate fctTpl = new SourceTemplate( template );
        for (Object behavioralFeature : functionRealizationList)
        {
            if( functionGeneralisationMap.containsKey(facade.getName(behavioralFeature)))
            {
                fctTpl.putToken("override",AS3CodeFormats.languageDeffinitionToken.get("override"));
                behavioralFeature = functionGeneralisationMap.get(facade.getName(behavioralFeature));
            }
            fctTpl.putToken( "doc", getDoc( behavioralFeature ) );
            fctTpl.putToken("visibility",AS3CodeFormats.functionVisibility.get(facade.getVisibility(behavioralFeature)));

            if (facade.isStatic(behavioralFeature))
            {
                fctTpl.putToken("static",AS3CodeFormats.languageDeffinitionToken.get("static"));
            }
            if(facade.isLeaf(behavioralFeature))
                fctTpl.putToken("final", "final ");

            fctTpl.putToken("name",facade.getName(behavioralFeature));

            fctTpl.putToken("returnType",AS3CodeFormats.languageDeffinitionToken.get("FunctionNoReturnType"));

            Collection<?> params = facade.getParameters(behavioralFeature);
            String separator = "";
            for(Object param : params)
            {
                Object paramType = facade.getType(param);
                if(! facade.isReturn(param))
                {
                    fctTpl.appendToken("arguments", getArgumentString( param, separator ) );
                    separator = AS3CodeFormats.languageDeffinitionToken.get("argumentSeparator");
                }
                else if (paramType!=null)
                {
                    addReturn( fctTpl, paramType );
                }
            }
            for (Object method : facade.getMethods(behavioralFeature))
            {
                addMethodBody( fctTpl, method );
            }
            bodyTpl.appendToken("functions", indentLines(fctTpl.parse(),AS3CodeFormats.INDENT));

            fctTpl.clearTokens();
        }
    }

    protected void addMethodBody( SourceTemplate fctTpl, Object method )
    {
        if (facade.getBody(method) != null)
        {
            String codeBody = ((String) facade.getBody(facade.getBody(method)));
            fctTpl.appendToken( "code",AS3CodeFormats.LINE_SEPARATOR+indentLines( codeBody, AS3CodeFormats.INDENT ) );
        }
    }

    protected void addReturn( SourceTemplate fctTpl, Object paramType )
    {
        fctTpl.putToken("returnType",facade.getName(paramType));
        SourceTemplate rtnTpl = new SourceTemplate( AS3CodeFormats.returnTemplate );
        rtnTpl.putToken("type", facade.getName(paramType));
        fctTpl.putToken("returnValue",rtnTpl);
    }

    /**
     * Given a document object, this returns a source Template related to its
     * corresponding String source.
     *
     * @param doc   The document source
     * @return      A template related to it.
     */
    protected SourceTemplate getTypedDocTemplate( Object doc )
    {
        String template = "";
        if (facade.isAClass(doc) || facade.isAInterface(doc))
        {
            template = AS3CodeFormats.classDocTemplate;
        }
        else if (facade.isAOperation(doc))
        {
            template = AS3CodeFormats.methodDocTemplate;
        }
        else if (facade.isAAttribute(doc) || facade.isAAssociationEnd(doc))
        {
            template = AS3CodeFormats.fieldDocTemplate;
        }
        return new SourceTemplate( template );
    }

    protected String getExtends( Object element )
    {
        String sExtends = "";
        Collection<?> generalizations = facade.getGeneralizations(element);
        if(!generalizations.isEmpty())
        {
            Object generalization = facade.getGeneral(generalizations.iterator().next());
            sExtends = " " + AS3CodeFormats.languageDeffinitionToken.get("extends") + facade.getName(generalization);

            Collection<?> operations=facade.getOperations(generalization);
            for(Object op:operations)
            {
                functionGeneralisationMap.put(facade.getName(op),op);
            }
        }
        return sExtends;
    }



    protected void writeFileToPath( String fileContents, String pathName )
    {
        File f = createFile( pathName );
        BufferedWriter fos = null;
        try {
            fos = getWriter( f );
            fos.write( fileContents );

        } catch (IOException exp) {
            LOG.error("IO Exception for file: " + f.getPath(), exp);
        } finally {

            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException exp) {
                LOG.error("FAILED: " + f.getPath(), exp);
            }
        }
    }

    protected BufferedWriter getWriter( File f ) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException
    {
        String configString = Configuration.getString(Argo.KEY_INPUT_SOURCE_ENCODING);
        if ( configString == null || configString.trim().equals("")) {
            configString = System.getProperty( "file.encoding" );
        }
        return new BufferedWriter( new OutputStreamWriter( new FileOutputStream(f), configString ) );
    }
    
    protected void addAttributes( SourceTemplate bodyTpl, Object element )
    {
        Collection<?> ends = facade.getAssociationEnds(element);

        if(facade.isAAssociationClass(element))
        {
            ends.addAll( facade.getConnections( element ) );
        }
        LOG.error( ( ends.size() ) + " connections length" );
        Collection<Object> listedEnds = new ArrayList<Object>();
        for(Object obj : ends)
        {
            Object association = facade.getAssociation(obj);

            //avoid double attribute for recursive association
            if (!listedEnds.contains(obj)|| facade.isAAssociationClass(element))
            {

                Collection<?> connections = facade.getOtherAssociationEnds(obj);
                for (Object connection : connections)
                {
                    if((facade.isNavigable(connection)&&!facade.isAbstract(association))
                            ||(facade.isAAssociationClass(element)&&!facade.isAbstract(association)))
                    {
                        SourceTemplate attribTpl= getAttributes( connection, element );
                        bodyTpl.appendToken("attributes", indentLines(attribTpl.parse(), AS3CodeFormats.INDENT) );
                    }
                    listedEnds.add(connection);
                }
            }
        }
        LOG.error( ( ends.size() ) + " connections length" );
    }

    
    /**
     * Given an argument parameter, this will translate that into a String
     * which will then be concatenated to create the parameters list of a
     * method.  Delimeter is included because of template system.
     * 
     * @param param         The parameter object... whatever that is.
     * @param isNotFirst    Whether there needs to be an argument delineator.
     * @return              The string representation of the argument.
     */
    protected String getArgumentString( Object param, String separator )
    {
        SourceTemplate argTpl= new SourceTemplate(AS3CodeFormats.functionAgrumentTemplate);
        Object paramType = facade.getType(param);
        argTpl.putToken("argumentSeparator", separator);
        argTpl.putToken("name",facade.getName(param));
        argTpl.putToken("typeAssignment",AS3CodeFormats.languageDeffinitionToken.get("typeAssignment"));
        String type      = ( paramType!=null )? facade.getName(paramType):AS3CodeFormats.languageDeffinitionToken.get("defaultType");
        argTpl.putToken("type", type);
        Object init = facade.getDefaultValue(param);
        if (facade.isAExpression(init))
           argTpl.putToken("initValue",AS3CodeFormats.languageDeffinitionToken.get("parameterAssignment")+facade.getBody(init));
        else
           argTpl.putToken("initValue","");

        return argTpl.parse();
    }

    /**
     * 
     * @param attrib    AssociationEnd or StructuralFeature to generate the
     *                  source code for
     * @param element   The context element
     * @return a SourceTemplate of the attribute without initValue
     */
    protected SourceTemplate getAttributes(Object attrib, Object element)
    {
        SourceTemplate attribTpl = getAttributeTemplate( attrib );    
        int upperMultiplicity = 1;
        Object attribType = facade.getType(attrib);
        String defaultName = "my" + facade.getName(attribType);
        if(facade.isAAssociationEnd(attrib))
        {
            if(facade.isAAssociationClass(facade.getAssociation(attrib)))
            {//this is an association class association
                if(!facade.isAAssociationClass(element))
                {
                    //we are parsing the class linked with the association class
                    //so the type have to be the AssociationClass herself
                    attribType = facade.getAssociation(attrib);
                    defaultName = "my" + facade.getName(attribType);
                }
            }else //this is a non class association
            {
                defaultName = nvl(  facade.getName(facade.getAssociation(attrib)),
                                    defaultName );
            }
            upperMultiplicity = facade.getUpper(attrib);
        }
        else //this is a StructuralFeature
        {
            if (facade.isStatic(attrib))
                attribTpl.putToken("static",AS3CodeFormats.languageDeffinitionToken.get("static"));

            try
            {
                // facade.getMultiplicity(attrib)
                upperMultiplicity = facade.getUpper(facade.getMultiplicity(attrib));
            }
            catch( Exception e )
            {
                upperMultiplicity = 1;
            }
        } 

        String type = facade.getName(attribType);
        if ( upperMultiplicity != 1 ) {
            type = "Vector.<"+type+">";
        }
        String name = nvl( facade.getName(attrib), defaultName );
        attribTpl.putToken("name",  name             );
        attribTpl.putToken("type",  type             );
        attribTpl.putToken("doc",   getDoc( attrib ) );
        
        return attribTpl;
    }

    /**
     * This is simply an easy way to abstract the basic steps needed to create
     * a SourceTemplate which would be used for defining attributes.
     *
     * @param attrib    The object this template will be related to
     * @return          The SourceTemplate which is to eventually represent the
     *                  provided attribute
     */
    protected SourceTemplate getAttributeTemplate( Object attrib )
    {
        SourceTemplate attribTpl = new SourceTemplate(AS3CodeFormats.varTemplate);

        attribTpl.putToken("visibility", AS3CodeFormats.attributesVisibility.get( facade.getVisibility( attrib ) ) );
        String changeability = ( facade.isReadOnly(attrib) )?"constant":"property";
        attribTpl.putToken("changeability",AS3CodeFormats.languageDeffinitionToken.get( changeability ));
        return attribTpl;
    }
    
    /**
     * A wrapper around getPackagePath which includes a packageClassSeparator
     * token if the package is empty
     *
     * @param element   The element to get the classpath of
     * @return          The classpath
     */
    protected String getClassPath(Object element)
    {
        String path = getPackagePath(element);
        return path.equals( "" ) ? "" : path + AS3CodeFormats.languageDeffinitionToken.get( "packageClassSeparator" );
    }

    /**
     * Generates a package path, ie: org.argouml.language.actionscript3.generator
     *
     * @param packageElement The package to parse
     * @return
     */
    protected String getPackagePath(Object packageElement)
    {
        if( packageElement == null  ) return "";
        SourceTemplate elementTpl = new SourceTemplate(AS3CodeFormats.packageElementTemplate);
        String separator     = "";
        // Prevents multiple string lookups.
        String baseSeparator = AS3CodeFormats.languageDeffinitionToken.get( "packageClassSeparator" );
        packageElement = facade.getNamespace( packageElement );
        while ( facade.getNamespace( packageElement ) != null )
        {
            elementTpl.prependToken( "name", facade.getName( packageElement ) + separator );
            // This assignment happens multiple times, but it is cleaner than
            // a "isFirstRun" test.
            separator = baseSeparator;
            packageElement = facade.getNamespace( packageElement );
        }

        return elementTpl.parse();
    }

    /**
     * Given an name, a value, and a description, this will generate a line of
     * JavaDoc notation.  NOTE: This will not create the entire doc, simply
     * that which is related to one key.
     *
     * @param tagName       For example, param, see, return, etc.
     * @param tagValue      The first data which comes after the name
     * @param description   The second data set which comes after the name
     *                      (not sure when useful)
     * @return  Concatenated String
     */
    protected String getTagDoc( String tagName, String tagValue, String description )
    {
        SourceTemplate docTpl = new SourceTemplate(AS3CodeFormats.tagTemplate);
        tagName = tagName.startsWith("@")? tagName : "@" + tagName + " ";
        docTpl.putToken("tagName", tagName);
        docTpl.putToken("tagValue", tagValue);
        docTpl.putToken("description", description);
        return docTpl.parse();
    }

    /**
     * Wrapper around getTagDoc( tagName, tagValue, "" );
     * @see GeneratorAS3#getTagDoc(java.lang.String, java.lang.String, java.lang.String);
     */
    protected String getTagDoc(String tagName, String tagValue)
    {
        return getTagDoc(tagName, tagValue,"");
    }

    /**
     * A String implementation of the Oracle method by the same name.  If the
     * first string is not null and has length, then return that.  Otherwise,
     * default to the second String.
     *
     * @param testedString  The string to test for null and length
     * @param defaultString The string to return if the other is null/empty
     * @return  One of the two, depending on the above conditions
     */
    protected String nvl( String testedString, String defaultString )
    {
        String ret = ( testedString != null && testedString.length() > 0 )?testedString: defaultString;
        return ret;
    }

    /**
     * Applies the provided indent to the provided text
     *
     * @param txt       The text to indent
     * @param indent    The indent to use
     * @return          The indented text
     */
    public String indentLines(String txt, String indent)
    {
       if( txt.length()>0 )
           return indent +( txt.replace("\r", "" ).replace( "\n", AS3CodeFormats.LINE_SEPARATOR ).replace( AS3CodeFormats.LINE_SEPARATOR, AS3CodeFormats.LINE_SEPARATOR + indent));
       else
           return "";
    }

    protected void writeElementsToPath( Collection elements, String path )
    {
        for (Object element : elements) {
            String fileContents = getFileContents(element);
            String fullPath     = getElementPath( element, path );
            writeFileToPath( fileContents, fullPath );
        }
    }

    protected String getElementPath( Object element, String path )
    {
        if (!path.endsWith(FILE_SEPARATOR)) {
            path += FILE_SEPARATOR;
        }
        path += getClassPath( element ).replace( AS3CodeFormats.languageDeffinitionToken.get( "packageClassSeparator" ), FILE_SEPARATOR );
        path += facade.getName(element)+AS3CodeFormats.languageDeffinitionToken.get("fileExtension");
        return path;
    }


    protected File createFile( String filePath )
    {
        ensurePathExists( filePath );
        File ret = new File( filePath );
        return ret;
    }

    protected void ensurePathExists( String filePath )
    {
        filePath    = filePath.substring( 0, filePath.lastIndexOf( FILE_SEPARATOR ) );
        File path   = new File( filePath );
        if( !path.exists() )
        {
            path.mkdirs();
        }
    }
}
