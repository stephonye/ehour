/**
 * Created on Jul 20, 2007
 * Created by Thies Edeling
 * Copyright (C) 2005, 2006 te-con, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * thies@te-con.nl
 * TE-CON
 * Legmeerstraat 4-2h, 1058ND, AMSTERDAM, The Netherlands
 *
 */

package net.rrm.ehour.ui.page.admin.user;

import java.util.ArrayList;
import java.util.List;

import net.rrm.ehour.ui.border.GreyRoundedBorder;
import net.rrm.ehour.ui.component.CustomAjaxTabbedPanel;
import net.rrm.ehour.ui.page.admin.BaseAdminPage;
import net.rrm.ehour.ui.panel.entryselector.EntrySelectorFilter;
import net.rrm.ehour.ui.panel.entryselector.EntrySelectorPanel;
import net.rrm.ehour.ui.panel.user.form.UserFormPanel;
import net.rrm.ehour.ui.panel.user.form.dto.UserBackingBean;
import net.rrm.ehour.ui.util.CommonStaticData;
import net.rrm.ehour.user.domain.User;
import net.rrm.ehour.user.domain.UserDepartment;
import net.rrm.ehour.user.domain.UserRole;
import net.rrm.ehour.user.service.UserService;
import net.rrm.ehour.util.EhourConstants;

import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Employee management page 
 **/

public class UserAdmin extends BaseAdminPage
{
	private final String	USER_SELECTOR_ID = "userSelector";
	
	@SpringBean
	private	UserService				userService;
	private	ListView				userListView;
	private	transient 	Logger		logger = Logger.getLogger(UserAdmin.class);
	private	AjaxTabbedPanel			tabbedPanel;
	private	UserBackingBean			addUser;
	private	UserBackingBean			editUser;
	private EntrySelectorFilter		currentFilter;
	private List<UserRole>			roles ;
	private List<UserDepartment>	departments;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1883278850247747252L;

	public UserAdmin()
	{
		super(new ResourceModel("admin.user.title"), null);
		
		List<User>	users;
		
		roles = userService.getUserRoles();
		departments = userService.getUserDepartments();
		
		addUser = getAddUserBackingBean();
		editUser = new UserBackingBean(new User());
		
		users = getUsers();
		Fragment userListHolder = getUserListHolder(users);
		
		add(new EntrySelectorPanel(USER_SELECTOR_ID,
				new ResourceModel("admin.user.title"),
				userListHolder,
				getLocalizer().getString("admin.user.filter", this) + "...",
				getLocalizer().getString("admin.user.hideInactive", this)));
		
		setUpTabs();
	}
	
	/**
	 * Setup tabs
	 */
	private void setUpTabs()
	{
		List<AbstractTab>	tabs = new ArrayList<AbstractTab>(2);
		
		tabbedPanel = new CustomAjaxTabbedPanel("tabs", tabs)
		{
			@Override
			protected void preProcessTabSwitch(int index)
			{
				// if "Add user" is clicked again, reset the backing bean as it's
				// only way out if for some reason the save went wrong and the page is stuck on
				// an error
				if (getSelectedTab() == index && index == 0)
				{
					addUser = getAddUserBackingBean();
				}
				
				// reset server messages
				addUser.setServerMessage(null);
				editUser.setServerMessage(null);
			}
		};

		addAddTab();
		addNoUserTab();
		
		add(tabbedPanel);
	}

	
	/**
	 * Get a the userListHolder fragment containing the listView
	 * @param users
	 * @return
	 */
	private Fragment getUserListHolder(List<User> users)
	{
		Fragment fragment = new Fragment("itemListHolder", "itemListHolder", UserAdmin.this);
		
		userListView = new ListView("itemList", users)
		{
			@Override
			protected void populateItem(ListItem item)
			{
				final User		user = (User)item.getModelObject();
				final Integer	userId = user.getUserId();
				
				AjaxLink	link = new AjaxLink("itemLink")
				{
					@Override
					public void onClick(AjaxRequestTarget target)
					{
						editUser = new UserBackingBean(userService.getUser(userId));
						switchTab(target, 1);
					}
				};
				
				item.add(link);
				link.add(new Label("linkLabel", user.getLastName() + ", " + user.getFirstName() + (user.isActive() ? "" : "*")));				
			}
		};
		
		fragment.add(userListView);
		
		return fragment;
	}
	
	/**
	 * Switch tab
	 * @param tab
	 * @param userId
	 */
	private void switchTab(AjaxRequestTarget target, int tabIndex)
	{
		if (tabIndex == 1)
		{
			addEditTab();
		}
		
		tabbedPanel.setSelectedTab(tabIndex);
		target.addComponent(tabbedPanel);
	}
	
	/**
	 * Handle Ajax request
	 * @param target
	 * @param type of ajax req
	 */
	@Override
	public void ajaxRequestReceived(AjaxRequestTarget target, int type, Object param)
	{
		switch (type)
		{
			case CommonStaticData.AJAX_ENTRYSELECTOR_FILTER_CHANGE:
			{
				currentFilter = (EntrySelectorFilter)param;
	
				List<User> users = getUsers();
				userListView.setList(users);
				break;
			}
			case CommonStaticData.AJAX_FORM_SUBMIT:
			{
				UserBackingBean	backingBean = (UserBackingBean) ((((IWrapModel) param)).getWrappedModel()).getObject();
				try
				{
					persistUser(backingBean);

					// update user list
					List<User> users = getUsers();
					userListView.setList(users);
					
					((EntrySelectorPanel)get(USER_SELECTOR_ID)).refreshList(target);
					
					addUser = getAddUserBackingBean();
					addUser.setServerMessage(getLocalizer().getString("admin.user.dataSaved", this));
					addAddTab();
					tabbedPanel.setSelectedTab(0);
					
					target.addComponent(tabbedPanel);
				} catch (Exception e)
				{
					logger.error("While persisting user", e);
					backingBean.setServerMessage(getLocalizer().getString("admin.user.saveError", this));
					target.addComponent(tabbedPanel);
				}
				
				break;
			}
		}
	}
	

	
	/**
	 * Add add tab at position 0
	 */
	private void addAddTab()
	{
		removeTab(0);
		
		AbstractTab addUserTab = new AbstractTab(new ResourceModel("admin.user.addUser"))
		{
			@Override
			public Panel getPanel(String panelId)
			{
				return new UserFormPanel(panelId,
						new CompoundPropertyModel(addUser),
						roles,
						departments);
			}
		};

		tabbedPanel.getTabs().add(0, addUserTab);	
	}	
	
	/**
	 * Add edit tab at position 1
	 */
	private void addEditTab()
	{
		removeTab(1);
		
		AbstractTab editUserTab = new AbstractTab(new ResourceModel("admin.user.editUser"))
		{
			@Override
			public Panel getPanel(String panelId)
			{
				return new UserFormPanel(panelId,
						new CompoundPropertyModel(editUser),
						roles,
						departments);
			}
		};

		tabbedPanel.getTabs().add(1, editUserTab);		
	}
	
	/**
	 * Add no user selected tab at position 1
	 */
	private void addNoUserTab()
	{
		removeTab(1);
		
		AbstractTab editUserTab = new AbstractTab(new ResourceModel("admin.user.editUser"))
		{
			@Override
			public Panel getPanel(String panelId)
			{
				return new NoUserSelected(panelId);
			}
		};

		tabbedPanel.getTabs().add(1, editUserTab);		
	}	
	
	/**
	 * 
	 * @param index
	 */
	private void removeTab(int index)
	{
		if (tabbedPanel.getTabs().size() >= index + 1)
		{
			tabbedPanel.getTabs().remove(index);;
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	private UserBackingBean getAddUserBackingBean()
	{
		UserBackingBean	userBean;
		
		userBean = new UserBackingBean(new User());
		userBean.getUser().setActive(true);

		return userBean;
	}
	
	/**
	 * Persist user
	 * @param userBackingBean
	 */
	private void persistUser(UserBackingBean userBackingBean) throws Exception
	{
		logger.info(((userBackingBean == editUser) ? "Updating" : "Adding") + " user :" + userBackingBean.getUser());
		
		if (userBackingBean.isPm())
		{
			logger.debug("Readding PM role after edit");
			userBackingBean.getUser().addUserRole(new UserRole(EhourConstants.ROLE_PROJECTMANAGER));
		}
		
		userService.persistUser(userBackingBean.getUser());
	}
	
	/**
	 * Get the users from the backend
	 * @param filter
	 * @param hideInactive
	 * @return
	 */
	private List<User> getUsers()
	{
		List<User>	users;
		
		if (currentFilter == null)
		{
			users = userService.getUsers();
		}
		else
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Filtering on " + currentFilter.getCleanFilterInput() + ", hide active: " + currentFilter.isActivateToggle());
			}
			
			users = userService.getUsersByNameMatch(currentFilter.getCleanFilterInput(), currentFilter.isActivateToggle());
		}
		
		return users;
	}
	
	/**
	 * 
	 * @author Thies
	 *
	 */
	private class NoUserSelected extends Panel
	{
		public NoUserSelected(String id)
		{
			super(id);
			
			GreyRoundedBorder greyBorder = new GreyRoundedBorder("border");
			add(greyBorder);

			greyBorder.add(new Label("noUser", new ResourceModel("admin.user.noUserSelected")));
		}
	}
}
