package com.qualcomm.ftccommon.configuration;

/*
 * FTC Team 25: cmacfarl
 * Adapted therefrom by Robert Atkinson
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.opmode.ClassFilter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RobotConfigResFilter} is a {@link ClassFilter} that filters for XML
 * resources which are robot configurations which have a type attribute of the indicated value.
 * Results of the filter are added to the indicated collection passed to the constructor.
 */
@SuppressWarnings("WeakerAccess")
public class RobotConfigResFilter implements ClassFilter {

    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String robotConfigRootTag = "Robot";
    public static final String robotConfigRootTypeAttribute = "type";

    protected Resources resources;
    protected String typeAttributeValue;            // the value sought for the 'type' attribute on the root Robot element
    protected ArrayList<Integer> xmlIdCollection;   // the output of the filter

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public RobotConfigResFilter(String typeAttributeValue)
    {
        this(AppUtil.getInstance().getApplication(), typeAttributeValue);
    }

    public RobotConfigResFilter(Context context, String typeAttributeValue)
    {
        this.typeAttributeValue = typeAttributeValue;
        this.resources = context.getResources();
        this.xmlIdCollection = new ArrayList<Integer>();
        clear();
    }

    protected void clear()
    {
        this.xmlIdCollection.clear();
    }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public List<Integer> getXmlIds()
    {
        return this.xmlIdCollection;
    }

    /*
     * A simple method of checking to see if this xml file is a robot configuration file.
     *
     * Expects (e.g.) <Robot type="FirstInspires-FTC"> as the root XML element
     */
    private boolean isRobotConfiguration(XmlResourceParser xpp)
    {
        return typeAttributeValue.equals(getRootAttribute(xpp, robotConfigRootTag, robotConfigRootTypeAttribute, null));
    }

    /**
     * If the root element of the XML is the indicated tag, then returns the value of the
     * attributeName'd attribute thereof, or defaultValue if the attribute does not exist. If
     * the root element is not of the indicated tag, then null is returned.
     */
    public static String getRootAttribute(XmlResourceParser xpp, String rootElement, String attributeName, String defaultValue)
    {
        try {
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    if (!xpp.getName().equals(rootElement)) {
                        return null;
                    }
                    // Attributes in XML are by definition unordered within their element;
                    // getAttributeValue() finds the attribute we're looking for (if it's there)
                    // no matter what order the attributes were declared in.
                    String result = xpp.getAttributeValue(null, attributeName);
                    return result != null ? result : defaultValue;
                }
                xpp.next();
            }
        } catch (XmlPullParserException |IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override public void filterAllClassesStart()
    {
        clear();
    }

    @Override public void filterOnBotJavaClassesStart()
    {

    }

    /*
     * Look for the XML classes, and then when found filter for the Resource ids for configurations.
     */
    @Override
    public void filterClass(Class clazz) {
      if (clazz.getName().endsWith("R$xml")) {
      /*
       * Pull out all the R.xml classes and then filter all the xml files for robot configurations.
       * Create a list of robot configurations to be used elsewhere.
       */
            Field[] fields = clazz.getFields();
            for (Field f : fields) {
                try {
                    Class<?> c = f.getType();
                    if (c.equals(Integer.TYPE)) {
                        int id = f.getInt(clazz);
                        if (isRobotConfiguration(resources.getXml(id))) {
                            xmlIdCollection.add(id);
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override public void filterOnBotJavaClass(Class clazz)
    {
        filterClass(clazz);
    }

    @Override public void filterAllClassesComplete()
    {
        // Nothing to do
    }

    @Override public void filterOnBotJavaClassesComplete()
    {
        filterAllClassesComplete();
    }
}
