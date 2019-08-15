package org.jabylon.csharp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ContentHandler.ByteOrderMark;
import org.jabylon.properties.PropertiesFactory;
import org.jabylon.properties.Property;
import org.jabylon.properties.PropertyFile;
import org.jabylon.properties.types.PropertyConverter;
import org.jabylon.properties.types.impl.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CSharpConverter implements PropertyConverter{

	private static final String NAME_ATTRIBUTE = "name";
	private static final String XML_SPACE = "xml:space";
	private static final String DATA = "data";
	private static final String COMMENT = "comment";
	private static final String PRESERVE = "preserve";
	private static final String VALUE = "value";
	private static final String TYPE = "type";
	private static final String SYSTEM_STRING = "System.String";
	private static final String INVARIANT = "@Invariant";			// denotes non translatable content
	private static final String ROOT_NODE = "root";
	private static final String EMPTY_RES_FILE = "<root>\r\n" + 
			"  <!-- \r\n" + 
			"    Microsoft ResX Schema \r\n" + 
			"    \r\n" + 
			"    Version 2.0\r\n" + 
			"    \r\n" + 
			"    The primary goals of this format is to allow a simple XML format \r\n" + 
			"    that is mostly human readable. The generation and parsing of the \r\n" + 
			"    various data types are done through the TypeConverter classes \r\n" + 
			"    associated with the data types.\r\n" + 
			"    \r\n" + 
			"    Example:\r\n" + 
			"    \r\n" + 
			"    ... ado.net/XML headers & schema ...\r\n" + 
			"    <resheader name=\"resmimetype\">text/microsoft-resx</resheader>\r\n" + 
			"    <resheader name=\"version\">2.0</resheader>\r\n" + 
			"    <resheader name=\"reader\">System.Resources.ResXResourceReader, System.Windows.Forms, ...</resheader>\r\n" + 
			"    <resheader name=\"writer\">System.Resources.ResXResourceWriter, System.Windows.Forms, ...</resheader>\r\n" + 
			"    <data name=\"Name1\"><value>this is my long string</value><comment>this is a comment</comment></data>\r\n" + 
			"    <data name=\"Color1\" type=\"System.Drawing.Color, System.Drawing\">Blue</data>\r\n" + 
			"    <data name=\"Bitmap1\" mimetype=\"application/x-microsoft.net.object.binary.base64\">\r\n" + 
			"        <value>[base64 mime encoded serialized .NET Framework object]</value>\r\n" + 
			"    </data>\r\n" + 
			"    <data name=\"Icon1\" type=\"System.Drawing.Icon, System.Drawing\" mimetype=\"application/x-microsoft.net.object.bytearray.base64\">\r\n" + 
			"        <value>[base64 mime encoded string representing a byte array form of the .NET Framework object]</value>\r\n" + 
			"        <comment>This is a comment</comment>\r\n" + 
			"    </data>\r\n" + 
			"                \r\n" + 
			"    There are any number of \"resheader\" rows that contain simple \r\n" + 
			"    name/value pairs.\r\n" + 
			"    \r\n" + 
			"    Each data row contains a name, and value. The row also contains a \r\n" + 
			"    type or mimetype. Type corresponds to a .NET class that support \r\n" + 
			"    text/value conversion through the TypeConverter architecture. \r\n" + 
			"    Classes that don't support this are serialized and stored with the \r\n" + 
			"    mimetype set.\r\n" + 
			"    \r\n" + 
			"    The mimetype is used for serialized objects, and tells the \r\n" + 
			"    ResXResourceReader how to depersist the object. This is currently not \r\n" + 
			"    extensible. For a given mimetype the value must be set accordingly:\r\n" + 
			"    \r\n" + 
			"    Note - application/x-microsoft.net.object.binary.base64 is the format \r\n" + 
			"    that the ResXResourceWriter will generate, however the reader can \r\n" + 
			"    read any of the formats listed below.\r\n" + 
			"    \r\n" + 
			"    mimetype: application/x-microsoft.net.object.binary.base64\r\n" + 
			"    value   : The object must be serialized with \r\n" + 
			"            : System.Serialization.Formatters.Binary.BinaryFormatter\r\n" + 
			"            : and then encoded with base64 encoding.\r\n" + 
			"    \r\n" + 
			"    mimetype: application/x-microsoft.net.object.soap.base64\r\n" + 
			"    value   : The object must be serialized with \r\n" + 
			"            : System.Runtime.Serialization.Formatters.Soap.SoapFormatter\r\n" + 
			"            : and then encoded with base64 encoding.\r\n" + 
			"\r\n" + 
			"    mimetype: application/x-microsoft.net.object.bytearray.base64\r\n" + 
			"    value   : The object must be serialized into a byte array \r\n" + 
			"            : using a System.ComponentModel.TypeConverter\r\n" + 
			"            : and then encoded with base64 encoding.\r\n" + 
			"    -->\r\n" + 
			"  <xsd:schema id=\"root\" xmlns=\"\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:msdata=\"urn:schemas-microsoft-com:xml-msdata\">\r\n" + 
			"    <xsd:element name=\"root\" msdata:IsDataSet=\"true\">\r\n" + 
			"      <xsd:complexType>\r\n" + 
			"        <xsd:choice maxOccurs=\"unbounded\">\r\n" + 
			"          <xsd:element name=\"metadata\">\r\n" + 
			"            <xsd:complexType>\r\n" + 
			"              <xsd:sequence>\r\n" + 
			"                <xsd:element name=\"value\" type=\"xsd:string\" minOccurs=\"0\" />\r\n" + 
			"              </xsd:sequence>\r\n" + 
			"              <xsd:attribute name=\"name\" type=\"xsd:string\" />\r\n" + 
			"              <xsd:attribute name=\"type\" type=\"xsd:string\" />\r\n" + 
			"              <xsd:attribute name=\"mimetype\" type=\"xsd:string\" />\r\n" + 
			"            </xsd:complexType>\r\n" + 
			"          </xsd:element>\r\n" + 
			"          <xsd:element name=\"assembly\">\r\n" + 
			"            <xsd:complexType>\r\n" + 
			"              <xsd:attribute name=\"alias\" type=\"xsd:string\" />\r\n" + 
			"              <xsd:attribute name=\"name\" type=\"xsd:string\" />\r\n" + 
			"            </xsd:complexType>\r\n" + 
			"          </xsd:element>\r\n" + 
			"          <xsd:element name=\"data\">\r\n" + 
			"            <xsd:complexType>\r\n" + 
			"              <xsd:sequence>\r\n" + 
			"                <xsd:element name=\"value\" type=\"xsd:string\" minOccurs=\"0\" msdata:Ordinal=\"1\" />\r\n" + 
			"                <xsd:element name=\"comment\" type=\"xsd:string\" minOccurs=\"0\" msdata:Ordinal=\"2\" />\r\n" + 
			"              </xsd:sequence>\r\n" + 
			"              <xsd:attribute name=\"name\" type=\"xsd:string\" msdata:Ordinal=\"1\" />\r\n" + 
			"              <xsd:attribute name=\"type\" type=\"xsd:string\" msdata:Ordinal=\"3\" />\r\n" + 
			"              <xsd:attribute name=\"mimetype\" type=\"xsd:string\" msdata:Ordinal=\"4\" />\r\n" + 
			"            </xsd:complexType>\r\n" + 
			"          </xsd:element>\r\n" + 
			"          <xsd:element name=\"resheader\">\r\n" + 
			"            <xsd:complexType>\r\n" + 
			"              <xsd:sequence>\r\n" + 
			"                <xsd:element name=\"value\" type=\"xsd:string\" minOccurs=\"0\" msdata:Ordinal=\"1\" />\r\n" + 
			"              </xsd:sequence>\r\n" + 
			"              <xsd:attribute name=\"name\" type=\"xsd:string\" use=\"required\" />\r\n" + 
			"            </xsd:complexType>\r\n" + 
			"          </xsd:element>\r\n" + 
			"        </xsd:choice>\r\n" + 
			"      </xsd:complexType>\r\n" + 
			"    </xsd:element>\r\n" + 
			"  </xsd:schema>\r\n" + 
			"  <resheader name=\"resmimetype\">\r\n" + 
			"    <value>text/microsoft-resx</value>\r\n" + 
			"  </resheader>\r\n" + 
			"  <resheader name=\"version\">\r\n" + 
			"    <value>2.0</value>\r\n" + 
			"  </resheader>\r\n" + 
			"  <resheader name=\"reader\">\r\n" + 
			"    <value>System.Resources.ResXResourceReader, System.Windows.Forms, Version=2.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089</value>\r\n" + 
			"  </resheader>\r\n" + 
			"  <resheader name=\"writer\">\r\n" + 
			"    <value>System.Resources.ResXResourceWriter, System.Windows.Forms, Version=2.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089</value>\r\n" + 
			"  </resheader>\r\n" + 
			"</root>";

	private static final Logger LOG = LoggerFactory.getLogger(CSharpConverter.class);

	/**
	 * allows to disable pretty print for unit tests
	 */
	private boolean prettyPrint = true;

	private String comment;

	public CSharpConverter(URI resourceLocation, boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		LOG.debug("C#:CSharpConverter1");
	}


	@Override
	public PropertyFile load(InputStream in, String encoding) throws IOException {
		LOG.debug("C#:load0");
        if(!in.markSupported())
            in = new BufferedInputStream(in);
        ByteOrderMark bom = PropertiesHelper.checkForBom(in);
	    LOG.debug("C#:load1");
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

				if (loadNode(node,file)) {
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
			file.setLicenseHeader(resFileAsString);		// we take the license header to store everything from our .resx

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
	 *         false: node is not provided for translation
	 */
	private boolean loadNode(Node node, PropertyFile file) {
		String name = node.getNodeName();
		LOG.debug("C#:loadNode1, Name: " + name);

		if(!name.equals(DATA)){
			LOG.debug("C#:loadNode2, NodeName not appropriate");
			return false;
		}

		NamedNodeMap namedNodeMap = node.getAttributes();
		if (null == namedNodeMap) {
			LOG.error("C#:loadNode3, namedNodeMap is null");
			return false;
		}

		Node namedNode = namedNodeMap.getNamedItem(NAME_ATTRIBUTE);
		if (null == namedNode) {
			LOG.error("C#:loadNode4, NAME_ATTRIBUTE not found");
			return false;
		}

		for (int j=0; j<namedNodeMap.getLength(); j++) {
			Node attribute = namedNodeMap.item(j);
			String value = attribute.getNodeValue();
			LOG.debug("C#:loadNode5, attribute_" + j + " Name: " + attribute.getNodeName() + " Value: " + value + " Content:" + attribute.getTextContent() + " Type: " + attribute.getNodeType());
			if (value.startsWith(">>")) {
				LOG.debug("C#:loadNode6, node value not appropriate");
				return false;
			}
			if (attribute.getNodeName().equals(TYPE)) {
				if (!attribute.getNodeValue().equals(SYSTEM_STRING)) {
					LOG.debug("C#:loadNode7, type attribute found which is not appropriate");
					return false;
				}
			}
		}

		// search for comment
		NodeList childNodes = node.getChildNodes();

		int childNodesLength = childNodes.getLength();
		LOG.debug("C#:loadNode8, nodesLength: " + childNodesLength);

		for (int k = childNodesLength-1; k >= 0; k--) {
			Node childNode = childNodes.item(k);
			String nodeName = childNode.getNodeName();
			String nodeContent = childNode.getTextContent();
			LOG.debug("C#:loadNode9, childNode_" + k + " Name: " + nodeName + " Value: " + childNode.getNodeValue() + " Content: " + nodeContent + " Type: " + childNode.getNodeType());
			if (null != nodeName) {
				if (nodeName.equals(COMMENT)) {
					LOG.debug("C#:loadNode10, comment found");
					if (nodeContent.equals(INVARIANT)) {		// identifies an entry that is not provided for translation
						LOG.debug("C#:loadNode11, comment indicates that this entry is not provided for translation, nodeContent: " + nodeContent);
						return false;
					}
					else {
						comment = nodeContent;
					}
				}
			}
		}

		String textContentName = namedNode.getTextContent();
		LOG.debug("C#:loadNode13, textContentName: " + textContentName);

		Property property = PropertiesFactory.eINSTANCE.createProperty();
		String key = namedNode.getNodeValue();
		property.setKey(key);

		LOG.debug("C#:loadNode14, provided for translation ");

		Node valueNode = getValueNode(node);
		if (null != valueNode) {
			String textcontent = valueNode.getTextContent(); 
			LOG.debug("C#:loadNode15, textcontent: " + textcontent);
			property.setValue(textcontent);
		}

		if (null != comment) {
			LOG.debug("C#:loadNode16, comment was set: " + comment);
			property.setComment(comment);
		}
		comment = null;
		LOG.debug("C#:loadNode17, property to be added");
		file.getProperties().add(property);
		return true;
	}


	private Node getValueNode(Node node) {
		LOG.debug("C#:getValueNode1");
		Node childNode = null;
		
		if (null != node) {
			childNode = getChildNode(node, VALUE);
		}
		
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


	@Override
	public int write(OutputStream out, PropertyFile file, String encoding) throws IOException {
		try {
			LOG.debug("C#:write1, file:" + file.toString() + " encoding: " + encoding + " out: " + out.toString());

			int counter = 0;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			LOG.debug("C#:write2");

			String licenseHeader = file.getLicenseHeader();
			
			if(isFilled(licenseHeader)) {
				LOG.debug("C#:write3, licenseHeader was filled: " + licenseHeader);
			}
			else {
				licenseHeader = EMPTY_RES_FILE;												// Obviously no translation for this .resx file existing
				LOG.debug("C#:write4, licenseHeader was not filled: " + licenseHeader);
				
				// remove the carriage return:
				//licenseHeader = licenseHeader.replaceAll("\\r", "");
				//LOG.debug("C#:write5, licenseHeader after removing CR: " + licenseHeader);
			}
			
            try {
            	document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(licenseHeader)));
            	LOG.debug("C#:write6, document was built");
            } catch (Exception e) {
            	LOG.error("C#:write8 Exception when rewriting the non translatable part of the .resx file", e);
            }


			EList<Property> properties = file.getProperties();
			LOG.debug("C#:write9, count properties: " + properties.size());
			
			XPath xPath = XPathFactory.newInstance().newXPath();

			for (Property property : properties) {
				LOG.debug("C#:write10, property.getKey: " + property.getKey() + " property.getValue: " + property.getValue());

				try {
					//XPath xPath = XPathFactory.newInstance().newXPath();
					Node nodeWithValue = getNodeWithValue(document, property.getKey(), xPath);

					if (null == nodeWithValue) {
						LOG.debug("C#:write11, node not found");
						Node rootNode = (Node)xPath.evaluate("/root", document, XPathConstants.NODE);
						
						if (null == rootNode) {
							LOG.debug("C#:write12, root node created");
							rootNode = document.createElement(ROOT_NODE);
							document.appendChild(rootNode);
							LOG.debug("C#:write13, root node appended");
						}

						Element newElem = document.createElement(DATA);
						newElem.setAttribute(NAME_ATTRIBUTE, property.getKey());
						newElem.setAttribute(XML_SPACE, PRESERVE);
						
						Element value = document.createElement(VALUE);
						value.setNodeValue(property.getValue());
						newElem.appendChild(value);
						
						LOG.debug("C#:write14, node with new translation created");
							
						rootNode.appendChild(newElem);
						nodeWithValue = getNodeWithValue(document, property.getKey(), xPath);
					}

					if (null != nodeWithValue) {
						LOG.debug("C#:write16, nodeWithValue.getNodeName: " + nodeWithValue.getNodeName() + " nodeWithValue.getTextContent: " + nodeWithValue.getTextContent() + " nodeWithValue.getNodeValue: " + nodeWithValue.getNodeValue()  + " nodeWithValue.getNodeType: " + nodeWithValue.getNodeType());
					}

					String newVal = property.getValue();
					// remove the carriage return:
					newVal = newVal.replaceAll("\\r", "");
					LOG.debug("C#:write17, newVal replaced: " + newVal);
					
					Node valueNode = getValueNode(nodeWithValue);
					if (null != valueNode) {
						LOG.debug("C#:write18, valueNode.getNodeName: " + valueNode.getNodeName() + " valueNode.getTextContent: " + valueNode.getTextContent() + " valueNode.getNodeValue: " + valueNode.getNodeValue()  + " valueNode.getNodeType: " + valueNode.getNodeType());
						valueNode.setTextContent(newVal);
					}
					else {
						Element newElem = document.createElement(VALUE);
						LOG.debug("C#:write19, value node inserted: " + newVal);
				        newElem.setTextContent(newVal);
					}

					String newComment = property.getComment();
					if (null != newComment) {
						// remove the carriage return:
						newComment = newComment.replaceAll("\\r", "");
						LOG.debug("C#:write20, newComment: " + newComment);
						Node commentNode = getCommentNode(nodeWithValue);
						if (null != commentNode) {
							LOG.debug("C#:write21, comment replaced: " + newComment);
							commentNode.setTextContent(newComment);
						}
						else {
							Node newNode = nodeWithValue.appendChild(document.createElement(COMMENT));
							LOG.debug("C#:write22, comment inserted");
					        newNode.setTextContent(newComment);
						}
					}
				} catch (Exception e) {
					LOG.error("C#:write23 ", e);
				}
			}

			LOG.debug("C#:write24");
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			if(prettyPrint) {
				LOG.debug("C#:write25");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(out);

			LOG.debug("C#:write26");
			transformer.transform(source, result);
			LOG.debug("C#:write27");
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


	private Node getNodeWithValue(Document document, String key, XPath xPath) {
		String searchStr = "//data[@name=\"" + key + "\"]";
		LOG.debug("C#:GetNodeWithValue1, searchstr: " + searchStr);
			Node nodeWithValue =null;
			try {
				nodeWithValue = (Node)xPath.evaluate(searchStr, document, XPathConstants.NODE);
			} catch (XPathExpressionException e) {
				LOG.debug("C#:GetNodeWithValue2 failed with exception: ", e);
			}
			return nodeWithValue;
	}


	private boolean isFilled(String s){
		LOG.debug("C#:isFilled1, s: " + s);
		return s!=null && !s.isEmpty();
	}
}