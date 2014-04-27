package org.apache.sling.handlebars;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.WebResourceScriptRunnerFactory;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.util.JCRUtils;
import org.apache.sling.webresource.util.ScriptUtils;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label="Handlebars Compiler Service", immediate=true, metatype=true)
@Service
public class HandlebarsCompilerImpl implements WebResourceScriptCompiler {
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private WebResourceScriptRunnerFactory webResourceScriptRunnerFactory;
    
    private ScriptableObject scope = null;
    
    @org.apache.felix.scr.annotations.Property(label="Handlebars Compiler Script Path", value="/system/handlebars/handlebars.js")
    private final static String HANDLEBARS_COMPILER_PATH = "handlebars.compiler.path";
    
    @org.apache.felix.scr.annotations.Property(label="Handlebars Cache Path", value="/var/handlebars")
    private final static String HANDLEBARS_CACHE_PATH = "handlebars.cache.path";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private String handlebarsCompilerPath;
    
    private String handlebarsCachePath;
    
    private WebResourceScriptRunner scriptRunner;
    
    public void activate(final ComponentContext context) throws Exception
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Dictionary config = context.getProperties();
        handlebarsCompilerPath = PropertiesUtil.toString(config.get(HANDLEBARS_COMPILER_PATH), "/system/handlebars/handlebars.js");
        handlebarsCachePath = PropertiesUtil.toString(config.get(HANDLEBARS_CACHE_PATH), "/var/handlebars");
        
        loadHandlebarsRunner();
   
        stopWatch.stop();
        log.info("Completed Handlebars Compiler Startup " + stopWatch);
    }

	private void loadHandlebarsRunner() throws LoginException,
			RepositoryException {
		ResourceResolver resolver = null;
        try{
            resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            
            InputStream handlebarsCompilerStream = JCRUtils.getFileResourceAsStream(resolver, handlebarsCompilerPath);
            this.scriptRunner = this.webResourceScriptRunnerFactory.createRunner("handlebars.js", handlebarsCompilerStream);
        }
        finally
        {
            if(resolver != null)
            {
                resolver.close();
            }
        }
	}
    
    public InputStream compile(InputStream handlebarsStream) throws WebResourceCompileException
    {
        return compile(handlebarsStream, null);
    }
    
    public InputStream compile(InputStream handlebarsStream, Map<String, Object> compileOptions) throws WebResourceCompileException
    {

        Map<String, Object> handlebarsCompileOptions = new HashMap<String, Object>();
        
        try{
            String handlebarsScript = IOUtils.toString(handlebarsStream);
            StringBuffer scriptBuffer = new StringBuffer();
            scriptBuffer.append("Handlebars.precompile(");
            scriptBuffer.append(ScriptUtils.toJSMultiLineString(handlebarsScript));
            scriptBuffer.append(", ");
            if(handlebarsCompileOptions.isEmpty())
            {
                scriptBuffer.append("{}");
            }
            else
            {
                scriptBuffer.append(ScriptUtils.generateCompileOptionsString(handlebarsCompileOptions));
            }
            scriptBuffer.append(");");
            InputStream handlebarsScriptStream = new ByteArrayInputStream(scriptBuffer.toString().getBytes());
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            String compiledScript = scriptRunner.evaluateScript(handlebarsScriptStream, null);
            compiledScript = "Handlebars.template(" + compiledScript + ");";
            stopWatch.stop();
            log.info("Completed Handlebars Precompile " + stopWatch);
            return new ByteArrayInputStream(compiledScript.getBytes());
        }
        catch(Exception e)
        {
           throw new WebResourceCompileException(e);
        }
    }
    
    public String getCacheRoot()
    {
        return this.handlebarsCachePath;
    }
    
    public boolean canCompileNode(Node sourceNode)
    {
        String extension = null;
        String mimeType = null;
        try{
           
            if (sourceNode.hasNode(Property.JCR_CONTENT)) {
                Node sourceContent = sourceNode.getNode(Property.JCR_CONTENT);
                if(sourceContent.hasProperty(Property.JCR_MIMETYPE))
                {
                    mimeType = sourceContent.getProperty(Property.JCR_MIMETYPE).getString();
                }
            }
           extension = JCRUtils.getNodeExtension(sourceNode);

        }catch(RepositoryException e)
        {
            //Log Exception
            log.info("Node Name can not be read.  Skipping node.");
        }
        
        return "hbs".equals(extension) || "handlebars".equals(extension) || "text/x-handlebars-template".equals(mimeType);
    }
    
    public String compiledScriptExtension()
    {
        return "js";
    }
    
    public void setResourceResolverFactory(
            ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }
}

