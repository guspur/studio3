package com.aptana.editor.js.parsing.ast;

import com.aptana.parsing.ast.IParseNode;

public class JSFinallyNode extends JSNode
{
	/**
	 * JSFinallyNode
	 * 
	 * @param start
	 * @param end
	 * @param children
	 */
	public JSFinallyNode(int start, int end, JSNode... children)
	{
		super(JSNodeTypes.FINALLY, start, end, children);
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.js.parsing.ast.JSNode#toString()
	 */
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		IParseNode[] children = getChildren();

		buffer.append("finally "); //$NON-NLS-1$
		buffer.append(children[0]);

		this.appendSemicolon(buffer);

		return buffer.toString();
	}
}