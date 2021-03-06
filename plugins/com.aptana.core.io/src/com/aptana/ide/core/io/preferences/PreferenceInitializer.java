/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.ide.core.io.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.aptana.core.util.EclipseUtil;
import com.aptana.ide.core.io.CoreIOPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer
{

	public static final long DEFAULT_FILE_PERMISSIONS = 0666;
	public static final long DEFAULT_DIRECTORY_PERMISSIONS = 0777;
	public static final String DEFAULT_CLOAK_EXPRESSIONS = ".svn;.tmp*~;.settings;CVS;.git;.DS_Store"; //$NON-NLS-1$

	@Override
	public void initializeDefaultPreferences()
	{
		IEclipsePreferences prefs = EclipseUtil.defaultScope().getNode(CoreIOPlugin.PLUGIN_ID);
		prefs.putLong(IPreferenceConstants.FILE_PERMISSION, DEFAULT_FILE_PERMISSIONS);
		prefs.putLong(IPreferenceConstants.DIRECTORY_PERMISSION, DEFAULT_DIRECTORY_PERMISSIONS);
		prefs.put(IPreferenceConstants.GLOBAL_CLOAKING_EXTENSIONS, DEFAULT_CLOAK_EXPRESSIONS);
	}
}
