package org.jabylon.csharp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.StringReader;

import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

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
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

public class CSharpConverter implements PropertyConverter{

	private static final String NAME_ATTRIBUTE = "name";
	private static final String XML_SPACE = "xml:space";
	private static final String ROOT_NODE = "root";
	private static final String DATA = "data";
	private static final String TYPE = "type";
	private static final String SYSTEM_STRING = "System.String";

	private static final String NO_TRANS = "No_Translation";							// denotes a property a non-candidate for translation (most of the properties are non-candidates)

	private static final Logger LOG = LoggerFactory.getLogger(CSharpConverter.class);

	/**
	 * allows to disable pretty print for unit tests
	 */
	private boolean prettyPrint = true;

	private URI uri;

	private String comment;

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

			NodeList nodes = resources.getChildNodes();
		
			int nodesLength = nodes.getLength();
			LOG.info("C#:load4, nodesLength: " + nodesLength);

			for (int i = nodesLength-1; i >= 0; i--) {
				Node node = nodes.item(i);
				LOG.info("C#:load5, child node_" + i + " Name: " + node.getNodeName() + " Value: " + node.getNodeValue() + " Content:" + node.getTextContent() + " Type: " + node.getNodeType());
				
				if (true == loadNode(node,file)) {
					// 5/14/2019: Now we try plan B and replace the values in the xml file therefore there is no removal anymore of the Jabylon treated nodes
					//resources.removeChild(node);
					//LOG.info("C#:load6, child node_" + i + " removed. nodes.getLength afterwards: " + nodes.getLength());
				}
			}

			// Build a new Document includes the nodes getting treated by Jabylon
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
		    
			file.setLicenseHeader(resFileAsString);		// we take the license header to store everything from our resx file that should not be treated by Jabylon 
			
			return file;

		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}
	

	/**
	 * 
	 * @param node
	 * @param file
	 * @return true:  node is provided for translation
	 *         false: node is not provided for translation and therefore transferred to licenseHeader for not being lost (no child left behind)
	 */
	private boolean loadNode(Node node, PropertyFile file) {
		Property property = PropertiesFactory.eINSTANCE.createProperty();
		String name = node.getNodeName();

		LOG.info("C#:loadNode1, Name: " + name);
		
		if(false == name.equals(DATA)){
			LOG.info("C#:loadNode2, NodeName not appropriate");
			return false;
		}
		
		NamedNodeMap namedNodeMap = node.getAttributes();
		if (null != namedNodeMap) {
			for (int j=0; j<namedNodeMap.getLength(); j++) {
				Node attribute = namedNodeMap.item(j);
				String value = attribute.getNodeValue();
				LOG.info(" " + j + " Name: " + attribute.getNodeName() + " Value: " + value + " Content:" + attribute.getTextContent() + " Type: " + attribute.getNodeType());
				if (value.startsWith(">>")) {
					LOG.info("C#:loadNode3, node value not appropriate");
					return false;
				}
				if (true == attribute.getNodeName().equals(TYPE)) {
					if (false == attribute.getNodeValue().equals(SYSTEM_STRING)) {
						LOG.info("C#:loadNode4, type attribute found which is not appropriate+");
						return false;
					}
				}
			}
		}
		
		if (false == loadString(node, property)) {
			LOG.info("C#:loadNode5, node not appropiate");
			return false;
		}
		
		// for analysis purpose
		EList<PropertyAnnotation> eListPropAnn = property.getAnnotations();
		int nCount = eListPropAnn.size();
		LOG.info("C#:loadNode6, was saved? nCount: " + nCount);

		// made our annotations disappear:
		//property.setComment(comment);
		
		LOG.info("C#:loadNode7, comment: " + comment);
		comment = null;
		file.getProperties().add(property);
		LOG.info("C#:loadNode8, comment: " + comment);
		return true;
	}

	
	/**
	 * 
	 * @param node	e.g.: 	<data name="labelProductName.Text" xml:space="preserve">
    							<value>Banana.AutoCode</value>
  							</data>
	 * @param property
	 * @return true:  node is provided for translation
	 *         false: node is not provided for translation and therefore transferred to licenseHeader for not being lost
	 */
	private boolean loadString(Node node, Property property) {
		LOG.info("C#:loadString1, node.getTextContent(): " + node.getTextContent() + " property: " + property.getValue() + " type: " + node.getNodeType());
		
		NamedNodeMap namedNodeMap = node.getAttributes();
		
		if (null == namedNodeMap) {
			LOG.info("C#:loadString2, namedNodeMap is null");
			return false;
		}

		Node namedNode = namedNodeMap.getNamedItem(NAME_ATTRIBUTE);

		if (null == namedNode) {
			LOG.info("C#:loadString3, NAME_ATTRIBUTE not found");
			return false;
		}

		String textContentName = namedNode.getTextContent();

		LOG.info("C#:loadString4, textContentName: " + textContentName);

		String key = namedNode.getNodeValue();
		property.setKey(key);
		
		LOG.info("C#:loadString6, provided for translation ");
		
		Node valueNode = getValueNode(node);
		if (null != valueNode) {
			property.setValue(valueNode.getTextContent());
		}
		return true;
	}

	
	private Node getValueNode(Node node) {
		NodeList childNodes = node.getChildNodes();
		
		if (null != childNodes) {
			int numberOfChildren = childNodes.getLength();
			
			LOG.debug("C#:getValueNode1, number of child nodes: " + numberOfChildren);
			
			for (int i=0; i<numberOfChildren; i++) {
				Node childNode = childNodes.item(i);
				String nodeName = childNode.getNodeName();
				LOG.info("C#:getValueNode2, node name: " + nodeName);
				if (0 == nodeName.compareTo("value")) {
					return childNode;
				}
			}
		}			
		return null;
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
				
	            Document docLicenseHeader = null;
	            try  
	            {
	            	document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(licenseHeader)));
	            } catch (Exception e) {  
	            	LOG.error("C#:write8 Exception when rewriting the non translatable part of the .resx file", e);  
	            } 
			}
			
			EList<Property> properties = file.getProperties();
			
			LOG.info("C#:write9, Count Properties: " + properties.size());
			
			for (Property property : properties) {
				LOG.info("C#:write10, property: " + property.toString());
				
				try {
					XPath xPath = XPathFactory.newInstance().newXPath();
				
					String searchStr = "//data[@name=\"" + property.getKey() + "\"]";
					
					LOG.info("C#:write11, searchstr: " + searchStr);
					
					Node nodeWithValue = (Node)xPath.evaluate(searchStr, document, XPathConstants.NODE);
					
					Node valueNode = getValueNode(nodeWithValue);
					
					if (null != valueNode) {
						String newVal = property.getValue();
						
						// remove the carriage return:
						newVal = newVal.replaceAll("\\r", "");
						valueNode.setTextContent(newVal);
					}
				} catch (Exception e) {
					LOG.info("C#:write16 ", e);
				}
			}

			LOG.info("C#:write17");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			if(prettyPrint){
				LOG.info("C#:write18");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(out);

			LOG.info("C#:write19");

			transformer.transform(source, result);

			LOG.info("C#:write20");

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

		// property was created by a translatable xml-element. So write back the translation
		writeString(root,document,property);					
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


	private boolean isFilled(String s){
		LOG.info("C#:isFilled1, s: " + s);
		return s!=null && !s.isEmpty();
	}
}