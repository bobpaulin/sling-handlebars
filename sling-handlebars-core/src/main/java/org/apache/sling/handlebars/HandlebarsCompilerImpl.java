package org.apache.sling.handlebars;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.service.component.ComponentContext;

import org.apache.sling.webresource.WebResourceScriptCompiler;
import org.apache.sling.webresource.exception.WebResourceCompileException;
import org.apache.sling.webresource.util.JCRUtils;
import org.apache.sling.webresource.util.ScriptUtils;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label="Handlebars Compiler Service", immediate=true, metatype=true)
@Service
public class HandlebarsCompilerImpl implements WebResourceScriptCompiler {
    
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    private ScriptableObject scope = null;
    
    @org.apache.felix.scr.annotations.Property(label="Handlebars Compiler Script Path", value="/system/handlebars/handlebars.js")
    private final static String HANDLEBARS_COMPILER_PATH = "handlebars.compiler.path";
    
    @org.apache.felix.scr.annotations.Property(label="Handlebars Cache Path", value="/var/handlebars")
    private final static String HANDLEBARS_CACHE_PATH = "handlebars.cache.path";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private String handlebarsCompilerPath;
    
    private String handlebarsCachePath;
    
    
    public void activate(final ComponentContext context) throws Exception
    {
        Dictionary config = context.getProperties();
        handlebarsCompilerPath = PropertiesUtil.toString(config.get(HANDLEBARS_COMPILER_PATH), "/system/handlebars/handlebars.js");
        handlebarsCachePath = PropertiesUtil.toString(config.get(HANDLEBARS_CACHE_PATH), "/var/handlebars");
        loadHandlebarsCompiler();
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
            StringReader handlebarsReader = new StringReader(scriptBuffer.toString());

            Context rhinoContext = getContext();
            rhinoContext.initStandardObjects(scope);

            String compiledScript = (String)rhinoContext.evaluateReader(scope, handlebarsReader, "HandlebarsCompile", 1, null);
            compiledScript = "Handlebars.template(" + compiledScript + ");";
            return new ByteArrayInputStream(compiledScript.getBytes());
        }
        catch(Exception e)
        {
           throw new WebResourceCompileException(e);
        }
        finally
        {
            if (Context.getCurrentContext() != null) {
                Context.exit();
            }
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
    
    protected void loadHandlebarsCompiler() throws LoginException,
        PathNotFoundException, RepositoryException, ValueFormatException,
        IOException {
        Context rhinoContext = getContext();
        ResourceResolver resolver = null;
        try{
            resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            
            InputStream handlebarsCompilerStream = JCRUtils.getFileResourceAsStream(resolver, handlebarsCompilerPath);
            scope = (ScriptableObject) rhinoContext.initStandardObjects(null);
            
            rhinoContext.evaluateReader(scope, new InputStreamReader(handlebarsCompilerStream), "handlebars.js", 1, null);
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
    public Context getContext()
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

