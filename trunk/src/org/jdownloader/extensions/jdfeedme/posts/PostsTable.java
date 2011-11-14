package org.jdownloader.extensions.jdfeedme.posts;

import java.util.ArrayList;

import jd.gui.swing.components.table.JDTable;

import org.jdownloader.extensions.jdfeedme.JDFeedMeFeed;

public class PostsTable extends JDTable 
{
	
	private static final long serialVersionUID = 4467333885884748758L;

    public PostsTable(ArrayList<JDFeedMePost> posts, JDFeedMeFeed feed) 
    {
        super(new PostsTableModel("jdfeedmeposts", posts,feed));
    }

    @Override
    public PostsTableModel getModel() 
    {
        return (PostsTableModel) super.getModel();
    }

}
