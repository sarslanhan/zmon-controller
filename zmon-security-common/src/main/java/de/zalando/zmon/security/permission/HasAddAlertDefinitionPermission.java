package de.zalando.zmon.security.permission;

import de.zalando.zmon.domain.AlertDefinition;
import de.zalando.zmon.security.authority.ZMonAuthority;

class HasAddAlertDefinitionPermission extends AlertDefinitionPermission {

	HasAddAlertDefinitionPermission(AlertDefinition alertDefinition) {
		super(alertDefinition);
	}

	@Override
	public Boolean apply(ZMonAuthority input) {
		return input.hasAddAlertDefinitionPermission(alertDefinition);
	}

}
