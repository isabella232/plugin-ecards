package com.dotcms.osgi.ecards.viewtools;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

/**
 * Declare the ecards viewtool key
 * @author Oswaldo Gallango
 *
 */
public class ECardsToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "ecardstool";
    }

    @Override
    public String getScope () {
        return ViewContext.APPLICATION;
    }

    @Override
    public String getClassname () {
        return ECardsViewTool.class.getName();
    }

    @Override
    public Object getInstance ( Object initData ) {

        ECardsViewTool viewTool = new ECardsViewTool();
        viewTool.init( initData );

        setScope( ViewContext.APPLICATION );

        return viewTool;
    }

}
