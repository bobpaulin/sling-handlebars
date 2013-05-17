package org.apache.sling.handlebars;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;

import javax.script.ScriptEngine;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.api.resource.LoginException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.webresource.util.JCRUtils;
import org.apache.sling.webresource.util.ScriptUtils;


@Component
@Service(value=javax.script.ScriptEngineFactory.class)
public class HandlebarsScriptEngineFactory extends AbstractScriptEngineFactory{
    
    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public final static String  HBS_SCRIPT_EXTENSION = "hbs";

    public final static String HANDLEBARS_SCRIPT_EXTENSION = "handlebars";
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    private ScriptableObject scope = null;
    
    @org.apache.felix.scr.annotations.Property(label="Handlebars Compiler Script Path", value="/system/handlebars/handlebars.js")
    private final static String HANDLEBARS_COMPILER_PATH = "handlebars.compiler.path";
    
    private String languageVersion;
    
    private String handlebarsCompilerPath;
    
    public ScriptEngine getScriptEngine() {
        return new HandlebarsScriptEngine(this, scope);
    }
    
    public String getLanguageVersion() {
        return languageVersion;
    }
    
    protected void activate(ComponentContext context) throws Exception {
        Dictionary<?, ?> props = context.getProperties();
        
        handlebarsCompilerPath = PropertiesUtil.toString(props.get(HANDLEBARS_COMPILER_PATH), "/system/handlebars/handlebars.js");
        
        setEngineName("Rhino");
        
        setExtensions(HBS_SCRIPT_EXTENSION, HANDLEBARS_SCRIPT_EXTENSION);
        setMimeTypes("text/javascript", "text/x-handlebars-template",
                "application/javascript");
        setNames("javascript", HBS_SCRIPT_EXTENSION, HANDLEBARS_SCRIPT_EXTENSION);
        
        loadHandlebars();
    }
    
    protected void deactivate(ComponentContext context) {
        
    }
    
    public String getLanguageName() {
        return "Handlebars";
    }
    
    protected void loadHandlebars() throws RepositoryException, LoginException, IOException {
        Context rhinoContext = getRhinoContext();
        ResourceResolver resolver = null;
        try{
            resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            
            InputStream handlebarsCompilerStream = JCRUtils.getFileResourceAsStream(resolver, handlebarsCompilerPath);
            scope = (ScriptableObject) rhinoContext.initStandardObjects(null);
            
            rhinoContext.evaluateReader(scope, new InputStreamReader(handlebarsCompilerStream), "handlebars.js", 1, null);
            
            InputStream handlebarsRenderStream = JCRUtils.getFileResourceAsStream(resolver, "/system/handlebars/handlebars-renderer.js");
            
            rhinoContext.evaluateReader(scope, new InputStreamReader(handlebarsRenderStream), "handlebars-renderer.js", 1, null);
        }
        finally
        {
            if(resolver != null)
            {
                resolver.close();
            }
        }
    }
    
    /**
     * 
     * Retrieves Rhino Context and sets language and optimizations.
     * 
     * @return
     */
    public Context getRhinoContext()
    {
        Context result = null;
        if(Context.getCurrentContext() == null)
        {
            Context.enter(); 
        }
        result = Context.getCurrentContext();
        result.setOptimizationLevel(-1);
        result.setLanguageVersion(Context.VERSION_1_7);
        return result;
    }
    
    public void setResourceResolverFactory(
            ResourceResolverFactory resourceResolverFactory) {
        this.resourceResolverFactory = resourceResolverFactory;
    }

}
