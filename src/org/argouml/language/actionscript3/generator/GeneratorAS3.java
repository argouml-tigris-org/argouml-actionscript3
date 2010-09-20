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
 *    cwallenpoole
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2004-2010 The Regents of the University of California. All
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
    // This is a shortcut... AS3CodeFormats.languageDeffinitionToken is
    // REALLY long.
    protected static Map<String, String> lang;
    protected Map<String, Object> functionGeneralisationMap;
    private static Facade facade;

    private static int uniqueIndex = 0;
	
    public GeneratorAS3() 
    {
        facade = Model.getFacade();
        lang = AS3CodeFormats.languageDeffinitionToken;
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
    public Collection<SourceUnit> generate(Collection elements, boolean deps)
    {
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
    public Collection<String> generateFileList(Collection elements, boolean deps)
    {

        Collection<String> files=new ArrayList<String>();
        for (Object element : elements) {
            files.add(
                        facade.getName(element)+
                        lang.get("fileExtension")
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
        Collection<String> files = TempFileUtils.readFileNames( new File( path ) );
        return files;
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

    /**
     * Adds the body of a class to a template inside of the main package
     *
     * @param classTpl      The template
     * @param publicClass   The class
     */
    protected void addMainClassToTemplate( SourceTemplate classTpl, Object publicClass )
    {
        classTpl.putToken("package", getPackagePath( publicClass ) );
        classTpl.putToken("mainClass", indentLines( generateBody(publicClass), AS3CodeFormats.INDENT ));
        addClassImportsToTemplate( classTpl, publicClass );
    }
	
    /**
     * This simply adds what are considered "inner" (nested, or private) classes
     * to a given class template.
     *
     * @param classTpl           The template to append
     * @param privateClassBlock  The objects which "owns" all of the private
     *                           class data.
     */
    protected void addPrivateClassesToTemplate( SourceTemplate classTpl, Object privateClassBlock )
    {
        Collection<?> inClass = facade.getOwnedElements( privateClassBlock );
        for (Object klass : inClass)
        {
            if( isType( klass ) )
            {
                classTpl.appendToken("innerClass", generateBody( klass ) );
            }
        }
        addInnerImportsToTemplate( classTpl, privateClassBlock );
    }

    /**
     * Creates the actual body of the class.
     *
     * @param klass The object to base the body off of.
     * @return      A string which represents the body of the class.
     */
    protected String generateBody(Object klass)
    {
        if( !isType( klass ) )
        {
            return "";
        }
        functionGeneralisationMap = new HashMap<String, Object>();

        SourceTemplate bodyTpl = getBodyTemplate( klass );
        addImplementations( bodyTpl, klass );

        //javadoc
        bodyTpl.putToken("doc", getDoc(klass).parse() );
        //is it a dynamic class
        bodyTpl.putToken( "dynamic", "" );

        //is it a final class
        bodyTpl.putToken("final", (facade.isLeaf(klass))? lang.get("final"):"");
        // class name
        bodyTpl.putToken("name", facade.getName(klass));
        // class visibility
        Map<Object, String> source = ( facade.isAClass(facade.getNamespace(klass)) )?AS3CodeFormats.innerClassVisibility: AS3CodeFormats.classVisibility;
        bodyTpl.putToken("visibility", source.get(facade.getVisibility(klass)));
        //********************* generate class extends
        bodyTpl.putToken("extends", getExtends( klass ));
        //****************generate attributes

        addStructuralAttributes( bodyTpl, klass );

        //****************generate attributes from association
	addAssociationAttributes( bodyTpl, klass );
        addFunctions( bodyTpl, klass );
        return bodyTpl.parse();
    }

    /**
     * Handles the addition of all attributes which are considered UML
     * "structural features"
     *
     * @param bodyTpl   Template to append
     * @param klass     The source class.
     */
    protected void addStructuralAttributes( SourceTemplate bodyTpl, Object klass )
    {
        Collection<?> sFeatures =  facade.getStructuralFeatures(klass);
        LOG.debug( "addStructuralAttributes" );
        for(Object structuralFeature : sFeatures)
        {
            SourceTemplate attribTpl= getAttributeTemplate( structuralFeature );

            //**attribute assignment
            Object init = facade.getInitialValue(structuralFeature);
            String initValue = "";
            if( facade.isAExpression( init ) )
            {
                initValue =  facade.getBody(init).toString();
            }
            LOG.debug( facade.getName( structuralFeature ) );

            if( !initValue.equals( "" ) )
            {
                attribTpl.putToken("initValue",lang.get("attributeAssignment")+initValue );
                attribTpl.putToken("doc:tags:default",getTagDoc("default",initValue)+AS3CodeFormats.LINE_SEPARATOR);
            }
            bodyTpl.appendToken("attributes", indentLines(attribTpl.parse(), AS3CodeFormats.INDENT));
        }
    }

    /**
     * Returns a SourceTemplate which has been initialized as a class or an
     * interface depending on the input.
     *
     * @param klass Either an interface or a class object
     * @return The resulting template
     */
    protected SourceTemplate getBodyTemplate( Object klass )
    {
        SourceTemplate bodyTpl = new SourceTemplate( AS3CodeFormats.bodyClassTemplate );

        String classType = facade.isAInterface(klass)?
                                lang.get("interface"):
                                lang.get("class");

        bodyTpl.putToken("classType",classType);
        return bodyTpl;
    }

    /**
     * Given a SourceTemplate and a class, this will add the imports to the 
     * template at the appropriate place
     *
     * @param classTpl  The template
     * @param klass     The class
     */
    protected void addClassImportsToTemplate( SourceTemplate classTpl, Object klass )
    {
        addImportsToTemplate( classTpl, getClassImports( klass ), "imports" );
    }

    /**
     * Given a SourceTemplate and an inner class block, this will add the
     * imports to the template at the appropriate place
     *
     * @param classTpl  The template
     * @param inner     The inner class block
     */
    protected void addInnerImportsToTemplate( SourceTemplate classTpl, Object inner )
    {
        addImportsToTemplate( classTpl, getInnerImports( inner ), "innerImports" );
    }

    /**
     * Adds all given imports to the SourceTemplate provided using the given
     * type.
     *
     * @param classTpl      The template to modify
     * @param imports       List of elements to imports
     * @param importType    Generally "imports" or "innerImports"
     *
     * @see #addClassImportsToTemplate(SourceTemplate classTpl,Object klass)
     * @see #addInnerImportsToTemplate(SourceTemplate classTpl,Object inner)
     */
    protected void addImportsToTemplate( SourceTemplate classTpl, HashSet imports, String importType )
    {
        classTpl.appendToken( importType, "" );
        SourceTemplate importTpl = new SourceTemplate( AS3CodeFormats.importTemplate );
        for(Object imported : imports )
        {
            importTpl.clearTokens();
            importTpl.putToken( "importPath", getClassPath( imported )  );
            importTpl.putToken( "ClassName", facade.getName( imported ) );
            classTpl.appendToken( importType, importTpl.parse() );
        }
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
        Collection<Object> realizations = facade.getSpecifications( klass );
        Object realization = getParentClass( klass );
        realizations.add( realization );
        if(!realizations.isEmpty())
        {
            Iterator<?> iterator = realizations.iterator();
            while(iterator.hasNext())
            {
                realization = iterator.next();
                if( elementNeedsImport( realization, klass ) )
                {
                    imports.add( realization );
                }
            }
        }
        return imports;
    }

    /**
     * Gets a list of imports for the inner/private/nested classes
     *
     * @param element   The "owner"
     * @return  A concatenated set of all of the private imports
     */
    protected HashSet getInnerImports( Object element )
    {
        HashSet imports = new HashSet<Object>();
        for( Object klass : facade.getOwnedElements(element) )
        {
            if( isType( klass ) )
            {
               imports.addAll( getClassImports( klass ) );
            }
        }
        return imports;
    }

    /**
     * Adds the list of interfaces to a template
     *
     * @param bodyTpl
     * @param element
     */
    protected void addImplementations( SourceTemplate bodyTpl, Object element )
    {
        Collection<?> realizations = facade.getSpecifications( element );
        String sImplements = ( realizations.isEmpty() )? 
                                "":
                                getImplementationString( realizations );
        bodyTpl.putToken( "implements", sImplements );
    }

    /**
     * Generates a string which represents all of the interfaces a class
     * represents.
     *
     * @param realizations  All of the interfaces which are implemented
     * @return              The concatenation of the interface names
     */
    protected String getImplementationString( Collection<?> realizations )
    {
        String sImplements =  " implements ";
        Iterator<?> iterator = realizations.iterator();
        while(iterator.hasNext())
        {
            Object implemented = iterator.next();
            String temp = facade.getName( implemented );

            //not sure why, but sometimes this was getting duplicates of the
            //same implementation.  I suspect it had to do with inheritence
            if( sImplements.indexOf( temp ) < 0 )
            {
                if( sImplements.length() > 0 ) sImplements += lang.get( "argumentSeparator" );
                sImplements += temp;
            }
        }
        return sImplements;
    }

    /**
     * Given an element, this returns the template which represents its documentation
     * @param element
     * @return A template which represents its documentation
     */
    protected SourceTemplate getDoc(Object element)
    {
        SourceTemplate docTpl = new SourceTemplate( AS3CodeFormats.docTemplate );

        // base documentation
        String doc = indentLines(facade.getTaggedValueValue(element, Argo.DOCUMENTATION_TAG)," *\t");
        if( !doc.equals( "" ) )
        {
            doc = doc + AS3CodeFormats.LINE_SEPARATOR;
            docTpl.putToken("doc", doc);
        }
        SourceTemplate innerTpl = getTypedDocTemplate( element );

        //Commune agroUML documentation
        Collection<?> tagsValue = facade.getTaggedValuesCollection(element);

        for(Object tv : tagsValue)
        {
            String tName = facade.getTagOfTag(tv);
            innerTpl.putToken(tName,getTagDoc(tName, facade.getValueOfTag(tv))+ AS3CodeFormats.LINE_SEPARATOR);
        }
        docTpl.putToken("tags", innerTpl);
        return docTpl;
    }

    //TODO: COMMENT!!!
    protected List getAnscestorFunctions( Object klass )
    {
        List functions = new ArrayList<Object>();
        while( klass != null )
        {
            functions.addAll( getCurrentFunctions( klass ) );
            klass = getParentClass( klass );
        }
        return functions;
    }

    //TODO: COMMENT!!!
    protected List getCurrentFunctions( Object klass )
    {
        return facade.getOperations( klass );
    }

    //TODO: COMMENT!!!
    protected Map<String,Object> getAncestorsMap( Object klass )
    {
        Map<String, Object> parentMap = new HashMap<String,Object>();
        klass = getParentClass( klass );
        List functions = getAnscestorFunctions( klass );
        for( Object function: functions )
        {
            parentMap.put( facade.getName( function ), function );
        }
        return parentMap;
    }
    
    /**
     * Adds the class's functions to the template which represents the class
     * body.  NOTE: As of Sept. 10, 2010, the functions lists are created
     * elsewhere in the class.
     * @param bodyTpl   The template to append.
     * @param klass     The context class.
     */
    //TODO: This method should be broken up into more readable parts
    protected void addFunctions( SourceTemplate bodyTpl, Object klass )
    {
        String template = ( facade.isAClass(klass) )? AS3CodeFormats.functionTemplate: AS3CodeFormats.functionInterfaceTemplate;
        SourceTemplate fctTpl = new SourceTemplate( template );
        List functions          = getCurrentFunctions( klass );
        Map parentFunctionsMap  = getAncestorsMap( klass );
        for (Object function : functions )
        {
            if( parentFunctionsMap.containsKey(facade.getName(function)))
            {
                fctTpl.putToken("override",lang.get("override"));
                function = parentFunctionsMap.get(facade.getName(function));
            }
            fctTpl.putToken( "doc", getDoc( function ) );
            fctTpl.putToken("visibility",AS3CodeFormats.functionVisibility.get(facade.getVisibility(function)));

            if (facade.isStatic(function))
            {
                fctTpl.putToken("static",lang.get("static"));
            }
            if(facade.isLeaf(function))
                fctTpl.putToken("final", "final ");

            fctTpl.putToken("name",facade.getName(function));

            fctTpl.putToken("returnType",lang.get("functionNoReturnType"));

            Collection<?> params = facade.getParameters(function);
            String separator = "";
            for(Object param : params)
            {
                Object paramType = facade.getType(param);
                if(! facade.isReturn(param))
                {
                    fctTpl.appendToken("arguments", separator + getArgumentString( param ) );
                    separator = lang.get("argumentSeparator");
                }
                else if (paramType!=null)
                {
                    addReturn( fctTpl, paramType );
                }
            }
            for (Object method : facade.getMethods(function))
            {
                addMethodBody( fctTpl, method );
            }
            bodyTpl.appendToken("functions", indentLines(fctTpl.parse(),AS3CodeFormats.INDENT));

            fctTpl.clearTokens();
        }
    }

    /**
     * Adds the body of a method to a template.
     *
     * @param fctTpl    The template to append
     * @param method    The object which is read by the facade.
     */
    protected void addMethodBody( SourceTemplate fctTpl, Object method )
    {
        if (facade.getBody(method) != null)
        {
            String codeBody = ((String) facade.getBody(facade.getBody(method)));
            fctTpl.appendToken( "code",AS3CodeFormats.LINE_SEPARATOR+indentLines( codeBody, AS3CodeFormats.INDENT ) );
        }
    }

    /**
     * Adds the return value to a template
     * 
     * @param fctTpl    The template representing the containing function
     * @param paramType The type of the object returned.
     */
    protected void addReturn( SourceTemplate fctTpl, Object paramType )
    {
        String retType  = facade.getName(paramType);
        //sometimes people will type "void" as the return.
        if( retType.equals( lang.get( "functionNoReturnType" ) ) )
        {
            fctTpl.putToken("returnValue","");
        }
        else
        {            
            SourceTemplate rtnTpl = new SourceTemplate( AS3CodeFormats.returnTemplate );
            fctTpl.putToken("returnType",retType);
            rtnTpl.putToken("type", retType);
            fctTpl.putToken("returnValue",rtnTpl);
        }
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

    /**
     * Creates a string which represents the parent class
     *
     * @param klass The child class
     * @return      Either an empty string or "extends &lt;class name&gt;"
     */
    protected String getExtends( Object klass )
    {
        String sExtends = "";
        Object parent = getParentClass( klass );
        if( parent != null )
        {
            sExtends = " " + lang.get( "extends" ) + facade.getName( parent );
        }
        return sExtends;
    }

    /**
     * Given a class, this returns the parent class.
     * @param type  The class or interface which is extended.
     * @return      An object which represents the parent class.
     */
    protected Object getParentClass( Object type )
    {
        Iterator it = facade.getGeneralizations( type ).iterator();
        return ( it.hasNext() )? facade.getGeneral(it.next()): null;
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

    /**
     * Writes the provided content to the provided filename
     * @param fileContents
     * @param fileName
     */
    protected void writeFileContentsToPath( String fileContents, String fileName )
    {
        File f = createFile( fileName );
        BufferedWriter fos = null;
        try {
            fos = getWriter( f );
            fos.write( fileContents );

        } catch (IOException exp) {
            LOG.debug("IO Exception for file: " + f.getPath(), exp);
        } finally {

            try {
                if( fos != null )
                {
                    fos.close();
                }
            } catch (IOException exp) {
                LOG.debug("FAILED: " + f.getPath(), exp);
            }
        }
    }

    /**
     * Given a file, this returns a BufferedWriter with the appropriate encoding
     * for writing to said file.
     *
     * @param file  The source to write to
     * @return The appropriate writer
     * @throws java.io.FileNotFoundException
     * @throws java.io.UnsupportedEncodingException
     */
    protected BufferedWriter getWriter( File file ) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException
    {
        String configString = Configuration.getString(Argo.KEY_INPUT_SOURCE_ENCODING);
        if ( configString == null || configString.trim().equals("")) {
            configString = System.getProperty( "file.encoding" );
        }
        return new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), configString ) );
    }

    /**
     * Adds all of the attributes which are UML associations
     * @param bodyTpl   The template to modify
     * @param klass     The class which is the source of the associations
     */
    protected void addAssociationAttributes( SourceTemplate bodyTpl, Object klass )
    {
        LOG.debug( "addAttributesToTemplate" );
        Boolean isAssoc = facade.isAAssociationClass( klass );
        Collection<Object> listedEnds = new ArrayList<Object>();
        Collection<?> ends = getAttributeElements( klass );
        for( Object obj : ends )
        {
            //avoid double attribute for recursive association
            if( !listedEnds.contains( obj ) || isAssoc )
            {
                Object association = facade.getAssociation(obj);
                Collection<?> connections = facade.getOtherAssociationEnds(obj);
                if( facade.isAbstract( association ) )
                {
                    for (Object connection : connections)
                    {
                        LOG.debug( connection );
                        if( facade.isNavigable( connection ) || isAssoc )
                        {
                            addAttributeToTemplate( bodyTpl, connection );
                        }
                        listedEnds.add(connection);
                    }
                }
                else
                {
                    listedEnds.addAll( connections );
                }
            }
        }
    }

    /**
     * Simply adds an attribute to a template.
     * @param bodyTpl       The template to append
     * @param connection    The context to append it with
     */
    protected void addAttributeToTemplate( SourceTemplate bodyTpl, Object connection )
    {
        SourceTemplate attribTpl = getAttributeTemplate( connection );
        bodyTpl.appendToken( "attributes", indentLines( attribTpl.parse(), AS3CodeFormats.INDENT ) );
    }

    /**
     * Returns all of the attribute objects related to a given element
     * @param element   The element with the attributes
     * @return          The attributes in a Collection
     */
    protected Collection<?> getAttributeElements( Object element )
    {
        Collection<?> ends = facade.getAssociationEnds(element);

        if(facade.isAAssociationClass(element))
        {
            ends.addAll( facade.getConnections( element ) );
        }
        return ends;
    }
    
    /**
     * Given an argument parameter, this will translate that into a String
     * which will then be concatenated to create the parameters list of a
     * method.
     * 
     * @param param         The parameter object
     * @return              The string representation of the argument.
     */
    protected String getArgumentString( Object param )
    {
        SourceTemplate argTpl= new SourceTemplate(AS3CodeFormats.functionAgrumentTemplate);
        Object paramType = facade.getType(param);
        String type      = ( paramType!=null )? 
                                facade.getName(paramType):
                                lang.get("defaultType");
        Object init = facade.getDefaultValue(param);
        String initValue = (facade.isAExpression(init))?
                                lang.get( "parameterAssignment" ) + facade.getBody(init):
                                "";
        String name = facade.getName( param );
        
        if( isBlank( name ) )
        {
            uniqueIndex++;
            name = "my" + type.substring( 0, 1 ).toUpperCase() + type.substring( 1 ) + String.valueOf( uniqueIndex );

        }

        argTpl.putToken("type",              type                           );
        argTpl.putToken("initValue",         initValue                      );
        argTpl.putToken("argumentSeparator", ""                             );
        argTpl.putToken("name",              name                           );
        argTpl.putToken("typeAssignment",    lang.get( "typeAssignment" )   );

        return argTpl.parse();
    }

    /**
     * Returns a pre-formatted SourceTemplate which reflects the state of the
     * provided attribute in its context
     *
     * @param attrib    AssociationEnd or StructuralFeature to generate the
     *                  source code for
     * @return a SourceTemplate of the attribute without initValue
     */
    protected SourceTemplate getAttributeTemplate( Object attrib )
    {
        SourceTemplate attribTpl = getBaseAttributeTemplate( attrib );
        int upperMultiplicity    = 1;
        Object attribType        = facade.getType(attrib);
        String defaultName       = "my" + facade.getName(attribType);
        if(facade.isAAssociationEnd(attrib))
        {
            Object assoc         = facade.getAssociation(attrib);
            if(facade.isAAssociationClass( assoc ) )
            {//this is an association class association
                if(!facade.isAAssociationClass(facade.getClassifier(attrib)))
                {
                    //we are parsing the class linked with the association class
                    //so the type have to be the AssociationClass herself
                    attribType = assoc;
                    defaultName = "my" + facade.getName(attribType);
                }
            }
            else //this is a non class association
            {
                defaultName = nvl(  facade.getName( assoc ), defaultName );
            }
            upperMultiplicity = facade.getUpper(attrib);
        }
        else //this is a StructuralFeature
        {
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

        if (facade.isStatic(attrib)) attribTpl.putToken("static",lang.get("static"));
        String type = facade.getName(attribType);
        if ( upperMultiplicity != 1 )
        {
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
    protected SourceTemplate getBaseAttributeTemplate( Object attrib )
    {
        SourceTemplate attribTpl = new SourceTemplate(AS3CodeFormats.varTemplate);
        attribTpl.putToken("visibility", AS3CodeFormats.attributesVisibility.get( facade.getVisibility( attrib ) ) );
        String changeability = ( facade.isReadOnly(attrib) )?"constant":"property";
        attribTpl.putToken("changeability",lang.get( changeability ));
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
        return path.equals( "" ) ? "" : path + lang.get( "packageClassSeparator" );
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
        String baseSeparator = lang.get( "packageClassSeparator" );
        packageElement = facade.getNamespace( packageElement );
        while ( facade.getNamespace( packageElement ) != null )
        {
            elementTpl.prependToken( "name", facade.getName( packageElement ) + separator );
            // This assignment happens multiple times, but it is cleaner than
            // a bool "isFirstRun" test.
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
     * Applies the provided indent to the provided text if the text string is
     * not empty
     *
     * @param txt       The text to indent
     * @param indent    The indent to use
     * @return          The indented text
     */
    public String indentLines(String txt, String indent)
    {
       if( txt.length()>0 )
           return indent + ( txt.replace("\r", "" )
                                .replace( "\n", AS3CodeFormats.LINE_SEPARATOR )
                                .replace
                                (
                                    AS3CodeFormats.LINE_SEPARATOR,
                                    AS3CodeFormats.LINE_SEPARATOR + indent
                                )
                           );
       else
           return "";
    }

    /**
     * Iterates through a collection of elements and then writes their "file
     * contents" to the path provided
     *
     * @param elements  The collection of elements.
     * @param path      The path to write to.
     */
    protected void writeElementsToPath( Collection elements, String path )
    {
        for (Object element : elements) {
            String fileContents = getFileContents( element );
            String fullPath     = appendElementPath( path, element );
            writeFileContentsToPath( fileContents, fullPath );
        }
    }

    /**
     * Given a path and an element, this will append the path/to/class of the
     * element's package.
     *
     * @param path      The path to the base of the source tree
     * @param element   The element to test
     * @return          The path + the element's class path
     */
    protected String appendElementPath( String path, Object element )
    {
        if (!path.endsWith(FILE_SEPARATOR)) {
            path += FILE_SEPARATOR;
        }
        path += getClassPath( element ).replace( lang.get( "packageClassSeparator" ), FILE_SEPARATOR );
        path += facade.getName(element) + lang.get("fileExtension");
        return path;
    }

    /**
     * Creates a File object given the full path to root.
     * 
     * @param filePath  The path
     * @return The file object at that location
     */
    protected File createFile( String filePath )
    {
        ensurePathExists( filePath );
        File ret = new File( filePath );
        return ret;
    }

    /**
     * Given a file's name, this makes sure that the path to that directory
     * actually exists. (It first calls exists and then mkdirs if necessary)
     *
     * @param filePath
     */
    //TODO: This does not account for situations where the final path segment
    // exists as a file...
    protected void ensurePathExists( String filePath )
    {
        int sep     = filePath.lastIndexOf( FILE_SEPARATOR );
        filePath    = filePath.substring( 0, sep );
        File path   = new File( filePath );
        if( !path.exists() )
        {
            path.mkdirs();
        }
    }

    /**
     * Given an object, this will return whether that object is ( a class || an
     * interface ) or not;
     *
     * @param obj   The object to test
     * @return      Whether the object can be considered a "type"
     */
    protected Boolean isType( Object obj )
    {
        return ( facade.isAClass( obj ) || facade.isAInterface( obj ) );
    }



    /**
     * This is the equivalent of StrinUtil.isBlank in the apache common's lib.
     * @param str   A String to test
     * @return      Whether it contains anything other than whitespace.
     */
    public static final boolean isBlank( String str )
    {
        return ( str == null || str.trim().length() == 0 );
    }
}
