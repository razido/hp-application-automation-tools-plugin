/*
 *     Copyright 2017 Hewlett-Packard Development Company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.application.automation.tools.octane.actions;

import com.hp.mqm.client.MqmRestClient;
import com.hp.mqm.client.model.PagedList;
import com.hp.mqm.client.model.Workspace;
import com.hp.application.automation.tools.octane.client.JenkinsMqmRestClientFactory;
import com.hp.application.automation.tools.octane.configuration.ConfigurationService;
import com.hp.application.automation.tools.octane.configuration.ServerConfiguration;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

public class UFTTestDetectionPublisher extends Recorder {

	private final String workspaceName;

	public String getWorkspaceName() {
		return workspaceName;
	}

	// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
	@DataBoundConstructor
	public UFTTestDetectionPublisher(String workspaceName) {
		this.workspaceName = workspaceName;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
		// This is where you 'build' the project.
		// Since this is a dummy, we just say 'hello world' and call that a build.

		// This also shows how you can consult the global configuration of the builder
		String message = "";

		UFTTestDetectionBuildAction buildAction = new UFTTestDetectionBuildAction(message, build, getWorkspaceName());
		build.addAction(buildAction);

		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	private static <T> T getExtension(Class<T> clazz) {
		ExtensionList<T> items = Jenkins.getInstance().getExtensionList(clazz);
		return items.get(0);
	}

	@Extension // This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		private MqmRestClient createClient() {
			ServerConfiguration configuration = ConfigurationService.getServerConfiguration();
			JenkinsMqmRestClientFactory clientFactory = getExtension(JenkinsMqmRestClientFactory.class);
			return clientFactory.obtain(
					configuration.location,
					configuration.sharedSpace,
					configuration.username,
					configuration.password);
		}

		private String workspace;

		public DescriptorImpl() {
			load();
		}

		/**
		 * This method determines the values of the album drop-down list box.
		 *
		 * @return ListBoxModel result
		 */
		public ListBoxModel doFillWorkspaceNameItems() {
			ListBoxModel m = new ListBoxModel();
			PagedList<Workspace> workspacePagedList = createClient().queryWorkspaces("", 0, 200);
			List<Workspace> items = workspacePagedList.getItems();
			for (Workspace workspace : items) {
				m.add(workspace.getName(), String.valueOf(workspace.getId()));
			}
			return m;
		}

		public FormValidation doCheckWorkspaceName(@QueryParameter String value) throws IOException, ServletException {
			if (value == null || value.length() == 0) {
				return FormValidation.error("Please select workspace");
			} else {
				return FormValidation.ok();
			}
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		}

		public String getDisplayName() {
			return "HP Octane UFT Tests Scanner";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			workspace = formData.getString("useFrench");
			// ^Can also use req.bindJSON(this, formData);
			//  (easier when there are many fields; need set* methods for this, like setUseFrench)
			save();
			return super.configure(req, formData);
		}

		public String getWorkspace() {
			return workspace;
		}
	}
}