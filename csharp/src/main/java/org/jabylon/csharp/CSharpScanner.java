/**
 *
 */
package org.jabylon.csharp;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.wicket.util.crypt.StringUtils;
import org.eclipse.emf.common.util.URI;
import org.jabylon.properties.PropertiesFactory;
import org.jabylon.properties.PropertiesPackage;
import org.jabylon.properties.ScanConfiguration;
import org.jabylon.properties.types.PropertyConverter;
import org.jabylon.properties.types.PropertyScanner;
import org.jabylon.properties.types.impl.AbstractPropertyScanner;
//import org.jabylon.security.CommonPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * adds support for the C# xml format.
 *
 * @author c.gromer@seeburger.de
 *
 */
@Component(enabled=true,immediate=true)
@Service
public class CSharpScanner implements PropertyScanner {

	@Property(name=PropertyScanner.TYPE, value="CSHARP")
	public  static final String TYPE = "CSHARP";

    private static final String[] DEFAULT_EXCLUDES = {};
    private static final String[] DEFAULT_INCLUDES = {"**/*.resx"};

    private static final Logger LOG = LoggerFactory.getLogger(CSharpScanner.class);

	public CSharpScanner() {
		LOG.info("C#:CSharpScanner1");
	}


	@Override
	public String[] getDefaultIncludes() {

		LOG.info("C#:getDefaultIncludes1");

		return DEFAULT_INCLUDES;
	}

	/* (non-Javadoc)
	 * @see org.jabylon.properties.types.PropertyScanner#getDefaultExcludes()
	 */
	@Override
	public String[] getDefaultExcludes() {

		LOG.info("C#:getDefaultExcludes1");

		return DEFAULT_EXCLUDES;
	}


	@Override
	public boolean isTemplate(File propertyFile, String masterLocale) {

		LOG.info("C#:isTemplate1");
		LOG.info("C#:isTemplate2 propertyFile: " + propertyFile.getName() + " masterLocale: " + masterLocale);

		if(isResourceFile(propertyFile) && countingPeriodsFullfillsTemplateFileCondition(propertyFile)) {
			return true;
		}
		return false;
	}


	@Override
	public boolean isTranslation(File propertyFile, ScanConfiguration config) {

		LOG.info("C#:isTranslation1");

		String fileName = propertyFile.getName();

		LOG.info("C#:isTranslation2 fileName: " + fileName);

		if (isResourceFile(propertyFile) && (countingPeriodsFullfillsTranslationFileCondition(propertyFile))) {
			return true;
		}
		return false;
	}


	private boolean countingPeriodsFullfillsTranslationFileCondition(File propertyFile) {
		//E.g.: Resources.nl.resx is a translation file, Resources.resx is a template file
        LOG.info("C#:countingPeriodsFullfillsTranslationFileCondition1: propertyFile: " + propertyFile.getName());

        if (2 == propertyFile.getName().length() - propertyFile.getName().replace(".","").length()) {
            LOG.info("C#:countingPeriodsFullfillsTranslationFileCondition2: translation file" );
            return true;
        }

        LOG.info("C#:countingPeriodsFullfillsTranslationFileCondition3: no translation file" );
        return false;
	}


	private boolean countingPeriodsFullfillsTemplateFileCondition(File propertyFile) {
		//E.g.: Resources.nl.resx is a translation file, Resources.resx is a template file

	    LOG.info("C#:countingPeriodsFullfillsTemplateFileCondition1: propertyFile: " + propertyFile.getName());

        if (1 == propertyFile.getName().length() - propertyFile.getName().replace(".","").length()) {
            LOG.info("C#:countingPeriodsFullfillsTemplateFileCondition2: template" );
            return true;
        }

        LOG.info("C#:countingPeriodsFullfillsTemplateFileCondition3: no template" );
        return false;

        // did not work:
		/*if (2 == propertyFile.getName().split(".", -1).length) {
			LOG.info("C#:countingPeriodsFullfillsTemplateFileCondition3: template" );
			return true;
		}
		LOG.info("C#:countingPeriodsFullfillsTemplateFileCondition4: no template" );
		return false;*/
	}


	private boolean isResourceFile(File propertyFile) {

		LOG.info("C#:isResourceFile1" );
		LOG.info("C#:isResourceFile2, propertyFile: " + propertyFile.getName());

		if(propertyFile.getName().endsWith(".resx")) {
			LOG.info("C#:isResourceFile3: resource file" );
			return true;
		}
		LOG.info("C#:isResourceFile4: no resource file" );
		return false;
	}


	@Override
	public File findTemplate(File propertyFile, ScanConfiguration config) {

		LOG.info("C#:findTemplate1");

		if (isTranslation(propertyFile, config)) {
			String fileName = propertyFile.getName();
			LOG.info("C#:findTemplate2: fileName: " + fileName );
			// e.g.: Resources.nl.resx
			String[] strings = fileName.split(".", -1);
			if (3 == strings.length) {
				String templateFile = strings[0] + "." + strings[2];		// e.g.: Resources.resx
				LOG.info("C#:findTemplate3: templateFile: " + templateFile);
				return new File(templateFile);
			}
		}
		LOG.info("C#:findTemplate4");
		return null;
	}


	@Override
	public Map<Locale, File> findTranslations(File template, ScanConfiguration config) {

		LOG.info("C#:findTranslations1");

		if (isTemplate(template, "")) {
			String folder = template.getParent();
			LOG.info("C#:findTranslations2: folder: " + folder);
			File[] files = new File(folder).listFiles();
			Map<Locale, File> translations = new HashMap<Locale, File>();
			for (File file : files) {
				LOG.info("C#:findTranslations3: file: " + file.getName());
				if (isTranslation(file, config)) {
					LOG.info("C#:findTranslations4: is translation");
					translations.put(getLocale(file), file);
				}
			}
			if (!translations.isEmpty()) {
				return translations;
			}
		}
		return null;
	}


	@Override
	public File computeTranslationPath(File template, Locale templateLocale, Locale translationLocale) {
		LOG.info("C#:computeTranslationPath1, template:" + template.getName() + " template.getPath: " + template.getPath());
		File folder = template.getParentFile();			// template and translation files reside in the same folder
		LOG.info("C#:computeTranslationPath2, folder: " + folder.getName());

		LOG.info("C#:computeTranslationPath2.1, folder.getPath: " + folder.getPath());
		LOG.info("C#:computeTranslationPath2.2, folder.getParent: " + folder.getParent());

		return folder;
	}


	@Override
	public Locale getLocale(File propertyFile) {

		LOG.info("C#:getLocale1, propertyFile: " + propertyFile);
		
		String propFileName = propertyFile.getName();				// e.g.: dialog.resx (template file), dialog.nl.resx (translation file)
		
		LOG.info("C#:getLocale2, propFileName: " + propFileName);
		
		String[] splittedPropFileName = propFileName.split("\\.");
		
		String language = "";		// "en" in "en_US"
		String culture = "";		// "US" in "en_US"
		boolean isTemplate = false;
		
		if (1 == splittedPropFileName.length) {
			LOG.info("C#:getLocale3, obviously no property file: " + propFileName);
		}
		else if (1 < splittedPropFileName.length) {
			LOG.info("C#:getLocale4, splittedPropFileName.length: " + splittedPropFileName.length);
			String extension = splittedPropFileName[splittedPropFileName.length-1];
			
			if (2 == splittedPropFileName.length) {
				//e.g.: dialog.resx (template file)
			}
			else {
				if (5 == splittedPropFileName[splittedPropFileName.length-2].length()) {
					if ("_" == splittedPropFileName[splittedPropFileName.length-2].substring(2,3)) {
						LOG.info("C#:getLocale5, language + culture ");
						language = splittedPropFileName[splittedPropFileName.length-2].substring(0,2);
						culture = splittedPropFileName[splittedPropFileName.length-2].substring(3,5);
					}
					else {
						LOG.info("C#:getLocale6, template ");
						isTemplate = true;
					}
				} else if (2 == splittedPropFileName[splittedPropFileName.length-2].length()) {
					LOG.info("C#:getLocale7, only language ");
					language = splittedPropFileName[splittedPropFileName.length-2];
				}
				else {
					LOG.info("C#:getLocale8, template ");
					isTemplate = true;
				}
			}
		}
		
		Locale loc = null;

		if (true == isTemplate) {
			LOG.info("C#:getLocale9, isTemplate");
			loc = new Locale("en", "US");		// In C# we actually translate from American English
		}
		else  if (0 < language.length()){
			LOG.info("C#:getLocale10, isTranslation File, language: " + language + " culture: " + culture);
			loc = new Locale(language, culture);
		}
		return loc;
	}


	@Override
	public boolean isBilingual() {

		LOG.info("C#:isBilingual1");

		return false;
	}


	@Override
	public PropertyConverter createConverter(URI resource) {

		LOG.info("C#:createConverter1, resource: " + resource.path());

		return new CSharpConverter(resource, true);
	}


	@Override
	public String getEncoding() {

		LOG.info("C#:getEncoding1");

		return "UTF-8";
	}
}
