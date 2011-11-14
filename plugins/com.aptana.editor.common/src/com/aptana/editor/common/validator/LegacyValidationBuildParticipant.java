/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.common.validator;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.osgi.util.NLS;

import com.aptana.core.build.AbstractBuildParticipant;
import com.aptana.core.build.IValidationItem;
import com.aptana.core.build.ValidationItem;
import com.aptana.core.logging.IdeLog;
import com.aptana.core.util.StringUtil;
import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.preferences.IPreferenceConstants;
import com.aptana.index.core.build.BuildContext;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.ast.IParseError;

public class LegacyValidationBuildParticipant extends AbstractBuildParticipant implements IValidationManager
{

	private Document fDocument;
	private BuildContext fContext;

	public void clean(IProject project, IProgressMonitor monitor)
	{
		// no-op
	}

	private static List<ValidatorReference> getValidatorRefs(String contentType)
	{
		List<ValidatorReference> result = new ArrayList<ValidatorReference>();

		List<ValidatorReference> validatorRefs = ValidatorLoader.getInstance().getValidators(contentType);
		String list = CommonEditorPlugin.getDefault().getPreferenceStore()
				.getString(getSelectedValidatorsPrefKey(contentType));
		if (StringUtil.isEmpty(list))
		{
			// by default uses the first validator that supports the content type
			if (validatorRefs.size() > 0)
			{
				result.add(validatorRefs.get(0));
			}
		}
		else
		{
			String[] selectedValidators = list.split(","); //$NON-NLS-1$
			for (String name : selectedValidators)
			{
				for (ValidatorReference validator : validatorRefs)
				{
					if (validator.getName().equals(name))
					{
						result.add(validator);
						break;
					}
				}
			}
		}
		return result;
	}

	private static String getSelectedValidatorsPrefKey(String language)
	{
		return MessageFormat.format("{0}:{1}", language, IPreferenceConstants.SELECTED_VALIDATORS); //$NON-NLS-1$
	}

	private static String getFilterExpressionsPrefKey(String language)
	{
		return MessageFormat.format("{0}:{1}", language, IPreferenceConstants.FILTER_EXPRESSIONS); //$NON-NLS-1$
	}

	private static String getParseErrorEnabledPrefKey(String language)
	{
		return MessageFormat.format("{0}:{1}", language, IPreferenceConstants.PARSE_ERROR_ENABLED); //$NON-NLS-1$
	}

	public void buildFile(BuildContext context, IProgressMonitor monitor)
	{
		try
		{
			fContext = context;
			Map<String, List<IValidationItem>> allItems = new HashMap<String, List<IValidationItem>>();
			List<ValidatorReference> validatorRefs = getValidatorRefs(context.getContentType());
			if (!validatorRefs.isEmpty())
			{
				fDocument = new Document(context.getContents());

				for (ValidatorReference validatorRef : validatorRefs)
				{
					List<IValidationItem> newItems = validatorRef.getValidator().validate(context.getContents(),
							context.getURI(), this);
					String type = validatorRef.getMarkerType();
					List<IValidationItem> items = allItems.get(type);
					if (items == null)
					{
						items = Collections.synchronizedList(new ArrayList<IValidationItem>());
						allItems.put(type, items);
					}
					items.addAll(newItems);

					// FIXME We need to handle nested languages here....
					// for (String nestedLanguage : fNestedLanguages)
					// {
					// processNestedLanguage(nestedLanguage, allItems);
					// }
				}

				// Now stick the generated problems into the context
				for (Map.Entry<String, List<IValidationItem>> entry : allItems.entrySet())
				{
					context.putProblems(entry.getKey(), entry.getValue());
				}
			}
		}
		catch (CoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fDocument = null;
		fContext = null;
	}

	public void deleteFile(BuildContext context, IProgressMonitor monitor)
	{
		// TODO Auto-generated method stub

	}

	public IValidationItem createError(String message, int lineNumber, int lineOffset, int length, URI sourcePath)
	{
		return addItem(IMarker.SEVERITY_ERROR, message, lineNumber, lineOffset, length, sourcePath);
	}

	public IValidationItem createWarning(String message, int lineNumber, int lineOffset, int length, URI sourcePath)
	{
		return addItem(IMarker.SEVERITY_WARNING, message, lineNumber, lineOffset, length, sourcePath);
	}

	private IValidationItem addItem(int severity, String message, int lineNumber, int lineOffset, int length,
			URI sourcePath)
	{
		int charLineOffset = 0;
		if (fDocument != null)
		{
			try
			{
				charLineOffset = fDocument.getLineOffset(lineNumber - 1);
			}
			catch (BadLocationException e)
			{
			}
		}
		int offset = charLineOffset + lineOffset;
		return new ValidationItem(severity, message, offset, length, lineNumber, sourcePath.toString());
	}

	public void addNestedLanguage(String language)
	{
		// TODO Auto-generated method stub

	}

	public boolean isIgnored(String message, String language)
	{
		String list = CommonEditorPlugin.getDefault().getPreferenceStore()
				.getString(getFilterExpressionsPrefKey(language));
		if (!StringUtil.isEmpty(list))
		{
			String[] expressions = list.split("####"); //$NON-NLS-1$
			for (String expression : expressions)
			{
				if (message.matches(expression))
				{
					return true;
				}
			}
		}
		return false;
	}

	public IParseState getParseState()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void addParseErrors(List<IValidationItem> items, String language)
	{
		if (fDocument == null
				|| !CommonEditorPlugin.getDefault().getPreferenceStore()
						.getBoolean(getParseErrorEnabledPrefKey(language)))
		{
			return;
		}

		for (IParseError parseError : fContext.getParseErrors())
		{
			try
			{
				if (parseError.getSeverity() == IParseError.Severity.ERROR)
				{
					items.add(createError(parseError.getMessage(),
							fDocument.getLineOfOffset(parseError.getOffset()) + 1, parseError.getOffset(), 0,
							fContext.getURI()));
				}
				else
				{
					items.add(createWarning(parseError.getMessage(),
							fDocument.getLineOfOffset(parseError.getOffset()) + 1, parseError.getOffset(), 0,
							fContext.getURI()));
				}

			}
			catch (BadLocationException e)
			{
				IdeLog.logError(CommonEditorPlugin.getDefault(),
						NLS.bind("Error finding line on given offset : {0}", parseError.getOffset() + 1), e); //$NON-NLS-1$
			}
		}

	}

	public List<IValidationItem> getValidationItems()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
