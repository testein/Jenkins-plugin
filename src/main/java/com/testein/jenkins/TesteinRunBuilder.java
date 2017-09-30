package com.testein.jenkins;
import com.testein.jenkins.runners.ApplicationExecutor;
import com.testein.jenkins.runners.IExecutor;
import com.testein.jenkins.runners.SuiteExecutor;
import com.testein.jenkins.runners.TestExecutor;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;

public class TesteinRunBuilder extends Builder implements SimpleBuildStep {
    public final static String _testTargetType = "test";
    public final static String _suiteTargetType = "suite";
    public final static String _applicationTargetType = "application";
    public final static String[] _targetTypes = new String[] {_testTargetType, _suiteTargetType, _applicationTargetType};

    private final String targetType;
    private final String targetId;
    private final Boolean downloadReport;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TesteinRunBuilder(String targetType, String targetId, Boolean downloadReport, Boolean downloadLogs) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.downloadReport = downloadReport;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException {
        listener.getLogger().println("Starting to run Testein " + targetType + " with id " + targetId + "...");
        TesteinRunDescriptorImpl descriptor = getDescriptor();
        String auth = descriptor.companyName + ":" + descriptor.userName + ":" + descriptor.userToken;

        IExecutor executor;
        switch (targetType.toLowerCase()){
            case _testTargetType:
                executor = new TestExecutor(auth, listener, workspace);
                break;

            case _suiteTargetType:
                executor = new SuiteExecutor(auth, listener, workspace);
                break;

            case _applicationTargetType:
                executor = new ApplicationExecutor(auth, listener, workspace);
                break;

            default:
                listener.error("Unknown target type: " + targetType);
                return;
        }

        UUID runId = executor.start(UUID.fromString(targetId));

        try {
            executor.poll(runId, downloadReport);
        } catch (Exception e){
            throw new IOException(e);
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public TesteinRunDescriptorImpl getDescriptor() {
        return (TesteinRunDescriptorImpl)super.getDescriptor();
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public Boolean getDownloadReport() {
        return downloadReport;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class TesteinRunDescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use {@code transient}.
         */
        private String companyName;
        private String userName;
        private String userToken;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public TesteinRunDescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public FormValidation doCheckCompanyName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a company name");
            return FormValidation.ok();
        }

        public FormValidation doCheckUserName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a user name");
            return FormValidation.ok();
        }

        public FormValidation doCheckUserToken(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a user token");
            return FormValidation.ok();
        }

        /*public FormValidation doCheckTargetType(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a target type");
            for (String type : _targetTypes){
                if (type.equalsIgnoreCase(value)){
                    return FormValidation.ok();
                }
            }

            return FormValidation.error("Please set a valid target type");
        }*/

        public FormValidation doCheckTargetId(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a target id");
            try {
                UUID.fromString(value);
            } catch (Exception e){
                return FormValidation.error("Please set a valid target id");
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillTargetTypeItems() {
            ListBoxModel items = new ListBoxModel();
            for (String type : _targetTypes) {
                items.add(type.substring(0,1).toUpperCase() + type.substring(1), type);
            }
            return items;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Run Testein test/suite/application";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            companyName = formData.getString("companyName");
            userName = formData.getString("userName");
            userToken = formData.getString("userToken");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }
        public String getCompanyName() {
            return companyName;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserToken() {
            return userToken;
        }
    }
}

