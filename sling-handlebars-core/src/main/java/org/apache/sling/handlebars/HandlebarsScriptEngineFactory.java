package org.apache.sling.handlebars;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Dictionary;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.script.ScriptEngine;

import org.apache.commons.lang.time.StopWatch;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.WebResourceScriptRunnerFactory;
import org.apache.sling.webresource.util.JCRUtils;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(value = javax.script.ScriptEngineFactory.class)
public class HandlebarsScriptEngineFactory extends AbstractScriptEngineFactory {

	/** default log */
	private final Logger log = LoggerFactory.getLogger(getClass());

	public final static String HBS_SCRIPT_EXTENSION = "hbs";

	public final static String HANDLEBARS_SCRIPT_EXTENSION = "handlebars";

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private WebResourceScriptRunnerFactory webResourceRunnerFactory;

	private ScriptableObject scope = null;

	@Property(label = "Handlebars Compiler Script Path", value = "/system/handlebars/handlebars.js")
	private final static String HANDLEBARS_COMPILER_PATH = "handlebars.compiler.path";

	@Property(label = "Handlebars Helper Script Path", value = "/system/handlbars/helpers")
	private final static String HANDLEBARS_HELPER_PATH = "handlebars.helper.path";

	private String handlebarsCompilerPath;

	private String handlebarsHelperPath;

	private WebResourceScriptRunner scriptRunner;

	public ScriptEngine getScriptEngine() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ScriptEngine result = new HandlebarsScriptEngine(this,
				this.scriptRunner);

		stopWatch.stop();
		log.info("Script Engine Handlebars created: " + stopWatch);
		return result;
	}

	public String getLanguageVersion() {
		return "1.0.0";
	}

	protected void activate(ComponentContext context) throws Exception {
		Dictionary<?, ?> props = context.getProperties();

		handlebarsCompilerPath = PropertiesUtil.toString(
				props.get(HANDLEBARS_COMPILER_PATH),
				"/system/handlebars/handlebars.js");

		handlebarsHelperPath = PropertiesUtil
				.toString(props.get(HANDLEBARS_HELPER_PATH),
						"/system/handlebars/helpers");

		setEngineName("Rhino");

		setExtensions(HBS_SCRIPT_EXTENSION, HANDLEBARS_SCRIPT_EXTENSION);
		setMimeTypes("text/javascript", "text/x-handlebars-template",
				"application/javascript");
		setEngineVersion("1.0.0");
		setNames("javascript", HBS_SCRIPT_EXTENSION,
				HANDLEBARS_SCRIPT_EXTENSION);

		loadHandlebarsScriptRunner();

	}

	private void loadHandlebarsScriptRunner() throws LoginException,
			RepositoryException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ResourceResolver resolver = null;
		try {
			resolver = resourceResolverFactory
					.getAdministrativeResourceResolver(null);

			InputStream handlebarsCompilerStream = JCRUtils
					.getFileResourceAsStream(resolver, handlebarsCompilerPath);

			InputStream handlebarsRenderStream = JCRUtils
					.getFileResourceAsStream(resolver,
							"/system/handlebars/handlebars-renderer.js");

			InputStream consolidatedHandlebarsScriptStream = new SequenceInputStream(
					handlebarsCompilerStream, handlebarsRenderStream);

			// Load Helpers by using a predefined helper directory and loading
			// all JS Files
			Resource handlebarsHelperFolderResource = resolver
					.getResource(handlebarsHelperPath);
			HandlebarUtils.processHandlebarHelperFolder(resolver,
					consolidatedHandlebarsScriptStream,
					handlebarsHelperFolderResource);

			this.scriptRunner = this.webResourceRunnerFactory.createRunner(
					"handlebars.js", consolidatedHandlebarsScriptStream);


		} finally {
			if (resolver != null) {
				resolver.close();
			}
		}
		stopWatch.stop();
		log.info("Handlebars Loaded: " + stopWatch);
	}

	protected void deactivate(ComponentContext context) {

	}

	public String getLanguageName() {
		return "Handlebars";
	}

	public void setResourceResolverFactory(
			ResourceResolverFactory resourceResolverFactory) {
		this.resourceResolverFactory = resourceResolverFactory;
	}

}
