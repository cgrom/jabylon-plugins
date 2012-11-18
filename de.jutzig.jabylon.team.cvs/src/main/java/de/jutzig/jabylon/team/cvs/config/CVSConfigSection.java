/**
 * 
 */
package de.jutzig.jabylon.team.cvs.config;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.osgi.service.prefs.Preferences;

import de.jutzig.jabylon.properties.Project;
import de.jutzig.jabylon.rest.ui.wicket.config.AbstractConfigSection;


/**
 * @author Johannes Utzig (jutzig.dev@googlemail.com)
 *
 */
public class CVSConfigSection extends AbstractConfigSection<Project>{

	private static final long serialVersionUID = 1L;

	private boolean cvsSelected(IModel<Project> model) {
		return "CVS".equals(model.getObject().getTeamProvider());
	}


	@Override
	public WebMarkupContainer createContents(String id, IModel<Project> input, Preferences config) {
		return new CVSConfigPanel(id, input, config);
	}

	@Override
	public void commit(IModel<Project> input, Preferences config) {
		// TODO Auto-generated method stub
		
	}

}
