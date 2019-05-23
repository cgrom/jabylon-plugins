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
	private static final String COMMENT = "comment";
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
		LOG.debug("C#:CSharpConverter1");
	}

	
	@Override
	public PropertyFile load(InputStream in, String encoding) throws IOException {
		// TODO Auto-generated method stub

	    LOG.debug("C#:load0, in: " + in.toString());

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document result = builder.parse(in);
			PropertyFile file = PropertiesFactory.eINSTANCE.createPropertyFile();

			Node resources = result.getDocumentElement();

			LOG.debug("C#:load2, resources, TextContent: " + resources.getTextContent());

			NodeList nodes = resources.getChildNodes();
		
			int nodesLength = nodes.getLength();
			LOG.debug("C#:load4, nodesLength: " + nodesLength);

			for (int i = nodesLength-1; i >= 0; i--) {
				Node node = nodes.item(i);
				LOG.debug("C#:load5, child node_" + i + " Name: " + node.getNodeName() + " Value: " + node.getNodeValue() + " Content:" + node.getTextContent() + " Type: " + node.getNodeType());
				
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
		      LOG.debug("C#:load7, resFileAsString: " + resFileAsString);
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

		LOG.debug("C#:loadNode1, Name: " + name);
		
		if(false == name.equals(DATA)){
			LOG.debug("C#:loadNode2, NodeName not appropriate");
			return false;
		}
		
		NamedNodeMap namedNodeMap = node.getAttributes();
		if (null != namedNodeMap) {
			for (int j=0; j<namedNodeMap.getLength(); j++) {
				Node attribute = namedNodeMap.item(j);
				String value = attribute.getNodeValue();
				LOG.debug("C#:loadNode3, attribute_" + j + " Name: " + attribute.getNodeName() + " Value: " + value + " Content:" + attribute.getTextContent() + " Type: " + attribute.getNodeType());
				if (value.startsWith(">>")) {
					LOG.debug("C#:loadNode4, node value not appropriate");
					return false;
				}
				if (true == attribute.getNodeName().equals(TYPE)) {
					if (false == attribute.getNodeValue().equals(SYSTEM_STRING)) {
						LOG.debug("C#:loadNode5, type attribute found which is not appropriate+");
						return false;
					}
				}
			}
		}
		
		// search for comment
		NodeList childNodes = node.getChildNodes();
		
		int childNodesLength = childNodes.getLength();
		LOG.debug("C#:loadNode6, nodesLength: " + childNodesLength);

		for (int k = childNodesLength-1; k >= 0; k--) {
			Node childNode = childNodes.item(k);
			String nodeName = childNode.getNodeName();
			String nodeContent = childNode.getTextContent();
			LOG.debug("C#:loadNode7, childNode_" + k + " Name: " + nodeName + " Value: " + childNode.getNodeValue() + " Content: " + nodeContent + " Type: " + childNode.getNodeType());
			if (null != nodeName) {
				if (nodeName.equals(COMMENT)) {
					LOG.debug("C#:loadNode8, comment found");
					comment = nodeContent;
				}
			}
		}
		
		if (false == loadString(node, property)) {
			LOG.debug("C#:loadNode9, node not appropiate");
			return false;
		}

		if (null != comment) {
			LOG.debug("C#:loadNode10, comment was set: " + comment);
			property.setComment(comment);
		}
		
		comment = null;
		file.getProperties().add(property);
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
		LOG.debug("C#:loadString1, node.getTextContent(): " + node.getTextContent() + " property: " + property.getValue() + " type: " + node.getNodeType());
		
		NamedNodeMap namedNodeMap = node.getAttributes();
		
		if (null == namedNodeMap) {
			LOG.error("C#:loadString2, namedNodeMap is null");
			return false;
		}

		Node namedNode = namedNodeMap.getNamedItem(NAME_ATTRIBUTE);

		if (null == namedNode) {
			LOG.error("C#:loadString3, NAME_ATTRIBUTE not found");
			return false;
		}

		String textContentName = namedNode.getTextContent();

		LOG.debug("C#:loadString4, textContentName: " + textContentName);

		String key = namedNode.getNodeValue();
		property.setKey(key);
		
		LOG.debug("C#:loadString6, provided for translation ");
		
		Node valueNode = getValueNode(node);
		if (null != valueNode) {
			property.setValue(valueNode.getTextContent());
		}
		return true;
	}

	
	private Node getValueNode(Node node) {
		LOG.debug("C#:getValueNode1");
		Node childNode = getChildNode(node, "value");
		return childNode;
	}

	
	private Node getCommentNode(Node node) {
		LOG.debug("C#:getCommentNode1");
		Node childNode = getChildNode(node, COMMENT);
		return childNode;
	}
	
	
	private Node getChildNode(Node node, String desiredNodeName) {
		LOG.debug("C#:getChildNode1, desiredNodeName: " + desiredNodeName);
		NodeList childNodes = node.getChildNodes();
		if (null != childNodes) {
			int numberOfChildren = childNodes.getLength();
			LOG.debug("C#:getChildNode2, number of child nodes: " + numberOfChildren);
			
			for (int i=0; i<numberOfChildren; i++) {
				Node childNode = childNodes.item(i);
				String nodeName = childNode.getNodeName();
				LOG.debug("C#:getChildNode3, node name: " + nodeName);
				if (0 == nodeName.compareTo(desiredNodeName)) {
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
			LOG.error("C#:decode1, textContent is null" );
			return null;
		}
		LOG.debug("C#:decode2, textContent: " + textContent);
		return textContent;
	}


	@Override
	public int write(OutputStream out, PropertyFile file, String encoding) throws IOException {
		try {
			LOG.debug("C#:write1, file:" + file.toString() + " encoding: " + encoding);

			int counter = 0;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			LOG.debug("C#:write2");
			
			String licenseHeader = file.getLicenseHeader(); 

			if(isFilled(licenseHeader))
			{
				LOG.debug("C#:write3, licenseHeader: " + licenseHeader);
				
	            Document docLicenseHeader = null;
	            try  
	            {
	            	document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(licenseHeader)));
	            } catch (Exception e) {  
	            	LOG.error("C#:write8 Exception when rewriting the non translatable part of the .resx file", e);  
	            } 
			}
			
			EList<Property> properties = file.getProperties();
			
			LOG.debug("C#:write9, Count Properties: " + properties.size());
			
			for (Property property : properties) {
				
				LOG.debug("C#:write10, property: " + property.toString());
				
				try {
					XPath xPath = XPathFactory.newInstance().newXPath();
				
					String searchStr = "//data[@name=\"" + property.getKey() + "\"]";
					
					LOG.debug("C#:write11, searchstr: " + searchStr);
					
					Node nodeWithValue = (Node)xPath.evaluate(searchStr, document, XPathConstants.NODE);
					
					LOG.debug("C#:write12, nodeWithValue.getNodeName: " + nodeWithValue.getNodeName() + " nodeWithValue.getTextContent: " + nodeWithValue.getTextContent() + " nodeWithValue.getNodeValue: " + nodeWithValue.getNodeValue()  + " nodeWithValue.getNodeType: " + nodeWithValue.getNodeType());
					
					Node valueNode = getValueNode(nodeWithValue);
					if (null != valueNode) {
						String newVal = property.getValue();
						// remove the carriage return:
						newVal = newVal.replaceAll("\\r", "");
						LOG.debug("C#:write13, newVal: " + newVal);
						valueNode.setTextContent(newVal);
					}

					
					String newComment = property.getComment();
					// remove the carriage return:
					newComment = newComment.replaceAll("\\r", "");
					if (null != newComment) {
						LOG.debug("C#:write14, newComment: " + newComment);
						Node commentNode = getCommentNode(nodeWithValue);
						if (null != commentNode) {
							LOG.debug("C#:write15, comment replaced");
							commentNode.setTextContent(newComment);
						}
						else {
							Node newNode = nodeWithValue.appendChild(document.createElement(COMMENT));
							LOG.debug("C#:write16, comment inserted");
					        newNode.setTextContent(newComment);
						}
					}
					
				} catch (Exception e) {
					LOG.error("C#:write17 ", e);
				}
			}

			LOG.debug("C#:write18");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			if(prettyPrint){
				LOG.debug("C#:write18");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(out);

			LOG.debug("C#:write19");

			transformer.transform(source, result);

			LOG.debug("C#:write20");

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
		LOG.debug("C#:writeProperty1");
		String value = property.getValue();
		LOG.debug("C#:writeProperty2, value of property: " + value);
		if(!isFilled(value)) {
			LOG.error("C#:writeProperty3, value not filled");
			return false;
		}
		
		writeCommentAndAnnotations(root, document, property);
		
		// property was created by a translatable xml-element. So write back the translation
		writeString(root,document,property);					
		return true;
	}


	private void writeCommentAndAnnotations(Element root, Document document, Property property) throws IOException {
		LOG.debug("C#:writeCommentAndAnnotations1");
        if(property.eIsSet(PropertiesPackage.Literals.PROPERTY__COMMENT) || property.getAnnotations().size()>0)
        {
        	LOG.debug("C#:writeCommentAndAnnotations2");
        	String comment = property.getCommentWithoutAnnotations();
        	LOG.debug("C#:writeCommentAndAnnotations3, comment: " + comment);
        	StringBuilder builder = new StringBuilder();
        	for (PropertyAnnotation annotation : property.getAnnotations()) {
				builder.append(annotation);
				LOG.debug("C#:writeCommentAndAnnotations4, builder: " + builder.toString());
			}
        	if(builder.length()>0 && comment!=null && comment.length()>0 )
        	{
        		builder.append("\n");
        		LOG.debug("C#:writeCommentAndAnnotations5, builder: " + builder.toString());
        	}
        	builder.append(comment);
        	String builtComment = builder.toString();
        	LOG.debug("C#:writeCommentAndAnnotations6, builder: " + builtComment);
        	Comment node = document.createComment(builtComment);
        	root.appendChild(node);
        	LOG.debug("C#:writeCommentAndAnnotations7");
        }
	}

	
	private void writeString(Element root, Document document, Property property) {
		LOG.debug("C#:writeString1");
		Element data = document.createElement(DATA);
		LOG.debug("C#:writeString2");
		root.appendChild(data);

		String key = property.getKey();
		LOG.debug("C#:writeString3, key: " + key);
		data.setAttribute(NAME_ATTRIBUTE, key);

		String value = property.getValue();
		LOG.debug("C#:writeString4, value: " + value);
		data.setTextContent(encode(value));
	}


	private String encode(String textContent) {
		if(textContent==null) {
			LOG.debug("C#:encode1, textContent is null");
			return null;
		}
		textContent = textContent.replace("'", "\\'").replace("\"", "\\\"");
		LOG.debug("C#:encode2, textContent: " + textContent);
		return textContent;
	}


	private boolean isFilled(String s){
		LOG.debug("C#:isFilled1, s: " + s);
		return s!=null && !s.isEmpty();
	}
}