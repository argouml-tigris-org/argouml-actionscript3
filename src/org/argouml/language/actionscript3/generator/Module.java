// $Id$
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason. IN NO EVENT SHALL THE
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

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.argouml.application.helpers.ResourceLoaderWrapper;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.uml.generator.GeneratorHelper;
import org.argouml.uml.generator.GeneratorManager;
import org.argouml.uml.generator.Language;

/**
 * Module manager for PHP4 generator.
 */
public class Module implements ModuleInterface {
    
    /**
     * The log4j logger to log messages to
     */
    private static final Logger LOG = Logger.getLogger(Module.class);

    private Language myLang; 

    /**
     * The name of the language this module generates source code for
     */
    protected static final String LANGUAGE_NAME = "ActionScript3";

    /**
     * Icon to represent this notation in the GUI
     */
    protected static final String ICON_NAME = "ActionScript3Notation";
    
    /**
     * Info block already written to log file?
     */
    protected static final TreeMap<String, String> TM_INFO_BLOCK_LOGGED = 
        new TreeMap<String, String>();

    /**
     * Constructor for ActionScript3 module.
     */
    public Module() {
    }
    
    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "Generator" + LANGUAGE_NAME.toUpperCase();
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "notation and source code generator for "
		+ LANGUAGE_NAME.toUpperCase();
        case AUTHOR:
            return "Kai Schr\u00F6der";
        case VERSION:
            return "0.0.$Revision$";
        default:
            return null;
        }
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        if (myLang == null) {
            myLang = GeneratorHelper.makeLanguage(
                    LANGUAGE_NAME,
                    ResourceLoaderWrapper.lookupIconResource(ICON_NAME));
        }
        GeneratorManager.getInstance().addGenerator(myLang,
						    new GeneratorAS3());
	LOG.info("Module " + LANGUAGE_NAME + " enabled.");
        return true;
    }
    /*
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        GeneratorManager.getInstance().removeGenerator(myLang);
        return true;
    }
}
