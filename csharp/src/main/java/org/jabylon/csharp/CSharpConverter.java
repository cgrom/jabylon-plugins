package org.jabylon.csharp;

import java.io.File;
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
		//System.out.println("CSharpConverter1");
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
				//System.out.println("load1, value first node: " + nodeValue);
				LOG.info("C#:load2, value first node: " + nodeValue);
				file.setLicenseHeader(nodeValue);
			}

			Node resources = result.getDocumentElement();

			//System.out.println("load2, resources, TextContent: " + resources.getTextContent());
			LOG.info("C#:load3, resources, TextContent: " + resources.getTextContent());

			// blueprint from Android:
			/*if(!ROOT_NODE.equals(resources.getNodeName())) {
				LOG.error("C#:XML does not start with "+ROOT_NODE+" but "+resources.getLocalName()+". Location: "+uri);
				return file;
			}*/

			NodeList nodes = resources.getChildNodes();

			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				//System.out.println("load3, child node_" + i + " Name: " + node.getNodeName() + " Value: " + node.getNodeValue());
				LOG.info("C#:load4, child node_" + i + " Name: " + node.getNodeName() + " Value: " + node.getNodeValue());
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

		//System.out.println("loadNode1, Name: " + name);
		LOG.info("C#:loadNode1, Name: " + name);

		if(name.equals(DATA)){
			loadString(node, property);
		}
		else {
			//System.out.println("loadNode2, error, Invalid tag "+name+" found in " + uri + " Skipping");
			LOG.error("C#:loadNode2, Invalid tag "+name+" found in " + uri + " Skipping");
			return;
		}

		property.setComment(comment);
		comment = null;
		file.getProperties().add(property);
		//System.out.println("loadNode3, end");
		LOG.info("C#:loadNode3, end");
	}


	private void loadString(Node node, Property property) {

		Node name = node.getAttributes().getNamedItem(NAME_ATTRIBUTE);

		if (null == name) {
			//System.out.println("loadString1, NAME_ATTRIBUTE not found TextContent: " + node.getTextContent());
			LOG.info("C#:loadString1, NAME_ATTRIBUTE not found TextContent: " + node.getTextContent());
			return;
		}

		String textContentName = name.getTextContent();

		//System.out.println("loadString2, textContentName: " + textContentName);
		LOG.info("C#:loadString2, textContentName: " + textContentName);

		String key = name.getNodeValue();
		property.setKey(key);

		// most of the xml-elements in CSharp are not for translation
		if (false == isCandidateForTranslation(key)) {
			PropertyAnnotation annotation = PropertiesFactory.eINSTANCE.createPropertyAnnotation();

			annotation.setName(PropertyAnnotations.NON_TRANSLATABLE);
			property.getAnnotations().add(annotation);
			String textContentNode = node.getTextContent();
			//System.out.println("loadString3, textContentNode: " + textContentNode);
			LOG.info("C#:loadString3, textContentNode: " + textContentNode);
			property.setValue(decode(textContentNode));
		}
		else {															// provided for translation
			//System.out.println("loadString4, provided for translation ");
			LOG.info("C#:loadString4, provided for translation ");
			property.setValue(decode(textContentName));
		}
	}


	private Boolean isCandidateForTranslation(String key) {
		Boolean bReturn = false;

		//System.out.println("isCandidateForTranslation1, key: " + key);
		LOG.info("C#:isCandidateForTranslation1, key: " + key);

		if (key.contains(".Text\"")) {
			bReturn = true;
		}

		//System.out.println("isCandidateForTranslation2, bReturn: " + bReturn);
		LOG.info("C#:isCandidateForTranslation2, bReturn: " + bReturn);
		return bReturn;
	}


	/**
	 * decodes a string
	 * @param textContent
	 * @return the decoded string
	 */
	private String decode(String textContent) {
		if(textContent==null) {
			//System.out.println("decode1, textContent is null" );
			LOG.info("C#:decode1, textContent is null" );
			return null;
		}

		// Android code as a blueprint
		/*if(textContent.startsWith("\"") && textContent.endsWith("\""))
			return textContent.substring(1, textContent.length()-1);
		return textContent.replace("\\'", "'");*/
		//System.out.println("decode1, textContent: " + textContent);
		LOG.info("C#:decode1, textContent: " + textContent);
		return textContent;
	}


	@Override
	public int write(OutputStream out, PropertyFile file, String encoding) throws IOException {
		try {
			//System.out.println("write1, file:" + file.toString() + " encoding: " + encoding);
			LOG.info("C#:write1, file:" + file.toString() + " encoding: " + encoding);

			int counter = 0;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document document = factory.newDocumentBuilder().newDocument();

			//System.out.println("write2");
			LOG.info("C#:write2");

			if(isFilled(file.getLicenseHeader()))
			{
				//System.out.println("write3");
				LOG.info("C#:write3");
				document.appendChild(document.createComment(file.getLicenseHeader()));
			}

			//System.out.println("write4");
			LOG.info("C#:write4");

			Element root = document.createElement(ROOT_NODE);
			document.appendChild(root);
			EList<Property> properties = file.getProperties();
			for (Property property : properties) {
				//System.out.println("write5, property: " + property.toString());
				LOG.info("C#:write5, property: " + property.toString());
				if(writeProperty(root, document, property)) {
					counter++;
					//System.out.println("write6, counter: " + counter);
					LOG.info("C#:write6, counter: " + counter);
				}
			}

			//System.out.println("write7");
			LOG.info("C#:write7");

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			if(prettyPrint){
				//System.out.println("write8");
				LOG.info("C#:write8");
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(out);

			//System.out.println("write9");
			LOG.info("C#:write9");

			transformer.transform(source, result);

			//System.out.println("write10");
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

		//System.out.println("writeProperty1");
		LOG.info("C#:writeProperty1");

		String value = property.getValue();

		//System.out.println("writeProperty2, value of property: " + value);
		LOG.info("C#:writeProperty2, value of property: " + value);

		if(!isFilled(value)) {
			//System.out.println("writeProperty3, value not filled");
			LOG.info("C#:writeProperty3, value not filled");
			return false;
		}

		if (false == writeCommentAndAnnotations(root, document, property)) {
			//System.out.println("writeProperty4 call to writeString");
			LOG.info("C#:writeProperty4 call to writeString");
			writeString(root,document,property);					// property was created by a translatable xml-element. So write back the translation
		}
		return true;
	}


	private void writeString(Element root, Document document, Property property) {
		//System.out.println("writeString1");
		LOG.info("C#:writeString1");
		Element data = document.createElement(DATA);
		//System.out.println("writeString2");
		LOG.info("C#:writeString2");
		root.appendChild(data);

		String key = property.getKey();
		//System.out.println("writeString3, key: " + key);
		LOG.info("C#:writeString3, key: " + key);
		data.setAttribute(NAME_ATTRIBUTE, key);

		String value = property.getValue();
		//System.out.println("writeString4, value: " + value);
		LOG.info("C#:writeString4, value: " + value);
		data.setTextContent(encode(value));
	}


	private String encode(String textContent) {
		if(textContent==null) {
			//System.out.println("encode1, textContent is null");
			LOG.info("C#:encode1, textContent is null");
			return null;
		}

		textContent = textContent.replace("'", "\\'").replace("\"", "\\\"");
		//System.out.println("encode2, textContent: " + textContent);
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

		//System.out.println("writeCommentAndAnnotations1, property: " + property.toString());
		LOG.info("C#:writeCommentAndAnnotations1, property: " + property.toString());

        if(property.eIsSet(PropertiesPackage.Literals.PROPERTY__COMMENT) || property.getAnnotations().size()>0)
        {
        	//System.out.println("writeCommentAndAnnotations2");
        	LOG.info("C#:writeCommentAndAnnotations2");

        	String comment = property.getCommentWithoutAnnotations();

        	//System.out.println("writeCommentAndAnnotations3, comment: " + comment);
        	LOG.info("C#:writeCommentAndAnnotations3, comment: " + comment);

        	StringBuilder builder = new StringBuilder();

        	PropertyAnnotation nonTranslatable = property.findAnnotation(PropertyAnnotations.NON_TRANSLATABLE);
        	if (null != nonTranslatable) {
        		bReturn = true;						// this property is non-translatable
        	}

        	if(builder.length()>0 && comment!=null && comment.length()>0 )
        	{
        		builder.append("\n");
        	}
        	builder.append(comment);

        	String stringFromComment = builder.toString();

        	//System.out.println("writeCommentAndAnnotations3, stringFromComment: " + stringFromComment);
        	LOG.info("C#:writeCommentAndAnnotations3, stringFromComment: " + stringFromComment);

        	Comment node = document.createComment(stringFromComment);

        	//System.out.println("writeCommentAndAnnotations4, node: " + node.toString());
        	LOG.info("C#:writeCommentAndAnnotations4, node: " + node.toString());

        	root.appendChild(node);
        }
        return bReturn;
	}


	private boolean isFilled(String s){
		//System.out.println("isFilled1, s: " + s);
		LOG.info("C#:isFilled1, s: " + s);
		return s!=null && !s.isEmpty();
	}
}
