package org.apache.sling.handlebars;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.service.component.ComponentContext;

import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.jcr.JsonJcrNode;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;

import org.apache.commons.io.IOUtils;

import org.apache.commons.lang.time.StopWatch;

import org.apache.sling.webresource.util.JCRUtils;
import org.apache.sling.webresource.util.ScriptUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlebarsScriptEngine extends AbstractSlingScriptEngine {
    
    private String handlebarsCompilerPath;
    
    private ScriptableObject scope;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public HandlebarsScriptEngine(ScriptEngineFactory factory, ScriptableObject scope) {
        super(factory);
        this.scope = scope;
    }
    
    public Object eval(Reader scriptReader, ScriptContext scriptContext)
            throws ScriptException {
        
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
        SlingHttpServletResponse response = (SlingHttpServletResponse) bindings.get(SlingBindings.RESPONSE);
        String renderedTemplate = null;
        try{
            JsonJcrNode jsonInput = new JsonJcrNode(request.getResource().adaptTo(Node.class));
            
            
            String handlebarsScript = IOUtils.toString(scriptReader);
            StringBuffer scriptBuffer = new StringBuffer();
            scriptBuffer.append("compileAndRenderTemplate(");
            scriptBuffer.append(ScriptUtils.toJSMultiLineString(handlebarsScript));
            scriptBuffer.append(", ");
            scriptBuffer.append(jsonInput.toString());
            scriptBuffer.append(");");
            StringReader handlebarsReader = new StringReader(scriptBuffer.toString());
    
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            
            Context rhinoContext = getRhinoContext();
            Scriptable scriptScope = rhinoContext.newObject(scope);
            scriptScope.setPrototype(scope);
            scriptScope.setParentScope(null);
            
            renderedTemplate = (String)rhinoContext.evaluateReader(scriptScope, handlebarsReader, "HandlebarsRender", 1, null);
            stopWatch.stop();
            log.debug("Completed Handlebars Compile " + stopWatch);
            response.getWriter().write(renderedTemplate);
        }catch(Exception e)
        {
            throw new ScriptException(e);
        }
        
        
        
        return renderedTemplate;
        
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

}
