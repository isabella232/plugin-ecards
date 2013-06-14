
README
------

How to add a ViewTool OSGI plugin
---------------------------------

--
In order to create this OSGI plugin, you must write the META-INF/MANIFEST
to be inserted into OSGI jar.

In this MANIFEST you must specify (see template plugin):

Bundle-Name: The name of your bundle

Bundle-SymbolicName: A short an unique name for the bundle

Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.viewtools.Activator)

DynamicImport-Package: *
    Dynamically add required imports the plugin may need without add them explicitly

Import-Package: This is a comma separated list of package's name.
                In this list there must be the packages that you are using inside
                the bundle plugin and that are exported by the dotCMS runtime.

Beware!!!
---------

In order to work inside the Apache Felix OSGI runtime, the import
and export directive must be bidirectional.

The DotCMS must declare the set of packages that will be available
to the OSGI plugins by changing the property:
felix.org.osgi.framework.system.packages.extra
inside the configuration file src-conf/dotmarketing-config.properties

Only after that exported packages are defined in this list,
a plugin can Import the packages to use them inside the OSGI blundle.

--
--
--
com.dotmarketing.osgi.viewtools.MyToolInfo -> For registering and initialization of our ViewTool implementation
com.dotmarketing.osgi.viewtools.MyViewTool -> ViewTool implementation

--
Activator
---------

This bundle activator extends from com.dotmarketing.osgi.GenericBundleActivator and implements BundleActivator.start().
This activator will allow you to register the MyToolInfo object using the GenericBundleActivator.registerViewToolService method

----------------------------------------------------------
NOTE:
--------------------------------------------------------
Remember to modify the variable 'public static final String HOST_PORT="";, in the Ecards.java file, If your server is in a port different of "80" and run "ant" in the osgi directory where you have the plugin. 

To use this plugin you need to:
1. Copy the com.dotcms.ecards.fragment and com.dotcms.ecards folders, plus the build.xml into the "<dotCMS_HOME>/docs/examples/osgi"
folder and run this command: ant deploy. 

This external build.xml will execute the two plugin build.xml files (inside the com.dotcms.ecards.fragment and com.dotcms.ecards folders) to generate the jar files.
If you prefer, you could run the build.xml files inside each folder one by one.

2. add first the "fragment-com.dotcms.ecards.fragment.jar" jar file in the dynamic plugins portlet
3. add the "bundle-com.dotcms.ecards.jar" jar file in the dynamic plugins portlet
4. add a widget in the page where you want to use the plugin. The widget should have this code: 
#dotParse('/ecards/velocity/ecards.vtl')
