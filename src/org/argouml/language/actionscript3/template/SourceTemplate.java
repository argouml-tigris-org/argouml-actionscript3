// $Id: org.eclipse.jdt.ui.prefs 3 2006-04-16 21:13:15Z linus $
// Copyright (c) 2009 The Regents of the University of California. All
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

package org.argouml.language.actionscript3.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** 
 *  make more readable the source code, and more easy to change template syntaxe
 *  take a string as template with inside tokens .
 *  asign tokens value.
 *  and get the result string.
 *  <code>
 *  myTpl =new SourceTemplate("a simple {token}template");
 *  myTpl.puttoken("token","pretty ");
 *  myTpl.parse()
 *  </code> 
 *  A template can have many same token name  but they will be fill with the same value.
 * 
 *  @author Flacher Romain
 *  @version=1
 *  @since=01 29 2009
 */
public class SourceTemplate {


  /** 
   *  the internal string value of template
   */
  private String srcTemplate = "";

  /** 
   *  a map of assignated tokens name and value
   */
  private HashMap<String,String> tokens = new HashMap<String,String>();

  /** 
   *  a list of template's token name
   */
  private List<String> tokensNames = new ArrayList<String>();

  /** 
   *  a map of assignated token name and source template object
   */
  private Map<String,SourceTemplate> compositeTokens = new HashMap<String,SourceTemplate>();

  /** 
   *  Create a new template, take the string to be the internal template source as parameter
   */
  public SourceTemplate(String template) {
      
      setTemplate(template);
      
  }
  /** 
   * TODO:  Create a new template, take the file to be the internal template source as parameter
   */

public SourceTemplate(File template) {
    
       
  }
  
  /** 
   *  Create a new empty template
   */
  public SourceTemplate() {
      
  }

  /** 
   *  replace the internal template source
   */
  public void setTemplate(String template) {
      srcTemplate=(template.length()>0)?template:"";
      fillTokensFromTemplate();
  }
  /** 
   *  replace the internal template source from a file
   */
  public void setTemplate(File template) {
      try {
          // TODO: This is using the default platform character encoding
          // specifying an encoding will produce more predictable results
          FileReader f = new FileReader(template.getAbsolutePath());
          BufferedReader fr = new BufferedReader(f);

          String line = "";
          StringBuilder content = new StringBuilder();
          while (line != null) {
              line = fr.readLine();
              if (line != null) {
                  content.append(line + System.getProperty("line.separator"));
              }
          setTemplate(content.toString());
          fillTokensFromTemplate();
          }
          fr.close();
          } catch (IOException e) {
              //LOG.error("Error: " + e.toString());
          }
  }

  /** 
   *  define a template tokenName to be replace with value
   */
  public void putToken(String tokenName, String value) {
      if(tokenName!=null&&tokenName.length()>0&&value!=null)
      {
          if (tokenName.contains(":"))
          {
              SourceTemplate scrTemplate = compositeTokens.get(tokenName.substring(0,tokenName.indexOf(":")));
              if (srcTemplate!=null)
                  scrTemplate.putToken( tokenName.replaceFirst("[a-zA-Z]*:", ""), value);
          }
          else{
              tokens.put(tokenName, value);
              
          }
      }   
  }
  /** 
   *  define a template tokenName to be replace with value
   */
  public void appendToken(String tokenName, String value) {
      if(tokenName!=null&&tokenName.length()>0&&value!=null)      {
          if (tokenName.contains(":"))
          {
              SourceTemplate scrTemplate = compositeTokens.get(tokenName.substring(0,tokenName.indexOf(":")));
              if (srcTemplate!=null)
                  scrTemplate.appendToken( tokenName.replaceFirst("[a-zA-Z]*:", ""), value);
          }
          else
          {
              if(tokens.containsKey(tokenName)&&value!=null)
              {
                  value=tokens.get(tokenName)+value;
              }
              tokens.put(tokenName, value);
          }
      }
  }
  
  /** 
   *  define a template tokenName to be replace with value
   */
  public void prependToken(String tokenName, String value) {
      if(tokenName!=null&&tokenName.length()>0&&value!=null)      {
          if (tokenName.contains(":"))
          {
              SourceTemplate scrTemplate = compositeTokens.get(tokenName.substring(0,tokenName.indexOf(":")));
              if (srcTemplate!=null)
                  scrTemplate.prependToken( tokenName.replaceFirst("[a-zA-Z]*:", ""), value);
          }
          else
          {
              if(tokens.containsKey(tokenName)&&value!=null)
              {
                  value=value+tokens.get(tokenName);
              }
              tokens.put(tokenName, value);
          }
      }
  }

  /** 
   *  for create a  composite template, define a tokenName to be replace by srcTemplate parse value when you parse the parent template
   */
  public void putToken(String tokenName, SourceTemplate scrTemplate) {
      if(tokenName!=null&&tokenName.length()>0)
          if (tokenName.contains(":"))
          {
              SourceTemplate childTemplate = compositeTokens.get(tokenName.substring(0,tokenName.indexOf(":")));
              if (srcTemplate!=null)
                  childTemplate.putToken( tokenName.replaceFirst("[a-zA-Z]*:", ""), scrTemplate);
          }
          else
              compositeTokens.put(tokenName, scrTemplate);
  }

  /** 
   *
   */
  public SourceTemplate getChild(String tokenName) {
      return compositeTokens.get(tokenName);
  }

  /** 
   *
   * add all elements from a map of tokens name and value
   */
  public  void putAllTokens(Map<String,String> tokensMap)
  {
      tokens.putAll(tokensMap);
  }
  /*
  public  void putAllTokens(Map<String,SourceTemplate> tokensMap)
  {
      compositeTokens.putAll(tokensMap);
  }*/

  /** 
   *  get the string result by replacing all token by his value
   */
  public String parse() {
       
      String tmpTemplate = srcTemplate.toString();
      
      for(String name :tokensNames)
      {
          String value;
          if(tokens.containsKey(name))
              value = tokens.get(name);
          else if(compositeTokens.containsKey(name))
              value = compositeTokens.get(name).parse();
          else
              value="";
          
          tmpTemplate=tmpTemplate.replace("{"+name+"}",value);
      }
      
      
      
  return tmpTemplate;
  }
  
  public boolean isEmpty()
  {
      if(!tokens.isEmpty())
          return false;
      else{
          boolean empty = true;
          for(String key :compositeTokens.keySet())
          {
              empty= empty && compositeTokens.get(key).isEmpty();
          }
          return empty;
      }
      
      
  }
  public String parse(boolean blank) {
      
      if(isEmpty())
          return "";
      else
      {
          String tmpTemplate = srcTemplate.toString();
          
          for(String name :tokensNames)
          {
              String value;
              if(tokens.containsKey(name))
                  value = tokens.get(name);
              else if(compositeTokens.containsKey(name))
                  value = compositeTokens.get(name).parse(blank);
              else
                  value="";
              
              tmpTemplate=tmpTemplate.replace("{"+name+"}",value);
          }
          return tmpTemplate;
      }
          
  }

  /** 
   * 
   *  return true if internal template have a token with tokenName
   *  if deep it true check  child template too
   */
  public boolean hasToken(String tokenName, boolean deep) {
      boolean hasToken = tokensNames.contains(tokenName);
      if(deep)
      {
          for(String name : compositeTokens.keySet())
          {
              hasToken = hasToken || compositeTokens.get(name).hasToken(tokenName, deep);
          }
      }
     
      return hasToken;
  }

  /** 
   *
   *  parse the internal  template string to fill the tokens name list
   */

  private void fillTokensFromTemplate()
  {
      tokensNames = new ArrayList<String>();
      Pattern pattern = 
          Pattern.compile("\\{([a-zA-Z]+)\\}");
      Matcher matcher = 
          pattern.matcher(srcTemplate);
          while (matcher.find()) {
              tokensNames.add(matcher.group(1));
          }
      
  }
  public void clearTokens()
  {
      tokens = new HashMap<String,String>();
      compositeTokens = new HashMap<String,SourceTemplate>();
  }
    public String toString()
    {
        return srcTemplate;
        
    }
    public SourceTemplate clone()
    {
        return new SourceTemplate(this.srcTemplate);
    }
}