package com.testein.jenkins;

import com.testein.jenkins.api.enums.UploadTargetType;
import com.testein.jenkins.runners.*;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

public class TesteinUploadStepBuilder extends Builder implements SimpleBuildStep {
    private final EnableJs enableJs;
    private final EnableJar enableJar;
    private final Boolean overwrite;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public TesteinUploadStepBuilder(EnableJs enableJs, EnableJar enableJar, Boolean overwrite) {
        this.enableJs = enableJs;
        this.enableJar = enableJar;
        this.overwrite = overwrite;
    }

    public EnableJs getEnableJs() {
        return enableJs;
    }

    public Boolean isJsEnabled(){
        return enableJs != null;
    }

    public EnableJar getEnableJar() {
        return enableJar;
    }

    public Boolean isJarEnabled(){
        return enableJar != null;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public String getJsFilePath(){
        return enableJs != null ? enableJs.jsFilePath : null;
    }

    public String getJsonFilePath(){
        return enableJs != null ? enableJs.jsonFilePath : null;
    }

    public String getJarFilePath(){
        return enableJar != null ? enableJar.jarFilePath : null;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException {
        listener.getLogger().println("Starting to upload custom test steps to Testein...");
        TesteinRunBuilder.TesteinRunDescriptorImpl descriptor = getTopLevelDescriptor();
        String auth = descriptor.getCompanyName() + ":" + descriptor.getUserName() + ":" + descriptor.getUserToken();

        Uploader uploader = new Uploader(listener, auth, workspace);
        if (enableJs != null){
            uploader.upload(UploadTargetType.Js, enableJs.jsFilePath, enableJs.jsonFilePath, null, overwrite);
        }
        if (enableJar != null){
            uploader.upload(UploadTargetType.Jar, null, null, enableJar.jarFilePath, overwrite);
        }

        listener.getLogger().println("Steps were uploaded successfully");
    }

    public static class EnableJs
    {
        private String jsFilePath;
        private String jsonFilePath;

        @DataBoundConstructor
        public EnableJs(String jsFilePath, String jsonFilePath)
        {
            this.jsFilePath = jsFilePath;
            this.jsonFilePath = jsonFilePath;
        }
    }

    public static class EnableJar
    {
        private String jarFilePath;

        @DataBoundConstructor
        public EnableJar(String jarFilePath)
        {
            this.jarFilePath = jarFilePath;
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public TesteinUploadDescriptorImpl getDescriptor() {
        return (TesteinUploadDescriptorImpl)super.getDescriptor();
    }

    protected static TesteinRunBuilder.TesteinRunDescriptorImpl getTopLevelDescriptor(){
        TesteinRunBuilder.TesteinRunDescriptorImpl sad = (TesteinRunBuilder.TesteinRunDescriptorImpl) Jenkins.getInstance().getDescriptor(TesteinRunBuilder.class);
        sad.load();
        return sad;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class TesteinUploadDescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public TesteinUploadDescriptorImpl() {
            load();
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

        public FormValidation doCheckJsFilePath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a js script file path");

            if (!value.toLowerCase().endsWith(".js"))
                return FormValidation.error("Please select a .js file");

            return FormValidation.ok();
        }

        public FormValidation doCheckJsonFilePath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a json descriptor file path");

            if (!value.toLowerCase().endsWith(".json"))
                return FormValidation.error("Please select a .json file");

            return FormValidation.ok();
        }

        public FormValidation doCheckJarFilePath(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a jar file path");

            if (!value.toLowerCase().endsWith(".jar"))
                return FormValidation.error("Please select a .jar file");

            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Upload Testein custom test steps";
        }
    }
}

