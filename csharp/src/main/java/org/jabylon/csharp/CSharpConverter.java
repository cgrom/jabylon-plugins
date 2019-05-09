package org.jabylon.csharp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.StringReader;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.jabylon.properties.PropertiesFactory;
import org.jabylon.properties.PropertiesPackage;
import org.jabylon.properties.Property;
import org.jabylon.properties.PropertyAnnotation;
import org.jabylon.properties.PropertyFile;
import org.jabylon.properties.types.PropertyAnnotations;
import org.jabylon.properties.types.PropertyConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

public class CSharpConverter implements PropertyConverter{

	private static final String NAME_ATTRIBUTE = "name";
	private static final String ROOT_NODE = "root";
	private static final String DATA = "data";

	private static final String NO_TRANS = "No_Translation";							// denotes a property a non-candidate for translation (most of the properties are non-candidates)

	private static final Logger LOG = LoggerFactory.getLogger(CSharpConverter.class);

	/**
	 * allows to disable pretty print for unit tests
	 */
	private boolean prettyPrint = true;

	private URI uri;

	private String comment;

	// If LOG... is running stable we can remove the System.out.println statemenets entirely


	public CSharpConverter(URI resourceLocation, boolean prettyPrint) {
		this.uri = resourceLocation;
		this.prettyPrint = prettyPrint;
		LOG.info("C#:CSharpConverter1");
	}


	
	@Override
	public PropertyFile load(InputStream in, String encoding) throws IOException {
		// TODO Auto-generated method stub

	    LOG.info("C#:load0, in: " + in.toString());

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document result = builder.parse(in);
			PropertyFile file = PropertiesFactory.eINSTANCE.createPropertyFile();

			Node resources = result.getDocumentElement();

			LOG.info("C#:load2, resources, TextContent: " + resources.getTextContent());
			
			LOG.info("C#:load3,  result.getTextContent: " + result.getTextContent());

			NodeList nodes = resources.getChildNodes();
		
			int nodesLength = nodes.getLength();
			LOG.info("C#:load4, nodesLength: " + nodesLength);

			for (int i = nodesLength-1; i >= 0; i--) {
				Node node = nodes.item(i);
				LOG.info("C#:load5, child node_" + i + " Name: " + node.getNodeName() + " Value: " + node.getNodeValue() + " Content:" + node.getTextContent() + " Type: " + node.getNodeType());
				
				if (true == loadNode(node,file)) {
					resources.removeChild(node);
					LOG.info("C#:load6, child node_" + i + " removed. nodes.getLength afterwards: " + nodes.getLength());
				}
			}

			// Build a new Document without the nodes being treated by Jabylon
			DocumentBuilderFactory docBuilderFact = DocumentBuilderFactory.newInstance();
			docBuilderFact.setNamespaceAware(true);
			DocumentBuilder docBuilder = docBuilderFact.newDocumentBuilder();
			Document newDoc = docBuilder.newDocument();
			Node importedNode = newDoc.importNode(resources, true);
			newDoc.appendChild(importedNode);

			StringWriter sw = new StringWriter();
			String resFileAsString = "";
			
			Node firstNode = newDoc.getChildNodes().item(0);
			
		    try {
		      Transformer t = TransformerFactory.newInstance().newTransformer();
		      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		      t.setOutputProperty(OutputKeys.INDENT, "yes");
		      t.transform(new DOMSource(firstNode), new StreamResult(sw));
		      resFileAsString = sw.toString();
		      LOG.info("C#:load7, resFileAsString: " + resFileAsString);
		    } catch (TransformerException te) {
		      LOG.error("C#:load8, nodeToString Transformer exception: ", te);
		    }
		    
			file.setLicenseHeader(resFileAsString);
			
			return file;

		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}


	private boolean  	// = true: node is provided for translation
						// = false: node is not provided for translation and therefore transferred to licenseHeader for not being lost
	loadNode(Node node, PropertyFile file) {

		Property property = PropertiesFactory.eINSTANCE.createProperty();
		String name = node.getNodeName();

		LOG.info("C#:loadNode1, Name: " + name);

		if (false == loadString(node, property)) {
			LOG.info("C#:loadNode2, node not appropiate");
			return false;
		}
		
		// for analysis purpose
		EList<PropertyAnnotation> eListPropAnn = property.getAnnotations();
		int nCount = eListPropAnn.size();
		LOG.info("C#:loadNode3, was saved? nCount: " + nCount);

		property.setComment(comment);
		
		LOG.info("C#:loadNode4, comment: " + comment);
		comment = null;
		file.getProperties().add(property);
		LOG.info("C#:loadNode5, comment: " + comment);
		return true;
	}


	private boolean hasTypeAttribute(Node node) {
		boolean hasTypeAttribute = false;
		
		NamedNodeMap namedNodeMap = node.getAttributes();
		if (null != namedNodeMap) {
			LOG.info("C#:hasTypeAttribute1, namedNodeMap.toString: " + namedNodeMap.toString());
			for (int j=0; j<namedNodeMap.getLength(); j++) {
				Node namedNode = namedNodeMap.item(j);
				LOG.info("C#:hasTypeAttribute2, namedNode_" + j + " Name: " + namedNode.getNodeName() + " Value: " + namedNode.getNodeValue() + " Content:" + namedNode.getTextContent() + " Type: " + namedNode.getNodeType() + " toString: " + namedNode.toString());
				if (("type" == namedNode.getNodeName()) && (2 == namedNode.getNodeType())) {
					hasTypeAttribute = true;
					break;
				}
			}
		}
	
		return hasTypeAttribute;
	}

	
	private boolean loadString(Node node, Property property) {
		
		LOG.info("C#:loadString1, node.getTextContent(): " + node.getTextContent() + " property: " + property.getValue());
		
		NamedNodeMap namedNodeMap = node.getAttributes();
		
		if (null == namedNodeMap) {
			LOG.info("C#:loadString2, namedNodeMap is null");
			return false;
		}

		Node name = namedNodeMap.getNamedItem(NAME_ATTRIBUTE);

		if (null == name) {
			LOG.info("C#:loadString3, NAME_ATTRIBUTE not found");
			return false;
		}

		String textContentName = name.getTextContent();

		LOG.info("C#:loadString4, textContentName: " + textContentName);

		String key = name.getNodeValue();
		property.setKey(key);
		
		String textContentNode = node.getTextContent();
		LOG.info("C#:loadString5, textContentNode: " + textContentNode);
		
		// most of the xml elements in a C# resource file are not for translation
		String nodeName = node.getNodeName();
		if (false == isCandidateForTranslation(key, nodeName)) {
			PropertyAnnotation annotation = PropertiesFactory.eINSTANCE.createPropertyAnnotation();

			annotation.setName(PropertyAnnotations.NON_TRANSLATABLE);
			
			// for better debugging:
			//property.getAnnotations().add(annotation);
			EList<PropertyAnnotation> eListPropAnn = property.getAnnotations();
			int nCount = eListPropAnn.size();
			LOG.info("C#:loadString7, nCount annotations before adding: " + nCount);
			eListPropAnn.add(annotation);
			
			nCount = eListPropAnn.size();
			LOG.info("C#:loadString8, nCount annotations after adding: " + nCount + " not provided for translation");
		}
		else {															// provided for translation
			LOG.info("C#:loadString10, provided for translation ");
		}
		property.setValue(decode(textContentNode));
		return true;
	}


	private boolean isCandidateForTranslation(String key, String nodeName) {
		boolean bReturn = false;

		LOG.info("C#:isCandidateForTranslation1, key: " + key + " nodeName: " + nodeName);

		if (key.endsWith(".Text")) {
			LOG.info("C#:isCandidateForTranslation2, key ends with .Text");
			if(nodeName.equals(DATA)){
				LOG.info("C#:isCandidateForTranslation3, nodeName equals DATA");
				bReturn = true;
			}
		}
		LOG.info("C#:isCandidateForTranslation4, bReturn: " + bReturn);
		return bReturn;
	}


	/**
	 * decodes a string
	 * @param textContent
	 * @return the decoded string
	 */
	private String decode(String textContent) {
		if(textContent==null) {
			LOG.info("C#:decode1, textContent is null" );
			return null;
		}

		// Android code as a blueprint
		/*if(textContent.startsWith("\"") && textContent.endsWith("\""))
			return textContent.substring(1, textContent.length()-1);
		return textContent.replace("\\'", "'");*/
		
		LOG.info("C#:decode1, textContent: " + textContent);
		return textContent;
	}


	@Override
	public int write(OutputStream out, PropertyFile file, String encoding) throws IOException {
		try {
			LOG.info("C#:write1, file:" + file.toString() + " encoding: " + encoding);

			int counter = 0;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			LOG.info("C#:write2");
			
			String licenseHeader = file.getLicenseHeader(); 

			if(isFilled(licenseHeader))
			{
				LOG.info("C#:write3, licenseHeader: " + licenseHeader);

				// not as comment{
//				Comment commentLicenseHeader = document.createComment(licenseHeader);
//				LOG.info("C#:write4, commentLicenseHeader.getTextContent: " + commentLicenseHeader.getTextContent() + " commentLicenseHeader.toString(): " + commentLicenseHeader.toString());
//				Node nodeLicenseHeader = document.appendChild(commentLicenseHeader);
//				LOG.info("C#:write5, nodeLicenseHeader.getTextContent: " + nodeLicenseHeader.getTextContent() + " nodeLicenseHeader.toString(): " + nodeLicenseHeader.toString());
				// not as comment}
				
	            Document docLicenseHeader = null;
	            try  
	            {
	            	document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(licenseHeader)));
	            } catch (Exception e) {  
	            	LOG.error("C#:write8 Exception when rewriting the non translatable part of the .resx file", e);  
	            } 
			}

			LOG.info("C#:write9");

			Element root = document.createElement(ROOT_NODE);
			document.appendChild(root);
				
			EList<Property> properties = file.getProperties();
			for (Property property : properties) {
				LOG.info("C#:write10, property: " + property.toString());
				if(writeProperty(root, document, property)) {
					counter++;
					LOG.info("C#:write11, counter: " + counter);
				}
			}

			LOG.info("C#:write12");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			if(prettyPrint){
				LOG.info("C#:write13");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(out);

			LOG.info("C#:write14");

			transformer.transform(source, result);

			LOG.info("C#:write15");

			return counter;
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException(e);
		} finally{
			out.close();
		}
	}


	private boolean writeProperty(Element root, Document document, Property property) throws IOException {

		LOG.info("C#:writeProperty1");

		String value = property.getValue();

		LOG.info("C#:writeProperty2, value of property: " + value);

		if(!isFilled(value)) {
			LOG.info("C#:writeProperty3, value not filled");
			return false;
		}

		if (false == writeCommentAndAnnotations(root, document, property)) {
			LOG.info("C#:writeProperty4 call to writeString");
			writeString(root,document,property);					// property was created by a translatable xml-element. So write back the translation
		}
		return true;
	}


	private void writeString(Element root, Document document, Property property) {
		LOG.info("C#:writeString1");
		Element data = document.createElement(DATA);
		LOG.info("C#:writeString2");
		root.appendChild(data);

		String key = property.getKey();
		LOG.info("C#:writeString3, key: " + key);
		data.setAttribute(NAME_ATTRIBUTE, key);

		String value = property.getValue();
		LOG.info("C#:writeString4, value: " + value);
		data.setTextContent(encode(value));
	}


	private String encode(String textContent) {
		if(textContent==null) {
			LOG.info("C#:encode1, textContent is null");
			return null;
		}

		textContent = textContent.replace("'", "\\'").replace("\"", "\\\"");
		LOG.info("C#:encode2, textContent: " + textContent);

		return textContent;
	}


	/**
	 *
	 * @param root
	 * @param document
	 * @param property
	 * @return	= true:  non translatable property
	 * 			= false: translatable property, the caller has to care for the property
	 * @throws IOException
	 */
	private boolean writeCommentAndAnnotations(Element root, Document document, Property property) throws IOException {

		boolean bReturn = false;

		LOG.info("C#:writeCommentAndAnnotations1, property: " + property.toString() + " property.getAnnotations().size(): " + property.getAnnotations().size());

        if(property.eIsSet(PropertiesPackage.Literals.PROPERTY__COMMENT) || property.getAnnotations().size()>0)
        {
        	LOG.info("C#:writeCommentAndAnnotations2");

        	String comment = property.getCommentWithoutAnnotations();

        	LOG.info("C#:writeCommentAndAnnotations3, comment: " + comment);

        	StringBuilder builder = new StringBuilder();

        	PropertyAnnotation nonTranslatable = property.findAnnotation(PropertyAnnotations.NON_TRANSLATABLE);
        	if (null != nonTranslatable) {
        		bReturn = true;						// this property is non-translatable
        		LOG.info("C#:writeCommentAndAnnotations4, property has NON_TRANSLATABLE annotation " + comment);
        	}

        	if(builder.length()>0 && comment!=null && comment.length()>0 )
        	{
        		builder.append("\n");
        	}
        	builder.append(comment);

        	String stringFromComment = builder.toString();

        	LOG.info("C#:writeCommentAndAnnotations5, stringFromComment: " + stringFromComment);

        	Comment node = document.createComment(stringFromComment);

        	LOG.info("C#:writeCommentAndAnnotations4, node: " + node.toString());

        	root.appendChild(node);
        }
        return bReturn;
	}

	private boolean isFilled(String s){
		LOG.info("C#:isFilled1, s: " + s);
		return s!=null && !s.isEmpty();
	}
}
