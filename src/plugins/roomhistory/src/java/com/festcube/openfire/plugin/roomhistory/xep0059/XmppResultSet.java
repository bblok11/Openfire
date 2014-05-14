package com.festcube.openfire.plugin.roomhistory.xep0059;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;

/**
 * A <a href="http://www.xmpp.org/extensions/xep-0059.html">XEP-0059</a> result set.
 */
public class XmppResultSet
{
    public static String NAMESPACE = "http://jabber.org/protocol/rsm";
    private String after;
    private String before;
    private Integer index;
    private Integer max;
    private String first;
    private Integer firstIndex;
    private String last;
    private Integer count;

    public XmppResultSet(Element setElement)
    {
        if (setElement.element("after") != null)
        {
            try
            {
                after = setElement.elementText("after");
            }
            catch (Exception e)
            {
                // swallow
            }
        }
        if (setElement.element("before") != null)
        {
            try
            {
                before = setElement.elementText("before");
            }
            catch (Exception e)
            {
                // swallow
            }
        }
        if (setElement.element("max") != null)
        {
            try
            {
                max = Integer.parseInt(setElement.elementText("max"));
                if (max < 0)
                {
                    max = null;
                }
            }
            catch (Exception e)
            {
                // swallow
            }
        }
        if (setElement.element("index") != null)
        {
            try
            {
                index = Integer.parseInt(setElement.elementText("index"));
                if (index < 0)
                {
                    index = null;
                }
            }
            catch (Exception e)
            {
                // swallow
            }
        }
    }

    public String getAfter()
    {
        return after;
    }

    public String getBefore()
    {
        return before;
    }

    /**
     * Returns the index of the first element to return.
     *
     * @return the index of the first element to return.
     */
    public Integer getIndex()
    {
        return index;
    }

    /**
     * Returns the maximum number of items to return.
     *
     * @return the maximum number of items to return.
     */
    public Integer getMax()
    {
        return max;
    }

    /**
     * Sets the id of the first element returned.
     *
     * @param first the id of the first element returned.
     */
    public void setFirst(String first)
    {
        this.first = first;
    }

    /**
     * Sets the index of the first element returned.
     *
     * @param firstIndex the index of the first element returned.
     */
    public void setFirstIndex(Integer firstIndex)
    {
        this.firstIndex = firstIndex;
    }

    /**
     * Sets the id of the last element returned.
     *
     * @param last the id of the last element returned.
     */
    public void setLast(String last)
    {
        this.last = last;
    }

    /**
     * Sets the number of elements returned.
     *
     * @param count the number of elements returned.
     */
    public void setCount(Integer count)
    {
        this.count = count;
    }

    public Element createResultElement()
    {
        final Element set;

        set = DocumentFactory.getInstance().createElement("set", NAMESPACE);
        if (first != null)
        {
            final Element firstElement;
            firstElement = set.addElement("first");
            firstElement.setText(first);
            if (firstIndex != null)
            {
                firstElement.addAttribute("index", firstIndex.toString());
            }
        }
        if (last != null)
        {
            set.addElement("last").setText(last);
        }
        if (count != null)
        {
            set.addElement("count").setText(count.toString());
        }

        return set;
    }
}
