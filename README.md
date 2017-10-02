# Introduction

Jenkins plugin for Testein - SaaS for easy and fast creating, managing and running automation tests without code knowledge.

It contains two modules:
1) **Run tests**
2) **Upload custom steps**

# Configuration

After plugin installation you need to configure your plugin with following properties:

    - Company id - your company id for sing in (not company display name), example: testein
    - User name - your user name
    - User token - can be found on your Testein settings page


# Run tests/application/suite

This module allows you to run your Testein tests, separately or in suite or application.

You need to specify:

    - Target type - what to run (test/test suite/application)
    - Target id - id of the test/test suite/application
    - Willing to download report - if checked, report for the run will be downloaded into workspace
    in format Testein-Report-{runId}.html

# Upload custom test steps

This module allows you to upload your custom test steps (JS or Java) to the Testein.
For example, you can build your Java custom steps using Maven and then upload them directly to Tetsein

You need to specify:

    - If checked **"Upload js step"** - pathes to .js script file and .json descriptor file
    - If checked **"Upload java steps"** - path to the .jar file

**Note:** all file paths are relative to workspace root