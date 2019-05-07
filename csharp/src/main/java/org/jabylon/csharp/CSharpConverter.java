package org.jabylon.csharp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	    LOG.info("C#:load1, in: " + in.toString());

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document result = builder.parse(in);
			PropertyFile file = PropertiesFactory.eINSTANCE.createPropertyFile();

			Node firstNode = result.getChildNodes().item(0);
			if(firstNode.getNodeType()==Node.COMMENT_NODE) {
				String nodeValue = firstNode.getNodeValue();
				LOG.info("C#:load2, value first node: " + nodeValue);
				file.setLicenseHeader(nodeValue);
			}

			Node resources = result.getDocumentElement();

			LOG.info("C#:load3, resources, TextContent: " + resources.getTextContent());

			NodeList nodes = resources.getChildNodes();

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				LOG.info("C#:load4, child node_" + i + " Name: " + node.getNodeName() + " Value: " + node.getNodeValue() + " Content:" + node.getTextContent() + " Type: " + node.getNodeType());
				
				loadNode(node,file);
			}
			return file;

		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}


	private void loadNode(Node node, PropertyFile file) {
		//Android as a blueprint:
		/*if(node.getNodeType()==Node.TEXT_NODE)
			return;
		if(node.getNodeType()==Node.COMMENT_NODE) {
			comment = node.getNodeValue();
			return;
		}*/

		Property property = PropertiesFactory.eINSTANCE.createProperty();
		String name = node.getNodeName();

		LOG.info("C#:loadNode1, Name: " + name);

		if (false == loadString(node, property)) {
			LOG.info("C#:loadNode2, node not appropiate");
			return;
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


	private Boolean isCandidateForTranslation(String key, String nodeName) {
		Boolean bReturn = false;

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
			Document document = factory.newDocumentBuilder().newDocument();

			LOG.info("C#:write2");

			if(isFilled(file.getLicenseHeader()))
			{
				LOG.info("C#:write3");
				document.appendChild(document.createComment(file.getLicenseHeader()));
			}

			LOG.info("C#:write4");

			Element root = document.createElement(ROOT_NODE);
			document.appendChild(root);
			EList<Property> properties = file.getProperties();
			for (Property property : properties) {
				LOG.info("C#:write5, property: " + property.toString());
				if(writeProperty(root, document, property)) {
					counter++;
					LOG.info("C#:write6, counter: " + counter);
				}
			}

			LOG.info("C#:write7");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			if(prettyPrint){
				LOG.info("C#:write8");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(out);

			LOG.info("C#:write9");

			transformer.transform(source, result);

			LOG.info("C#:write10");

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
