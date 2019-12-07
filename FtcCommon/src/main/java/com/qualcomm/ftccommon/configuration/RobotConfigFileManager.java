/*
 * FTC Team 25: cmacfarl, June 18, 2016
 */

package com.qualcomm.ftccommon.configuration;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;
import android.view.View;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.qualcomm.ftccommon.CommandList;
import com.qualcomm.ftccommon.R;
import com.qualcomm.robotcore.exception.DuplicateNameException;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.configuration.ControllerConfiguration;
import com.qualcomm.robotcore.hardware.configuration.ReadXMLFileHandler;
import com.qualcomm.robotcore.hardware.configuration.WriteXMLFileHandler;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.SerialNumber;

import org.firstinspires.ftc.robotcore.external.Supplier;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.system.Dom2XmlPullBuilder;
import org.firstinspires.ftc.robotcore.internal.collections.SimpleGson;
import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * A class that helps to manage robot configuration files and stores our list of
 * resource ids that maps to robot configurations packaged with the apk.
 */
@SuppressWarnings("WeakerAccess")
public class RobotConfigFileManager {

    public static final String TAG = "RobotConfigFileManager";
    public static final boolean DEBUG = false;

    public static final String ROBOT_CONFIG_DESCRIPTION_GENERATE_XSLT = "RobotConfigDescriptionGenerate.xslt";
    public static final String ROBOT_CONFIG_TAXONOMY_XML = "RobotConfigTaxonomy.xml";
    public static final String FILE_LIST_COMMAND_DELIMITER = ";";
    public static final String FILE_EXT = ".xml";
    public        final String noConfig;

    private Context context;
    private Activity activity;
    private Resources resources;
    private WriteXMLFileHandler writer;
    private SharedPreferences preferences;
    private final @IdRes int idActiveConfigName = R.id.idActiveConfigName;
    private final @IdRes int idActiveConfigHeader = R.id.idActiveConfigHeader;
    private NetworkConnectionHandler networkConnectionHandler = NetworkConnectionHandler.getInstance();
    private AppUtil appUtil = AppUtil.getInstance();

    /*
     * Initialized once very early in execution, these supply the resource ids of configurations and
     * configuration templates respectively.
     */
    private static Supplier<Collection<Integer>> xmlResourceIdSupplier = null;
    private static Supplier<Collection<Integer>> xmlResourceTemplateIdsSupplier = null;

    public RobotConfigFileManager(Activity activity)
    {
        this.activity = activity;
        this.context = appUtil.getApplication();
        this.resources = context.getResources();
        this.writer = new WriteXMLFileHandler(context);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.noConfig = this.context.getString(R.string.noCurrentConfigFile);
    }

    /*
     * Use only when no UI interaction feedback / colorization is required
     */
    public RobotConfigFileManager()
    {
        this(null);
    }

    /**
     * Builds the Folder on the sdcard that holds all of the configuration files
     * if it doesn't exist. If this fails, a toast will pop up.
     */
    public void createConfigFolder()
    {
        File robotDir = AppUtil.CONFIG_FILES_DIR;
        boolean createdDir = true;

        if (!robotDir.exists()){
            createdDir = robotDir.mkdir();
        }

        if (!createdDir){
            RobotLog.ee(TAG, "Can't create the Robot Config Files directory!");
            appUtil.showToast(UILocation.BOTH, context.getString(R.string.toastCantCreateRobotConfigFilesDir));
        }
    }

    /** Returns the value of the Robot@type attribute of the XML resources that we are interested in */
    public static String getRobotConfigTypeAttribute()
    {
        return "FirstInspires-FTC";
    }

    public static String getRobotConfigTemplateAttribute()
    {
        return "FirstInspires-FTC-template";
    }

    /** Sets the collection into which our class filter will insert the resource ids of
     * the resource-based robot XML configurations.
     * @see RobotConfigResFilter */
    public static void setXmlResourceIdSupplier(Supplier<Collection<Integer>> supplier)
    {
        xmlResourceIdSupplier = supplier;
    }
    public static void setXmlResourceTemplateIdSupplier(Supplier<Collection<Integer>> supplier)
    {
        xmlResourceTemplateIdsSupplier = supplier;
    }
    private Collection<Integer> getXmlResourceIds()
    {
        return xmlResourceIdSupplier != null ? xmlResourceIdSupplier.get() : new ArrayList<Integer>();
    }
    private Collection<Integer> getXmlResourceTemplateIds()
    {
        return xmlResourceTemplateIdsSupplier != null ? xmlResourceTemplateIdsSupplier.get() : new ArrayList<Integer>();
    }

    public @NonNull RobotConfigFile getConfigFromString(String objSerialized)
    {
        return RobotConfigFile.fromString(this, objSerialized);
    }

    public @NonNull RobotConfigFile getActiveConfigAndUpdateUI()
    {
        final RobotConfigFile file = getActiveConfig();
        this.updateActiveConfigHeader(file);
        return file;
    }

    public @NonNull RobotConfigFile getActiveConfig()
    {
        String key = context.getString(R.string.pref_hardware_config_filename);
        String objSerialized;
        RobotConfigFile file;

        objSerialized = preferences.getString(key, null);
        if (objSerialized == null) {
            file = RobotConfigFile.noConfig(this);
        } else {
            file = getConfigFromString(objSerialized);
        }

        if (DEBUG) RobotLog.vv(TAG, "getActiveConfig()='%s'", file.getName());
        return file;
    }

    public void sendActiveConfigToDriverStation()
    {
        RobotConfigFile configFile = this.getActiveConfig();
        String serialized = configFile.toString();
        // RobotLog.vv(TAG, "reporting active config: %s", configFile.getName());
        networkConnectionHandler.sendCommand(new Command(CommandList.CMD_NOTIFY_ACTIVE_CONFIGURATION, serialized));
    }

    public void setActiveConfigAndUpdateUI(boolean runningOnDriverStation, @NonNull RobotConfigFile configFile)
    {
        setActiveConfig(runningOnDriverStation, configFile);
        this.updateActiveConfigHeader(configFile);
    }

    public void setActiveConfigAndUpdateUI(@NonNull RobotConfigFile config)
    {
        setActiveConfig(config);
        this.updateActiveConfigHeader(config);
    }

    public void setActiveConfig(boolean runningOnDriverStation, @NonNull RobotConfigFile config)
    {
        if (runningOnDriverStation) {
            sendRobotControllerActiveConfigAndUpdateUI(config);
        } else {
            setActiveConfig(config);
            sendActiveConfigToDriverStation();
        }
    }

    public void setActiveConfig(@NonNull RobotConfigFile cfgFile)
    {
        if (DEBUG) RobotLog.vv(TAG, "setActiveConfig('%s')", cfgFile.getName());
        String objSerialized = SimpleGson.getInstance().toJson(cfgFile);
        SharedPreferences.Editor edit = preferences.edit();
        String key = context.getString(R.string.pref_hardware_config_filename);

        edit.putString(key, objSerialized);
        edit.apply();
    }

    public void sendRobotControllerActiveConfigAndUpdateUI(@NonNull RobotConfigFile config)
    {
        networkConnectionHandler.sendCommand(new Command(CommandList.CMD_ACTIVATE_CONFIGURATION, config.toString()));
    }

    public void updateActiveConfigHeader(RobotConfigFile robotConfigFile)
    {
        updateActiveConfigHeader(robotConfigFile.getName(), robotConfigFile.isDirty());
    }

    public void updateActiveConfigHeader(final String fileNameIn, final boolean dirty)
    {
        if (activity != null) {
            appUtil.runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    String fileName = stripFileNameExtension(fileNameIn).trim();
                    if (fileName.isEmpty()) {
                        fileName = noConfig;
                    }
                    if (dirty) {
                        fileName = String.format(context.getString(R.string.configDirtyLabel), fileName);
                    }

                    TextView activeFile = (TextView) activity.findViewById(idActiveConfigName);
                    if (activeFile != null) {
                        activeFile.setText(fileName);
                    } else {
                        RobotLog.ee(TAG, "unable to find header text 0x%08x", idActiveConfigName);
                    }

                    if (!dirty && fileName.equalsIgnoreCase(noConfig)) {
                        changeHeaderBackground(R.id.backgroundLightHolder);
                    } else if (dirty) {
                        changeHeaderBackground(R.id.backgroundDarkGrayHolder);
                    } else {
                        changeHeaderBackground(R.id.backgroundMediumHolder);
                    }
                }
            });
        } else {
            RobotLog.ee(TAG, "updateActiveConfigHeader called with null activity");
        }
    }

    public void changeHeaderBackground(@IdRes int idColorHolder)
    {
        if (activity != null) {
            View colorHolder = activity.findViewById(idColorHolder);
            View view = activity.findViewById(R.id.idActiveConfigHeader);
            // Views should only be null in activities that don't have a header
            if (colorHolder != null && view != null) {
                view.setBackground(colorHolder.getBackground());
            }
        } else {
            RobotLog.ee(TAG, "changeHeaderBackground called with null activity");
        }
    }

    public static class ConfigNameCheckResult
    {
        public boolean success = true;
        public String  errorFormat = null;
        public ConfigNameCheckResult(boolean success)
        {
            this.success = success;
        }
        public ConfigNameCheckResult(@NonNull String errorFormat)
        {
            this.success = false;
            this.errorFormat = errorFormat;
        }
    }

    /**
     * Answers as to whether the candidate config name is a plausible one to use as the name
     * of a new robot configuration.
     *
     * @param candidate   the new name to test
     * @param extantConfigurations  the list of the currently existing robot configurations
     * @return whether the candidate config name is a plausible one
     */
    public ConfigNameCheckResult isPlausibleConfigName(RobotConfigFile existingConfig, String candidate, List<RobotConfigFile> extantConfigurations)
    {
        // We disallow whitespace at front and back to reduce user confusion
        if (!candidate.equals(candidate.trim())) return new ConfigNameCheckResult(context.getString(R.string.configNameWhitespace));

        // Empty names aren't allowed
        if (candidate.length()==0) return new ConfigNameCheckResult(context.getString(R.string.configNameEmpty));

        // Names that contain path components aren't allowed as they'd end up in the wrong folder
        File file = new File(candidate);
        if (!file.getName().equals(candidate)) return new ConfigNameCheckResult(context.getString(R.string.configNameIllegalCharacters));

        // Certain characters can't be used in the Android SDCARD file system (which is, we believe, FAT32)
        // This may not be definititive, but will catch a lot of bad file names before they're attempted
        String reservedChars = "?:\"*|/\\<>";
        for (char candidateChar : candidate.toCharArray()) {
            if (reservedChars.indexOf(candidateChar) != -1) {
                return new ConfigNameCheckResult(context.getString(R.string.configNameIllegalCharacters));
            }
        }

        // Can't mirror the 'nothing here' guy
        if (candidate.equalsIgnoreCase(noConfig)) return new ConfigNameCheckResult(context.getString(R.string.configNameReserved));

        // Always ok to save on top of existing, unless it's read-only
        if (candidate.equalsIgnoreCase(existingConfig.getName())) {
            return existingConfig.isReadOnly()
                    ? new ConfigNameCheckResult(context.getString(R.string.configNameReadOnly))
                    : new ConfigNameCheckResult(true);
        }

        // Otherwise can't be the same as any of the existing configurations
        for (RobotConfigFile configFile : extantConfigurations) {
            if (candidate.equalsIgnoreCase(configFile.getName())) {
                return new ConfigNameCheckResult(context.getString(R.string.configNameExists));
            }
        }

        return new ConfigNameCheckResult(true);
    }

    public static String stripFileNameExtension(String fileName)
    {
        fileName = fileName.replaceFirst("[.][^.]+$", "");
        return fileName;
    }

    public static File stripFileNameExtension(File path)
    {
        File folder = path.getParentFile();
        String file = path.getName();
        file = stripFileNameExtension(file);
        return new File(folder, file);
    }

    public static String withExtension(String fileName)
    {
        return stripFileNameExtension(fileName) + FILE_EXT;
    }

    public static File getFullPath(String fileNameWithoutExtension)
    {
        fileNameWithoutExtension = withExtension(fileNameWithoutExtension);
        return new File(AppUtil.CONFIG_FILES_DIR, fileNameWithoutExtension);
    }

    /**
     * Gets the list of files from the Configuration File directory, and populates the global list
     * used by the fileSpinner. Note that this should only ever be executed on the robot controller,
     * not the driver station, as the robot controller is the authoritative source of configurations.
     */
    public ArrayList<RobotConfigFile> getXMLFiles()
    {
        File robotDir = AppUtil.CONFIG_FILES_DIR;
        File[] configFiles = robotDir.listFiles();

        ArrayList<RobotConfigFile> fileList = new ArrayList<RobotConfigFile>();

        for (File f: configFiles) {
            if (f.isFile()) {
                String name = f.getName();
                Pattern pattern = Pattern.compile("(?i).xml");
                if (pattern.matcher(name).find()) {
                    String nameNoExt = stripFileNameExtension(name);
                    fileList.add(new RobotConfigFile(this, nameNoExt));
                }
            }
        }

        /*
         * Pull the cached list of filtered resources
         */
        for (@XmlRes int id : getXmlResourceIds()) {

            /**
             * With file-based configurations, the name of the configuration always
             * matches the name of the file in which the XML is stored. And by default,
             * the same is similarly true of resource-based configurations. Unfortunately,
             * however, resource names have significant syntax restrictions on them. To
             * work around this limitation, the name of a resource-based configuration
             * may optionally be taken from the "name" attribute of the root element.
             */
            XmlResourceParser xpp = resources.getXml(id);
            String name = RobotConfigResFilter.getRootAttribute(xpp, RobotConfigResFilter.robotConfigRootTag, "name", resources.getResourceEntryName(id));

            // Avoid duplicate names
            RobotConfigFile configFile = new RobotConfigFile(name, id);
            if (!configFile.containedIn(fileList)) {
                fileList.add(configFile);
            }
        }

        return fileList;
    }

    public ArrayList<RobotConfigFile> getXMLTemplates()
    {
        ArrayList<RobotConfigFile> templateList = new ArrayList<RobotConfigFile>();
        for (@XmlRes int id : getXmlResourceTemplateIds()) {
            /**  @see #getXMLFiles() */
            XmlResourceParser xpp = resources.getXml(id);
            String name = RobotConfigResFilter.getRootAttribute(xpp, RobotConfigResFilter.robotConfigRootTag, "name", resources.getResourceEntryName(id));

            // Avoid duplicate names
            RobotConfigFile configFile = new RobotConfigFile(name, id);
            if (!configFile.containedIn(templateList)) {
                templateList.add(configFile);
            }
        }
        return templateList;
    }

    /**
     * Returns a high-level human-readable description of an XML robot configuration
     * or configuration template.
     */
    @NonNull public String getRobotConfigDescription(@NonNull XmlPullParser xpp)
    {
        try {
            /**
             * It's surprisingly difficult to get at the simple underlying file stream of an XML file
             * stored in an Android resource. I haven't in fact found any way to do it, none at all.
             * And that's what we need here to pass to the XLST transformer. Either that or a
             * {@link Source} that would take an {@link XmlPullParser} as input, which we can't find
             * either. So, we do the only thing we've figured out that we CAN do: use the pull parser
             * to build an XML DOM tree, then use that as the input source.
             *
             * It turns out that the root problem is that XML resources seem to be stored as *binary*
             * XML. So there *isn't* a string form to open. The approach here thus seems more reasonable.
             * Indeed, it might be the only feasible approach.
             */
            Source xmlTemplate = getSourceFromPullParser(xpp);

            Source xslt = getRobotConfigDescriptionTransform();
            StringWriter output = new StringWriter();
            Result result = new StreamResult(output);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xslt);
            transformer.transform(xmlTemplate, result);

            return output.toString().trim();  // trim to eliminate any trailing newline at end

        } catch (IOException|TransformerException|XmlPullParserException e) {
            RobotLog.logStackTrace(e);
        }
        return context.getString(R.string.templateConfigureNoDescriptionAvailable);
    }

    /**
     * Returns a {@link Source} to the XSLT transformation that will transform robot config
     * templates/configurations into human-readable descriptions.
     */
    @NonNull protected Source getRobotConfigDescriptionTransform() throws XmlPullParserException, IOException, TransformerConfigurationException,TransformerException
    {
        // Load RobotConfigTaxonomy as a DOM
        Reader xmlConfigTaxonomyReader = new InputStreamReader(context.getAssets().open(ROBOT_CONFIG_TAXONOMY_XML));
        XmlPullParser xmlConfigTaxonomyParser = ReadXMLFileHandler.xmlPullParserFromReader(xmlConfigTaxonomyReader);
        Dom2XmlPullBuilder builder = new Dom2XmlPullBuilder();
        Element rootElement = builder.parseSubTree(xmlConfigTaxonomyParser);
        Document document = rootElement.getOwnerDocument();

        // TODO: Adapt this code for the new way of getting annotated types (ConfigTypeManger#getApplicableTypes).
        // See issue #1249

        /*
         for (UserConfigurationType userConfigurationType : UserConfigurationTypeManager.getInstance().allUserTypes(UserConfigurationType.Flavor.I2C)) {
            //
            UserI2cSensorType userI2cSensorType = (UserI2cSensorType) userConfigurationType;
            Element sensor = document.createElement("Sensor");
            addChild(document, sensor, "XmlTag", userI2cSensorType.getXmlTag());
            addChild(document, sensor, "Description", userI2cSensorType.getDescription());
            addChild(document, sensor, "Bus", "i2cbus");
            addChild(document, sensor, "BusDefault", context.getString(R.string.userSensorTypeBusDefault));
            //
            rootElement.appendChild(sensor);
        }*/

        // Turn that augmented taxonomy into a source
        Source sourceConfigTaxonomy = new DOMSource(rootElement);

        // Load the transform that will generate the transform we need
        Source xsltGenerate = new StreamSource(context.getAssets().open(ROBOT_CONFIG_DESCRIPTION_GENERATE_XSLT));

        // Transform the taxonomy to generate the description transformer
        StringWriter xsltDescriptionWriter = new StringWriter();
        Result transformerResult = new StreamResult(xsltDescriptionWriter);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(xsltGenerate);
        transformer.transform(sourceConfigTaxonomy, transformerResult);
        String xsltDescriptionTransform = xsltDescriptionWriter.toString().trim();

        // Return that as a Source
        StringReader xsltDescriptionTransformReader = new StringReader(xsltDescriptionTransform);
        Source result = new StreamSource(xsltDescriptionTransformReader);
        return result;
    }

    protected void addChild(Document document, Element parent, String tag, String contents)
    {
        Element child = document.createElement(tag);
        child.setTextContent(contents);
        parent.appendChild(child);
    }

    protected Source getSourceFromPullParser(@NonNull XmlPullParser xpp) throws XmlPullParserException, IOException
    {
        Dom2XmlPullBuilder builder = new Dom2XmlPullBuilder();
        Element rootElement = builder.parseSubTree(xpp);
        return new DOMSource(rootElement);
    }

    public static String serializeXMLConfigList(List<RobotConfigFile> configList)
    {
        String objsSerialized = SimpleGson.getInstance().toJson(configList);
        return objsSerialized;
    }

    public static String serializeConfig(RobotConfigFile configFile)
    {
        String serialized = SimpleGson.getInstance().toJson(configFile);
        return serialized;
    }

    public static List<RobotConfigFile> deserializeXMLConfigList(String objsSerialized)
    {
        Type collectionType = new TypeToken<Collection<RobotConfigFile>>(){}.getType();
        List<RobotConfigFile> configList = SimpleGson.getInstance().fromJson(objsSerialized, collectionType);
        return configList;
    }

    public static RobotConfigFile deserializeConfig(String serialized)
    {
        Type type = RobotConfigFile.class;
        RobotConfigFile config = SimpleGson.getInstance().fromJson(serialized, type);
        return config;
    }

    /**
     * Calls the writer to write the current list of devices out to a XML string.
     * Checks for duplicate names, and will throw an error if any are found. In that case,
     * a helpful complainToast pops up with the duplicate names, and the file is not saved or written.
     *
     * @return the XML string. This will be non-null if toXml happened successfully, null otherwise
     */
    public String toXml(Map<SerialNumber, ControllerConfiguration> deviceControllers) {
        ArrayList<ControllerConfiguration> deviceList = new ArrayList<ControllerConfiguration>();
        deviceList.addAll(deviceControllers.values());
        String output = null;
        try {
            output = writer.toXml(deviceList, RobotConfigResFilter.robotConfigRootTypeAttribute, RobotConfigFileManager.getRobotConfigTypeAttribute());
        } catch (DuplicateNameException e) {
            appUtil.showToast(UILocation.BOTH, String.format(context.getString(R.string.toastDuplicateName), e.getMessage()));
            RobotLog.ee(TAG, "Found " + e.getMessage());
            output = null;
        } catch (RuntimeException e) {
            RobotLog.ee(TAG, e, "exception while writing XML");
            output = null;
        }
        return output;
    }

    public String toXml(RobotConfigMap robotConfigMap) {
        return toXml(robotConfigMap.map);
    }


    void writeXMLToFile(String filenameWithExt, String data) throws RobotCoreException, IOException
    {
        writer.writeToFile(data, AppUtil.CONFIG_FILES_DIR, filenameWithExt);
    }

    void writeToRobotController(RobotConfigFile cfgFile, String data)
    {
        networkConnectionHandler.sendCommand(new Command(CommandList.CMD_SAVE_CONFIGURATION, cfgFile.toString() + FILE_LIST_COMMAND_DELIMITER + data));
    }

    public void writeToFile(RobotConfigFile cfgFile, boolean runningOnDriverStation, @NonNull String data) throws RobotCoreException, IOException
    {
        // If we successfully save, then cfgFile is clean clean. And only then is cfgFile clean.
        boolean wasDirty = cfgFile.isDirty();
        cfgFile.markClean();
        try {
            if (runningOnDriverStation) {
                this.writeToRobotController(cfgFile, data);
            }
            else {
                this.writeXMLToFile(RobotConfigFileManager.withExtension(cfgFile.getName()), data);
            }
        } catch (RobotCoreException|IOException|RuntimeException e) {
            if (wasDirty) cfgFile.markDirty();
            throw e;
        }
    }

}
