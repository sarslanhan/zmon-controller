package org.zalando.zmon.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.zmon.domain.AlertDefinition;
import org.zalando.zmon.domain.AlertDefinitionIsEqual;
import org.zalando.zmon.domain.AlertDefinitionsDiff;
import org.zalando.zmon.domain.CheckDefinition;
import org.zalando.zmon.domain.CheckDefinitionImport;
import org.zalando.zmon.domain.CheckDefinitionIsEqual;
import org.zalando.zmon.domain.CheckDefinitions;
import org.zalando.zmon.domain.CheckDefinitionsDiff;
import org.zalando.zmon.domain.DefinitionStatus;
import org.zalando.zmon.generator.AlertDefinitionGenerator;
import org.zalando.zmon.generator.CheckDefinitionImportGenerator;
import org.zalando.zmon.generator.DataGenerator;
import org.zalando.zmon.persistence.CheckDefinitionImportResult;
import org.zalando.zmon.service.AlertService;
import org.zalando.zmon.service.ZMonService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@DirtiesContext
public class ZMonServiceImplIT {

    @Autowired
    private ZMonService service;

    @Autowired
    private AlertService alertService;

    private DataGenerator<CheckDefinitionImport> checkImportGenerator;
    private DataGenerator<AlertDefinition> alertGenerator;

    private static final String USER_NAME ="default_user";
    private static final List<String> USER_TEAMS = Arrays.asList("Platform/Software","Platform/Monitoring");

    @Before
    public void setup() {
        checkImportGenerator = new CheckDefinitionImportGenerator();
        alertGenerator = new AlertDefinitionGenerator();
    }

    @Test
    public void testCreateCheckDefinition() throws Exception {
        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(
                checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();

        final CheckDefinitions checkDefinitions = service.getCheckDefinitions(null);

        MatcherAssert.assertThat(checkDefinitions, IsNull.notNullValue());
        MatcherAssert.assertThat(checkDefinitions.getSnapshotId(), Matchers.greaterThan(0L));
        MatcherAssert.assertThat(checkDefinitions.getCheckDefinitions(),
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testCreateCheckDefinitionWithWrongTeam() throws Exception {
        CheckDefinitionImport newCheck = checkImportGenerator.generate();
        CheckDefinitionImportResult result;

        result = service.createOrUpdateCheckDefinition(newCheck, USER_NAME, Arrays.asList("Platform/Database"));

        MatcherAssert.assertThat("Permission is not ok (do not create for different team)", result.isPermissionDenied());

        result = service.createOrUpdateCheckDefinition(newCheck, USER_NAME, Arrays.asList("Platform/Software"));

        MatcherAssert.assertThat("Permission is ok (same team)", !result.isPermissionDenied());

        MatcherAssert.assertThat("Created new entity", result.isNewEntity());

        newCheck.setId(result.getEntity().getId());

        CheckDefinitionImportResult failedResult = service.createOrUpdateCheckDefinition(newCheck, USER_NAME + "_X", Arrays.asList("Platform/System"));

        MatcherAssert.assertThat("Permission is denied (No team match, No user match)", failedResult.isPermissionDenied());

        CheckDefinitionImportResult okResult = service.createOrUpdateCheckDefinition(newCheck, USER_NAME + "2", USER_TEAMS);

        MatcherAssert.assertThat("Permission is not denied for same team", !okResult.isPermissionDenied());
    }

    @Test
    public void testUpdateCheckDefinitionWithExistingSourceURL() throws Exception {
        final CheckDefinitionImport toImport = checkImportGenerator.generate();
        service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        toImport.setName(toImport.getName() + " UPDATE");

        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        final CheckDefinitions checkDefinitions = service.getCheckDefinitions(null);

        MatcherAssert.assertThat(checkDefinitions, IsNull.notNullValue());
        MatcherAssert.assertThat(checkDefinitions.getSnapshotId(), Matchers.greaterThan(0L));
        MatcherAssert.assertThat(checkDefinitions.getCheckDefinitions(),
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testUpdateCheckDefinitionWithExistingNameAndTeam() throws Exception {
        final CheckDefinitionImport toImport = checkImportGenerator.generate();
        service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS);

        toImport.setSourceUrl(toImport.getSourceUrl() + "?update=1");

        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        final CheckDefinitions checkDefinitions = service.getCheckDefinitions(null);

        MatcherAssert.assertThat(checkDefinitions, IsNull.notNullValue());
        MatcherAssert.assertThat(checkDefinitions.getSnapshotId(), Matchers.greaterThan(0L));
        MatcherAssert.assertThat(checkDefinitions.getCheckDefinitions(),
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testGetActiveCheckDefinitions() throws Exception {

        // create an inactive check
        CheckDefinitionImport toImport = checkImportGenerator.generate();
        toImport.setStatus(DefinitionStatus.INACTIVE);
        service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        // create an active check
        toImport = checkImportGenerator.generate();

        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        final CheckDefinitions checkDefinitions = service.getCheckDefinitions(DefinitionStatus.ACTIVE);

        MatcherAssert.assertThat(checkDefinitions, IsNull.notNullValue());
        MatcherAssert.assertThat(checkDefinitions.getSnapshotId(), Matchers.greaterThan(0L));
        MatcherAssert.assertThat(checkDefinitions.getCheckDefinitions(),
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testGetCheckDefinitionsById() throws Exception {
        final CheckDefinitionImport toImport = checkImportGenerator.generate();
        final CheckDefinition newCheckDefinition0 = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        toImport.setName(toImport.getName() + " UPDATE");
        toImport.setSourceUrl(toImport.getSourceUrl() + "?update=1");

        final CheckDefinition newCheckDefinition1 = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        final List<CheckDefinition> checkDefinitions = service.getCheckDefinitions(null,
                Collections.singletonList(newCheckDefinition0.getId()));

        MatcherAssert.assertThat(newCheckDefinition0.getId(), Matchers.not(newCheckDefinition1.getId()));
        MatcherAssert.assertThat(checkDefinitions,
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition0)));
    }

    @Test
    public void testGetCheckDefinitionsByMultipleIds() throws Exception {
        final List<CheckDefinition> newChecks = new ArrayList<>(2);

        final CheckDefinitionImport toImport = checkImportGenerator.generate();
        newChecks.add(service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity());

        toImport.setName(toImport.getName() + " UPDATE");
        toImport.setSourceUrl(toImport.getSourceUrl() + "?update=1");

        newChecks.add(service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity());

        final List<CheckDefinition> checkDefinitions = service.getCheckDefinitions(null,
                Lists.newArrayList(newChecks.get(0).getId(), newChecks.get(1).getId()));

        MatcherAssert.assertThat(checkDefinitions,
            Matchers.containsInAnyOrder(CheckDefinitionIsEqual.equalTo(newChecks)));
    }

    @Test
    public void testGetCheckDefinitionsByOwningTeam() throws Exception {
        final CheckDefinitionImport toImport = checkImportGenerator.generate();
        final CheckDefinition newCheckDefinition0 = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        toImport.setOwningTeam("TEST");
        toImport.setName(toImport.getName() + " UPDATE");
        toImport.setSourceUrl(toImport.getSourceUrl() + "?update=1");
        service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        final List<CheckDefinition> checkDefinitions = service.getCheckDefinitions(null,
                Sets.newHashSet(newCheckDefinition0.getOwningTeam()));

        MatcherAssert.assertThat(checkDefinitions,
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition0)));
    }

    @Test
    public void testAllCheckDefinitionsDiff() throws Exception {
        final CheckDefinitionImport toImport = checkImportGenerator.generate();
        final CheckDefinition newCheckDefinition0 = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        toImport.setOwningTeam("Platform/Monitoring");
        toImport.setName(toImport.getName() + " UPDATE");
        toImport.setSourceUrl(toImport.getSourceUrl() + "?update=1");
        toImport.setStatus(DefinitionStatus.INACTIVE);

        final CheckDefinition newCheckDefinition1 = service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        final CheckDefinitionsDiff diff = service.getCheckDefinitionsDiff(null);

        MatcherAssert.assertThat(diff, IsNull.notNullValue());
        MatcherAssert.assertThat(diff.getSnapshotId(), Matchers.greaterThan(0L));
        MatcherAssert.assertThat(diff.getDisabledDefinitions(), Matchers.contains(newCheckDefinition1.getId()));
        MatcherAssert.assertThat(diff.getChangedDefinitions(),
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition0)));
    }

    @Test
    public void testEmptyCheckDefinitionsDiff() throws Exception {
        service.createOrUpdateCheckDefinition(checkImportGenerator.generate(), USER_NAME, USER_TEAMS);

        final CheckDefinitions allCheckDefinitions = service.getCheckDefinitions(null);
        final CheckDefinitionsDiff diff = service.getCheckDefinitionsDiff(allCheckDefinitions.getSnapshotId());

        MatcherAssert.assertThat(diff, IsNull.notNullValue());
        MatcherAssert.assertThat(diff.getSnapshotId(), Matchers.equalTo(allCheckDefinitions.getSnapshotId()));
        MatcherAssert.assertThat(diff.getDisabledDefinitions(), IsNull.nullValue());
        MatcherAssert.assertThat(diff.getChangedDefinitions(), IsNull.nullValue());
    }

    @Test
    public void testDeleteNonDetachedCheckDefinitions() throws Exception {
        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(
                checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();

        service.deleteDetachedCheckDefinitions();

        final CheckDefinitions diff = service.getCheckDefinitions(null);

        MatcherAssert.assertThat(diff, IsNull.notNullValue());
        MatcherAssert.assertThat(diff.getSnapshotId(), Matchers.greaterThan(0L));
        MatcherAssert.assertThat(diff.getCheckDefinitions(),
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testDeleteDetachedCheckDefinitions() throws Exception {
        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(
                checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();

        // delete the check definition
        service.deleteCheckDefinition(newCheckDefinition.getLastModifiedBy(), newCheckDefinition.getName(),
            newCheckDefinition.getOwningTeam());
        service.deleteDetachedCheckDefinitions();

        final CheckDefinitions allCheckDefinitionsAfter = service.getCheckDefinitions(null);

        MatcherAssert.assertThat(allCheckDefinitionsAfter.getCheckDefinitions(),
            Matchers.not(Matchers.hasItem(CheckDefinitionIsEqual.equalTo(newCheckDefinition))));
    }

    @Ignore
    @Test
    public void testDeleteExistingCheckDefinitionWithAlerts() throws Exception {

        // create a new check
        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(
                checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();
        newCheckDefinition.setStatus(DefinitionStatus.DELETED);

        // create a new alert
        AlertDefinition newAlertDefinition = alertGenerator.generate();
        newAlertDefinition.setCheckDefinitionId(newCheckDefinition.getId());
        newAlertDefinition = alertService.createOrUpdateAlertDefinition(newAlertDefinition);
        newAlertDefinition.setStatus(DefinitionStatus.DELETED);

        // delete the check definition
        service.deleteCheckDefinition(checkImportGenerator.generate().getLastModifiedBy(), newCheckDefinition.getName(),
            newCheckDefinition.getOwningTeam());

        // test if the alert definition is available and with status DELETED
        final List<AlertDefinition> alertDefinitions = alertService.getAlertDefinitions(null,
                Collections.singletonList(newAlertDefinition.getId()));

        MatcherAssert.assertThat(alertDefinitions,
            Matchers.contains(AlertDefinitionIsEqual.equalTo(newAlertDefinition)));

        // test if the check definition is available
        final List<CheckDefinition> checkDefinitions = service.getCheckDefinitions(null,
                Collections.singletonList(newCheckDefinition.getId()));

        // since we have alerts using this check, the check shouldn't be deleted but set status as DELETED
        MatcherAssert.assertThat(checkDefinitions,
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testDeleteExistingCheckDefinitionWithAlertsDiff() throws Exception {

        // create a new check
        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(
                checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();
        newCheckDefinition.setStatus(DefinitionStatus.DELETED);

        // create a new alert
        AlertDefinition newAlertDefinition = alertGenerator.generate();
        newAlertDefinition.setCheckDefinitionId(newCheckDefinition.getId());
        newAlertDefinition = alertService.createOrUpdateAlertDefinition(newAlertDefinition);
        newAlertDefinition.setStatus(DefinitionStatus.DELETED);

        final CheckDefinitions checks = service.getCheckDefinitions(null);
        final AlertDefinitionsDiff alerts = alertService.getAlertDefinitionsDiff(null);

        // delete the check definition
        service.deleteCheckDefinition(newCheckDefinition.getLastModifiedBy(), newCheckDefinition.getName(),
            newCheckDefinition.getOwningTeam());

        // test if the check was disabled
        final CheckDefinitionsDiff checkDiff = service.getCheckDefinitionsDiff(checks.getSnapshotId());
        MatcherAssert.assertThat(checkDiff, IsNull.notNullValue());
        MatcherAssert.assertThat(checkDiff.getSnapshotId(), Matchers.greaterThan(checks.getSnapshotId()));
        MatcherAssert.assertThat(checkDiff.getDisabledDefinitions(), Matchers.contains(newCheckDefinition.getId()));
        MatcherAssert.assertThat(checkDiff.getChangedDefinitions(), IsNull.nullValue());

        // test if the alert was disabled
        final AlertDefinitionsDiff alertDiff = alertService.getAlertDefinitionsDiff(alerts.getSnapshotId());
        MatcherAssert.assertThat(alertDiff, IsNull.notNullValue());
        MatcherAssert.assertThat(alertDiff.getSnapshotId(), Matchers.greaterThan(alerts.getSnapshotId()));
        MatcherAssert.assertThat(alertDiff.getDisabledDefinitions(), Matchers.contains(newAlertDefinition.getId()));
        MatcherAssert.assertThat(alertDiff.getChangedDefinitions(), IsNull.nullValue());
    }

    @Test
    public void testDeleteExistingCheckDefinitionWithoutAlerts() {

        // create a new check
        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(
                checkImportGenerator.generate(), USER_NAME, USER_TEAMS).getEntity();
        newCheckDefinition.setStatus(DefinitionStatus.DELETED);

        // delete the check definition
        service.deleteCheckDefinition(newCheckDefinition.getLastModifiedBy(), newCheckDefinition.getName(),
            newCheckDefinition.getOwningTeam());

        // check if the alert definition is there
        final List<CheckDefinition> checkDefinitions = service.getCheckDefinitions(null,
                Collections.singletonList(newCheckDefinition.getId()));

        MatcherAssert.assertThat(checkDefinitions,
            Matchers.contains(CheckDefinitionIsEqual.equalTo(newCheckDefinition)));
    }

    @Test
    public void testDeleteNonExistingCheckDefinition() {

        // delete non existing check definition
        service.deleteCheckDefinition("pribeiro", "foo", "bar");
    }

    @Test
    public void testGetAllTeams() throws Exception {

        // create a new check
        CheckDefinitionImport toImport = checkImportGenerator.generate();
        toImport.setOwningTeam("Platform/Software");
        service.createOrUpdateCheckDefinition(toImport, USER_NAME, USER_TEAMS).getEntity();

        toImport = checkImportGenerator.generate();
        toImport.setName(toImport.getName() + " UPDATE");
        toImport.setSourceUrl(toImport.getSourceUrl() + "?update=1");
        toImport.setOwningTeam("Platform/RQM");

        final CheckDefinition newCheckDefinition = service.createOrUpdateCheckDefinition(toImport, USER_NAME, Arrays.asList("Platform/RQM")).getEntity();

        // create a new alert
        final AlertDefinition newAlertDefinition = alertGenerator.generate();
        newAlertDefinition.setTeam("Platform/Database");
        newAlertDefinition.setResponsibleTeam("Platform/System");
        newAlertDefinition.setCheckDefinitionId(newCheckDefinition.getId());

        alertService.createOrUpdateAlertDefinition(newAlertDefinition);

        final List<String> teams = service.getAllTeams();
        MatcherAssert.assertThat(teams,
            Matchers.containsInAnyOrder("Platform/Software", "Platform/RQM", "Platform/Database", "Platform/System"));
    }
}
