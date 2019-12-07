/*
 * This file is (slightly adapted) from
 *      http://www.extreme.indiana.edu/xmlpull-website/v1/addons/java/dom2_builder/src/org/xmlpull/v1/dom2_builder/DOM2XmlPullBuilder.java
 *
 * The LICENSE.txt referred to in the original banner comment below was located at
 * http://www.xmlpull.org/v1/download/unpacked/, and found to contain the following:
 *
 *    "XMLPULL API IS FREE
 *    -------------------
 *
 *    All of the XMLPULL API source code, compiled code, and documentation
 *    contained in this distribution *except* for tests (see separate LICENSE_TESTS.txt)
 *    are in the Public Domain.
 *
 *    XMLPULL API comes with NO WARRANTY or guarantee of fitness for any purpose.
 *
 *    Initial authors:
 *
 *      Stefan Haustein
 *      Aleksander Slominski
 *
 *    2001-12-12"
 *
 *          Robert Atkinson, 12 July 2016
 */

package org.firstinspires.ftc.robotcore.internal.system;

/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

//TOOD add parse methodthat will usenextToken() to reconstruct complete XML infoset

/**
 * <strong>Simplistic</strong> DOM2 builder that should be enough to do support most cases.
 * Requires JAXP DOMBuilder to provide DOM2 implementation.
 * <p>NOTE:this class is stateless factory and it is safe to share between multiple threads.
 * </p>
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Dom2XmlPullBuilder
    {
    protected final static boolean NAMESPACES_SUPPORTED = false;

    //protected XmlPullParser pp;
    //protected XmlPullParserFactory factory;

    public Dom2XmlPullBuilder() { //throws XmlPullParserException {
        //factory = XmlPullParserFactory.newInstance();
    }
    //public DOM2XmlPullBuilder(XmlPullParser pp) throws XmlPullParserException {
    //public DOM2XmlPullBuilder(XmlPullParserFactory factory)throws XmlPullParserException
    //{
    //    this.factory = factory;
    //}

    protected Document newDoc() throws XmlPullParserException {
        try {
            // if you see dreaded "java.lang.NoClassDefFoundError: org/w3c/dom/ranges/DocumentRange"
            // add the newest xercesImpl and apis JAR file to JRE/lib/endorsed
            // for more deatils see: http://java.sun.com/j2se/1.4.2/docs/guide/standards/index.html
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            DOMImplementation impl = builder.getDOMImplementation();
            return builder.newDocument();
        } catch (FactoryConfigurationError ex) {
            throw new XmlPullParserException(
                "could not configure factory JAXP DocumentBuilderFactory: "+ex, null, ex);
        } catch (ParserConfigurationException ex) {
            throw new XmlPullParserException(
                "could not configure parser JAXP DocumentBuilderFactory: "+ex, null, ex);
        }
    }

    protected XmlPullParser newParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        return factory.newPullParser();
    }

    public Element parse(Reader reader) throws XmlPullParserException, IOException {
        Document docFactory = newDoc();
        return parse(reader, docFactory);
    }

    public Element parse(Reader reader, Document docFactory)
        throws XmlPullParserException, IOException
    {
        XmlPullParser pp = newParser();
        pp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        pp.setInput(reader);
        pp.next();
        return parse(pp, docFactory);
    }

    public Element parse(XmlPullParser pp, Document docFactory)
        throws XmlPullParserException, IOException
    {
        Element root = parseSubTree(pp, docFactory);
        return root;
    }

    public Element parseSubTree(XmlPullParser pp) throws XmlPullParserException, IOException {
        Document doc = newDoc();
        Element root = parseSubTree(pp, doc);
        return root;
    }

    public Element parseSubTree(XmlPullParser pp, Document docFactory)
        throws XmlPullParserException, IOException
    {
        BuildProcess process = new BuildProcess();
        return process.parseSubTree(pp, docFactory);
    }

    static class BuildProcess {
        private XmlPullParser pp;
        private Document docFactory;
        private boolean scanNamespaces = true;

        private BuildProcess() {
        }

        public Element parseSubTree(XmlPullParser pp, Document docFactory)
            throws XmlPullParserException, IOException
        {
            this.pp = pp;
            this.docFactory = docFactory;
            return parseSubTree();
        }

        private Element parseSubTree()
            throws XmlPullParserException, IOException
        {
            // If we're at the start of the document, ignore that and go on to the root element
            if (pp.getEventType() == XmlPullParser.START_DOCUMENT) {
                while (pp.getEventType() != XmlPullParser.START_TAG) {
                    pp.next();
                }
            }

            pp.require(XmlPullParser.START_TAG, null, null);
            String name = pp.getName();
            String ns = pp.getNamespace();

            String prefix = null;

            if (NAMESPACES_SUPPORTED) {
                /**
                 * Not all pull parsers support prefixes. In particular, the Android
                 * {@link android.content.res.XmlResourceParser} does not, presumably because
                 * it's parsing a binary XML format.
                 */
                prefix = pp.getPrefix();
            }

            String qname = prefix != null ? prefix+":"+name : name;
            Element parent = docFactory.createElementNS(ns, qname);

            if (NAMESPACES_SUPPORTED) {
                //declare namespaces - quite painful and easy to fail process in DOM2
                declareNamespaces(pp, parent);
            }

            // process attributes
            for (int i = 0; i < pp.getAttributeCount(); i++)
            {
                String attrNs = pp.getAttributeNamespace(i);
                String attrName = pp.getAttributeName(i);
                String attrValue = pp.getAttributeValue(i);
                if(attrNs == null || attrNs.length() == 0) {
                    parent.setAttribute(attrName, attrValue);
                } else {
                    String attrPrefix = pp.getAttributePrefix(i);
                    String attrQname = attrPrefix != null ? attrPrefix+":"+attrName : attrName;
                    parent.setAttributeNS(attrNs, attrQname, attrValue);
                }
            }

            // process children
            while( pp.next() != XmlPullParser.END_TAG ) {
                if (pp.getEventType() == XmlPullParser.START_TAG) {
                    Element el = parseSubTree(pp, docFactory);
                    parent.appendChild(el);
                } else if (pp.getEventType() == XmlPullParser.TEXT) {
                    String text = pp.getText();
                    Text textEl = docFactory.createTextNode(text);
                    parent.appendChild(textEl);
                } else {
                    throw new XmlPullParserException(
                        "unexpected event "+XmlPullParser.TYPES[ pp.getEventType() ], pp, null);
                }
            }
            pp.require( XmlPullParser.END_TAG, ns, name);
            return parent;
        }

        private void declareNamespaces(XmlPullParser pp, Element parent)
            throws DOMException, XmlPullParserException
        {
            if (scanNamespaces) {
                scanNamespaces = false;
                int top = pp.getNamespaceCount(pp.getDepth()) - 1;
                // this loop computes list of all in-scope prefixes
                LOOP:
                for (int i = top; i >= pp.getNamespaceCount(0); --i)
                {
                    // make sure that no prefix is duplicated
                    String prefix = pp.getNamespacePrefix(i);
                    for (int j = top; j > i; --j)
                    {
                        String prefixJ = pp.getNamespacePrefix(j);
                        if((prefix != null && prefix.equals(prefixJ))
                               || (prefix != null && prefix == prefixJ) )
                        {
                            // prefix is already declared -- skip it
                            continue LOOP;
                        }
                    }
                    declareOneNamespace(pp, i, parent);
                }
            } else {
                for (int i = pp.getNamespaceCount(pp.getDepth()-1);
                         i < pp.getNamespaceCount(pp.getDepth());
                     ++i)
                {
                    declareOneNamespace(pp, i, parent);
                }
            }
        }

        private void declareOneNamespace(XmlPullParser pp, int i, Element parent)
            throws DOMException, XmlPullParserException {
            String xmlnsPrefix = pp.getNamespacePrefix(i);
            String xmlnsUri = pp.getNamespaceUri(i);
            String xmlnsDecl = (xmlnsPrefix != null) ? "xmlns:"+xmlnsPrefix : "xmlns";
            parent.setAttributeNS("http://www.w3.org/2000/xmlns/", xmlnsDecl, xmlnsUri);
        }
    }
}

