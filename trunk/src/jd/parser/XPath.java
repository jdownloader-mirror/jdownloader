//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.parser;

import java.io.*;
import java.util.ArrayList;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class XPath {
    private javax.xml.xpath.XPath xpath;
    private String query;
    private Document doc;
    private String source;
    HtmlCleaner cleaner;
    
    public XPath(String source, String query) {
        this(source, query, true);
    }
    
    public XPath(String source, String query, boolean transform) {
        try  {
            if(transform) {
                cleaner = new HtmlCleaner();
                CleanerProperties props = cleaner.getProperties();
                props.setNamespacesAware(true);
                doc = new DomSerializer(props, true).createDOM(cleaner.clean(source));
            }
            else {
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
                domFactory.setNamespaceAware(true);
                DocumentBuilder builder = domFactory.newDocumentBuilder();
                doc = builder.parse(new InputSource(new ByteArrayInputStream(source.getBytes())));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        
        XPathFactory factory = XPathFactory.newInstance();
        xpath = factory.newXPath();
        this.source = source;
        this.query = query;
    }
    
    /**
     * Gibt den Transformierten HTML Code zurück
     */
    public String showTransformation() {
        try {
            cleaner = new HtmlCleaner();
            CleanerProperties props = cleaner.getProperties();
            return (new SimpleXmlSerializer(props).getXmlAsString(cleaner.clean(source)));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * Gibt den ersten Treffer zurück
     */
    public String getFirstMatch() {
        try {
            NodeList result = (NodeList) xpath.compile(query + "/text()").evaluate(doc, XPathConstants.NODESET);
            return result.item(0).getNodeValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * Gibt einen beliebigen Treffer zurück
     */
    public String getFirstMatch(int group) {
        try {
            NodeList result = (NodeList) xpath.compile(query + "/text()").evaluate(doc, XPathConstants.NODESET);
            return result.item(group).getNodeValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * Gibt den alle Treffer zurück
     */
    public ArrayList<String> getMatches() {
        ArrayList<String> erg = new ArrayList<String>();
        try {
            NodeList result = (NodeList) xpath.compile(query + "/text()").evaluate(doc, XPathConstants.NODESET);
            for(int i=0; i<result.getLength(); i++) {
                erg.add(result.item(i).getNodeValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return erg;
    }
    
    /**
     * Gibt von allen Treffern ein bestimmtes Attribut zurück
     */
    public ArrayList<String> getAttributeMatches(String attribute) {
        ArrayList<String> erg = new ArrayList<String>();
        try {
            NodeList result = (NodeList) xpath.compile(query).evaluate(doc, XPathConstants.NODESET);
            int attr = 0;
            for(int j=0; j<result.item(0).getAttributes().getLength();j++) {
                if(result.item(0).getAttributes().item(j).getNodeValue().equals(attribute))
                    attr = j;
            }
            for(int i=0; i<result.getLength(); i++) {
                erg.add(result.item(i).getAttributes().item(attr).getNodeValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return erg;
    }
    
    /**
     * Gibt den ersten Treffer mit einem bestimmten Attribut zurück
     */
    public String getFirstAttributeMatch(String attribute) {
        try {
            NodeList result = (NodeList) xpath.compile(query).evaluate(doc, XPathConstants.NODESET);
            int attr = 0;
            for(int j=0; j<result.item(0).getAttributes().getLength();j++) {
                if(result.item(0).getAttributes().item(j).getNodeValue().equals(attribute))
                    attr = j;
            }
            
            return result.item(0).getAttributes().item(attr).getNodeValue();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * Gibt von einen bestimmten Treffer mit einem bestimmten Attribut zurück
     */
    public String getAttributeMatch(String attribute, int group) {
        try {
            NodeList result = (NodeList) xpath.compile(query).evaluate(doc, XPathConstants.NODESET);
            int attr = 0;
            for(int j=0; j<result.item(0).getAttributes().getLength();j++) {
                if(result.item(0).getAttributes().item(j).getNodeValue().equals(attribute))
                    attr = j;
            }
            
            return result.item(group).getAttributes().item(attr).getNodeValue();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    /**
     * Gibt die Anzahl der Treffer zurück
     */
    public int getCount() {
        try {
            NodeList result = (NodeList) xpath.compile(query).evaluate(doc, XPathConstants.NODESET);
            
            return result.getLength();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
