package org.jboss.devnation.iotbof.beans;
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jboss.devnation.iotbof.ejbs.IProgress;
import org.jboss.devnation.iotbof.ejbs.NSPConnector;
import org.jboss.devnation.iotbof.events.INotificationService;
import org.jboss.devnation.iotbof.events.NspNotificationMsg;
import org.jboss.devnation.iotbof.rest.Endpoint;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Scott Stark (sstark@redhat.com) (C) 2014 Red Hat Inc.
 */
@Named("nspmodel")
@ApplicationScoped
public class NSPModel implements IProgress {
   private static Logger logger = Logger.getLogger(NSPModel.class);

   @EJB
   private NSPConnector nspConnector;
   @Inject
   private INotificationService notificationService;
   /** */
   private List<Endpoint> endpoints;
   /** */
   private ArrayList<NspNotificationMsg> notificationMsgs = new ArrayList<>();
   private ProgressBarBean progressBar = new ProgressBarBean();

   @PostConstruct
   private void init() {
      logger.infof("Initializing, %s", this);
      // Initialize the EndpointConverter, AsyncResponseConverter
      EndpointConverter.setConnector(nspConnector);
      AsyncResponseConverter.setNotificationService(notificationService);
   }

   public String reload() {
      logger.infof("Reloading endpoints");
      progressBar.startProcess();
      // Load the current NSP endpoints
      nspConnector.reload(this);
      endpoints = nspConnector.getEndpoints();
      logger.infof("Initialized with the following endpoints: %s\n", endpoints);
      return null;
   }
   public void enableCallbacks() {
      logger.infof("Reloading endpoints");
      // Enable callbacks to the NspNotificationService
      try {
         nspConnector.enableNotificationHandler();
         logger.infof("Notifications are enabled");
      } catch (Exception e) {
         logger.warn("Notifications could not be enabled");
         FacesContext context = FacesContext.getCurrentInstance();
         FacesMessage msg = new FacesMessage("Failed to register for notifications, "+e.getMessage());
         msg.setSeverity(FacesMessage.SEVERITY_WARN);
         context.addMessage(null, msg);
         FacesContext.getCurrentInstance().getPartialViewContext().getRenderIds().add("appMessages");
      }
   }

   @Override
   public void updateProgress(int current, int max, String msg) {
      int pct = current * 100 / max;
      progressBar.setCurrent(pct);
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage(msg));
   }
   public int getProgress() {
      return progressBar.getCurrentValue();
   }
   public void onComplete() {
      logger.infof("Adding completed message, %d endpoints\n", endpoints.size());
      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
         "Loading Completed", "Loading Completed"));
    }

   public List<Endpoint> getEndpoints() {
      logger.infof("Returning %d endpoints\n", getEndpointsSize());
      return endpoints;
   }

   public String getConnectionStatus() {
      String serverInfo = nspConnector.getServerInfo();
      return "Connected to: "+ serverInfo;
   }

   public ProgressBarBean getProgressBar() {
      return progressBar;
   }
   public int getEndpointsSize() {
      int size = endpoints != null ? endpoints.size() : 0;
      return size;
   }
   public List<NspNotificationMsg> getNotificationMsgs() {
      return notificationMsgs;
   }

   public void save(ActionEvent actionEvent) {
      String ok = null;
      //String ok = nspConnector.setEndpointResource(endpoint, resource, editValue);
      FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Saved value, msg=" + ok));
   }
}