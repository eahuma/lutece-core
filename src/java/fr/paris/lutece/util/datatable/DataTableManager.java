package fr.paris.lutece.util.datatable;

import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.web.constants.Parameters;
import fr.paris.lutece.portal.web.util.LocalizedDelegatePaginator;
import fr.paris.lutece.portal.web.util.LocalizedPaginator;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.html.IPaginator;
import fr.paris.lutece.util.html.Paginator;
import fr.paris.lutece.util.sort.AttributeComparator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang.StringUtils;


/**
 * Class to manage data tables with freemarker macros
 */
public class DataTableManager<T>
{
	private static final String CONSTANT_GET = "get";
	private static final String CONSTANT_IS = "is";

	private String _strSortUrl;

	private List<DataTableColumn> _listColumn = new ArrayList<DataTableColumn>( );
	private FilterPanel _filterPanel;
	private IPaginator<T> _paginator;
	private String _strCurrentPageIndex = StringUtils.EMPTY;
	private int _nItemsPerPage = 0;
	private int _nDefautlItemsPerPage = 0;
	private boolean _bEnablePaginator;
	private Locale _locale;
	private String _strSortedAttributeName;
	private boolean _bIsAscSort;

	/**
	 * Private constructor
	 */
	protected DataTableManager( )
	{

	}

	/**
	 * Constructor of the DataTableManager class
	 * @param strSortUrl URL used by the paginator and to sort data
	 * @param strFilterUrl URL used to filter data
	 * @param nDefautlItemsPerPage Default number of items to display per page
	 * @param bEnablePaginator True to enable pagination, false to disable it
	 */
	public DataTableManager( String strSortUrl, String strFilterUrl, int nDefautlItemsPerPage, boolean bEnablePaginator )
	{
		_strSortUrl = strSortUrl;
		_filterPanel = new FilterPanel( strFilterUrl );
		_nDefautlItemsPerPage = nDefautlItemsPerPage;
		_bEnablePaginator = bEnablePaginator;
	}

	/**
	 * Add a column to this DataTableManager
	 * @param strColumnTitle I18n key of the title of the column
	 * @param strObjectName Name of the property of objects that should be displayed in this column.<br />
	 * For example, if a class "Data" contains a property named "title", then the value of the parameter <i>strObjectName</i> should be "title".
	 * @param bSortable True if the column is sortable, false otherwise
	 */
	public void addColumn( String strColumnTitle, String strObjectName, boolean bSortable )
	{
		_listColumn.add( new DataTableColumn( strColumnTitle, strObjectName, bSortable, DataTableColumnType.STRING ) );
	}

	/**
	 * Add a label column to this DataTableManager. Values of cells of this column will be interpreted as i18n keys.
	 * @param strColumnTitle I18n key of the title of the column
	 * @param strObjectName Name of the property of objects that should be displayed in this column. This properties must be i18n keys.<br />
	 * For example, if a class "Data" contains a property named "title", then the value of the parameter <i>strObjectName</i> should be "title".
	 * @param bSortable True if the column is sortable, false otherwise
	 */
	public void addLabelColumn( String strColumnTitle, String strObjectName, boolean bSortable )
	{
		_listColumn.add( new DataTableColumn( strColumnTitle, strObjectName, bSortable, DataTableColumnType.LABEL ) );
	}

	/**
	 * Add an column to this DataTableManager that will display actions on items. Actions are usually parameterized links. A DataTableManager can only have 1 action column. The content of the action
	 * column must be generated by a macro.this macro must have one parameter named "item", and its name must be given to the macro <i>@tableData</i>.
	 * @param strColumnTitle I18n key of the title of the column
	 */
	public void addActionColumn( String strColumnTitle )
	{
		_listColumn.add( new DataTableColumn( strColumnTitle, null, false, DataTableColumnType.ACTION ) );
	}

	/**
	 * Add a column to this DataTableManager
	 * @param strColumnTitle I18n key of the title of the column
	 * @param strObjectName Name of the property of objects that should be displayed in this column.<br />
	 * For example, if a class "Data" contains a property named "title", then the value of the parameter <i>strObjectName</i> should be "title".
	 * @param strLabelTrue I18n key of the label to display when the value is true
	 * @param strLabelFalse I18n key of the label to display when the value is false
	 */
	public void addBooleanColumn( String strColumnTitle, String strObjectName, String strLabelTrue, String strLabelFalse )
	{
		_listColumn.add( new DataTableColumn( strColumnTitle, strObjectName, false, DataTableColumnType.BOOLEAN, strLabelTrue, strLabelFalse ) );
	}

	/**
	 * Add a free column to this DataTableManager. The content of this column must be generated by a macro. The macro must have one parameter named "item".
	 * @param strColumnTitle I18n key of the title of the column
	 * @param strFreemarkerMacroName Name of the freemarker macro that will display the content of the column.<br />
	 * The macro must have a single parameter named <i>item</i> of type T that will contain the object associated with a row of the table.
	 */
	public void addFreeColumn( String strColumnTitle, String strFreemarkerMacroName )
	{
		_listColumn.add( new DataTableColumn( strColumnTitle, strFreemarkerMacroName, false, DataTableColumnType.ACTION ) );
	}

	/**
	 * Add an email column to this DataTableManager. Displayed cell will be a "mailto:" link.
	 * @param strColumnTitle I18n key of the title of the column
	 * @param strObjectName Name of the property of objects that should be displayed in this column.<br />
	 * For example, if a class "Data" contains a property named "title", then the value of the parameter <i>strObjectName</i> should be "title".
	 * @param bSortable True if the column is sortable, false otherwise
	 */
	public void addEmailColumn( String strColumnTitle, String strObjectName, boolean bSortable )
	{
		_listColumn.add( new DataTableColumn( strColumnTitle, strObjectName, bSortable, DataTableColumnType.EMAIL ) );
	}

	/**
	 * Add a filter to the filter panel of this DataTableManager
	 * @param filterType data type of the filter. For drop down list, use {@link DataTableManager#addDropDownListFilter(String, String, ReferenceList) addDropDownListFilter} instead
	 * @param strParameterName Name of the parameter of the object to filter.<br/>
	 * For example, if this filter should be applied on the parameter "title" of a class named "Data", then the value of the parameter <i>strParameterName</i> should be "title".
	 * @param strFilterLabel Label describing the filter
	 */
	public void addFilter( DataTableFilterType filterType, String strParameterName, String strFilterLabel )
	{
		_filterPanel.addFilter( filterType, strParameterName, strFilterLabel );
	}

	/**
	 * Add a drop down list filter to the filter panel of this DataTableManager
	 * @param strParameterName Name of the parameter of the object to filter.<br/>
	 * For example, if this filter should be applied on the parameter "title" of a class named "Data", then the value of the parameter <i>strParameterName</i> should be "title".
	 * @param strFilterLabel Label describing the filter
	 * @param refList Reference list containing data of the drop down list
	 */
	public void addDropDownListFilter( String strParameterName, String strFilterLabel, ReferenceList refList )
	{
		_filterPanel.addDropDownListFilter( strParameterName, strFilterLabel, refList );
	}

	/**
	 * Apply filters on an objects list, sort it and update pagination values.
	 * @param request The request
	 * @param items Collection of objects to filter, sort and paginate
	 */
	public void filterSortAndPaginate( HttpServletRequest request, List<T> items )
	{
		List<T> filteredSortedPaginatedItems = new ArrayList<T>( items );

		// FILTER
		Collection<DataTableFilter> listFilters = _filterPanel.getListFilter( );
		boolean bResetFilter = StringUtils.equals( request.getParameter( FilterPanel.PARAM_FILTER_PANEL_PREFIX + FilterPanel.PARAM_RESET_FILTERS ), Boolean.TRUE.toString( ) );
		boolean bUpdateFilter = true;
		if ( !bResetFilter )
		{
			bUpdateFilter = StringUtils.equals( request.getParameter( FilterPanel.PARAM_FILTER_PANEL_PREFIX + FilterPanel.PARAM_UPDATE_FILTERS ), Boolean.TRUE.toString( ) );
		}
		for ( DataTableFilter filter : listFilters )
		{
			String strFilterValue;
			if ( bUpdateFilter )
			{
				strFilterValue = request.getParameter( FilterPanel.PARAM_FILTER_PANEL_PREFIX + filter.getParameterName( ) );
				if ( !bResetFilter && filter.getFilterType( ) == DataTableFilterType.BOOLEAN && strFilterValue == null )
				{
					strFilterValue = Boolean.FALSE.toString( );
				}
				filter.setValue( strFilterValue );
			}
			else
			{
				strFilterValue = filter.getValue( );
			}
			if ( StringUtils.isNotBlank( strFilterValue ) )
			{
				List<T> bufferList = new ArrayList<T>( );
				for ( T item : filteredSortedPaginatedItems )
				{
					Method method = getMethod( item, filter.getParameterName( ), CONSTANT_GET );
					if ( method == null && filter.getFilterType( ) == DataTableFilterType.BOOLEAN )
					{
						method = getMethod( item, filter.getParameterName( ), CONSTANT_IS );
					}
					if ( method != null )
					{
						try
						{
							Object value = method.invoke( item );
							if ( value != null && strFilterValue.equals( value.toString( ) ) )
							{
								bufferList.add( item );
							}
						}
						catch ( Exception e )
						{
							AppLogService.error( e.getMessage( ), e );
						}
					}
				}
				filteredSortedPaginatedItems.retainAll( bufferList );
			}
		}

		// SORT
		String strSortedAttributeName = request.getParameter( Parameters.SORTED_ATTRIBUTE_NAME );
		if ( strSortedAttributeName != null )
		{
			// We update sort properties
			_strSortedAttributeName = strSortedAttributeName;
			_bIsAscSort = Boolean.parseBoolean( request.getParameter( Parameters.SORTED_ASC ) );
		}
		if ( _strSortedAttributeName != null )
		{
			Collections.sort( filteredSortedPaginatedItems, new AttributeComparator( _strSortedAttributeName, _bIsAscSort ) );
		}

		// PAGINATION
		if ( _bEnablePaginator )
		{
			_strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );
			_nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage, _nDefautlItemsPerPage );
			_paginator = new LocalizedPaginator<T>( filteredSortedPaginatedItems, _nItemsPerPage, getSortUrl( ), Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex, request.getLocale( ) );
		}
	}

	/**
	 * Get the filter panel of the DataTableManager
	 * @return The filter panel of the DataTableManager
	 */
	public FilterPanel getFilterPanel( )
	{
		return _filterPanel;
	}

	/**
	 * Set the filter panel of the DataTableManager
	 * @param filterPanel Filter panel
	 */
	public void setFilterPanel( FilterPanel filterPanel )
	{
		_filterPanel = filterPanel;
	}

	/**
	 * Get the list of columns of this DataTableManager
	 * @return The list of columns of this DataTableManager
	 */
	public List<DataTableColumn> getListColumn( )
	{
		return _listColumn;
	}

	/**
	 * Set the list of columns of this DataTableManager
	 * @param listColumn The list of columns of this DataTableManager
	 */
	public void setListColumn( List<DataTableColumn> listColumn )
	{
		_listColumn = listColumn;
	}

	/**
	 * Get the sort url of this DataTableManager
	 * @return The sort url of this DataTableManager
	 */
	public String getSortUrl( )
	{
		return _strSortUrl;
	}

	/**
	 * Set the sort url of this DataTableManager
	 * @param strSortUrl The sort url of this DataTableManager
	 */
	public void setSortUrl( String strSortUrl )
	{
		_strSortUrl = strSortUrl;
	}

	/**
	 * Get the filtered, sorted and paginated items collection of this DataTableManager
	 * @return The filtered, sorted and paginated items collection of this DataTableManager
	 */
	public List<T> getItems( )
	{
		return _paginator.getPageItems( );
	}

	/**
	 * Set the items to display. The list of items must be fintered, sorted and paginated. Methods {@link DataTableManager#getAndUpdatePaginator(HttpServletRequest ) getAndUpdatePaginator},
	 * {@link DataTableManager#getAndUpdateSort(HttpServletRequest ) getAndUpdateSort} and {@link DataTableManager#getAndUpdateFilter(HttpServletRequest, Object) getAndUpdateFilter} must have been
	 * called before the generation of the list of items.
	 * @param items The filtered sorted and paginated list of items to display
	 * @param nTotalItemsNumber The total number of items
	 */
	public void setItems( List<T> items, int nTotalItemsNumber )
	{
		_paginator = new LocalizedDelegatePaginator<T>( items, _nItemsPerPage, getSortUrl( ), Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex, nTotalItemsNumber, _locale );
	}

	/**
	 * Clear the items stored by this DataTableManager so that the garbage collector can free the memory they use.
	 */
	public void clearItems( )
	{
		_paginator = null;
		_locale = null;
	}

	/**
	 * Internal method. Get the paginator.<br/>
	 * Do not use this method, use {@link DataTableManager#getAndUpdatePaginator(HttpServletRequest ) getAndUpdatePaginator} instead to get up to date values !
	 * @return The paginator
	 */
	public IPaginator<T> getPaginator( )
	{
		return _paginator;
	}

	/**
	 * Get the enable paginator boolean
	 * @return True if pagination is active, false otherwise
	 */
	public boolean getEnablePaginator( )
	{
		return _bEnablePaginator;
	}

	/**
	 * Get the locale
	 * @return The locale
	 */
	public Locale getLocale( )
	{
		return _locale;
	}

	/**
	 * Set the locale
	 * @param locale The locale
	 */
	public void setLocale( Locale locale )
	{
		_locale = locale;
	}

	/**
	 * Get the paginator updated with values in the request
	 * @param request The request
	 * @return The paginator up to date
	 */
	public DataTablePaginationProperties getAndUpdatePaginator( HttpServletRequest request )
	{
		DataTablePaginationProperties paginationProperties = null;
		if ( _bEnablePaginator )
		{
			paginationProperties = new DataTablePaginationProperties( );
			_strCurrentPageIndex = Paginator.getPageIndex( request, Paginator.PARAMETER_PAGE_INDEX, _strCurrentPageIndex );
			_nItemsPerPage = Paginator.getItemsPerPage( request, Paginator.PARAMETER_ITEMS_PER_PAGE, _nItemsPerPage, _nDefautlItemsPerPage );
			paginationProperties.setItemsPerPage( _nItemsPerPage );
			int nCurrentPageIndex = 1;
			if ( !StringUtils.isEmpty( _strCurrentPageIndex ) )
			{
				nCurrentPageIndex = Integer.parseInt( _strCurrentPageIndex );
			}
			paginationProperties.setCurrentPageIndex( nCurrentPageIndex );
		}
		_locale = request.getLocale( );
		return paginationProperties;
	}

	/**
	 * Get sort properties updated with values in the request
	 * @param request The request
	 * @return The sort properties up to date
	 */
	public DataTableSort getAndUpdateSort( HttpServletRequest request )
	{
		String strSortedAttributeName = request.getParameter( Parameters.SORTED_ATTRIBUTE_NAME );
		if ( strSortedAttributeName != null )
		{
			// We update sort properties
			_strSortedAttributeName = strSortedAttributeName;
			_bIsAscSort = Boolean.parseBoolean( request.getParameter( Parameters.SORTED_ASC ) );
		}
		DataTableSort sort = new DataTableSort( _strSortedAttributeName, _bIsAscSort );
		return sort;
	}

	/**
	 * Get filter properties updated with values in the request
	 * @param request The request
	 * @return The filter properties up to date
	 */
	public <K> K getAndUpdateFilter( HttpServletRequest request, K filterObject )
	{
		List<DataTableFilter> listFilters = _filterPanel.getListFilter( );

		boolean bResetFilter = StringUtils.equals( request.getParameter( FilterPanel.PARAM_FILTER_PANEL_PREFIX + FilterPanel.PARAM_RESET_FILTERS ), Boolean.TRUE.toString( ) );
		boolean bUpdateFilter = true;
		if ( !bResetFilter )
		{
			bUpdateFilter = StringUtils.equals( request.getParameter( FilterPanel.PARAM_FILTER_PANEL_PREFIX + FilterPanel.PARAM_UPDATE_FILTERS ), Boolean.TRUE.toString( ) );
		}
		Map<String, Object> mapFilter = new HashMap<String, Object>( );
		for ( DataTableFilter filter : listFilters )
		{
			String strFilterValue = request.getParameter( FilterPanel.PARAM_FILTER_PANEL_PREFIX + filter.getParameterName( ) );
			if ( bUpdateFilter )
			{

			}
			filter.setValue( strFilterValue );
			if ( StringUtils.isNotBlank( strFilterValue ) )
			{
				mapFilter.put( filter.getParameterName( ), strFilterValue );
			}
		}
		try
		{
			BeanUtilsBean.getInstance( ).populate( filterObject, mapFilter );
		}
		catch ( IllegalAccessException e )
		{
			AppLogService.error( e.getMessage( ), e );
			return null;
		}
		catch ( InvocationTargetException e )
		{
			AppLogService.error( e.getMessage( ), e );
			return null;
		}
		return filterObject;
	}

	/**
	 * Internal method. Get the prefix of html attributes used by filters
	 * @return The prefix of html attributes used by filters
	 */
	public String getFilterPanelPrefix( )
	{
		return FilterPanel.PARAM_FILTER_PANEL_PREFIX;
	}

	/**
	 * Return the getter method of the object obj for the attribute <i>strAttributName</i>
	 * @param obj the object
	 * @param strAttributName The name of the attribute to get the getter
	 * @param strMethodPrefix Prefix of the name of the method
	 * @return method Method of the object obj for the attribute <i>strAttributName</i>
	 */
	private Method getMethod( Object obj, String strAttributName, String strMethodPrefix )
	{
		Method method = null;
		String strFirstLetter = strAttributName.substring( 0, 1 ).toUpperCase( );

		String strMethodName = strMethodPrefix + strFirstLetter + strAttributName.substring( 1, strAttributName.length( ) );

		try
		{
			method = obj.getClass( ).getMethod( strMethodName );
		}
		catch ( Exception e )
		{
			AppLogService.debug( e );
		}

		return method;
	}
}