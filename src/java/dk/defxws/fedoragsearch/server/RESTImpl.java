//$Id: RESTImpl.java 7838 2008-11-21 11:41:12Z gertsp $
/*
 * <p><b>License and Copyright: </b>The contents of this file is subject to the
 * same open source license as the Fedora Repository System at www.fedora-commons.org
 * Copyright &copy; 2006, 2007, 2008 by The Technical University of Denmark.
 * All rights reserved.</p>
 */
package dk.defxws.fedoragsearch.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import dk.defxws.fedoragsearch.server.utils.IOUtils;
import dk.defxws.fedoragsearch.server.utils.Stream;
import org.apache.log4j.Logger;

import dk.defxws.fedoragsearch.server.errors.GenericSearchException;

/**
 * servlet for REST calls, calls the operationsImpl
 * 
 * @author  gsp@dtv.dk
 * @version 
 */
public class RESTImpl extends HttpServlet {

	private static final long serialVersionUID = 1L;

    private static final Logger logger =
        Logger.getLogger(RESTImpl.class);
    
    private Config config;
    
    private static final String PARAM_RESTXSLT = "restXslt";
    private static final String PARAM_INDEXDOCXSLT = "indexDocXslt";
    private static final String PARAM_RESULTPAGEXSLT = "resultPageXslt";
    
    private String repositoryName;
    private String indexName;
    private String resultPageXslt;
    private String restXslt;
    
    private static final String CONTENTTYPEHTML = "Html";
    
    private static final String OP_GFINDOBJECTS = "gfindObjects";
    private static final String OP_GETREPOSITORYINFO = "getRepositoryInfo";
    private static final String OP_GETINDEXINFO = "getIndexInfo";
    private static final String OP_UPDATEINDEX = "updateIndex";
    private static final String OP_BROWSEINDEX = "browseIndex";
    //MIH: added
    private static final String OP_FLUSHURLRESOURCES = "flushUrlResources";
    private static final String PARAM_CONFIGNAME = "configName";
    private static final String PARAM_OPERATION = "operation";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_HITPAGESTART = "hitPageStart";
    private static final String PARAM_HITPAGESIZE = "hitPageSize";
    private static final String PARAM_SNIPPETSMAX = "snippetsMax";
    private static final String PARAM_FIELDMAXLENGTH = "fieldMaxLength";
    private static final String PARAM_SORTFIELDS = "sortFields";
    private static final String PARAM_STARTTERM = "startTerm";
    private static final String PARAM_TERMPAGESIZE = "termPageSize";
    private static final String PARAM_REPOSITORYNAME = "repositoryName";
    private static final String PARAM_INDEXNAME = "indexName";
    private static final String PARAM_FIELDNAME = "fieldName";
    private static final String PARAM_ACTION = "action";
    private static final String PARAM_VALUE = "value";
    
    /** Exactly the same behavior as doGet */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        doGet(request, response);
    }
    
    /** Process http request */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException { 
    	Date startTime = new Date();
        String configName = request.getParameter(PARAM_CONFIGNAME);
        String operation = request.getParameter(PARAM_OPERATION);
        config = Config.getCurrentConfig();
        if (configName!=null && !"configure".equals(operation)) {
        	// mainly for test purposes
        	config = Config.getConfig(configName);
        }
        if (logger.isInfoEnabled())
            logger.info("request="+request.getQueryString()+" remoteUser="+request.getRemoteUser());
        repositoryName = request.getParameter(PARAM_REPOSITORYNAME);
        if (repositoryName==null) repositoryName="";
        indexName = request.getParameter(PARAM_INDEXNAME);
        if (indexName==null) indexName="";
        resultPageXslt = request.getParameter(PARAM_RESULTPAGEXSLT);
        if (resultPageXslt==null) resultPageXslt="";
        restXslt = request.getParameter(PARAM_RESTXSLT);
        if (restXslt==null) restXslt="";
        String[] params = new String[8];
        params[0] = "ERRORMESSAGE";
        params[1] = "";
        params[2] = "TIMEUSEDMS";
        params[3] = "";

        StringBuffer resultXml;
        try {
            if (OP_GFINDOBJECTS.equals(operation)) {
                resultXml = new StringBuffer(gfindObjects(request, response));
            } else if (OP_GETREPOSITORYINFO.equals(operation)) {
                resultXml = new StringBuffer(getRepositoryInfo(request, response));
            } else if (OP_GETINDEXINFO.equals(operation)) {
                resultXml = new StringBuffer(getIndexInfo(request, response));
            } else if (OP_UPDATEINDEX.equals(operation)) {
                resultXml = new StringBuffer(updateIndex(request, response));
            //MIH: added    
            } else if (OP_FLUSHURLRESOURCES.equals(operation)) {
                resultXml = new StringBuffer(flushUrlResources(request, response));
            } else if ("configure".equals(operation)) {
                resultXml = new StringBuffer(configure(request, response));
            } else if ("getIndexConfigInfo".equals(operation)) {
                resultXml = new StringBuffer(getIndexConfigInfo(request, response));
            } else {
                resultXml = new StringBuffer("<resultPage/>");
                if (restXslt==null || restXslt.isEmpty()) 
                    restXslt = config.getDefaultGfindObjectsRestXslt();
                if (operation!=null && !operation.isEmpty()) {
                    throw new GenericSearchException("ERROR: operation "+operation+" is unknown!");
                }
            }
        } catch (java.rmi.RemoteException e) {
//            throw new ServletException("ERROR: \n", e);
//            params[1] = e.toString();
            resultXml = new StringBuffer("<resultPage>");
            resultXml.append("<error><message><![CDATA[");
            resultXml.append(e.getMessage().replaceAll("!\\[CDATA\\[", "").replaceAll("\\]\\]", ""));
            resultXml.append("]]></message></error>");
            resultXml.append("</resultPage>");
            params[1] = e.getMessage();
            logger.error(e);
            //e.printStackTrace();
        }
        String timeusedms = Long.toString((new Date()).getTime() - startTime.getTime());
        params[3] = timeusedms;
        params[4] = "FGSUSERNAME";
        params[5] = request.getRemoteUser();
        params[6] = "SRFTYPE";
        params[7] = config.getSearchResultFilteringType();
        Stream stream = new GTransformer().transform(
        				config.getConfigName()+"/rest/"+restXslt,
        				resultXml, params);
        stream.lock();

        /*
         *  write content to servlet output stream
         */
        if (restXslt.indexOf(CONTENTTYPEHTML)>=0) {
            response.setContentType("text/html; charset=UTF-8");
        }
        else {
            response.setContentType("text/xml; charset=UTF-8");
        }
        
        ServletOutputStream out = response.getOutputStream();
        InputStream in = stream.getInputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out);
        response.setContentLength((int) stream.size()); // FIXME downcast!
        byte[] buff = new byte[4096];

        int bytesRead;
        while (-1 != (bytesRead = in.read(buff, 0, buff.length))) {
            bufferedOutputStream.write(buff, 0, bytesRead);
        }
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        out.flush();
        out.close();
        in.close();
        
        stream.close();
        
        if (logger.isInfoEnabled())
            logger.info("request="+request.getQueryString()+" timeusedms="+timeusedms);
    }
    
    private String gfindObjects(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        if (restXslt==null || restXslt.isEmpty()) {
            restXslt = config.getDefaultGfindObjectsRestXslt();
        }
        String query = request.getParameter(PARAM_QUERY);
        if (query==null || query.isEmpty()) {
            return "<resultPage/>";
        }
        int hitPageStart = config.getDefaultGfindObjectsHitPageStart();
        try {
            hitPageStart = Integer.parseInt(request.getParameter(PARAM_HITPAGESTART));
        } catch (NumberFormatException nfe) {
        }
        int hitPageSize = config.getDefaultGfindObjectsHitPageSize();
        try {
            hitPageSize = Integer.parseInt(request.getParameter(PARAM_HITPAGESIZE));
        } catch (NumberFormatException nfe) {
        }
        if (hitPageSize > config.getMaxPageSize()) hitPageSize = config.getMaxPageSize();
        int snippetsMax = config.getDefaultGfindObjectsSnippetsMax();
        try {
            snippetsMax = Integer.parseInt(request.getParameter(PARAM_SNIPPETSMAX));
        } catch (NumberFormatException nfe) {
        }
        int fieldMaxLength = config.getDefaultGfindObjectsFieldMaxLength();
        try {
            fieldMaxLength = Integer.parseInt(request.getParameter(PARAM_FIELDMAXLENGTH));
        } catch (NumberFormatException nfe) {
        }
        String sortFields = request.getParameter(PARAM_SORTFIELDS);
        if (sortFields==null) {
        	sortFields = "";
        }
        Operations ops = config.getOperationsImpl(request.getRemoteUser(), indexName);
        String result = ops.gfindObjects(query, hitPageStart, hitPageSize, snippetsMax, fieldMaxLength, indexName, sortFields,resultPageXslt);
        return result;
    }
    
    private String getRepositoryInfo(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        if (restXslt==null || restXslt.isEmpty()) {
            restXslt = config.getDefaultGetRepositoryInfoRestXslt();
        }
        GenericOperationsImpl ops = new GenericOperationsImpl();
        ops.init(indexName, config);
        String result = ops.getRepositoryInfo(repositoryName, resultPageXslt);
        return result;
    }
    
    private String getIndexInfo(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        if (restXslt==null || restXslt.isEmpty()) {
            restXslt = config.getDefaultGetIndexInfoRestXslt();
        }
        Operations ops = config.getOperationsImpl(indexName);
        String result = ops.getIndexInfo(indexName, resultPageXslt);
        return result;
    }
    
    //MIH: flush stored url-resources
    private String flushUrlResources(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        if (restXslt==null || restXslt.isEmpty()) {
            restXslt = config.getDefaultGetIndexInfoRestXslt();
        }
    	config.setUrlResources(new Hashtable<String, byte[]>());
    	return "<resultPage><flushUrlResources>OK</flushUrlResources></resultPage>";
    }
    
    public String updateIndex(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        if (restXslt==null || restXslt.isEmpty()) {
            restXslt = config.getDefaultUpdateIndexRestXslt();
        }
        String action = request.getParameter(PARAM_ACTION);
        if (action==null) action="";
        String value = request.getParameter(PARAM_VALUE);
        if (value==null) value="";
        String indexDocXslt = request.getParameter(PARAM_INDEXDOCXSLT);
        if (indexDocXslt==null) indexDocXslt="";
        GenericOperationsImpl ops = new GenericOperationsImpl();
        ops.init(request.getRemoteUser(), indexName, config);
        String result = ops.updateIndex(action, value, repositoryName, indexName, indexDocXslt, resultPageXslt);
        return result;
    }
    
    private String configure(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        String configName = request.getParameter(PARAM_CONFIGNAME);
        String propertyName = request.getParameter("propertyName");
        String propertyValue = "";
        if (!(propertyName==null || propertyName.isEmpty())) {
            propertyValue = request.getParameter("propertyValue");
            // used to set or change a property value, mainly for test purposes
            Config.configure(configName, propertyName, propertyValue);
        } else {
        	// used to create a new currentConfig, mainly for test purposes
            Config.configure(configName);
            config = Config.getCurrentConfig();
        }
        if (restXslt==null || restXslt.isEmpty()) 
            restXslt = config.getDefaultGfindObjectsRestXslt();
        return "<resultPage/>";
    }
    
    private String getIndexConfigInfo(HttpServletRequest request, HttpServletResponse response)
    throws java.rmi.RemoteException {
        if (restXslt==null || restXslt.isEmpty()) 
            restXslt = "copyXml";
        StringBuffer resultXml = new StringBuffer("<resultPage>");
    	String[] indexNames = config.getIndexNames(null).split("\\s");
    	for (int i=0;i<indexNames.length;i++) {
    		resultXml.append("<index>");
    		resultXml.append("<name>").append(indexNames[i]).append("</name>");
    		Properties props = config.getIndexProps(indexNames[i]);
    		for (Iterator iterator = props.keySet().iterator(); iterator
					.hasNext();) {
				String key = (String) iterator.next();
				String value = props.getProperty(key);
				resultXml.append("<property><key>")
						.append(key)
						.append("</key><value>")
						.append(value)
						.append("</value></property>");
			}
    		resultXml.append("</index>");
    	}
    	resultXml.append("</resultPage>");
        return resultXml.toString();
    }
    
    /**
     * Initialize servlet.
     *
     * @throws ServletException If the servlet cannot be initialized.
     */
    public void init() throws ServletException {
        //		DOMConfigurator.configure("log4j.xml");
        if (logger.isDebugEnabled())
            logger.debug("Servlet init");
    }
    
}