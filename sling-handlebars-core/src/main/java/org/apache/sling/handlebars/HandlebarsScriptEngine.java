package org.apache.sling.handlebars;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;

import javax.jcr.Node;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.json.jcr.JsonJcrNode;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.util.ScriptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlebarsScriptEngine extends AbstractSlingScriptEngine {
    
    private WebResourceScriptRunner scriptRunner;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public HandlebarsScriptEngine(ScriptEngineFactory factory, WebResourceScriptRunner scriptRunner) {
        super(factory);
        this.scriptRunner = scriptRunner;
    }
    
    public Object eval(Reader scriptReader, ScriptContext scriptContext)
            throws ScriptException {
    	StopWatch stopWatch = new StopWatch();
    	stopWatch.start();
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingHttpServletRequest request = (SlingHttpServletRequest) bindings.get(SlingBindings.REQUEST);
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
            InputStream handlebarsScriptStream = new ByteArrayInputStream(scriptBuffer.toString().getBytes());
            stopWatch.stop();
            log.info("Completed Handlebars Setup " + stopWatch);
            stopWatch = new StopWatch();
            stopWatch.start();
            
            renderedTemplate = this.scriptRunner.evaluateScript(handlebarsScriptStream, null);
            scriptContext.getWriter().write(renderedTemplate);
            stopWatch.stop();
            log.info("Completed Handlebars Render " + stopWatch);
        }catch(Exception e)
        {
            throw new ScriptException(e);
        }
        
        
        
        return renderedTemplate;
        
    }

}
