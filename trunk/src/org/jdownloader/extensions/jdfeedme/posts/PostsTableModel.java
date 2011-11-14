package org.jdownloader.extensions.jdfeedme.posts;

import java.util.ArrayList;

import jd.gui.swing.components.table.JDTableModel;

import org.jdownloader.extensions.jdfeedme.JDFeedMeFeed;
import org.jdownloader.extensions.jdfeedme.posts.columns.DownloadColumn;
import org.jdownloader.extensions.jdfeedme.posts.columns.StatusColumn;
import org.jdownloader.extensions.jdfeedme.posts.columns.TimestampColumn;
import org.jdownloader.extensions.jdfeedme.posts.columns.TitleColumn;
import org.jdownloader.extensions.jdfeedme.posts.columns.VisitColumn;

public class PostsTableModel extends JDTableModel 
{

	private static final long serialVersionUID = -3377812370684343642L;

	private JDFeedMeFeed feed;
    private ArrayList<JDFeedMePost> posts;

    public PostsTableModel(String configname, ArrayList<JDFeedMePost> posts, JDFeedMeFeed feed) 
    {
        super(configname);

        this.posts = posts;
        this.feed = feed;
    }

    protected void initColumns() 
    {
    	this.addColumn(new VisitColumn("Visit", this));
    	this.addColumn(new DownloadColumn("Add", this));
    	this.addColumn(new TitleColumn("Title", this));
        this.addColumn(new TimestampColumn("Published", this));
        this.addColumn(new StatusColumn("Status", this));
    }

    @Override
    public void refreshModel() 
    {
        synchronized (list) {
            list.clear();
            list.addAll(posts);
        }
    }

    public ArrayList<JDFeedMePost> getPosts() 
    {
        return posts;
    }

    public void setPosts(ArrayList<JDFeedMePost> posts) 
    {
        this.posts = posts;
    }
    
    public JDFeedMeFeed getFeed() 
    {
    	return feed;
    }

}
