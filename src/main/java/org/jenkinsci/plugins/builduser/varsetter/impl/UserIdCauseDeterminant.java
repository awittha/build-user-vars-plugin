package org.jenkinsci.plugins.builduser.varsetter.impl;

import hudson.model.Cause.UserIdCause;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.builduser.BuildUser;
import org.jenkinsci.plugins.builduser.utils.UsernameUtils;
import org.jenkinsci.plugins.builduser.varsetter.IUsernameSettable;

import hudson.security.ACL;
import hudson.tasks.Mailer;
import hudson.model.User;
import hudson.model.UserProperty;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.acegisecurity.GrantedAuthority;

/**
 * This implementation is used to determine build username variables from <b>{@link UserIdCause}</b>.
 * This will let to get whole set of variables:
 * <ul>
 *   <li>{@link IUsernameSettable#BUILD_USER_ID}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_VAR_NAME}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_VAR_GROUPS}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_FIRST_NAME_VAR_NAME}</li>
 *   <li>{@link IUsernameSettable#BUILD_USER_LAST_NAME_VAR_NAME}</li>
 * </ul>
 * 
 * @author GKonovalenko
 */
public class UserIdCauseDeterminant implements IUsernameSettable<UserIdCause> {
	
	final Class<UserIdCause> causeClass = UserIdCause.class;
	private static final Logger log = Logger.getLogger(BuildUser.class.getName());


	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>{@link UserIdCause}</b> based implementation.
	 */
	public boolean setJenkinsUserBuildVars(UserIdCause cause,
			Map<String, String> variables) {
		if(null != cause) {
			String username = cause.getUserName();
			UsernameUtils.setUsernameVars(username, variables);

			String trimmedUserId = StringUtils.trimToEmpty(cause.getUserId());
			String userid = trimmedUserId.isEmpty() ? ACL.ANONYMOUS_USERNAME : trimmedUserId;
			variables.put(BUILD_USER_ID, userid);
			String groupString = "";
			try {
				GrantedAuthority[] authorities = Jenkins.getInstance().getSecurityRealm().loadUserByUsername(userid).getAuthorities();
				String[] groups = new String[authorities.length];
				for(int i = 0; i < authorities.length; i++)
					groups[i] = authorities[i].getAuthority();
				groupString = String.join(",", groups);
			} catch (Exception err) {
				// Error
				log.warning(String.format("Failed to get groups for user: %s error: %s ", userid, err.toString()));
			}
			variables.put(BUILD_USER_VAR_GROUPS, groupString);


			User user=User.get(userid);
            		if(null != user) {
            		    UserProperty prop = user.getProperty(Mailer.UserProperty.class);
            		    if(null != prop) {
            		        String adrs = StringUtils.trimToEmpty(((Mailer.UserProperty)prop).getAddress());
            		        variables.put(BUILD_USER_EMAIL, adrs);
            		    }
            		}
			
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Class<UserIdCause> getUsedCauseClass() {
		return causeClass;
	}

}
