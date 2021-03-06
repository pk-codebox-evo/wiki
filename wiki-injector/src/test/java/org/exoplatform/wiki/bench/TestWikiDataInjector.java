/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.bench;

import junit.framework.TestCase;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.bench.WikiDataInjector.CONSTANTS;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.WikiService;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class TestWikiDataInjector extends TestCase {

  private static StandaloneContainer container;

  private final static String        WIKI_WS = "collaboration".intern();

  private WikiDataInjector           injector;
  
  private WikiService wikiService;

  protected void begin() {
    RequestLifeCycle.begin(container);
  }

  protected void end() {
    RequestLifeCycle.end();
  }

  protected void setUp() throws Exception {
    initContainer();
    initJCR();
    begin();
    Identity systemIdentity = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(systemIdentity));
    this.wikiService = container.getComponentInstanceOfType(WikiService.class);
    this.injector = new WikiDataInjector(wikiService, null);
  }

  protected void tearDown() throws Exception {
    end();
  }

  private static void initContainer() {
    try {
      String containerConf = Thread.currentThread().getContextClassLoader().getResource("conf/standalone/configuration.xml").toString();
      StandaloneContainer.addConfigurationURL(containerConf);

      //
      container = StandaloneContainer.getInstance();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize standalone container: " + e.getMessage(), e);
    }
  }
  
  private static void initJCR() {
    try {
      RepositoryService  repositoryService = container.getComponentInstanceOfType(RepositoryService.class);
      assertNotNull(repositoryService);
      Session session = repositoryService.getCurrentRepository().getSystemSession(WIKI_WS);
   // Remove old data before to starting test case.
        	
      StringBuffer stringBuffer = new StringBuffer();
      	  	
      stringBuffer.append("/jcr:root").append("//*[fn:name() = 'eXoWiki' or fn:name() = 'ApplicationData']");
      	  	
      QueryManager qm = session.getWorkspace().getQueryManager();      	  	
      Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);      	  	
      QueryResult result = query.execute();      	  	
      NodeIterator iter = result.getNodes();      	  	
      while (iter.hasNext()) {      	  	
        Node node = iter.nextNode();      	  	
        try {      	  	
          node.remove();      	  	
        } catch (Exception e) {}      	  	
      } 	  	
      session.save();
      assertNotNull(session);
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize JCR: ", e);
    }
  }
  
  private HashMap<String, String> createInjectPageParam(String quantity,
                                                        String prefix,
                                                        String totalPage,
                                                        String attSize,
                                                        String wikiOwner,
                                                        String wikiType) {
    HashMap<String, String> queryParams = new HashMap<String, String>();
    queryParams.put(CONSTANTS.TYPE.getName(), CONSTANTS.DATA.getName());
    queryParams.put(WikiDataInjector.QUANTITY, quantity);
    queryParams.put(WikiDataInjector.PREFIX, prefix);
    queryParams.put(WikiDataInjector.PAGE_SIZE, totalPage);
    queryParams.put(WikiDataInjector.ATTACH_SIZE, attSize);
    queryParams.put(WikiDataInjector.WIKI_OWNER, wikiOwner);
    queryParams.put(WikiDataInjector.WIKI_TYPE, wikiType);
    return queryParams;
  }
  
  private HashMap<String, String> createRejectPageParam(String quantity,
                                                        String prefix,
                                                        String wikiOwner,
                                                        String wikiType) {
    HashMap<String, String> queryParams = new HashMap<String, String>();
    queryParams.put(CONSTANTS.TYPE.getName(), CONSTANTS.DATA.getName());
    queryParams.put(WikiDataInjector.QUANTITY, quantity);
    queryParams.put(WikiDataInjector.PREFIX, prefix);
    queryParams.put(WikiDataInjector.WIKI_OWNER, wikiOwner);
    queryParams.put(WikiDataInjector.WIKI_TYPE, wikiType);
    return queryParams;
  }

  public void testInjectData() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page wikiHome = wiki.getWikiHome();
    HashMap<String, String> injectParams = createInjectPageParam("2", "a", "100", "100", "classic", PortalConfig.PORTAL_TYPE);
    injector.inject(injectParams);
    assertTrue(injector.getPagesByPrefix("a", wikiHome).size() == 2);
    
    injectParams = createInjectPageParam("2,3", "a,b", "100", "100", "classic", PortalConfig.PORTAL_TYPE);
    injector.inject(injectParams);
    List<Page> pages = injector.getPagesByPrefix("a", wikiHome);
    Iterator<Page> iter = pages.iterator();
    assertTrue(pages.size() == 2);
    assertTrue(injector.getPagesByPrefix("b", iter.next()).size() == 3);
    
    injectParams = createInjectPageParam("1,2", "c,b", "100", "100", "classic", PortalConfig.PORTAL_TYPE);
    injector.inject(injectParams);
    pages = injector.getPagesByPrefix("c", wikiHome);
    iter = pages.iterator();
    assertTrue(injector.getPagesByPrefix("b", iter.next()).size() == 2);

    pages = injector.getPagesByPrefix("a", wikiHome);
    iter = pages.iterator();
    assertTrue(injector.getPagesByPrefix("b", iter.next()).size() == 3);
  }
  
  public void testRejectData() throws Exception {
    Wiki wiki = wikiService.createWiki(PortalConfig.PORTAL_TYPE, "classic");
    Page wikiHome = wiki.getWikiHome();
    HashMap<String, String> injectParams = createInjectPageParam("2", "e", "100", "100", "classic", PortalConfig.PORTAL_TYPE);
    injector.inject(injectParams);
    assertTrue(injector.getPagesByPrefix("e", wikiHome).size() == 2);
    HashMap<String, String> rejectParams = createRejectPageParam("2", "e", "classic", PortalConfig.PORTAL_TYPE);
    injector.reject(rejectParams);
    assertTrue(injector.getPagesByPrefix("e", wikiHome).size() == 0);
  }

}
